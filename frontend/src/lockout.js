import { onMounted, ref } from 'vue'
import { activeLockout, getLimits } from './profile'

// When the player's self-lockout ends, loaded as a game opens; null while they may play.
// The server refuses bets during a lockout anyway, this only tells the player why up front.
export function useLockout() {
  const until = ref(null)
  onMounted(async () => {
    try {
      until.value = activeLockout(await getLimits())
    } catch {
      // Without the limits the game still works; the server has the last word
    }
  })
  return until
}
