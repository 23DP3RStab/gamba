// Standalone profile storage. It does not depend on login (auth.js): the profile
// is kept on its own in this browser's localStorage, password in plain text.
// When the real login exists, replace these functions with backend calls for
// the logged-in user; ProfileView only talks to this file.
const PROFILE_KEY = 'gamba.profile'

const EMPTY = { firstName: '', lastName: '', email: '', phone: '', birthDate: '' }

function read() {
  try {
    return JSON.parse(localStorage.getItem(PROFILE_KEY)) ?? {}
  } catch {
    return {}
  }
}

function write(profile) {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

// The stored profile details, or empty fields when nothing is saved yet
export function getProfile() {
  const profile = { ...EMPTY, ...read() }
  delete profile.password
  return profile
}

export function hasPassword() {
  return Boolean(read().password)
}

export function saveProfile(details) {
  write({ ...read(), ...details })
}

export function changePassword(current, next) {
  const profile = read()
  if (profile.password && profile.password !== current) {
    const error = new Error('Current password is wrong')
    error.field = 'current'
    throw error
  }
  write({ ...profile, password: next })
}

export function deleteProfile() {
  localStorage.removeItem(PROFILE_KEY)
}
