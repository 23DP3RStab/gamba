import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { rules } from '../validation'

// Test IDs (V-xx) match the tables in docs/testesana.md

describe('Vārda / uzvārda lauks', () => {
  it('V-01 tukšs vārds → jāaizpilda', () => {
    expect(rules.firstName('')).toBe('First name is required')
  })
  it('V-02 tikai atstarpes → jāaizpilda', () => {
    expect(rules.lastName('   ')).toBe('Last name is required')
  })
  it('V-03 30 simboli (robeža) → atļauts', () => {
    expect(rules.firstName('A'.repeat(30))).toBe('')
  })
  it('V-04 31 simbols → kļūda', () => {
    expect(rules.firstName('A'.repeat(31))).toBe('First name can be at most 30 characters')
  })
})

describe('E-pasta lauks', () => {
  it('V-05 tukšs e-pasts → jāaizpilda', () => {
    expect(rules.email('')).toBe('E-mail is required')
  })
  it('V-06 "abc.lv" bez @ → kļūda', () => {
    expect(rules.email('abc.lv')).toBe('Enter a valid e-mail address')
  })
  it('V-07 "a@b" bez domēna → kļūda', () => {
    expect(rules.email('a@b')).toBe('Enter a valid e-mail address')
  })
  it('V-08 40 simboli (robeža) → atļauts', () => {
    const email = 'a'.repeat(31) + '@gmail.lv'
    expect(email).toHaveLength(40)
    expect(rules.email(email)).toBe('')
  })
  it('V-09 41 simbols → kļūda', () => {
    expect(rules.email('a'.repeat(32) + '@gmail.lv')).toBe('E-mail can be at most 40 characters')
  })
})

describe('Tālruņa lauks', () => {
  it('V-10 7 cipari → kļūda', () => {
    expect(rules.phone('2123456')).toBe('Phone number must be 8 digits')
  })
  it('V-11 8 cipari → atļauts', () => {
    expect(rules.phone('21234567')).toBe('')
  })
  it('V-12 9 cipari → kļūda', () => {
    expect(rules.phone('212345678')).toBe('Phone number must be 8 digits')
  })
  it('V-13 burti → kļūda', () => {
    expect(rules.phone('abcdefgh')).toBe('Phone number must be 8 digits')
  })
})

describe('Paroles lauks', () => {
  const message = '8–64 characters with upper and lower case letters, a digit and one of @#$%^&+=!'
  it('V-14 "1234567" (7 simboli) → kļūda', () => {
    expect(rules.password('1234567')).toBe(message)
  })
  it('V-15 "!Janka6" (7 simboli, visi tipi) → kļūda', () => {
    expect(rules.password('!Janka6')).toBe(message)
  })
  it('V-16 "Janka123" (bez simbola) → kļūda', () => {
    expect(rules.password('Janka123')).toBe(message)
  })
  it('V-17 "!janka68" (bez lielā burta) → kļūda', () => {
    expect(rules.password('!janka68')).toBe(message)
  })
  it('V-18 "!Janka68" (8 simboli) → atļauts', () => {
    expect(rules.password('!Janka68')).toBe('')
  })
  it('V-19 "*Janka68" (simbols, ko backend nepieņem) → kļūda', () => {
    expect(rules.password('*Janka68')).toBe(message)
  })
  it('V-20 65 simboli → kļūda', () => {
    expect(rules.password('!Janka68' + 'a'.repeat(57))).toBe(message)
  })
})

describe('Dzimšanas datums (šodien 2026-10-21)', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 9, 21, 12))
  })
  afterEach(() => vi.useRealTimers())

  it('V-19 tukšs datums → jāaizpilda', () => {
    expect(rules.birthDate('')).toBe('Date of birth is required')
  })
  it('V-20 rītdienas datums → nākotnē', () => {
    expect(rules.birthDate('2026-10-22')).toBe('Date of birth cannot be in the future')
  })
  it('V-21 šodien dzimis → jaunāks par 18', () => {
    expect(rules.birthDate('2026-10-21')).toBe('You must be at least 18 years old')
  })
  it('V-22 18 gadi bez vienas dienas (2008-10-22) → kļūda', () => {
    expect(rules.birthDate('2008-10-22')).toBe('You must be at least 18 years old')
  })
  it('V-23 tieši 18 gadi šodien (2008-10-21) → atļauts', () => {
    expect(rules.birthDate('2008-10-21')).toBe('')
  })
  it('V-24 pieaugušais (1990-05-15) → atļauts', () => {
    expect(rules.birthDate('1990-05-15')).toBe('')
  })
  it('V-25 dzimis 29. februārī: 18 gadi tikai no 1. marta', () => {
    vi.setSystemTime(new Date(2026, 1, 28, 12))
    expect(rules.birthDate('2008-02-29')).toBe('You must be at least 18 years old')
    vi.setSystemTime(new Date(2026, 2, 1, 12))
    expect(rules.birthDate('2008-02-29')).toBe('')
  })
})
