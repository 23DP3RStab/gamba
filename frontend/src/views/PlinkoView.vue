<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import PlinkoBoard from '../components/PlinkoBoard.vue'
import { RISKS, dropPlinko, getPlinkoTables } from '../plinko'
import { formatDateTime, formatMoney } from '../format'
import { useLockout } from '../lockout'
import { useAuthStore } from '../stores/auth'

// Plinko on the real balance: the server drops the ball and settles the bet at once, then the
// board animates the path it chose. Several balls can be in the air at the same time.
const authStore = useAuthStore()
const balance = computed(() => Number(authStore.user?.balance ?? 0))
const lockedUntil = useLockout()

const MIN_BET = 0.1
const RECENT_COUNT = 14
const cents = (value) => Math.round(value * 100) / 100
const times = (value) => `${Number(value).toLocaleString('lv-LV')}×`
const tone = (multiplier) => (multiplier >= 10 ? 'high' : multiplier >= 1 ? 'mid' : 'low')

const tables = ref(null)
const risk = ref('MEDIUM')
const bet = ref(1)
const sending = ref(false) // a drop is on its way to the server
const falling = ref(0) // balls still in the air
const recent = ref([]) // landed balls, newest first: { id, multiplier, payout }
const message = ref('')
const board = ref(null)
let dropId = 0

// The server settles a drop straight away, but its win should only show once the ball lands:
// the shown balance is the server's latest, less the payouts of balls that are still falling
let serverBalance = null
let notLanded = 0
function showBalance() {
  if (serverBalance !== null) authStore.updateBalance(cents(serverBalance - notLanded))
}

const multipliers = computed(() => tables.value?.[risk.value] ?? [])
const betAmount = computed(() => {
  const n = Number(bet.value)
  return Number.isFinite(n) && n > 0 ? cents(n) : 0
})

onMounted(async () => {
  try {
    tables.value = await getPlinkoTables()
  } catch (e) {
    message.value = e.message
  }
})

async function drop() {
  if (sending.value || lockedUntil.value || !tables.value) return
  const amount = betAmount.value
  if (amount < MIN_BET) {
    message.value = `The smallest bet is ${formatMoney(MIN_BET)}`
    return
  }
  if (amount > balance.value) {
    message.value = `Not enough balance for a ${formatMoney(amount)} bet`
    return
  }
  message.value = ''
  sending.value = true
  let result
  try {
    result = await dropPlinko(amount, risk.value)
  } catch (e) {
    message.value = e.message
    return
  } finally {
    sending.value = false
  }

  const payout = Number(result.payout)
  serverBalance = Number(result.balance)
  notLanded = cents(notLanded + payout)
  showBalance()
  falling.value++
  await board.value?.drop(result.path, result.slot)
  if (gone) return // the page was left; the full balance is already shown
  falling.value--
  notLanded = cents(notLanded - payout)
  showBalance()
  recent.value = [{ id: ++dropId, multiplier: Number(result.multiplier), payout }, ...recent.value].slice(
    0,
    RECENT_COUNT,
  )
}

// The risk applies to the next ball, so it can only change while none are falling
function pickRisk(key) {
  if (falling.value === 0) risk.value = key
}

const halveBet = () => (bet.value = cents(betAmount.value / 2))
const doubleBet = () => (bet.value = cents(betAmount.value * 2))

// Leaving with balls in the air: their wins are already on the server, show them now
let gone = false
onBeforeUnmount(() => {
  gone = true
  notLanded = 0
  showBalance()
})
</script>

<template>
  <section class="plinko-page">
    <RouterLink to="/lobby" class="back-btn">← Lobby</RouterLink>

    <header class="page-head">
      <h1 class="title">Plinko</h1>
      <p class="subtitle">Drop the ball · 12 rows of pegs</p>
    </header>

    <p v-if="lockedUntil" class="lockout" role="alert">
      Your account is locked until {{ formatDateTime(lockedUntil) }}. You can't play until then.
    </p>

    <div class="layout">
      <aside class="side">
        <div class="card controls">
          <label class="bet-field">
            <span class="stat-label">Bet (€)</span>
            <input
              v-model="bet"
              type="number"
              min="0.10"
              step="0.01"
              inputmode="decimal"
              @keydown.enter="drop"
            />
          </label>
          <div class="bet-tools">
            <button type="button" class="btn btn-outline" @click="halveBet">½</button>
            <button type="button" class="btn btn-outline" @click="doubleBet">2×</button>
          </div>

          <div class="risk-field">
            <span class="stat-label">Risk</span>
            <div class="risks" role="group" aria-label="Risk">
              <button
                v-for="option in RISKS"
                :key="option.key"
                type="button"
                class="risk"
                :class="{ active: risk === option.key }"
                :aria-pressed="risk === option.key"
                :disabled="falling > 0"
                @click="pickRisk(option.key)"
              >
                {{ option.label }}
              </button>
            </div>
            <p v-if="falling > 0" class="hint">The risk can change once the balls have landed</p>
          </div>

          <button
            type="button"
            class="btn btn-gold action"
            :disabled="sending || !!lockedUntil || !tables"
            @click="drop"
          >
            Drop ball
          </button>
          <p v-if="message" class="error message" role="alert">{{ message }}</p>
        </div>

        <div class="card stats">
          <div>
            <span class="stat-label">Balance</span>
            <span class="stat-value">{{ formatMoney(balance) }}</span>
          </div>
        </div>
      </aside>

      <div class="board-col">
        <div class="board-frame">
          <PlinkoBoard ref="board" :multipliers="multipliers" />
        </div>
        <ol class="recent" aria-label="Last balls">
          <li v-for="ball in recent" :key="ball.id" :class="tone(ball.multiplier)">
            {{ times(ball.multiplier) }}
          </li>
        </ol>
      </div>
    </div>
  </section>
</template>

<style scoped>
.plinko-page {
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

.layout {
  display: grid;
  grid-template-columns: minmax(0, 300px) minmax(0, 1fr);
  gap: 1.5rem;
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
}

.side,
.board-col {
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
}

.bet-field,
.risk-field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.bet-tools {
  display: flex;
  gap: 0.5rem;
}

.bet-tools .btn {
  flex: 1;
}

.risks {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.4rem;
}

.risk {
  padding: 0.5rem 0.4rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #0d0d10;
  color: inherit;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.risk.active {
  border-color: var(--gold);
  background: rgba(240, 165, 0, 0.15);
  color: var(--gold);
}

.risk:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.hint {
  margin: 0;
  font-size: 0.8rem;
  opacity: 0.65;
}

.controls .btn:disabled {
  opacity: 0.45;
  cursor: default;
}

.action {
  font-size: 1.05rem;
  font-weight: bold;
  box-shadow: 0 0 18px rgba(240, 165, 0, 0.45);
}

.message {
  margin: 0;
  font-size: 0.9rem;
}

.stats {
  display: grid;
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

.board-frame {
  position: relative;
  aspect-ratio: 1 / 1;
  max-height: 680px;
  border: 1px solid rgba(240, 165, 0, 0.35);
  border-radius: 12px;
  background: radial-gradient(circle at 50% 0%, #1d1a10 0%, #0b0b10 70%);
  overflow: hidden;
}

.recent {
  display: flex;
  gap: 0.4rem;
  min-height: 1.6rem;
  margin: 0;
  padding: 0;
  overflow: hidden;
  list-style: none;
  mask-image: linear-gradient(90deg, #000 85%, transparent);
}

.recent li {
  flex: none;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: bold;
  font-variant-numeric: tabular-nums;
}

.recent .low {
  background: rgba(125, 125, 138, 0.18);
  color: #b0b0bc;
}

.recent .mid {
  background: rgba(240, 165, 0, 0.18);
  color: var(--gold);
}

.recent .high {
  background: rgba(255, 77, 77, 0.18);
  color: #ff7b7b;
}

@media (max-width: 860px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .board-col {
    order: -1;
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
</style>
