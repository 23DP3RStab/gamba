import { afterEach, describe, expect, it, vi } from 'vitest'
import { HOUSES, blowSucceeds } from '../piggies'

// Makes the game's random() return exactly value / 2^32
function stubRandom(value) {
  vi.stubGlobal('crypto', {
    getRandomValues: (array) => {
      array[0] = value
      return array
    },
  })
}

afterEach(() => vi.unstubAllGlobals())

describe('Trīs sivēntiņu mājas', () => {
  it('S-01 mājas, varbūtības un reizinātāji', () => {
    expect(HOUSES.map((h) => [h.key, h.chance, h.multiplier])).toEqual([
      ['straw', 0.5, 2],
      ['wood', 0.4, 5],
      ['brick', 0.3, 16.67],
    ])
  })

  it.each([
    [0, 'straw'],
    [1, 'wood'],
    [2, 'brick'],
  ])('S-02 māja %i (%s): tieši zem robežas krīt, uz robežas paliek', (index) => {
    const edge = Math.ceil(HOUSES[index].chance * 2 ** 32) // first value with random() >= chance
    stubRandom(edge - 1)
    expect(blowSucceeds(index)).toBe(true)
    stubRandom(edge)
    expect(blowSucceeds(index)).toBe(false)
  })

  it('S-03 mazākā un lielākā nejaušā vērtība', () => {
    stubRandom(0)
    expect(blowSucceeds(2)).toBe(true)
    stubRandom(2 ** 32 - 1)
    expect(blowSucceeds(0)).toBe(false)
  })
})
