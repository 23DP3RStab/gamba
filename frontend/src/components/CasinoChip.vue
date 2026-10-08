<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: Number, required: true },
  // CSS size of the chip, e.g. '3rem'
  size: { type: String, default: '3rem' },
})

// Casino colours by denomination; a stack shows the colour of its largest chip
const COLORS = [
  [500, '#6c2bd9'],
  [100, '#1b1b1b'],
  [25, '#1f8a4c'],
  [10, '#1f5fbf'],
  [5, '#c0392b'],
  [1, '#e9e9e9'],
]

const color = computed(() => (COLORS.find(([v]) => props.value >= v) ?? COLORS.at(-1))[1])
const light = computed(() => props.value < 5)

const label = computed(() => {
  const v = props.value
  if (v >= 1000) return `${Math.round(v / 100) / 10}k`
  return String(v)
})
</script>

<template>
  <span class="chip" :class="{ light }" :style="{ '--chip': color, '--size': size }">
    <span class="face">{{ label }}</span>
  </span>
</template>

<style scoped>
.chip {
  display: inline-grid;
  place-items: center;
  width: var(--size);
  height: var(--size);
  border-radius: 50%;
  background: repeating-conic-gradient(#fff 0 12deg, var(--chip) 12deg 45deg);
  box-shadow:
    0 2px 0 rgba(0, 0, 0, 0.45),
    0 3px 6px rgba(0, 0, 0, 0.5);
}

.face {
  display: grid;
  place-items: center;
  width: 72%;
  height: 72%;
  border: 1px dashed rgba(255, 255, 255, 0.75);
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, rgba(255, 255, 255, 0.25), transparent 60%),
    var(--chip);
  color: #fff;
  font-family: system-ui, sans-serif;
  font-size: calc(var(--size) * 0.3);
  font-weight: 800;
  line-height: 1;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.6);
}

.light .face {
  border-color: rgba(0, 0, 0, 0.35);
  color: #222;
  text-shadow: none;
}
</style>
