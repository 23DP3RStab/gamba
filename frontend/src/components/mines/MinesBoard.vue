<script setup>
const props = defineProps({
  isPlaying: { type: Boolean, default: false },
  revealedTiles: { type: Array, default: () => [] },
  minePositions: { type: Array, default: () => null },
  isBusted: { type: Boolean, default: false }
});

const emit = defineEmits(['reveal']);

const getTileState = (index) => {
  if (props.revealedTiles.includes(index)) {
    if (props.minePositions && props.minePositions.includes(index)) {
      return 'exploded-mine';
    }
    return 'gem';
  }

  if (props.minePositions) {
    if (props.minePositions.includes(index)) {
      return 'hidden-mine';
    }
    return 'dimmed-gem';
  }

  return 'unrevealed';
};

const handleTileClick = (index) => {
  if (!props.isPlaying || props.revealedTiles.includes(index)) return;
  emit('reveal', index);
};
</script>

<template>
  <div class="mines-grid" :class="{ disabled: !isPlaying }">
    <button
      v-for="index in 25"
      :key="index - 1"
      class="mine-tile"
      :class="getTileState(index - 1)"
      :disabled="!isPlaying || revealedTiles.includes(index - 1)"
      @click="handleTileClick(index - 1)"
    >
      <div class="tile-inner">
        <span v-if="getTileState(index - 1) === 'gem'" class="symbol gem">💎</span>

        <span v-else-if="getTileState(index - 1) === 'exploded-mine'" class="symbol exploded">💥</span>

        <span v-else-if="getTileState(index - 1) === 'dimmed-gem'" class="symbol dimmed">💎</span>

        <span v-else-if="getTileState(index - 1) === 'hidden-mine'" class="symbol mine">💣</span>
      </div>
    </button>
  </div>
</template>

<style scoped>
.mines-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  background: #111116;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid rgba(212, 175, 55, 0.2);
  aspect-ratio: 1 / 1;
  max-width: 540px;
  min-width: 280px;
  width: 100%;
  box-sizing: border-box;
}

.mine-tile {
  width: 100%;
  aspect-ratio: 1 / 1;
  min-width: 0;
  min-height: 0;
  background: #181822;
  border: 1px solid rgba(212, 175, 55, 0.2);
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all 0.2s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.tile-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.mine-tile:hover:not(:disabled) {
  background: #242434;
  border-color: #d4af37;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.25);
}

.mine-tile.gem {
  background: radial-gradient(circle, #213a2d 0%, #101c16 100%);
  border-color: #26a69a;
  box-shadow: 0 0 15px rgba(38, 166, 154, 0.4);
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.mine-tile.exploded-mine {
  background: radial-gradient(circle, #4a1215 0%, #20080a 100%);
  border-color: #e63946;
  box-shadow: 0 0 20px rgba(230, 57, 70, 0.6);
  animation: shake 0.4s ease;
}

.mine-tile.hidden-mine {
  background: #14141a;
  border-color: rgba(230, 57, 70, 0.3);
  opacity: 0.6;
}

.mine-tile.dimmed-gem {
  background: #14141a;
  border-color: rgba(38, 166, 154, 0.3);
  opacity: 0.4;
}

.symbol {
  font-size: 1.8rem;
}

@keyframes popIn {
  from { transform: scale(0.6); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

@media (max-width: 600px) {
  .mines-grid {
    gap: 8px;
    padding: 14px;
  }
  .symbol {
    font-size: 1.3rem;
  }
}
</style>
