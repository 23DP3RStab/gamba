import { ref } from 'vue'
import { api, getToken, setToken } from './api'

// Accounts live on the backend. Registering or logging in returns a token that is
// sent with every later request; `user` keeps the name for the navbar.
const USER_KEY = 'gamba.user'

function readUser() {
  if (!getToken()) return null
  try {
    const stored = JSON.parse(localStorage.getItem(USER_KEY))
    return stored && typeof stored === 'object' ? stored : null
  } catch {
    return null
  }
}

// Logged-in user as { firstName, lastName, email }, or null
export const user = ref(readUser())

// Whether the login modal is showing
export const loginOpen = ref(false)

export function setUser(account) {
  if (account) {
    const { firstName, lastName, email } = account
    user.value = { firstName, lastName, email }
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  } else {
    user.value = null
    localStorage.removeItem(USER_KEY)
  }
}

function startSession({ token, user: account }) {
  setToken(token)
  setUser(account)
}

// Both throw an Error with `fields` ({ field: message }) when the server rejects the data
export async function register(account) {
  startSession(await api('/auth/register', { method: 'POST', body: account, auth: false }))
}

export async function login(email, password) {
  startSession(await api('/auth/login', { method: 'POST', body: { email, password }, auth: false }))
}

export function logout() {
  setToken(null)
  setUser(null)
}
