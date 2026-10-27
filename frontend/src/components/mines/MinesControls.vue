<script setup>
import { computed } from 'vue';
import BaseInput from '../common/BaseInput.vue';
import BaseButton from '../common/BaseButton.vue';

const props = defineProps({
  betAmount: { type: [Number, String], required: true },
  mineCount: { type: Number, required: true },
  isPlaying: { type: Boolean, default: false },
  isLoading: { type: Boolean, default: false },
  currentMultiplier: { type: Number, default: 1.0 },
  nextMultiplier: { type: Number, default: null },
  revealedCount: { type: Number, default: 0 },
  userBalance: { type: Number, default: 0 }
});

const emit = defineEmits([
  'update:betAmount',
  'update:mineCount',
  'start',
  'cashout'
]);

const cashoutAmount = computed(() => {
  const bet = parseFloat(props.betAmount) || 0;
  return (bet * props.currentMultiplier).toFixed(2);
});

const adjustBet = (multiplier) => {
  if (props.isPlaying) return;
  const current = parseFloat(props.betAmount) || 0;
  let next = current * multiplier;
  if (multiplier === 'MAX') next = props.userBalance;
  emit('update:betAmount', Math.max(0.1, Number(next.toFixed(2))));
};
</script>

<template>
  <div class="mines-controls">
    <div class="control-block">
      <BaseInput
        id="bet-amount"
        label="Bet Amount"
        type="number"
        step="0.10"
        :model-value="String(betAmount)"
        :disabled="isPlaying"
        @update:model-value="emit('update:betAmount', $event)"
      />
      <div class="quick-bet-row" :class="{ disabled: isPlaying }">
        <button type="button" @click="adjustBet(0.5)">½</button>
        <button type="button" @click="adjustBet(2)">2×</button>
        <button type="button" @click="adjustBet('MAX')">MAX</button>
      </div>
    </div>

    <div class="control-block">
      <label class="block-label">Mines</label>
      <div class="mines-select-wrapper">
        <select
          :value="mineCount"
          :disabled="isPlaying"
          class="mines-select"
          @change="emit('update:mineCount', Number($event.target.value))"
        >
          <option v-for="n in 24" :key="n" :value="n">
            {{ n }} {{ n === 1 ? 'Mine' : 'Mines' }}
          </option>
        </select>
      </div>
    </div>

    <div v-if="isPlaying" class="stats-preview">
      <div class="stat-row">
        <span>Current Win:</span>
        <strong class="gold-text">{{ cashoutAmount }} EUR ({{ Number(currentMultiplier).toFixed(2) }}x)</strong>
      </div>
      <div v-if="nextMultiplier" class="stat-row">
        <span>Next Tile:</span>
        <span class="next-gold">{{ Number(nextMultiplier).toFixed(2) }}×</span>
      </div>
    </div>

    <div class="action-block">
      <BaseButton
        v-if="!isPlaying"
        :loading="isLoading"
        @click="emit('start')"
      >
        Bet
      </BaseButton>

      <button
        v-else
        class="cashout-button"
        :disabled="revealedCount === 0 || isLoading"
        @click="emit('cashout')"
      >
        <span v-if="revealedCount === 0">Pick a Tile</span>
        <span v-else>Cash Out {{ cashoutAmount }} EUR</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.mines-controls {
  background: #121217;
  border: 1px solid rgba(212, 175, 55, 0.25);
  border-radius: 14px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.control-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.block-label {
  font-size: 0.85rem;
  font-weight: 500;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #c5a059;
}

.quick-bet-row {
  display: flex;
  gap: 6px;
}

.quick-bet-row button {
  flex: 1;
  background: #181822;
  border: 1px solid rgba(212, 175, 55, 0.2);
  color: #d4af37;
  font-weight: 700;
  border-radius: 6px;
  padding: 8px 0;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-bet-row button:hover {
  background: rgba(212, 175, 55, 0.2);
}

.quick-bet-row.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.mines-select-wrapper {
  background: #141417;
  border: 1px solid rgba(197, 160, 89, 0.25);
  border-radius: 8px;
  overflow: hidden;
}

.mines-select {
  width: 100%;
  padding: 12px 16px;
  background-color: #141417;
  color: #f1f1f1;
  border: none;
  outline: none;
  font-size: 0.95rem;
  cursor: pointer;
  color-scheme: dark;
}

.mines-select option {
  background-color: #141417;
  color: #f1f1f1;
  padding: 8px 12px;
}

.mines-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.stats-preview {
  background: #0d0d12;
  border: 1px solid rgba(212, 175, 55, 0.15);
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 0.9rem;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  color: #a0a0ab;
}

.gold-text {
  color: #d4af37;
}

.next-gold {
  color: #c5a059;
}

.cashout-button {
  width: 100%;
  padding: 14px 20px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #26a69a 0%, #00796b 100%);
  color: #ffffff;
  font-weight: 700;
  font-size: 1rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 15px rgba(38, 166, 154, 0.3);
}

.cashout-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #4db6ac 0%, #00897b 100%);
  box-shadow: 0 6px 20px rgba(38, 166, 154, 0.45);
  transform: translateY(-1px);
}

.cashout-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
