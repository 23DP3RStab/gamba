import { api } from './api'

// Roulette and Piggies on the real balance. The server decides every outcome and answers
// with the new balance; the pages only animate what it sent. Every call throws an Error
// with `status` when it fails (see api.js).

// bets: { betKey: amount } → { number, totalBet, payout, balance }
export function spinRoulette(bets) {
  return api('/games/roulette/spin', { method: 'POST', body: { bets } })
}

// The Piggies calls answer { betAmount, housesDown, fell, status, payout, balance },
// status being IN_PROGRESS, LOST, CASHED_OUT or WON. Starting already blows at the straw house.
export function startPiggies(betAmount) {
  return api('/games/piggies/start', { method: 'POST', body: { betAmount } })
}

export function blowPiggies() {
  return api('/games/piggies/blow', { method: 'POST' })
}

export function cashOutPiggies() {
  return api('/games/piggies/cashout', { method: 'POST' })
}

// The round left unfinished (say, the tab was closed), or null
export function activePiggies() {
  return api('/games/piggies/active')
}
