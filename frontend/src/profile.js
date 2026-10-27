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
