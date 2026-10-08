<script setup>
import { ref } from 'vue'
import { POCKETS, colorOf } from '../roulette'

const SEG = 360 / POCKETS.length
const FILLS = { red: '#b52a1f', black: '#151515', green: '#1b7a43' }
const POCKET_FILLS = { red: '#7e1c14', black: '#0a0a0a', green: '#125630' }

// Radii in SVG units (viewBox is -100..100)
const NUMBER_RING = [60, 74]
const POCKET_RING = [48, 60]
const BALL_TRACK_R = 82
const BALL_POCKET_R = 54

const reducedMotion =
  typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
const SPIN_MS = reducedMotion ? 1000 : 5500
const BALL_DROP_AT_MS = reducedMotion ? 500 : 3800

function point(angle, r) {
  const rad = (angle * Math.PI) / 180
  return `${(r * Math.sin(rad)).toFixed(3)} ${(-r * Math.cos(rad)).toFixed(3)}`
}

// Ring slice between radii r1 < r2, centred on `centre` degrees clockwise from the top
function slice(centre, [r1, r2]) {
  const a1 = centre - SEG / 2
  const a2 = centre + SEG / 2
  return (
    `M${point(a1, r2)} A${r2} ${r2} 0 0 1 ${point(a2, r2)} ` +
    `L${point(a2, r1)} A${r1} ${r1} 0 0 0 ${point(a1, r1)} Z`
  )
}

// Pocket i is centred at i * SEG degrees, clockwise from the top
const pockets = POCKETS.map((number, i) => {
  const centre = i * SEG
  const color = colorOf(number)
  return {
    number,
    centre,
    numberPath: slice(centre, NUMBER_RING),
    numberFill: FILLS[color],
    pocketPath: slice(centre, POCKET_RING),
    pocketFill: POCKET_FILLS[color],
  }
})

const deflectors = Array.from({ length: 8 }, (_, i) => i * 45 + 22.5)

const rotation = ref(0) // wheel head, clockwise
const ballAngle = ref(0) // ball orbit, counter-clockwise; always ends at the top
const ballRadius = ref(BALL_POCKET_R)
const ballDropping = ref(false)
let finishSpin = null
let dropTimer = null

// Spin until `number` is at the top with the ball in it; resolves when the wheel stops
function spinTo(number) {
  const index = POCKETS.indexOf(number)
  // Turning the wheel by (360 - centre) brings that pocket to the top
  const target = 360 - index * SEG
  const current = rotation.value % 360
  rotation.value += 5 * 360 + ((((target - current) % 360) + 360) % 360)

  // Ball goes back up to the track, orbits the other way, then drops in near the end
  ballDropping.value = false
  ballRadius.value = BALL_TRACK_R
  ballAngle.value -= 4 * 360
  clearTimeout(dropTimer)
  dropTimer = setTimeout(() => {
    ballDropping.value = true
    ballRadius.value = BALL_POCKET_R
  }, BALL_DROP_AT_MS)

  return new Promise((resolve) => {
    // Fallback in case the browser skips the transition and never fires transitionend
    const timer = setTimeout(() => finishSpin?.(), SPIN_MS + 1500)
    finishSpin = () => {
      clearTimeout(timer)
      finishSpin = null
      resolve()
    }
  })
}

function onTransitionEnd(e) {
  if (e.propertyName === 'transform') finishSpin?.()
}

defineExpose({ spinTo })
</script>

<template>
  <div class="wheel-wrap" role="img" aria-label="Roulette wheel">
    <!-- Static bowl: wooden rim, ball track and deflectors -->
    <svg class="layer" viewBox="-100 -100 200 200" aria-hidden="true">
      <defs>
        <radialGradient id="wood" cx="50%" cy="50%" r="50%">
          <stop offset="80%" stop-color="#3a1f0a" />
          <stop offset="92%" stop-color="#6b3d16" />
          <stop offset="100%" stop-color="#2a1506" />
        </radialGradient>
        <radialGradient id="track" cx="50%" cy="50%" r="50%">
          <stop offset="70%" stop-color="#0c0c0c" />
          <stop offset="100%" stop-color="#262626" />
        </radialGradient>
      </defs>
      <circle r="99" fill="url(#wood)" />
      <circle r="99" class="gold-line" stroke-width="1.5" />
      <circle r="88" fill="url(#track)" />
      <circle r="88" class="gold-line" stroke-width="1" />
      <path
        v-for="a in deflectors"
        :key="a"
        :transform="`rotate(${a}) translate(0 -77)`"
        d="M0 -2.6 L1.4 0 L0 2.6 L-1.4 0 Z"
        class="deflector"
      />
    </svg>

    <!-- Rotating wheel head: numbers, pockets, cone and turret -->
    <svg
      class="layer head"
      viewBox="-100 -100 200 200"
      aria-hidden="true"
      :style="{
        transform: `rotate(${rotation}deg)`,
        transition: `transform ${SPIN_MS}ms cubic-bezier(0.15, 0.6, 0.15, 1)`,
      }"
      @transitionend="onTransitionEnd"
    >
      <defs>
        <radialGradient id="cone" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#8a5523" />
          <stop offset="70%" stop-color="#5a3313" />
          <stop offset="100%" stop-color="#2e1706" />
        </radialGradient>
        <radialGradient id="dome" cx="40%" cy="35%" r="65%">
          <stop offset="0%" stop-color="#fff2c4" />
          <stop offset="45%" stop-color="#f0a500" />
          <stop offset="100%" stop-color="#7a5200" />
        </radialGradient>
      </defs>

      <circle r="74.5" class="gold-line" stroke-width="1" />
      <g v-for="p in pockets" :key="p.number">
        <path :d="p.numberPath" :fill="p.numberFill" class="fret" />
        <path :d="p.pocketPath" :fill="p.pocketFill" class="fret" />
        <text :transform="`rotate(${p.centre}) translate(0 -67)`" class="number">
          {{ p.number }}
        </text>
      </g>

      <circle r="48" fill="url(#cone)" />
      <circle r="48" class="gold-line" stroke-width="1" />
      <circle r="24" class="gold-line" stroke-width="0.6" opacity="0.6" />

      <g class="turret">
        <rect
          v-for="a in [0, 90, 180, 270]"
          :key="a"
          x="-1.6"
          y="-26"
          width="3.2"
          height="26"
          rx="1.6"
          :transform="`rotate(${a})`"
          fill="url(#dome)"
        />
        <circle
          v-for="a in [0, 90, 180, 270]"
          :key="`k${a}`"
          r="3.4"
          :transform="`rotate(${a}) translate(0 -26)`"
          fill="url(#dome)"
        />
        <circle r="8" fill="url(#dome)" />
      </g>
    </svg>

    <!-- Ball -->
    <svg class="layer" viewBox="-100 -100 200 200" aria-hidden="true">
      <defs>
        <radialGradient id="ball" cx="35%" cy="30%" r="70%">
          <stop offset="0%" stop-color="#ffffff" />
          <stop offset="60%" stop-color="#e6e6e6" />
          <stop offset="100%" stop-color="#9a9a9a" />
        </radialGradient>
      </defs>
      <g
        class="orbit"
        :style="{
          transform: `rotate(${ballAngle}deg)`,
          transition: `transform ${SPIN_MS}ms cubic-bezier(0.2, 0.55, 0.2, 1)`,
        }"
      >
        <g
          class="ball"
          :class="{ dropping: ballDropping }"
          :style="{ transform: `translate(0px, ${-ballRadius}px)` }"
        >
          <circle r="3.6" fill="url(#ball)" class="ball-shape" />
        </g>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.wheel-wrap {
  position: relative;
  width: min(86vw, 380px);
  aspect-ratio: 1;
  max-width: 100%;
  border-radius: 50%;
  box-shadow:
    0 0 0 2px rgba(240, 165, 0, 0.35),
    0 18px 50px rgba(0, 0, 0, 0.8),
    0 0 60px rgba(240, 165, 0, 0.15);
}

.layer {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.gold-line {
  fill: none;
  stroke: var(--gold);
}

.deflector {
  fill: var(--gold);
  opacity: 0.85;
}

.fret {
  stroke: #c9a24a;
  stroke-width: 0.35;
}

.number {
  fill: #fff;
  font-family: system-ui, sans-serif;
  font-size: 7.5px;
  font-weight: 700;
  text-anchor: middle;
  dominant-baseline: middle;
}

.turret {
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.6));
}

/* Ball moves out to the track quickly, then drops into the pocket when .dropping is set */
.ball {
  transition: transform 0.4s ease-out;
}

.ball.dropping {
  transition: transform 1.2s cubic-bezier(0.5, 0, 0.75, 0.4);
}

.ball-shape {
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.7));
}
</style>
