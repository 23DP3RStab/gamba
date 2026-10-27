<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import PiggiesScene from '../components/PiggiesScene.vue'
import { HOUSES } from '../piggies'
import { formatDateTime, formatMoney } from '../format'
import { activePiggies, blowPiggies, cashOutPiggies, startPiggies } from '../games'
import { useLockout } from '../lockout'
import { useAuthStore } from '../stores/auth'

// Played on the real balance: the server takes the bet, rolls every blow and pays out;
// the page animates the wolf and then shows what the server decided
const authStore = useAuthStore()
const balance = computed(() => Number(authStore.user?.balance ?? 0))
const lockedUntil = useLockout()

const MIN_BET = 0.1
const WALK_MS = 700
const PUFF_MS = 1600
const FALL_MS = 1100

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
const cents = (value) => Math.round(value * 100) / 100

// 'betting' → 'blowing' → 'decide' → 'blowing' → … → 'won' | 'lost' | 'cashed'
const phase = ref('betting')
const bet = ref(10)
const stake = ref(0) // amount bet on the current round
const level = ref(0) // houses blown down this round
const round = ref(0) // bumps each round so the houses are rebuilt
const wolfAt = ref(0) // index of the house the wolf stands at
const puffing = ref(false)
const walking = ref(false)
const houseStates = ref(HOUSES.map(() => 'standing')) // 'standing' | 'fallen' | 'held'
const banner = ref(null) // latest moment for the scene banner
let bannerId = 0
const message = ref('')
const cashing = ref(false) // a cash out is on its way to the server

const playing = computed(() => phase.value === 'blowing' || phase.value === 'decide')
const deciding = computed(() => phase.value === 'decide')
const betAmount = computed(() => {
  const n = Number(bet.value)
  return Number.isFinite(n) && n > 0 ? cents(n) : 0
})
const current = computed(() =>
  level.value ? stake.value * HOUSES[level.value - 1].multiplier : stake.value,
)
const target = computed(() => HOUSES[wolfAt.value])
const nextHouse = computed(() => HOUSES[level.value])
// The ladder shows what the bet being typed would pay until a round starts
const ladderStake = computed(() => (phase.value === 'betting' ? betAmount.value : stake.value))

function resetScene() {
  phase.value = 'betting'
  level.value = 0
  houseStates.value = HOUSES.map(() => 'standing')
  round.value++
}

// Changing the bet after a round has ended clears the old scene
function editBet(value) {
  if (playing.value) return
  if (phase.value !== 'betting') resetScene()
  bet.value = value
  message.value = ''
}

const halveBet = () => editBet(cents(betAmount.value / 2))
const doubleBet = () => editBet(cents(betAmount.value * 2))

async function start() {
  if (playing.value || lockedUntil.value) return
  const amount = betAmount.value
  if (amount <= 0) {
    message.value = 'Enter a bet first'
    return
  }
  if (amount < MIN_BET) {
    message.value = `The smallest bet is ${formatMoney(MIN_BET)}`
    return
  }
  if (amount > balance.value) {
    message.value = `Not enough balance for a ${formatMoney(amount)} bet`
    return
  }
  if (phase.value !== 'betting') resetScene()
  message.value = ''
  phase.value = 'blowing'

  let result
  try {
    result = await startPiggies(amount)
  } catch (e) {
    message.value = e.message
    phase.value = 'betting'
    return
  }
  stake.value = amount
  authStore.updateBalance(result.balance) // the stake is taken; the straw house can't win yet
  await play(0, result)
}

async function next() {
  if (phase.value !== 'decide' || cashing.value) return
  const index = level.value
  phase.value = 'blowing'
  let result
  try {
    result = await blowPiggies()
  } catch (e) {
    message.value = e.message
    phase.value = 'decide'
    return
  }
  await play(index, result)
}

// Walks the wolf to house `index`, huffs and puffs, then shows the server's `result`.
// The balance only changes after the animation, so a win isn't given away early.
async function play(index, result) {
  phase.value = 'blowing'
  if (wolfAt.value !== index) {
    wolfAt.value = index
    walking.value = true
    await sleep(WALK_MS)
    walking.value = false
  }
  puffing.value = true
  await sleep(PUFF_MS)
  puffing.value = false

  const { fell } = result
  houseStates.value[index] = fell ? 'fallen' : 'held'
  const last = index === HOUSES.length - 1
  showBanner(!fell ? 'held' : last ? 'won' : 'fell', {
    multiplier: HOUSES[index].multiplier,
    amount: stake.value * HOUSES[index].multiplier,
  })
  await sleep(FALL_MS)
  authStore.updateBalance(result.balance)

  if (!fell) {
    phase.value = 'lost'
    return
  }
  level.value = index + 1
  if (result.status === 'WON') {
    phase.value = 'won'
  } else {
    phase.value = 'decide'
    // Leaving the page mid-round banks what was already won
    if (gone) cashOut()
  }
}

async function cashOut() {
  if (phase.value !== 'decide' || cashing.value) return
  cashing.value = true
  try {
    const result = await cashOutPiggies()
    authStore.updateBalance(result.balance)
    showBanner('cashed', { amount: result.payout })
    phase.value = 'cashed'
  } catch (e) {
    message.value = e.message
  } finally {
    cashing.value = false
  }
}

function showBanner(kind, details) {
  banner.value = { id: ++bannerId, kind, ...details }
}

// A round left unfinished (the tab was closed mid-round) carries on where it stopped
onMounted(async () => {
  try {
    const game = await activePiggies()
    if (!game || phase.value !== 'betting') return
    bet.value = game.betAmount
    stake.value = game.betAmount
    level.value = game.housesDown
    wolfAt.value = game.housesDown - 1
    houseStates.value = HOUSES.map((_, i) => (i < game.housesDown ? 'fallen' : 'standing'))
    phase.value = 'decide'
  } catch {
    // Nothing to carry on with
  }
})

// Animated number that counts up to `source` (jumps straight down, or when motion is reduced)
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
function countUp(source) {
  const shown = ref(source())
  let frame = null
  watch(source, (to) => {
    cancelAnimationFrame(frame)
    const from = shown.value
    if (reducedMotion || to <= from) {
      shown.value = to
      return
    }
    const startedAt = performance.now()
    const tick = (now) => {
      const t = Math.min((now - startedAt) / 600, 1)
      shown.value = cents(from + (to - from) * (1 - (1 - t) ** 3))
      if (t < 1) frame = requestAnimationFrame(tick)
    }
    frame = requestAnimationFrame(tick)
  })
  onBeforeUnmount(() => cancelAnimationFrame(frame))
  return shown
}

const inPlayShown = countUp(() => (playing.value ? current.value : 0))
const winShown = countUp(() =>
  ['decide', 'won', 'cashed'].includes(phase.value) ? current.value : 0,
)

let gone = false
onBeforeUnmount(() => {
  gone = true
  cashOut()
})

function rowClass(i) {
  return {
    done: houseStates.value[i] === 'fallen',
    failed: houseStates.value[i] === 'held',
    next: playing.value && i === level.value,
  }
}
</script>

<template>
  <section class="piggies-page">
    <RouterLink to="/lobby" class="back-btn">← Lobby</RouterLink>

    <header class="page-head">
      <h1 class="title">The Three Piggies</h1>
      <p class="subtitle">and the Big Bad Wolf</p>
    </header>

    <p v-if="lockedUntil" class="lockout" role="alert">
      Your account is locked until {{ formatDateTime(lockedUntil) }}. You can't play until then.
    </p>

    <PiggiesScene
      :house-states="houseStates"
      :round="round"
      :wolf-at="wolfAt"
      :puffing="puffing"
      :walking="walking"
      :level="level"
      :phase="phase"
      :banner="banner"
    />

    <div class="lower">
      <div class="main-col">
        <div class="card result" aria-live="polite">
          <template v-if="phase === 'blowing'">
            <p class="huff">Huff… and puff…</p>
            <p class="muted center">The wolf is at the {{ target.label.toLowerCase() }}</p>
          </template>
          <template v-else-if="phase === 'decide'">
            <p class="success big">
              The {{ HOUSES[level - 1].label.toLowerCase() }} fell! You have
              {{ formatMoney(winShown) }}
            </p>
            <p class="muted">
              Cash out now, or send the wolf to the {{ nextHouse.label.toLowerCase() }}
              ({{ Math.round(nextHouse.chance * 100) }} % chance) for
              {{ formatMoney(stake * nextHouse.multiplier) }}.
            </p>
          </template>
          <p v-else-if="phase === 'won'" class="success big">
            All three houses blown down! You won {{ formatMoney(winShown) }}
          </p>
          <p v-else-if="phase === 'cashed'" class="success big">
            You cashed out {{ formatMoney(winShown) }}
          </p>
          <p v-else-if="phase === 'lost'" class="lost big">
            The {{ target.label.toLowerCase() }} held! You lost {{ formatMoney(stake) }}
          </p>
          <p v-else class="muted">
            Place a bet and let the wolf huff and puff. Each house he blows down multiplies your
            bet: ×2, ×5, then ×16,67. If one stands, you lose it. Cash out whenever you like.
          </p>
        </div>

        <div class="card controls" :class="{ deciding }">
          <template v-if="phase === 'decide'">
            <div class="decide">
              <button
                type="button"
                class="btn btn-gold action cash-out"
                :disabled="cashing"
                @click="cashOut"
              >
                Cash out {{ formatMoney(current) }}
              </button>
              <button
                type="button"
                class="btn btn-outline-gold action"
                :disabled="cashing"
                @click="next"
              >
                Blow the {{ nextHouse.key }} house → {{ formatMoney(stake * nextHouse.multiplier) }}
              </button>
            </div>
          </template>

          <template v-else>
            <div class="bet-row">
              <label class="bet-field">
                <span class="stat-label">Bet (€)</span>
                <input
                  type="number"
                  min="0.10"
                  step="0.01"
                  inputmode="decimal"
                  :value="bet"
                  :disabled="playing"
                  @input="editBet($event.target.value)"
                  @keydown.enter="start"
                />
              </label>
              <div class="bet-tools">
                <button type="button" class="btn btn-outline" :disabled="playing" @click="halveBet">
                  ½
                </button>
                <button type="button" class="btn btn-outline" :disabled="playing" @click="doubleBet">
                  2×
                </button>
                <button
                  type="button"
                  class="btn btn-outline"
                  :disabled="playing"
                  @click="editBet(0)"
                >
                  Clear
                </button>
              </div>
            </div>

            <button
              type="button"
              class="btn btn-gold action start"
              :disabled="playing || !!lockedUntil"
              @click="start"
            >
              {{ playing ? 'Huffing and puffing…' : 'Huff and puff!' }}
            </button>
          </template>

          <p v-if="message" class="error message" role="alert">{{ message }}</p>
        </div>
      </div>

      <aside class="side">
        <div class="card stats">
          <div>
            <span class="stat-label">Balance</span>
            <span class="stat-value">{{ formatMoney(balance) }}</span>
          </div>
          <div>
            <span class="stat-label">{{ playing ? 'In play' : 'Bet' }}</span>
            <span class="stat-value">{{ formatMoney(playing ? inPlayShown : betAmount) }}</span>
          </div>
        </div>

        <div class="card ladder">
          <span class="stat-label">Payouts</span>
          <ol>
            <li v-for="(house, i) in HOUSES" :key="house.key" :class="rowClass(i)">
              <span class="ladder-name">{{ house.label }}</span>
              <span class="ladder-chance">{{ Math.round(house.chance * 100) }} %</span>
              <span class="ladder-amount">{{ formatMoney(ladderStake * house.multiplier) }}</span>
            </li>
          </ol>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.piggies-page {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
  padding-bottom: 2rem;
}

.back-btn {
  align-self: flex-start;
  margin: 16px 0 0 36px;
  display: inline-block;
  background: transparent;
  border: 1px solid rgba(212, 175, 55, 0.3);
  color: #d4af37;
  padding: 6px 14px;
  border-radius: 6px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(212, 175, 55, 0.15);
}

.page-head {
  text-align: center;
}

.lockout {
  align-self: center;
  max-width: 980px;
  margin: 0;
  padding: 0.75rem 1.25rem;
  border: 1px solid #ff6b6b;
  border-radius: 8px;
  background: rgba(255, 107, 107, 0.1);
  color: #ff6b6b;
  text-align: center;
}

.title {
  font-family: var(--font-serif);
  font-size: 2.4rem;
  font-weight: bold;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--gold);
  text-shadow: 0 0 18px rgba(240, 165, 0, 0.35);
}

.subtitle {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.25em;
  opacity: 0.7;
}

/* Status, controls and side panel */
.lower {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 320px);
  gap: 1.5rem;
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
}

.main-col,
.side {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card {
  padding: 1rem 1.15rem;
  border: 1px solid rgba(240, 165, 0, 0.35);
  border-radius: 10px;
  background: linear-gradient(180deg, #121212, #070707);
}

.result {
  min-height: 6rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0.35rem;
}

.huff {
  font-family: var(--font-serif);
  font-size: 1.3rem;
  text-align: center;
  color: var(--gold);
  animation: pulse 0.8s ease-in-out infinite;
}

@keyframes pulse {
  50% {
    opacity: 0.4;
  }
}

.big {
  font-family: var(--font-serif);
  font-size: 1.2rem;
  font-weight: bold;
}

.lost {
  color: #ff6b6b;
}

.muted {
  opacity: 0.75;
}

.center {
  text-align: center;
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transition: border-color 0.3s;
}

/* Cash out or carry on: make the choice stand out */
.controls.deciding {
  border-color: var(--gold);
  animation: glow 1.6s ease-in-out infinite;
}

@keyframes glow {
  50% {
    box-shadow: 0 0 22px rgba(240, 165, 0, 0.45);
  }
}

.cash-out {
  animation: pulse-btn 1.2s ease-in-out infinite;
}

@keyframes pulse-btn {
  50% {
    transform: scale(1.03);
    box-shadow: 0 0 16px rgba(240, 165, 0, 0.7);
  }
}

.bet-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0.75rem;
}

.bet-field {
  display: flex;
  flex: 1 1 10rem;
  flex-direction: column;
  gap: 0.3rem;
}

.bet-tools {
  display: flex;
  gap: 0.5rem;
}

.bet-tools .btn {
  padding-inline: 0.9rem;
}

.controls .btn:disabled {
  opacity: 0.45;
  cursor: default;
}

.action {
  font-size: 1.05rem;
  font-weight: bold;
}

.start {
  box-shadow: 0 0 18px rgba(240, 165, 0, 0.45);
}

.decide {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.message {
  font-size: 0.9rem;
}

.stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.stats > div {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.stat-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  opacity: 0.75;
}

.stat-value {
  font-family: var(--font-serif);
  font-size: 1.25rem;
  color: var(--gold);
  white-space: nowrap;
}

.ladder ol {
  margin: 0.5rem 0 0;
  padding: 0;
  list-style: none;
  font-size: 0.85rem;
}

.ladder li {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 0.75rem;
  padding: 0.4rem 0.5rem;
  border-bottom: 1px solid var(--color-border);
  border-radius: 4px;
}

.ladder-chance {
  opacity: 0.6;
}

.ladder-amount {
  min-width: 6.5rem;
  text-align: right;
  font-family: var(--font-serif);
  color: var(--gold);
}

.ladder .next {
  background: rgba(240, 165, 0, 0.12);
}

.ladder .done .ladder-name {
  color: #5fd38d;
}

.ladder .done .ladder-name::before {
  content: '✓ ';
}

.ladder .failed .ladder-name {
  color: #ff6b6b;
}

.ladder .failed .ladder-name::before {
  content: '✗ ';
}

@media (max-width: 860px) {
  .lower {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 560px) {
  .title {
    font-size: 1.8rem;
  }

  .decide {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .controls.deciding,
  .cash-out,
  .huff {
    animation: none;
  }
}
</style>
