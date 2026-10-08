<script setup>
import CasinoChip from './CasinoChip.vue'
import {
  BETS,
  COLUMN_BETS,
  DOZEN_BETS,
  EVEN_MONEY_BETS,
  LINE_BETS,
  TABLE_ROWS,
  colorOf,
} from '../roulette'

defineProps({
  // Chips on the table as { betKey: amount }
  placed: { type: Object, required: true },
  // Last winning number, highlighted on the table
  winning: { type: Number, default: null },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['place'])

const CHIP_SIZE = '1.9rem'

function lineStyle(b) {
  return { left: `${(b.x / 12) * 100}%`, top: `${(b.y / 3) * 100}%` }
}
</script>

<template>
  <div class="table-scroll">
    <div class="frame">
      <div class="table" :class="{ disabled }">
        <button
          type="button"
          class="cell zero green"
          :class="{ win: winning === 0 }"
          :disabled="disabled"
          :aria-label="BETS.get('n0').label"
          @click="emit('place', 'n0')"
        >
          <span class="num">0</span>
          <CasinoChip v-if="placed.n0" class="placed" :value="placed.n0" :size="CHIP_SIZE" />
        </button>

        <div class="numbers">
          <template v-for="row in TABLE_ROWS" :key="row[0]">
            <button
              v-for="n in row"
              :key="n"
              type="button"
              class="cell"
              :class="{ win: winning === n }"
              :disabled="disabled"
              :aria-label="BETS.get(`n${n}`).label"
              @click="emit('place', `n${n}`)"
            >
              <span class="num" :class="colorOf(n)">{{ n }}</span>
              <CasinoChip
                v-if="placed[`n${n}`]"
                class="placed"
                :value="placed[`n${n}`]"
                :size="CHIP_SIZE"
              />
            </button>
          </template>

          <button
            v-for="b in LINE_BETS"
            :key="b.key"
            type="button"
            class="spot"
            :class="{ filled: placed[b.key] }"
            :style="lineStyle(b)"
            :disabled="disabled"
            :aria-label="b.label"
            :title="b.label"
            @click="emit('place', b.key)"
          >
            <CasinoChip
              v-if="placed[b.key]"
              class="placed"
              :value="placed[b.key]"
              :size="CHIP_SIZE"
            />
          </button>
        </div>

        <button
          v-for="(b, i) in COLUMN_BETS"
          :key="b.key"
          type="button"
          class="cell outside column"
          :style="{ gridRow: i + 1 }"
          :disabled="disabled"
          :aria-label="b.label"
          @click="emit('place', b.key)"
        >
          2 to 1
          <CasinoChip v-if="placed[b.key]" class="placed" :value="placed[b.key]" :size="CHIP_SIZE" />
        </button>

        <button
          v-for="(b, i) in DOZEN_BETS"
          :key="b.key"
          type="button"
          class="cell outside dozen"
          :style="{ gridColumn: `${2 + i * 4} / span 4` }"
          :disabled="disabled"
          @click="emit('place', b.key)"
        >
          {{ b.label }}
          <CasinoChip v-if="placed[b.key]" class="placed" :value="placed[b.key]" :size="CHIP_SIZE" />
        </button>

        <button
          v-for="(b, i) in EVEN_MONEY_BETS"
          :key="b.key"
          type="button"
          class="cell outside even-money"
          :class="{ red: b.key === 'red', black: b.key === 'black' }"
          :style="{ gridColumn: `${2 + i * 2} / span 2` }"
          :disabled="disabled"
          :aria-label="b.label"
          @click="emit('place', b.key)"
        >
          <span
            v-if="b.key === 'red' || b.key === 'black'"
            class="diamond"
            aria-hidden="true"
          ></span>
          <template v-else>{{ b.label }}</template>
          <CasinoChip v-if="placed[b.key]" class="placed" :value="placed[b.key]" :size="CHIP_SIZE" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.table-scroll {
  width: 100%;
  overflow-x: auto;
  padding: 6px 4px 10px;
}

/* Wooden rail around the felt */
.frame {
  min-width: 680px;
  max-width: 980px;
  margin: 0 auto;
  padding: 12px;
  border-radius: 16px;
  background: linear-gradient(180deg, #6b3d16, #3a1f0a 55%, #2a1506);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.15),
    0 14px 40px rgba(0, 0, 0, 0.7);
}

.table {
  --line: rgba(255, 255, 255, 0.55);
  display: grid;
  grid-template-columns: 1.3fr repeat(12, 1fr) 1.3fr;
  grid-template-rows: repeat(3, 3.4rem) 2.8rem 2.8rem;
  border: 2px solid var(--gold);
  border-radius: 8px;
  background: radial-gradient(ellipse at 50% 40%, #17804a 0%, #0e5a33 55%, #093d23 100%);
  box-shadow: inset 0 0 30px rgba(0, 0, 0, 0.45);
}

.cell {
  position: relative;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid var(--line);
  background: transparent;
  color: #fff;
  font: inherit;
  font-family: var(--font-serif);
  font-size: 1.05rem;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.15s;
}

.cell:hover:not(:disabled) {
  background-color: rgba(255, 255, 255, 0.08);
}

/* Numbers sit in a coloured oval, like a printed table layout */
.num {
  display: grid;
  place-items: center;
  min-width: 2.1rem;
  height: 2.1rem;
  padding: 0 0.3rem;
  border-radius: 50%;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.6);
}

.num.red {
  background: #b52a1f;
}

.num.black {
  background: #121212;
}

.zero .num {
  font-size: 1.5rem;
}

.cell.win {
  background-color: rgba(240, 165, 0, 0.25);
  box-shadow: inset 0 0 0 3px var(--gold);
}

.cell.win .num {
  animation: win 0.7s ease-in-out 4;
}

@keyframes win {
  50% {
    transform: scale(1.18);
    box-shadow: 0 0 14px 4px rgba(240, 165, 0, 0.9);
  }
}

.zero {
  grid-column: 1;
  grid-row: 1 / span 3;
  border-radius: 6px 0 0 0;
}

.numbers {
  position: relative;
  grid-column: 2 / span 12;
  grid-row: 1 / span 3;
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.outside {
  font-size: 0.8rem;
  font-weight: normal;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.column {
  grid-column: 14;
}

.dozen {
  grid-row: 4;
}

.even-money {
  grid-row: 5;
}

.diamond {
  width: 1.2rem;
  height: 1.2rem;
  transform: rotate(45deg);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.red .diamond {
  background: #b52a1f;
}

.black .diamond {
  background: #121212;
}

/* Hit areas on the lines between numbers for split, street, corner, six line and 0 bets */
.spot {
  position: absolute;
  z-index: 1;
  width: 18px;
  height: 18px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  transform: translate(-50%, -50%);
  cursor: pointer;
}

.spot:hover:not(:disabled),
.spot:focus-visible {
  background: rgba(240, 165, 0, 0.5);
  box-shadow: 0 0 0 2px var(--gold);
}

.spot.filled {
  z-index: 2;
}

.placed {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 2;
  transform: translate(-50%, -50%);
  pointer-events: none;
  animation: drop 0.18s ease-out;
}

@keyframes drop {
  from {
    transform: translate(-50%, -80%) scale(1.15);
    opacity: 0.4;
  }
}

.table.disabled .cell,
.table.disabled .spot {
  cursor: default;
}

.cell:disabled {
  color: #fff;
}
</style>
