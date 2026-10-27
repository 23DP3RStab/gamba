import { afterEach, describe, expect, it, vi } from 'vitest'
import { BETS, POCKETS, STRAIGHT_BETS, colorOf, payoutOf, randomPocketIndex, settle } from '../roulette'

afterEach(() => vi.unstubAllGlobals())

describe('Ruletes izmaksu koeficienti', () => {
  it.each([
    ['n17', 'Straight', 35],
    ['split-1-4', 'Split', 17],
    ['street-1', 'Street', 11],
    ['trio-0-1-2', 'Trio', 11],
    ['corner-1', 'Corner', 8],
    ['first-four', 'First four', 8],
    ['line-1', 'Six line', 5],
    ['column-1', 'Column', 2],
    ['dozen-1', 'Dozen', 2],
    ['red', 'Red', 1],
    ['black', 'Black', 1],
    ['odd', 'Odd', 1],
    ['even', 'Even', 1],
    ['low', '1 to 18', 1],
    ['high', '19 to 36', 1],
  ])('G-01 likme %s (%s) maksā %i:1', (key, _label, payout) => {
    expect(payoutOf(BETS.get(key))).toBe(payout)
  })
})

describe('Likmju nokārtošana', () => {
  it('G-02 uzvara uz viena skaitļa: 10 € uz 17 atdod 360 €', () => {
    expect(settle({ n17: 10 }, 17)).toMatchObject({ stake: 10, returned: 360 })
  })
  it('G-03 zaudējums: 10 € uz 17, uzkrīt 18 → atdod 0 €', () => {
    expect(settle({ n17: 10 }, 18)).toMatchObject({ stake: 10, returned: 0, winners: [] })
  })
  it('G-04 nulle zaudē likmes uz sarkano un melno', () => {
    expect(settle({ red: 10, black: 10 }, 0)).toMatchObject({ stake: 20, returned: 0 })
  })
  it('G-05 vairākas likmes: sarkanais + 3. ducis, uzkrīt 36', () => {
    const result = settle({ red: 10, 'dozen-3': 5 }, 36)
    expect(result.stake).toBe(15)
    expect(result.returned).toBe(20 + 15)
    expect(result.winners.map((w) => w.label)).toEqual(['Red', '3rd 12'])
  })
})

describe('Ruletes rats un galds', () => {
  it('G-06 37 kabatas (0–36), katra tieši vienreiz', () => {
    expect([...POCKETS].sort((a, b) => a - b)).toEqual(Array.from({ length: 37 }, (_, i) => i))
    expect(STRAIGHT_BETS).toHaveLength(37)
  })
  it('G-07 18 sarkani, 18 melni, 0 zaļš', () => {
    const colors = POCKETS.map(colorOf)
    expect(colors.filter((c) => c === 'red')).toHaveLength(18)
    expect(colors.filter((c) => c === 'black')).toHaveLength(18)
    expect(colorOf(0)).toBe('green')
  })
  it.each([0, 36, 37, 2 ** 32 - 1])(
    'G-08 nejaušā vērtība %i dod indeksu 0–36',
    (value) => {
      vi.stubGlobal('crypto', {
        getRandomValues: (array) => {
          array[0] = value
          return array
        },
      })
      const index = randomPocketIndex()
      expect(index).toBeGreaterThanOrEqual(0)
      expect(index).toBeLessThanOrEqual(36)
    },
  )
  it('G-09 10 000 īstu griezienu paliek robežās un trāpa visās kabatās', () => {
    const seen = new Set()
    for (let i = 0; i < 10000; i++) seen.add(randomPocketIndex())
    expect(Math.min(...seen)).toBe(0)
    expect(Math.max(...seen)).toBe(36)
    expect(seen.size).toBe(37)
  })
})
