import { api } from './api'
import { useAuthStore } from './stores/auth'

// Crash, played live by everyone at once. The server runs the rounds and pushes every change
// over a stream; the page works out the rising multiplier itself from when the round took off,
// the same way as the backend's CrashRules: e^(0.00006·ms), cut to hundredths.

export const GROWTH_PER_MS = 0.00006
export const MIN_BET = 0.1
export const MIN_AUTO_CASHOUT = 1.01
export const MAX_MULTIPLIER = 1000

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const RETRY_MS = 2000

export function multiplierAt(ms) {
  const multiplier = Math.exp(GROWTH_PER_MS * Math.max(0, ms))
  return Math.min(Math.floor(multiplier * 100 + 1e-6) / 100, MAX_MULTIPLIER)
}

// How long the flight takes to reach `multiplier`
export function msUntil(multiplier) {
  return Math.ceil(Math.log(multiplier) / GROWTH_PER_MS)
}

// → { amount, autoCashout, balance }. autoCashout is optional.
export function placeCrashBet(amount, autoCashout = null) {
  return api('/games/crash/bet', { method: 'POST', body: { amount, autoCashout } })
}

// → { multiplier, payout, balance }
export function cashOutCrash() {
  return api('/games/crash/cashout', { method: 'POST' })
}

// Opens the live stream and calls onEvent(name, data) for each server event: "state", "bet" and
// "cashout". It also reports "open" when connected and "down" when the
// stream drops; it then reconnects by itself, except after "expired" (the login ran out).
// Read with fetch rather than EventSource, which can't send the login token.
// Returns a function that closes the stream.
export function connectCrashStream(onEvent) {
  let stopped = false
  let controller = null
  let retry = null

  function dispatch(block) {
    let name = 'message'
    const data = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) name = line.slice(6).trim()
      else if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
    }
    if (data.length) onEvent(name, JSON.parse(data.join('\n')))
  }

  async function connect() {
    controller = new AbortController()
    const { token } = useAuthStore()
    try {
      const response = await fetch(`${API_BASE_URL}/api/games/crash/stream`, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        signal: controller.signal,
      })
      if (response.status === 401 || response.status === 403) {
        onEvent('expired')
        return
      }
      if (!response.ok || !response.body) throw new Error(`Stream refused (${response.status})`)
      onEvent('open')

      const reader = response.body.pipeThrough(new TextDecoderStream()).getReader()
      let buffer = ''
      for (;;) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += value.replace(/\r\n/g, '\n')
        let end
        while ((end = buffer.indexOf('\n\n')) >= 0) {
          dispatch(buffer.slice(0, end))
          buffer = buffer.slice(end + 2)
        }
      }
    } catch {
      // Dropped, refused or closed by us; reconnect below unless closed
    }
    if (stopped) return
    onEvent('down')
    retry = setTimeout(connect, RETRY_MS)
  }

  connect()
  return () => {
    stopped = true
    clearTimeout(retry)
    controller?.abort()
  }
}
