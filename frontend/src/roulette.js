// European roulette: 37 pockets, listed in the order they sit on the wheel (clockwise from 0)
export const POCKETS = [
  0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14,
  31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26,
]

const RED = new Set([1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36])

export function colorOf(n) {
  if (n === 0) return 'green'
  return RED.has(n) ? 'red' : 'black'
}

// Uniform random index into POCKETS
export function randomPocketIndex() {
  const [r] = crypto.getRandomValues(new Uint32Array(1))
  return r % POCKETS.length
}

// --- Bets ---------------------------------------------------------------
// Every bet covers a set of numbers. With 37 pockets the standard payout is
// 36 / count - 1 to 1: straight 35, split 17, street/trio 11, corner/first four 8,
// six line 5, dozen/column 2, even-money bets 1.
export function payoutOf(bet) {
  return 36 / bet.numbers.length - 1
}

function range(from, to) {
  return Array.from({ length: to - from + 1 }, (_, i) => from + i)
}

function bet(key, label, numbers, x, y) {
  return { key, label, numbers, x, y }
}

// The table's number grid has 12 columns (1-3, 4-6, ... 34-36) and 3 rows,
// with the top row holding 3, 6, ... 36 and the bottom row 1, 4, ... 34.
const numAt = (col, row) => 3 * col + 3 - row
const streetOf = (col) => [3 * col + 1, 3 * col + 2, 3 * col + 3]

// Number rows as drawn on the table, top to bottom
export const TABLE_ROWS = [0, 1, 2].map((row) => range(0, 11).map((col) => numAt(col, row)))

// Straight-up bets on 0-36
export const STRAIGHT_BETS = range(0, 36).map((n) => bet(`n${n}`, `Straight ${n}`, [n]))

// Inside bets placed on the lines between numbers. x / y are positions in grid
// units over the number grid: x from 0 (edge next to 0) to 12, y from 0 (top) to 3.
export const LINE_BETS = (() => {
  const list = []
  for (let row = 0; row < 3; row++) {
    const n = numAt(0, row)
    list.push(bet(`split-0-${n}`, `Split 0/${n}`, [0, n], 0, row + 0.5))
  }
  list.push(bet('trio-0-2-3', 'Trio 0/2/3', [0, 2, 3], 0, 1))
  list.push(bet('trio-0-1-2', 'Trio 0/1/2', [0, 1, 2], 0, 2))
  list.push(bet('first-four', 'First four 0/1/2/3', [0, 1, 2, 3], 0, 3))

  for (let col = 0; col < 12; col++) {
    for (let row = 0; row < 3; row++) {
      const n = numAt(col, row)
      if (col < 11) {
        list.push(bet(`split-${n}-${n + 3}`, `Split ${n}/${n + 3}`, [n, n + 3], col + 1, row + 0.5))
      }
      if (row < 2) {
        list.push(bet(`split-${n - 1}-${n}`, `Split ${n - 1}/${n}`, [n - 1, n], col + 0.5, row + 1))
      }
      if (col < 11 && row < 2) {
        const corner = [n - 1, n, n + 2, n + 3]
        list.push(bet(`corner-${n - 1}`, `Corner ${corner.join('/')}`, corner, col + 1, row + 1))
      }
    }
    const street = streetOf(col)
    list.push(bet(`street-${street[0]}`, `Street ${street.join('/')}`, street, col + 0.5, 3))
    if (col < 11) {
      const six = [...street, ...streetOf(col + 1)]
      list.push(bet(`line-${six[0]}`, `Six line ${six[0]}-${six[5]}`, six, col + 1, 3))
    }
  }
  return list
})()

// Column bets, in table row order (top row = 3rd column)
export const COLUMN_BETS = [3, 2, 1].map((c) =>
  bet(`column-${c}`, `Column ${c}`, range(1, 36).filter((n) => (n - c) % 3 === 0)),
)

export const DOZEN_BETS = [1, 2, 3].map((d) =>
  bet(`dozen-${d}`, `${d === 1 ? '1st' : d === 2 ? '2nd' : '3rd'} 12`, range(12 * d - 11, 12 * d)),
)

export const EVEN_MONEY_BETS = [
  bet('low', '1 to 18', range(1, 18)),
  bet('even', 'Even', range(1, 36).filter((n) => n % 2 === 0)),
  bet('red', 'Red', range(1, 36).filter((n) => colorOf(n) === 'red')),
  bet('black', 'Black', range(1, 36).filter((n) => colorOf(n) === 'black')),
  bet('odd', 'Odd', range(1, 36).filter((n) => n % 2 === 1)),
  bet('high', '19 to 36', range(19, 36)),
]

export const BETS = new Map(
  [...STRAIGHT_BETS, ...LINE_BETS, ...COLUMN_BETS, ...DOZEN_BETS, ...EVEN_MONEY_BETS].map((b) => [
    b.key,
    b,
  ]),
)

// Settle placed bets ({ betKey: amount }) against the winning number.
// `returned` includes the stake of each winning bet.
export function settle(placed, number) {
  let stake = 0
  let returned = 0
  const winners = []
  for (const [key, amount] of Object.entries(placed)) {
    const b = BETS.get(key)
    stake += amount
    if (b.numbers.includes(number)) {
      const win = amount * (payoutOf(b) + 1)
      returned += win
      winners.push({ label: b.label, amount: win })
    }
  }
  return { stake, returned, winners }
}
