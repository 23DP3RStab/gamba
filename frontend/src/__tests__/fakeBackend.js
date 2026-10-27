import { vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../stores/auth'

const TOKEN = 'test-token'

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

// In-memory stand-in for the backend's /api/profile endpoints, answering like ApiResponse.
// Returns its state so tests can check what the "server" saved.
export function fakeBackend(profile, { password = '!Janka68', takenEmails = [] } = {}) {
  const state = { profile: { ...profile }, password, deleted: false }

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
