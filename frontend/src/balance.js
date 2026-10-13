import { ref } from 'vue'

// Demo balance shared by all games, kept in this browser only.
// The key still says roulette so balances saved before other games existed carry over.
export const START_BALANCE = 100000
const BALANCE_KEY = 'gamba.roulette.balance'

function loadBalance() {
  const stored = Number(localStorage.getItem(BALANCE_KEY))
  return Number.isFinite(stored) && localStorage.getItem(BALANCE_KEY) !== null
    ? stored
    : START_BALANCE
}

export const balance = ref(loadBalance())

export function setBalance(value) {
  balance.value = value
  localStorage.setItem(BALANCE_KEY, String(value))
}

export function resetBalance() {
  setBalance(START_BALANCE)
}
