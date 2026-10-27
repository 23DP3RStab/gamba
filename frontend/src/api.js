import { useAuthStore } from './stores/auth'

// Calls to the GAMBA backend, which answers with { success, message, data }
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

function apiError(status, message) {
  const error = new Error(message)
  error.status = status
  error.fields = {} // { field: message } for form fields the server rejected
  return error
}

// Sends a JSON request with the logged-in user's token.
// Resolves to the response's `data` (null when there is none); throws an apiError.
export async function api(path, { method = 'GET', body } = {}) {
  const headers = {}
  const options = { method, headers }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    options.body = JSON.stringify(body)
  }
  const { token } = useAuthStore()
  if (token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(`${API_BASE_URL}/api${path}`, options)
  } catch {
    throw apiError(0, 'Network error. Please verify the backend is running.')
  }

  const data = await response.json().catch(() => null)
  if (response.ok && data?.success !== false) return data?.data ?? null
  // Spring Security answers a missing or expired token with 401 or 403
  if (response.status === 401 || response.status === 403) {
    throw apiError(response.status, 'Your session has ended, please log in again')
  }
  throw apiError(response.status, data?.message || 'Something went wrong, please try again later')
}
