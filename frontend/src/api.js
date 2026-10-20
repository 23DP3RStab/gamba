// Calls to the GAMBA backend. In development Vite forwards /api to the Spring Boot
// server on port 8080 (see vite.config.js).
const TOKEN_KEY = 'gamba.token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

function apiError(status, message, fields) {
  const error = new Error(message)
  error.status = status
  error.fields = fields ?? {} // { field: message } for form fields the server rejected
  return error
}

// Sends a JSON request, with the logged-in user's token unless `auth` is false.
// Resolves to the response body (null when there is none); throws an apiError.
export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  const options = { method, headers }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    options.body = JSON.stringify(body)
  }
  const token = auth && getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(`/api${path}`, options)
  } catch {
    throw apiError(0, 'The server is not available, try again later')
  }

  const data = await response.json().catch(() => null)
  if (response.ok) return data
  if (data?.message) throw apiError(response.status, data.message, data.errors)
  if (response.status === 401) throw apiError(401, 'Your session has ended, please log in again')
  throw apiError(response.status, 'The server is not available, try again later')
}
