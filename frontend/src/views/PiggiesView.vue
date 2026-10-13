<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import PiggyHouse from '../components/PiggyHouse.vue'
import BigBadWolf from '../components/BigBadWolf.vue'
import CasinoChip from '../components/CasinoChip.vue'
import { HOUSES, blowSucceeds } from '../piggies'
import { balance, setBalance, resetBalance as resetStoredBalance, START_BALANCE } from '../balance'
import { formatMoney } from '../format'

const CHIPS = [1, 5, 10, 25, 100, 500]
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
const houseStates = ref(HOUSES.map(() => 'standing')) // 'standing' | 'fallen' | 'held'
const message = ref('')

const playing = computed(() => phase.value === 'blowing' || phase.value === 'decide')
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

const addChip = (c) => editBet(cents(betAmount.value + c))
const halveBet = () => editBet(cents(betAmount.value / 2))
const doubleBet = () => editBet(cents(betAmount.value * 2))

async function start() {
  if (playing.value) return
  const amount = betAmount.value
  if (amount <= 0) {
    message.value = 'Enter a bet first'
    return
  }
  if (amount > balance.value) {
    message.value = `Not enough balance for a ${formatMoney(amount)} bet`
    return
  }
  if (phase.value !== 'betting') resetScene()
  message.value = ''
  setBalance(cents(balance.value - amount))
  stake.value = amount
  await attack(0)
}

async function attack(index) {
  phase.value = 'blowing'
  if (wolfAt.value !== index) {
    wolfAt.value = index
    await sleep(WALK_MS)
  }
  puffing.value = true
  await sleep(PUFF_MS)
  puffing.value = false

  const fell = blowSucceeds(index)
  houseStates.value[index] = fell ? 'fallen' : 'held'
  await sleep(FALL_MS)

  if (!fell) {
    phase.value = 'lost'
    return
  }
  level.value = index + 1
  if (level.value === HOUSES.length) {
    payOut('won')
  } else {
    phase.value = 'decide'
    // Leaving the page mid-round banks what was already won
    if (gone) cashOut()
  }
}

function payOut(endPhase) {
  setBalance(cents(balance.value + current.value))
  phase.value = endPhase
}

function cashOut() {
  if (phase.value === 'decide') payOut('cashed')
}

function next() {
  if (phase.value === 'decide') attack(level.value)
}

function resetBalance() {
  resetStoredBalance()
  message.value = `Balance reset to ${formatMoney(START_BALANCE)}`
}

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
    <header class="page-head">
      <h1 class="title">The Three Piggies</h1>
      <p class="subtitle">and the Big Bad Wolf</p>
    </header>

    <div class="board">
      <div class="scene" :style="{ '--target': wolfAt }">
        <div class="sun" aria-hidden="true"></div>
        <div class="cloud c1" aria-hidden="true"></div>
        <div class="cloud c2" aria-hidden="true"></div>

        <div class="houses">
          <div v-for="(house, i) in HOUSES" :key="house.key" class="slot">
            <PiggyHouse
              :key="`${house.key}-${round}`"
              :kind="house.key"
              :state="houseStates[i]"
              :shaking="puffing && wolfAt === i"
            />
          </div>
        </div>

        <div class="wolf-wrap">
          <BigBadWolf :puffing="puffing" :beaten="phase === 'lost'" />
        </div>

        <div class="ground"></div>
      </div>

      <ol class="house-labels">
        <li v-for="(house, i) in HOUSES" :key="house.key" :class="rowClass(i)">
          <span class="label-name">{{ house.label }}</span>
          <span class="label-mult">×{{ house.multiplier.toLocaleString('lv-LV') }}</span>
        </li>
      </ol>
    </div>

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
              {{ formatMoney(current) }}
            </p>
            <p class="muted">
              Cash out now, or send the wolf to the {{ nextHouse.label.toLowerCase() }}
              ({{ Math.round(nextHouse.chance * 100) }} % chance) for
              {{ formatMoney(stake * nextHouse.multiplier) }}.
            </p>
          </template>
          <p v-else-if="phase === 'won'" class="success big">
            All three houses blown down! You won {{ formatMoney(current) }}
          </p>
          <p v-else-if="phase === 'cashed'" class="success big">
            You cashed out {{ formatMoney(current) }}
          </p>
          <p v-else-if="phase === 'lost'" class="lost big">
            The {{ target.label.toLowerCase() }} held! You lost {{ formatMoney(stake) }}
          </p>
          <p v-else class="muted">
            Place a bet and let the wolf huff and puff. Every house he blows down doubles your
            money; if one stands, you lose it all. Cash out whenever you like.
          </p>
        </div>

        <div class="card controls">
          <template v-if="phase === 'decide'">
            <div class="decide">
              <button type="button" class="btn btn-gold action" @click="cashOut">
                Cash out {{ formatMoney(current) }}
              </button>
              <button type="button" class="btn btn-outline-gold action" @click="next">
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
                  min="0.01"
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

            <div class="chips" aria-label="Add to bet">
              <button
                v-for="c in CHIPS"
                :key="c"
                type="button"
                class="chip-btn"
                :aria-label="`Add ${c} €`"
                :disabled="playing"
                @click="addChip(c)"
              >
                <CasinoChip :value="c" size="2.8rem" />
              </button>
            </div>

            <button type="button" class="btn btn-gold action start" :disabled="playing" @click="start">
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
            <span class="stat-value">{{ formatMoney(playing ? current : betAmount) }}</span>
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

        <button
          v-if="balance < CHIPS[0] && !playing"
          type="button"
          class="btn btn-outline-gold"
          @click="resetBalance"
        >
          Reset balance
        </button>
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

.page-head {
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

/* Scene: the wolf's lane on the left, then one column per house */
.board {
  --lane: 16%;
  --wolf-w: 15%;
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
}

.scene {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(240, 165, 0, 0.35);
  border-radius: 12px 12px 0 0;
  background: linear-gradient(180deg, #0d1b33 0%, #24406b 55%, #3e5f86 100%);
}

.sun {
  position: absolute;
  top: 9%;
  right: 6%;
  width: 7%;
  aspect-ratio: 1;
  border-radius: 50%;
  background: #f6e7b0;
  box-shadow: 0 0 30px rgba(246, 231, 176, 0.55);
}

.cloud {
  position: absolute;
  height: 7%;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
}

.c1 {
  top: 12%;
  left: 22%;
  width: 16%;
}

.c2 {
  top: 22%;
  left: 52%;
  width: 11%;
}

.houses {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: end;
  margin-left: var(--lane);
  padding-top: 4.5rem;
}

.slot {
  padding: 0 14%;
}

.wolf-wrap {
  position: absolute;
  z-index: 2;
  bottom: 1.4rem;
  left: calc(var(--lane) + (100% - var(--lane)) * var(--target) / 3 - var(--wolf-w) + 3%);
  width: var(--wolf-w);
  transition: left 0.7s ease-in-out;
}

.ground {
  height: 1.6rem;
  margin-top: -0.2rem;
  background: linear-gradient(180deg, #2f6b34, #1c4721);
  border-top: 2px solid #3f8a45;
}

.house-labels {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0;
  padding: 0.55rem 0 0.55rem var(--lane);
  list-style: none;
  border: 1px solid rgba(240, 165, 0, 0.35);
  border-top: none;
  border-radius: 0 0 12px 12px;
  background: linear-gradient(180deg, #121212, #070707);
}

.house-labels li {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 0.8rem;
  opacity: 0.75;
  transition: color 0.3s, opacity 0.3s;
}

.label-name {
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.label-mult {
  font-family: var(--font-serif);
  font-size: 1.1rem;
  color: var(--gold);
}

.house-labels .next,
.house-labels .done,
.house-labels .failed {
  opacity: 1;
}

.house-labels .next .label-name {
  color: var(--gold);
}

.house-labels .done .label-name {
  color: #5fd38d;
}

.house-labels .failed .label-name {
  color: #ff6b6b;
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

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.chip-btn {
  padding: 0;
  border: none;
  border-radius: 50%;
  background: none;
  cursor: pointer;
  transition: transform 0.15s;
}

.chip-btn:hover:not(:disabled) {
  transform: translateY(-3px);
}

.controls .btn:disabled,
.chip-btn:disabled {
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

  .board {
    --lane: 18%;
    --wolf-w: 18%;
  }

  .houses {
    padding-top: 2.5rem;
  }

  .slot {
    padding: 0 4%;
  }

  .chips {
    gap: 0.4rem;
  }

  .chip-btn :deep(.chip) {
    --size: 2.4rem !important;
  }

  .house-labels li {
    font-size: 0.65rem;
  }

  .decide {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .wolf-wrap {
    transition: none;
  }
}
</style>
