<script setup>
import { computed, reactive, ref } from 'vue'
import RouletteWheel from '../components/RouletteWheel.vue'
import RouletteTable from '../components/RouletteTable.vue'
import CasinoChip from '../components/CasinoChip.vue'
import { BETS, POCKETS, colorOf, randomPocketIndex, settle } from '../roulette'
import { formatMoney } from '../format'

const CHIPS = [1, 5, 10, 25, 100, 500]
const START_BALANCE = 100000
const BALANCE_KEY = 'gamba.roulette.balance'
const RECENT_COUNT = 12

// Demo balance, kept in this browser only. It changes when a spin is settled;
// chips on the table are only reserved against it until then.
function loadBalance() {
  const stored = Number(localStorage.getItem(BALANCE_KEY))
  return Number.isFinite(stored) && localStorage.getItem(BALANCE_KEY) !== null
    ? stored
    : START_BALANCE
}

const balance = ref(loadBalance())
function setBalance(value) {
  balance.value = value
  localStorage.setItem(BALANCE_KEY, String(value))
}

const wheel = ref(null)
const chip = ref(5)
const placed = reactive({}) // { betKey: amount }
const history = [] // chip placements, for undo
let lastBets = {}

const spinning = ref(false)
const message = ref('')
const outcome = ref(null) // { number, stake, returned, winners }
const recent = ref([]) // last winning numbers, newest first

const totalBet = computed(() => Object.values(placed).reduce((sum, a) => sum + a, 0))
const available = computed(() => balance.value - totalBet.value)
const lastBetsTotal = () => Object.values(lastBets).reduce((sum, a) => sum + a, 0)

function addChip(key, amount) {
  placed[key] = (placed[key] ?? 0) + amount
  history.push({ key, amount })
}

function place(key) {
  if (spinning.value) return
  if (chip.value > available.value) {
    message.value = `Not enough balance for a ${formatMoney(chip.value)} chip`
    return
  }
  message.value = ''
  addChip(key, chip.value)
}

function undo() {
  const last = history.pop()
  if (!last) return
  placed[last.key] -= last.amount
  if (placed[last.key] === 0) delete placed[last.key]
  message.value = ''
}

function clearBets() {
  for (const key of Object.keys(placed)) delete placed[key]
  history.length = 0
  message.value = ''
}

function rebet() {
  if (lastBetsTotal() > available.value) {
    message.value = 'Not enough balance to repeat the last bets'
    return
  }
  for (const [key, amount] of Object.entries(lastBets)) addChip(key, amount)
  message.value = ''
}

async function spin() {
  if (spinning.value) return
  if (totalBet.value === 0) {
    message.value = 'Place a bet first: pick a chip, then click the table'
    return
  }
  message.value = ''
  outcome.value = null
  spinning.value = true

  const number = POCKETS[randomPocketIndex()]
  await wheel.value.spinTo(number)

  const result = settle(placed, number)
  setBalance(balance.value - result.stake + result.returned)
  outcome.value = { number, ...result }
  recent.value = [number, ...recent.value].slice(0, RECENT_COUNT)
  lastBets = { ...placed }
  clearBets()
  spinning.value = false
}

function resetBalance() {
  setBalance(START_BALANCE)
  message.value = `Balance reset to ${formatMoney(START_BALANCE)}`
}

const placedList = computed(() =>
  Object.entries(placed).map(([key, amount]) => ({ key, amount, label: BETS.get(key).label })),
)
</script>

<template>
  <section class="roulette-page">
    <header class="page-head">
      <h1 class="title">Roulette</h1>
      <p class="subtitle">European · single zero</p>
    </header>

    <div class="stage">
      <div class="wheel-area">
        <RouletteWheel ref="wheel" />
      </div>

      <aside class="side">
        <div class="card stats">
          <div>
            <span class="stat-label">Balance</span>
            <span class="stat-value">{{ formatMoney(balance) }}</span>
          </div>
          <div>
            <span class="stat-label">Total bet</span>
            <span class="stat-value">{{ formatMoney(totalBet) }}</span>
          </div>
        </div>

        <div class="card result" aria-live="polite">
          <p v-if="spinning" class="no-more">No more bets…</p>
          <template v-else-if="outcome">
            <div class="result-line">
              <span class="badge" :class="colorOf(outcome.number)">{{ outcome.number }}</span>
              <div>
                <p class="result-color">{{ colorOf(outcome.number) }}</p>
                <p v-if="outcome.returned > 0" class="success win-text">
                  You won {{ formatMoney(outcome.returned) }}
                </p>
                <p v-else class="muted">You lost {{ formatMoney(outcome.stake) }}</p>
              </div>
            </div>
            <ul v-if="outcome.winners.length" class="winners">
              <li v-for="w in outcome.winners" :key="w.label">
                <span>{{ w.label }}</span>
                <span>{{ formatMoney(w.amount) }}</span>
              </li>
            </ul>
          </template>
          <p v-else class="muted">Pick a chip, click the table to place bets, then spin.</p>
        </div>

        <div class="card recent">
          <span class="stat-label">Last numbers</span>
          <ol v-if="recent.length" class="recent-list">
            <li v-for="(n, i) in recent" :key="i" class="dot" :class="colorOf(n)">{{ n }}</li>
          </ol>
          <p v-else class="muted small">No spins yet</p>
        </div>

        <button
          v-if="available < CHIPS[0] && totalBet === 0 && !spinning"
          type="button"
          class="btn btn-outline-gold"
          @click="resetBalance"
        >
          Reset balance
        </button>
      </aside>
    </div>

    <div class="table-area">
      <RouletteTable
        :placed="placed"
        :winning="outcome?.number ?? null"
        :disabled="spinning"
        @place="place"
      />
      <p class="scroll-hint muted small">Swipe sideways to see the whole table</p>

      <div class="control-bar">
        <div class="chips" role="radiogroup" aria-label="Chip value">
          <button
            v-for="c in CHIPS"
            :key="c"
            type="button"
            role="radio"
            class="chip-btn"
            :class="{ selected: chip === c }"
            :aria-checked="chip === c"
            :aria-label="`${c} chip`"
            :disabled="spinning"
            @click="chip = c"
          >
            <CasinoChip :value="c" size="3.1rem" />
          </button>
        </div>

        <div class="controls">
          <button
            type="button"
            class="btn btn-outline"
            :disabled="spinning || !totalBet"
            @click="undo"
          >
            Undo
          </button>
          <button
            type="button"
            class="btn btn-outline"
            :disabled="spinning || !totalBet"
            @click="clearBets"
          >
            Clear
          </button>
          <button
            type="button"
            class="btn btn-outline"
            :disabled="spinning || !lastBetsTotal()"
            @click="rebet"
          >
            Rebet
          </button>
          <button type="button" class="btn btn-gold spin" :disabled="spinning" @click="spin">
            {{ spinning ? 'Spinning…' : 'Spin' }}
          </button>
        </div>
      </div>

      <p v-if="message" class="error message" role="alert">{{ message }}</p>

      <ul v-if="placedList.length" class="bet-list">
        <li v-for="b in placedList" :key="b.key">
          <span>{{ b.label }}</span>
          <span>{{ formatMoney(b.amount) }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.roulette-page {
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

/* Wheel and side panel */
.stage {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 340px);
  gap: 2.5rem;
  align-items: center;
  max-width: 980px;
  width: 100%;
  margin: 0 auto;
}

.wheel-area {
  display: grid;
  place-items: center;
}

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

.result {
  min-height: 7.5rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.no-more {
  font-family: var(--font-serif);
  font-size: 1.2rem;
  text-align: center;
  color: var(--gold);
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  50% {
    opacity: 0.4;
  }
}

.result-line {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.badge {
  display: grid;
  flex: none;
  place-items: center;
  width: 3.6rem;
  height: 3.6rem;
  border: 3px solid var(--gold);
  border-radius: 50%;
  color: #fff;
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: bold;
  box-shadow: 0 0 16px rgba(240, 165, 0, 0.45);
  animation: pop 0.35s ease-out;
}

@keyframes pop {
  from {
    transform: scale(0.6);
    opacity: 0;
  }
}

.badge.red,
.dot.red {
  background: #b52a1f;
}

.badge.black,
.dot.black {
  background: #151515;
}

.badge.green,
.dot.green {
  background: #1b7a43;
}

.result-color {
  font-family: var(--font-serif);
  font-size: 1.2rem;
  text-transform: capitalize;
  color: var(--color-heading);
}

.win-text {
  font-size: 1rem;
  font-weight: bold;
}

.muted {
  opacity: 0.75;
}

.small {
  font-size: 0.8rem;
}

.winners {
  margin: 0.75rem 0 0;
  padding: 0.6rem 0 0;
  border-top: 1px solid var(--color-border);
  list-style: none;
  font-size: 0.85rem;
}

.winners li {
  display: flex;
  justify-content: space-between;
}

.recent {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.recent-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.dot {
  display: grid;
  place-items: center;
  width: 1.9rem;
  height: 1.9rem;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 50%;
  color: #fff;
  font-size: 0.75rem;
  font-weight: bold;
}

.dot:first-child {
  border: 2px solid var(--gold);
}

/* Table and controls */
.table-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  width: 100%;
}

.scroll-hint {
  display: none;
  margin-top: -0.5rem;
}

.control-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 1rem 2rem;
  width: 100%;
  max-width: 980px;
  padding: 0.9rem 1.2rem;
  border: 1px solid rgba(240, 165, 0, 0.35);
  border-radius: 12px;
  background: linear-gradient(180deg, #121212, #070707);
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

.chip-btn.selected {
  transform: translateY(-5px);
  box-shadow: 0 0 0 3px var(--gold), 0 0 18px rgba(240, 165, 0, 0.7);
}

.chip-btn:disabled {
  opacity: 0.55;
  cursor: default;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.controls .btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.spin {
  min-width: 9rem;
  font-size: 1.15rem;
  font-weight: bold;
  box-shadow: 0 0 18px rgba(240, 165, 0, 0.45);
}

.message {
  font-size: 0.9rem;
}

.bet-list {
  width: min(100%, 460px);
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 0.85rem;
}

.bet-list li {
  display: flex;
  justify-content: space-between;
  padding: 0.3rem 0;
  border-bottom: 1px solid var(--color-border);
}

@media (max-width: 860px) {
  .stage {
    grid-template-columns: minmax(0, 1fr);
    gap: 1.5rem;
  }

  .side {
    width: min(100%, 420px);
    margin: 0 auto;
  }
}

@media (max-width: 720px) {
  .scroll-hint {
    display: block;
  }

  .control-bar {
    flex-direction: column;
    align-items: stretch;
    padding: 0.9rem;
  }

  .chips {
    justify-content: center;
    gap: 0.45rem;
  }

  .chip-btn :deep(.chip) {
    --size: 2.55rem !important;
  }

  .controls {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }

  .controls .btn {
    padding-inline: 0.5rem;
  }

  .spin {
    grid-column: 1 / -1;
  }
}
</style>
