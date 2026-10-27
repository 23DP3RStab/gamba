<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import MinesControls from '../components/mines/MinesControls.vue';
import MinesBoard from '../components/mines/MinesBoard.vue';
import { formatDateTime } from '../format';
import { useLockout } from '../lockout';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const router = useRouter();
const authStore = useAuthStore();
const lockedUntil = useLockout();

const betAmount = ref(1.0);
const mineCount = ref(3);
const isPlaying = ref(false);
const isLoading = ref(false);
const errorMessage = ref('');
const bannerMessage = ref('');

const revealedTiles = ref([]);
const minePositions = ref(null);
const currentMultiplier = ref(1.0);
const nextMultiplier = ref(null);
const isBusted = ref(false);

const checkActiveGame = async () => {
  const token = localStorage.getItem('token');
  try {
    const res = await fetch(`${API_BASE_URL}/api/games/mines/active`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    const data = await res.json();
    if (data.success && data.data) {
      applyGameState(data.data);
    }
  } catch (err) {
    console.error('Failed to load active game:', err);
  }
};

const handleStart = async () => {
  if (lockedUntil.value) return;
  errorMessage.value = '';
  bannerMessage.value = '';
  isLoading.value = true;
  minePositions.value = null;
  isBusted.value = false;

  const token = localStorage.getItem('token');
  try {
    const res = await fetch(`${API_BASE_URL}/api/games/mines/start`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        betAmount: Number(betAmount.value),
        mineCount: mineCount.value
      })
    });

    const data = await res.json();
    if (!res.ok || !data.success) {
      errorMessage.value = data.message || 'Failed to start game';
    } else {
      applyGameState(data.data);
      authStore.updateBalance(data.data.balance);
    }
  } catch (err) {
    errorMessage.value = 'Network error while contacting game server.';
  } finally {
    isLoading.value = false;
  }
};

const handleReveal = async (index) => {
  if (!isPlaying.value) return;
  errorMessage.value = '';

  const token = localStorage.getItem('token');
  try {
    const res = await fetch(`${API_BASE_URL}/api/games/mines/reveal`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({ tileIndex: index })
    });

    const data = await res.json();
    if (data.success && data.data) {
      applyGameState(data.data);
      if (data.data.balance !== undefined) {
        authStore.updateBalance(data.data.balance);
      }
    } else {
      errorMessage.value = data.message || 'Action failed';
    }
  } catch (err) {
    errorMessage.value = 'Failed to reveal tile';
  }
};

const handleCashout = async () => {
  if (!isPlaying.value || revealedTiles.value.length === 0) return;
  errorMessage.value = '';

  const token = localStorage.getItem('token');
  try {
    const res = await fetch(`${API_BASE_URL}/api/games/mines/cashout`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` }
    });

    const data = await res.json();
    if (data.success && data.data) {
      applyGameState(data.data);
      authStore.updateBalance(data.data.balance);
    }
  } catch (err) {
    errorMessage.value = 'Failed to cash out';
  }
};

const applyGameState = (game) => {
  betAmount.value = game.betAmount;
  mineCount.value = game.mineCount;
  revealedTiles.value = game.revealedTiles || [];
  currentMultiplier.value = game.currentMultiplier || 1.0;
  nextMultiplier.value = game.nextMultiplier || null;

  if (game.status === 'IN_PROGRESS') {
    isPlaying.value = true;
    minePositions.value = null;
    isBusted.value = false;
  } else if (game.status === 'BUSTED') {
    isPlaying.value = false;
    isBusted.value = true;
    minePositions.value = game.minePositions;
    bannerMessage.value = '💥 BUSTED! Better luck next round.';
  } else if (game.status === 'CASHED_OUT') {
    isPlaying.value = false;
    isBusted.value = false;
    minePositions.value = game.minePositions;
    bannerMessage.value = `🎉 CASHED OUT! Won ${Number(game.payout).toFixed(2)} EUR (${Number(game.currentMultiplier).toFixed(2)}x)`;
  }
};

onMounted(() => {
  checkActiveGame();
});
</script>

<template>
  <div class="mines-view">
    <header class="game-header">
      <div class="header-left">
        <button class="back-btn" @click="router.push('/lobby')">← Lobby</button>
        <span class="game-title">MINES</span>
      </div>

      <div class="wallet-badge">
        <span class="label">Balance</span>
        <span class="amount">
          {{ Number(authStore.user?.balance || 0).toFixed(2) }} {{ authStore.user?.currency || 'EUR' }}
        </span>
      </div>
    </header>

    <div v-if="bannerMessage" class="game-banner" :class="{ bust: isBusted, win: !isBusted }">
      {{ bannerMessage }}
    </div>

    <div v-if="lockedUntil" class="error-banner" role="alert">
      Your account is locked until {{ formatDateTime(lockedUntil) }}. You can't play until then.
    </div>

    <div v-if="errorMessage" class="error-banner">
      {{ errorMessage }}
    </div>

    <main class="game-arena">
      <div class="controls-column">
        <MinesControls
          v-model:betAmount="betAmount"
          v-model:mineCount="mineCount"
          :isPlaying="isPlaying"
          :isLoading="isLoading"
          :currentMultiplier="currentMultiplier"
          :nextMultiplier="nextMultiplier"
          :revealedCount="revealedTiles.length"
          :userBalance="Number(authStore.user?.balance || 0)"
          @start="handleStart"
          @cashout="handleCashout"
        />
      </div>

      <div class="board-column">
        <MinesBoard
          :isPlaying="isPlaying"
          :revealedTiles="revealedTiles"
          :minePositions="minePositions"
          :isBusted="isBusted"
          @reveal="handleReveal"
        />
      </div>
    </main>
  </div>
</template>

<style scoped>
.mines-view {
  min-height: 100vh;
  background: radial-gradient(circle at 50% 10%, #16161f 0%, #09090c 100%);
  color: #f1f1f1;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  display: flex;
  flex-direction: column;
}

.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 36px;
  background: #111116;
  border-bottom: 1px solid rgba(212, 175, 55, 0.25);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.back-btn {
  background: transparent;
  border: 1px solid rgba(212, 175, 55, 0.3);
  color: #d4af37;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(212, 175, 55, 0.15);
}

.game-title {
  font-size: 1.25rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: #ffffff;
}

.wallet-badge {
  display: flex;
  flex-direction: column;
  background: rgba(212, 175, 55, 0.08);
  border: 1px solid rgba(212, 175, 55, 0.3);
  padding: 6px 14px;
  border-radius: 8px;
  text-align: right;
}

.wallet-badge .label {
  font-size: 0.7rem;
  text-transform: uppercase;
  color: #c5a059;
}

.wallet-badge .amount {
  font-weight: 700;
  color: #d4af37;
  font-size: 0.95rem;
}

.game-banner {
  text-align: center;
  padding: 14px;
  font-weight: 700;
  font-size: 1rem;
}

.game-banner.win {
  background: rgba(38, 166, 154, 0.2);
  color: #4db6ac;
  border-bottom: 1px solid rgba(38, 166, 154, 0.4);
}

.game-banner.bust {
  background: rgba(230, 57, 70, 0.2);
  color: #ff6b6b;
  border-bottom: 1px solid rgba(230, 57, 70, 0.4);
}

.error-banner {
  background: rgba(230, 57, 70, 0.15);
  color: #ff6b6b;
  text-align: center;
  padding: 10px;
  font-size: 0.9rem;
}

.game-arena {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 36px;
  padding: 40px 24px;
  max-width: 1040px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.controls-column {
  width: 320px;
}

.board-column {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  max-width: 540px;
}

@media (max-width: 840px) {
  .game-arena {
    flex-direction: column-reverse;
    gap: 24px;
    padding: 20px 16px;
  }
  .controls-column {
    width: 100%;
    max-width: 540px;
  }
  .board-column {
    width: 100%;
    max-width: 540px;
  }
}
</style>
