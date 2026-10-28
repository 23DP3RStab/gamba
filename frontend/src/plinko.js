import { api } from './api'

// Plinko: the server drops the ball and settles the bet at once; the board only animates the
// path it sent back. Every call throws an Error with `status` when it fails (see api.js).

export const ROWS = 12

export const RISKS = [
  { key: 'LOW', label: 'Low' },
  { key: 'MEDIUM', label: 'Medium' },
  { key: 'HIGH', label: 'High' },
]

// { LOW: [13 multipliers], MEDIUM: [...], HIGH: [...] }, left to right
export function getPlinkoTables() {
  return api('/games/plinko/tables')
}

// → { path: 'LRRL…' (12 bounces), slot, multiplier, payout, balance }
export function dropPlinko(betAmount, risk) {
  return api('/games/plinko/drop', { method: 'POST', body: { betAmount, risk } })
}
