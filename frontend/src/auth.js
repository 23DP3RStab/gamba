import { ref } from 'vue'

// Placeholder auth: accounts live in this browser's localStorage and passwords
// are stored in plain text. Replace with real backend calls before going live.
const USER_KEY = 'gamba.user'
const USERS_KEY = 'gamba.users'

function readJson(key, fallback) {
  try {
    return JSON.parse(localStorage.getItem(key)) ?? fallback
  } catch {
    return fallback
  }
}

function readUsers() {
  return readJson(USERS_KEY, [])
}

function readUser() {
  const stored = readJson(USER_KEY, null)
  return stored && typeof stored === 'object' ? stored : null
}

// Logged-in user as { firstName, lastName, email }, or null
export const user = ref(readUser())

// Whether the login modal is showing
export const loginOpen = ref(false)

function setUser(account) {
  if (account) {
    const { firstName, lastName, email } = account
    user.value = { firstName, lastName, email }
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  } else {
    user.value = null
    localStorage.removeItem(USER_KEY)
  }
}

function fieldError(field, message) {
  const error = new Error(message)
  error.field = field
  return error
}

export function register(account) {
  const users = readUsers()
  const email = account.email.toLowerCase()
  if (users.some((u) => u.email === email)) {
    throw fieldError('email', 'This e-mail is already registered')
  }
  const saved = { ...account, email }
  users.push(saved)
  localStorage.setItem(USERS_KEY, JSON.stringify(users))
  setUser(saved)
}

export function login(email, password) {
  const match = readUsers().find(
    (u) => u.email === email.toLowerCase() && u.password === password,
  )
  if (!match) {
    throw new Error('Wrong e-mail or password')
  }
  setUser(match)
}

export function logout() {
  setUser(null)
}
