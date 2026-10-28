<script setup>
import { onMounted, ref } from 'vue'
import { getStats } from '../../profile'
import { formatMoney } from '../../format'

// Totals per game over the player's finished rounds, loaded from the server
const emit = defineEmits(['failed'])

const GAMES = [
  { key: 'mines', name: 'Mines' },
  { key: 'roulette', name: 'Roulette' },
  { key: 'piggies', name: '3 Piggies & The Wolf' },
  { key: 'crash', name: 'Crash' },
]

const stats = ref(null)
const error = ref('')

onMounted(async () => {
  try {
    stats.value = await getStats()
  } catch (e) {
    emit('failed', e)
    error.value = e.message
  }
})

const winRate = (game) => Math.round((game.wins / game.played) * 100)
const signed = (value) => (value > 0 ? '+' : '') + formatMoney(value)
</script>

<template>
  <p v-if="error" class="error" role="alert">{{ error }}</p>
  <p v-else-if="!stats" class="muted">Loading your stats…</p>
  <div v-else class="games">
    <div v-for="game in GAMES" :key="game.key" class="game">
      <h3 class="game-name">{{ game.name }}</h3>
      <p v-if="!stats[game.key].played" class="muted">Not played yet</p>
      <dl v-else>
        <div>
          <dt>Rounds</dt>
          <dd>{{ stats[game.key].played }}</dd>
        </div>
        <div>
          <dt>Wins</dt>
          <dd>{{ stats[game.key].wins }} ({{ winRate(stats[game.key]) }} %)</dd>
        </div>
        <div>
          <dt>Wagered</dt>
          <dd>{{ formatMoney(stats[game.key].wagered) }}</dd>
        </div>
        <div>
          <dt>Net</dt>
          <dd :class="stats[game.key].net >= 0 ? 'up' : 'down'">{{ signed(stats[game.key].net) }}</dd>
        </div>
        <div>
          <dt>Biggest win</dt>
          <dd>{{ formatMoney(stats[game.key].biggestWin) }}</dd>
        </div>
      </dl>
    </div>
  </div>
</template>

<style scoped>
.games {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.game {
  padding: 0.75rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: var(--color-background-soft);
}

.game-name {
  margin: 0 0 0.6rem;
  font-size: 0.95rem;
  color: var(--color-heading);
}

dl {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin: 0;
  font-size: 0.85rem;
}

dl div {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}

dt {
  opacity: 0.7;
}

dd {
  margin: 0;
  font-weight: bold;
  text-align: right;
}

.up {
  color: #5fd38d;
}

.down {
  color: #ff6b6b;
}

.muted {
  opacity: 0.7;
  font-size: 0.85rem;
}

@media (max-width: 560px) {
  .games {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
