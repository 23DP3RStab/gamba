import { describe, expect, it } from 'vitest'
import { HOUSES } from '../piggies'

describe('Trīs sivēntiņu mājas', () => {
  it('S-01 mājas, varbūtības un reizinātāji', () => {
    expect(HOUSES.map((h) => [h.key, h.chance, h.multiplier])).toEqual([
      ['straw', 0.5, 2],
      ['wood', 0.4, 5],
      ['brick', 0.3, 16.67],
    ])
  })
})
