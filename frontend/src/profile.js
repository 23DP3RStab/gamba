import { api } from './api'
import { logout, setUser } from './auth'

// The logged-in user's profile on the backend. Every call throws an Error with
// `status` and `fields` ({ field: message }) when it fails.

// { firstName, lastName, email, phone, birthDate }
export function getProfile() {
  return api('/profile')
}

export async function saveProfile(details) {
  const saved = await api('/profile', { method: 'PUT', body: details })
  setUser(saved) // the navbar shows the new name
  return saved
}

export function changePassword(current, next) {
  return api('/profile/password', { method: 'PUT', body: { current, next } })
}

export async function deleteProfile() {
  await api('/profile', { method: 'DELETE' })
  logout()
}
