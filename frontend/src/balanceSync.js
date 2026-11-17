import { api } from './api'
import { useAuthStore } from './stores/auth'

// The balance on every page comes from the auth store, which used to change only when this
// page placed a bet or used the wallet. Money that arrives any other way (an admin adjustment,
// a payment in another tab) now shows up too: the balance is reloaded from the server on every
// page change, when the tab comes back into view, and every 15 s.
// Game pages are only reloaded when opened: Roulette and Piggies hold the new balance back until
// the wheel or the wolf has finished, and a reload mid-round would give the result away early.

const POLL_MS = 15000

export async function refreshBalance() {
  const authStore = useAuthStore()
  if (!authStore.isAuthenticated) return
  const version = authStore.balanceVersion
  try {
    const { balance } = await api('/profile/balance')
    // A bet or payout answered while this was on its way is newer; keep that one
    if (authStore.balanceVersion === version) authStore.updateBalance(balance)
  } catch {
    // Offline or logged out; the next reload tries again
  }
}

// Keeps the shown balance up to date while the app runs. Returns a function that stops it.
export function syncBalance(router) {
  const notInGame = () => !router.currentRoute.value.meta.game
  const removeHook = router.afterEach(() => refreshBalance())
  const onVisible = () => {
    if (!document.hidden && notInGame()) refreshBalance()
  }
  document.addEventListener('visibilitychange', onVisible)
  const timer = setInterval(onVisible, POLL_MS)

  return () => {
    removeHook()
    document.removeEventListener('visibilitychange', onVisible)
    clearInterval(timer)
  }
}
