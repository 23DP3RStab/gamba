import { api } from './api'
import { useAuthStore } from './stores/auth'

// The logged-in user's profile on the backend. Every call throws an Error with
// `status` and `fields` ({ field: message }) when it fails.

// { username, email, firstName, lastName, birthDate, phone }; details not filled in yet are null
export function getProfile() {
  return api('/profile')
}

export async function saveProfile(details) {
  const saved = await api('/profile', { method: 'PUT', body: details })
  useAuthStore().updateUser({ email: saved.email })
  return saved
}

export function changePassword(current, next) {
  return api('/profile/password', { method: 'PUT', body: { current, next } })
}

export async function deleteProfile() {
  await api('/profile', { method: 'DELETE' })
  useAuthStore().logout()
}

// { mines | roulette | piggies: { played, wins, wagered, won, net, biggestWin } }
export function getStats() {
  return api('/profile/stats')
}

// { dailyLossLimit, lostToday, leftToday, pendingLossLimit, pendingFrom, lockedUntil }.
// pendingFrom is set while a raised or removed limit waits out its 24 hours.
export function getLimits() {
  return api('/profile/limits')
}

// null removes the limit; lowering applies at once, raising or removing after 24 hours
export function setLossLimit(amount) {
  return api('/profile/limits', { method: 'PUT', body: { dailyLossLimit: amount } })
}

// days: 1, 7, 30 or 180; a lockout can be extended but not shortened
export function lockOut(days) {
  return api('/profile/lockout', { method: 'POST', body: { days } })
}

// When the self-lockout in `limits` ends, or null when it is over
export function activeLockout(limits) {
  const until = limits?.lockedUntil ? new Date(limits.lockedUntil) : null
  return until && until > new Date() ? until : null
}
