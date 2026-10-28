<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import CrashScene from '../components/CrashScene.vue'
import {
  MAX_MULTIPLIER,
  MIN_AUTO_CASHOUT,
  MIN_BET,
  cashOutCrash,
  connectCrashStream,
  multiplierAt,
  placeCrashBet,
} from '../crash'
import { formatDateTime, formatMoney } from '../format'
import { useLockout } from '../lockout'
import { useAuthStore } from '../stores/auth'

// Crash, played live with everyone else on the site. The server runs the rounds and sends each
// change over the stream; between updates the page counts the multiplier up itself.
const authStore = useAuthStore()
const balance = computed(() => Number(authStore.user?.balance ?? 0))
const me = computed(() => authStore.user?.username)
const lockedUntil = useLockout()

const TICK_MS = 50
const cents = (value) => Math.round(value * 100) / 100
const times = (value) =>
  `${Number(value).toLocaleString('lv-LV', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}×`

const round = ref(null) // the server's latest "state": phase, times, crash point, history
const bets = ref([]) // everyone's bets this round: { username, amount, cashedOutAt, payout }
const offset = ref(0) // the server's clock minus this browser's
const connection = ref('connecting') // 'connecting' | 'live' | 'down' | 'expired'
const now = ref(Date.now())

const bet = ref(1)
const autoCashout = ref('')
const message = ref('')
const busy = ref(false) // a bet or cash out is on its way to the server
let paidRound = null // the round whose cash out is already in the balance

const phase = computed(() => round.value?.phase ?? null)
const takeoff = computed(() =>
  round.value?.startedAt != null ? round.value.startedAt - offset.value : null,
)
const crashPoint = computed(() =>
  round.value?.crashPoint != null ? Number(round.value.crashPoint) : null,
)
const multiplier = computed(() => {
  if (phase.value === 'RUNNING') return multiplierAt(now.value - takeoff.value)
  return phase.value === 'CRASHED' ? crashPoint.value : 1
})
const secondsLeft = computed(() =>
  phase.value === 'BETTING'
    ? Math.max(0, Math.ceil((round.value.bettingEndsAt - offset.value - now.value) / 1000))
    : 0,
)
const myBet = computed(() => bets.value.find((b) => b.username === me.value) ?? null)
const betAmount = computed(() => {
  const n = Number(bet.value)
  return Number.isFinite(n) && n > 0 ? cents(n) : 0
})
const inPlay = computed(() => bets.value.reduce((sum, b) => sum + Number(b.amount), 0))
const history = computed(() => round.value?.history ?? [])

// What the big button does right now
const action = computed(() => {
  const mine = myBet.value
  if (mine?.cashedOutAt != null) return { kind: 'done', label: `Cashed out at ${times(mine.cashedOutAt)}` }
  if (phase.value === 'RUNNING' && mine) {
    const worth = Math.floor(Number(mine.amount) * multiplier.value * 100) / 100
    return { kind: 'cashout', label: `Cash out ${formatMoney(worth)}` }
  }
  if (phase.value === 'BETTING' && mine) return { kind: 'wait', label: 'Bet placed, waiting for take-off' }
  if (phase.value === 'BETTING') return { kind: 'bet', label: `Bet ${formatMoney(betAmount.value)}` }
  return { kind: 'wait', label: 'Wait for the next round' }
})

function act() {
  if (action.value.kind === 'bet') placeBet()
  else if (action.value.kind === 'cashout') cashOut()
}

function addBet(entry) {
  bets.value = [...bets.value.filter((b) => b.username !== entry.username), entry]
}

function updateBet(entry) {
  bets.value = bets.value.map((b) => (b.username === entry.username ? entry : b))
}

async function placeBet() {
  if (busy.value || lockedUntil.value || phase.value !== 'BETTING' || myBet.value) return
  const amount = betAmount.value
  if (amount < MIN_BET) {
    message.value = `The smallest bet is ${formatMoney(MIN_BET)}`
    return
  }
  if (amount > balance.value) {
    message.value = `Not enough balance for a ${formatMoney(amount)} bet`
    return
  }
  let auto = null
  if (String(autoCashout.value).trim() !== '') {
    auto = cents(Number(autoCashout.value))
    if (!(auto >= MIN_AUTO_CASHOUT && auto <= MAX_MULTIPLIER)) {
      message.value = 'Auto cash-out must be between 1,01× and 1000×'
      return
    }
  }
  message.value = ''
  busy.value = true
  try {
    const result = await placeCrashBet(amount, auto)
    authStore.updateBalance(result.balance)
    addBet({ username: me.value, amount, cashedOutAt: null, payout: null })
  } catch (e) {
    message.value = e.message
  } finally {
    busy.value = false
  }
}

async function cashOut() {
  const mine = myBet.value
  if (busy.value || phase.value !== 'RUNNING' || !mine || mine.cashedOutAt != null) return
  const roundId = round.value.roundId
  message.value = ''
  busy.value = true
  try {
    const result = await cashOutCrash()
    authStore.updateBalance(result.balance)
    paidRound = roundId
    updateBet({ ...mine, cashedOutAt: result.multiplier, payout: result.payout })
  } catch (e) {
    message.value = e.message
  } finally {
    busy.value = false
  }
}

function onEvent(name, data) {
  if (name === 'open') connection.value = 'live'
  else if (name === 'down') connection.value = 'down'
  else if (name === 'expired') connection.value = 'expired'
  else if (name === 'state') {
    offset.value = data.serverNow - Date.now()
    now.value = Date.now()
    round.value = data
    bets.value = data.bets
  } else if (name === 'bet') addBet(data)
  else if (name === 'cashout') {
    // An auto cash-out pays without a request from this page, so its win is added here
    const roundId = round.value?.roundId
    if (data.username === me.value && !busy.value && paidRound !== roundId) {
      authStore.updateBalance(cents(balance.value + Number(data.payout)))
      paidRound = roundId
    }
    updateBet(data)
  }
}

const halveBet = () => (bet.value = cents(betAmount.value / 2))
const doubleBet = () => (bet.value = cents(betAmount.value * 2))

function profit(entry) {
  if (entry.cashedOutAt != null) return '+' + formatMoney(Number(entry.payout) - Number(entry.amount))
  if (phase.value === 'CRASHED') return '−' + formatMoney(entry.amount)
  return '–'
}

const pointClass = (point) => (point >= 10 ? 'high' : point >= 2 ? 'mid' : 'low')

let ticker = null
let closeStream = null
onMounted(() => {
  closeStream = connectCrashStream(onEvent)
  ticker = setInterval(() => (now.value = Date.now()), TICK_MS)
})
onBeforeUnmount(() => {
  closeStream?.()
  clearInterval(ticker)
})
</script>

<template>
  <section class="crash-page">
    <RouterLink to="/lobby" class="back-btn">← Lobby</RouterLink>

    <header class="page-head">
      <h1 class="title">Crash</h1>
      <p class="subtitle">Live with everyone · cash out before it crashes</p>
    </header>

    <p v-if="lockedUntil" class="lockout" role="alert">
      Your account is locked until {{ formatDateTime(lockedUntil) }}. You can't play until then.
    </p>
    <p v-if="connection === 'expired'" class="lockout" role="alert">
      Your session has ended, please log in again
    </p>
    <p v-else-if="connection === 'down'" class="notice" role="status">
      Lost the connection to the live round, reconnecting…
    </p>

    <div class="board">
      <ol class="history" aria-label="Recent crash points">
        <li v-for="(point, i) in history" :key="i" :class="pointClass(point)">{{ times(point) }}</li>
      </ol>

      <div class="screen" :class="phase?.toLowerCase()">
        <CrashScene :phase="phase" :takeoff="takeoff" :crash-point="crashPoint" />
        <div class="readout" aria-live="polite">
          <p v-if="!round" class="readout-sub">Joining the live round…</p>
          <template v-else-if="phase === 'BETTING'">
            <p class="readout-sub">Taking off in</p>
            <p class="readout-big">{{ secondsLeft }} s</p>
            <p class="readout-sub">Place your bets</p>
          </template>
          <template v-else-if="phase === 'RUNNING'">
            <p class="readout-big flying">{{ times(multiplier) }}</p>
            <p v-if="myBet?.cashedOutAt != null" class="readout-sub won">
              You cashed out {{ formatMoney(myBet.payout) }}
            </p>
          </template>
          <template v-else>
            <p class="readout-sub">Crashed at</p>
            <p class="readout-big crashed">{{ times(multiplier) }}</p>
            <p v-if="myBet?.cashedOutAt != null" class="readout-sub won">
              You won {{ formatMoney(myBet.payout) }}
            </p>
            <p v-else-if="myBet" class="readout-sub lost">You lost {{ formatMoney(myBet.amount) }}</p>
          </template>
        </div>
      </div>
    </div>

    <div class="lower">
      <div class="main-col">
        <div class="card controls" :class="{ hot: action.kind === 'cashout' }">
          <div class="bet-row">
            <label class="bet-field">
              <span class="stat-label">Bet (€)</span>
              <input
                v-model="bet"
                type="number"
                min="0.10"
                step="0.01"
                inputmode="decimal"
                :disabled="!!myBet || busy"
                @keydown.enter="act"
              />
            </label>
            <div class="bet-tools">
              <button type="button" class="btn btn-outline" :disabled="!!myBet" @click="halveBet">
                ½
              </button>
              <button type="button" class="btn btn-outline" :disabled="!!myBet" @click="doubleBet">
                2×
              </button>
            </div>
            <label class="bet-field">
              <span class="stat-label">Auto cash-out (×)</span>
              <input
                v-model="autoCashout"
                type="number"
                min="1.01"
                step="0.01"
                inputmode="decimal"
                placeholder="Off"
                :disabled="!!myBet || busy"
                @keydown.enter="act"
              />
            </label>
          </div>

          <button
            type="button"
            class="btn btn-gold action"
            :class="action.kind"
            :disabled="
              busy || action.kind === 'wait' || action.kind === 'done' || !round ||
              (action.kind === 'bet' && !!lockedUntil)
            "
            @click="act"
          >
            {{ action.label }}
          </button>

          <p v-if="message" class="error message" role="alert">{{ message }}</p>
        </div>

        <div class="card players">
          <div class="players-head">
            <span class="stat-label">Players this round</span>
            <span class="muted">{{ bets.length }} · {{ formatMoney(inPlay) }} in play</span>
          </div>
          <p v-if="!bets.length" class="muted">No bets yet this round.</p>
          <table v-else>
            <thead>
              <tr>
                <th>Player</th>
                <th>Bet</th>
                <th>Cashed out</th>
                <th>Profit</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="entry in bets"
                :key="entry.username"
                :class="{
                  me: entry.username === me,
                  cashed: entry.cashedOutAt != null,
                  lost: phase === 'CRASHED' && entry.cashedOutAt == null,
                }"
              >
                <td>{{ entry.username }}</td>
                <td>{{ formatMoney(entry.amount) }}</td>
                <td>{{ entry.cashedOutAt != null ? times(entry.cashedOutAt) : '–' }}</td>
                <td>{{ profit(entry) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <aside class="side">
        <div class="card stats">
          <div>
            <span class="stat-label">Balance</span>
            <span class="stat-value">{{ formatMoney(balance) }}</span>
          </div>
          <div>
            <span class="stat-label">Your bet</span>
            <span class="stat-value">{{ myBet ? formatMoney(myBet.amount) : '–' }}</span>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.crash-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
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

.lockout,
.notice {
  align-self: center;
  max-width: 980px;
  margin: 0;
  padding: 0.75rem 1.25rem;
  border-radius: 8px;
  text-align: center;
}

.lockout {
  border: 1px solid #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
  color: #ff6b6b;
}

.notice {
  border: 1px solid rgba(240, 165, 0, 0.5);
  background: rgba(240, 165, 0, 0.08);
}

/* The graph */
.board {
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
}

.history {
  display: flex;
  gap: 0.4rem;
  margin: 0 0 0.6rem;
  padding: 0;
  overflow: hidden;
  list-style: none;
  mask-image: linear-gradient(90deg, #000 85%, transparent);
}

.history li {
  flex: none;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: bold;
  font-variant-numeric: tabular-nums;
}

.history .low {
  background: rgba(255, 90, 90, 0.15);
  color: #ff7b7b;
}

.history .mid {
  background: rgba(95, 211, 141, 0.15);
  color: #5fd38d;
}

.history .high {
  background: rgba(240, 165, 0, 0.18);
  color: var(--gold);
}

.screen {
  position: relative;
  aspect-ratio: 16 / 7;
  min-height: 260px;
  overflow: hidden;
  border: 1px solid rgba(240, 165, 0, 0.35);
  border-radius: 12px;
  background: radial-gradient(circle at 20% 100%, #1d1a10 0%, #0b0b10 65%);
  transition: border-color 0.3s;
}

.screen.crashed {
  border-color: rgba(255, 90, 90, 0.6);
}

.readout {
  position: absolute;
  top: 12%;
  left: 50%;
  translate: -50% 0;
  text-align: center;
  pointer-events: none;
}

.readout p {
  margin: 0;
}

.readout-big {
  font-family: var(--font-serif);
  font-size: clamp(2.4rem, 7vw, 4.2rem);
  font-weight: bold;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
  color: #ffffff;
  text-shadow: 0 0 24px rgba(0, 0, 0, 0.8);
}

.readout-big.flying {
  color: var(--gold);
}

.readout-big.crashed {
  color: #ff5a5a;
}

.readout-sub {
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  opacity: 0.8;
}

.readout-sub.won {
  color: #5fd38d;
  opacity: 1;
}

.readout-sub.lost {
  color: #ff7b7b;
  opacity: 1;
}

/* Controls, players and side panel */
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

.controls {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transition: border-color 0.3s;
}

.controls.hot {
  border-color: var(--gold);
  animation: glow 1.4s ease-in-out infinite;
}

@keyframes glow {
  50% {
    box-shadow: 0 0 22px rgba(240, 165, 0, 0.45);
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
  flex: 1 1 9rem;
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

.action.cashout {
  animation: pulse-btn 1s ease-in-out infinite;
}

@keyframes pulse-btn {
  50% {
    transform: scale(1.03);
    box-shadow: 0 0 16px rgba(240, 165, 0, 0.7);
  }
}

.message {
  font-size: 0.9rem;
}

.muted {
  opacity: 0.7;
}

.players-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.5rem;
  font-size: 0.85rem;
}

.players table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.players th {
  padding: 0.35rem 0.5rem;
  text-align: left;
  font-size: 0.7rem;
  font-weight: normal;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  opacity: 0.6;
}

.players td {
  padding: 0.4rem 0.5rem;
  border-top: 1px solid var(--color-border);
  font-variant-numeric: tabular-nums;
}

.players tr.me td:first-child {
  color: var(--gold);
  font-weight: bold;
}

.players tr.cashed td:nth-child(3),
.players tr.cashed td:nth-child(4) {
  color: #5fd38d;
}

.players tr.lost td:nth-child(4) {
  color: #ff7b7b;
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

@media (max-width: 860px) {
  .lower {
    grid-template-columns: minmax(0, 1fr);
  }

  .screen {
    aspect-ratio: 4 / 3;
  }
}

@media (max-width: 560px) {
  .back-btn {
    margin-left: 16px;
  }

  .title {
    font-size: 1.8rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .controls.hot,
  .action.cashout {
    animation: none;
  }
}
</style>
