import { vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../stores/auth'
import { settle } from '../roulette'
import { HOUSES } from '../piggies'

const TOKEN = 'test-token'
const cents = (value) => Math.round(value * 100) / 100

// Fresh store with a logged-in player, the way LoginForm leaves it
export function logIn() {
  setActivePinia(createPinia())
  const authStore = useAuthStore()
  authStore.setAuth({
    token: TOKEN,
    userId: '1',
    username: 'janis',
    email: 'janis@example.lv',
    role: 'PLAYER',
    balance: 0,
    currency: 'EUR',
  })
  return authStore
}

const noLimits = {
  dailyLossLimit: null,
  lostToday: 0,
  leftToday: null,
  pendingLossLimit: null,
  pendingFrom: null,
  lockedUntil: null,
}

const unplayed = { played: 0, wins: 0, wagered: 0, won: 0, net: 0, biggestWin: 0 }

// In-memory stand-in for the backend's /api/profile and /api/games endpoints, answering like
// ApiResponse. Returns its state so tests can check what the "server" saved, and steer it:
// `number` is where the next roulette spin lands, `blows` says whether each wolf blow topples,
// `crashAt` is the Crash cash-out multiplier.
export function fakeBackend(profile, { password = '!Janka68', takenEmails = [], balance = 0 } = {}) {
  const state = {
    profile: { ...profile },
    password,
    deleted: false,
    balance,
    limits: { ...noLimits },
    stats: {
      mines: { ...unplayed },
      roulette: { ...unplayed },
      piggies: { ...unplayed },
      crash: { ...unplayed },
    },
    number: 0,
    blows: [],
    piggies: null, // the round in progress
    crashBet: null, // the bet on the current Crash round
    crashAt: 2,
  }
  const lockedOut = () => state.limits.lockedUntil && new Date(state.limits.lockedUntil) > new Date()

  const reply = (status, body) => ({ ok: status < 300, status, json: async () => body })
  const ok = (data) => reply(200, { success: true, message: 'ok', data })
  const fail = (status, message) => reply(status, { success: false, message, data: null })

  const fetch = vi.fn(async (url, { method = 'GET', headers = {}, body } = {}) => {
    // Spring Security answers a missing or unknown token with an empty 403
    if (headers.Authorization !== `Bearer ${TOKEN}` || state.deleted) {
      return { ok: false, status: 403, json: async () => Promise.reject(new SyntaxError()) }
    }
    const path = url.slice(url.indexOf('/api'))
    const data = body && JSON.parse(body)

    if (path === '/api/profile' && method === 'GET') return ok(state.profile)
    if (path === '/api/profile' && method === 'PUT') {
      const email = data.email.toLowerCase()
      if (takenEmails.includes(email)) return fail(400, 'Email is already registered')
      state.profile = { ...state.profile, ...data, email }
      return ok(state.profile)
    }
    if (path === '/api/profile/password' && method === 'PUT') {
      if (data.current !== state.password) return fail(400, 'Current password is wrong')
      state.password = data.next
      return ok(null)
    }
    if (path === '/api/profile' && method === 'DELETE') {
      state.deleted = true
      return ok(null)
    }

    if (path === '/api/profile/balance') return ok({ balance: state.balance, currency: 'EUR' })
    if (path === '/api/profile/stats') return ok(state.stats)
    if (path === '/api/profile/limits' && method === 'GET') return ok({ ...state.limits })
    if (path === '/api/profile/limits' && method === 'PUT') {
      const limit = data.dailyLossLimit
      const current = state.limits.dailyLossLimit
      if (limit !== null && (current === null || limit <= current)) {
        Object.assign(state.limits, {
          dailyLossLimit: limit,
          leftToday: limit - state.limits.lostToday,
          pendingLossLimit: null,
          pendingFrom: null,
        })
      } else {
        // Raising or removing waits 24 hours
        Object.assign(state.limits, {
          pendingLossLimit: limit,
          pendingFrom: new Date(Date.now() + 86400000).toISOString(),
        })
      }
      return ok({ ...state.limits })
    }
    if (path === '/api/profile/lockout' && method === 'POST') {
      state.limits.lockedUntil = new Date(Date.now() + data.days * 86400000).toISOString()
      return ok({ ...state.limits })
    }

    if (path.startsWith('/api/games/') && method === 'POST' && lockedOut()) {
      return fail(400, 'Your account is locked')
    }
    if (path === '/api/games/roulette/spin' && method === 'POST') {
      const totalBet = Object.values(data.bets).reduce((sum, amount) => sum + amount, 0)
      if (totalBet > state.balance) return fail(400, 'Insufficient balance')
      const { returned } = settle(data.bets, state.number)
      state.balance += returned - totalBet
      return ok({ number: state.number, totalBet, payout: returned, balance: state.balance })
    }

    if (path === '/api/games/crash/bet' && method === 'POST') {
      if (state.crashBet) return fail(400, 'You already have a bet on this round')
      if (data.amount > state.balance) return fail(400, 'Insufficient balance')
      state.balance -= data.amount
      state.crashBet = { ...data }
      return ok({ ...data, balance: state.balance })
    }
    if (path === '/api/games/crash/cashout' && method === 'POST') {
      if (!state.crashBet) return fail(400, 'You have no bet on this round')
      const payout = cents(state.crashBet.amount * state.crashAt)
      state.balance += payout
      state.crashBet = null
      return ok({ multiplier: state.crashAt, payout, balance: state.balance })
    }

    const piggies = (fell) => ok({ ...state.piggies, fell, balance: state.balance })
    const endPiggies = (status, payout) => {
      Object.assign(state.piggies, { status, payout })
      state.balance += payout
    }
    const blow = () => {
      const fell = state.blows.shift() ?? false
      if (!fell) endPiggies('LOST', 0)
      else if (++state.piggies.housesDown === HOUSES.length) {
        endPiggies('WON', cents(state.piggies.betAmount * HOUSES.at(-1).multiplier))
      }
      const reply = piggies(fell)
      if (state.piggies.status !== 'IN_PROGRESS') state.piggies = null
      return reply
    }
    if (path === '/api/games/piggies/active') return ok(state.piggies && { ...state.piggies, fell: null })
    if (path === '/api/games/piggies/start' && method === 'POST') {
      if (state.piggies) return fail(400, 'You already have a round in progress')
      if (data.betAmount > state.balance) return fail(400, 'Insufficient balance')
      state.balance -= data.betAmount
      state.piggies = { betAmount: data.betAmount, housesDown: 0, status: 'IN_PROGRESS', payout: 0 }
      return blow()
    }
    if (path === '/api/games/piggies/blow' && method === 'POST') return blow()
    if (path === '/api/games/piggies/cashout' && method === 'POST') {
      const { betAmount, housesDown } = state.piggies
      endPiggies('CASHED_OUT', cents(betAmount * HOUSES[housesDown - 1].multiplier))
      const reply = piggies(null)
      state.piggies = null
      return reply
    }
    return fail(404, 'Not found')
  })

  vi.stubGlobal('fetch', fetch)
  return state
}

// A profile as the backend returns it for a user who just registered
export const newUser = {
  username: 'janis',
  email: 'janis@example.lv',
  firstName: null,
  lastName: null,
  birthDate: null,
  phone: null,
}

export const filledIn = {
  ...newUser,
  firstName: 'Jānis',
  lastName: 'Bērziņš',
  birthDate: '1995-04-12',
  phone: '21234567',
}
