<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 'straw' | 'wood' | 'brick'
  kind: { type: String, required: true },
  // 'standing' | 'fallen' | 'held'
  state: { type: String, default: 'standing' },
  // The wolf is blowing at this house right now
  shaking: { type: Boolean, default: false },
})

const LOOKS = {
  straw: { wall: '#e2bf55', line: '#a9852a', roof: '#c99b2e', roofLine: '#8f6c1c' },
  wood: { wall: '#9a6233', line: '#5e381a', roof: '#6b3f1f', roofLine: '#43260f' },
  brick: { wall: '#b5462f', line: '#ead8c2', roof: '#4b4b4b', roofLine: '#2c2c2c' },
}

const look = computed(() => LOOKS[props.kind])
const patternId = computed(() => `piggies-wall-${props.kind}`)
</script>

<template>
  <svg
    class="house"
    :class="[kind, state, { shaking }]"
    viewBox="0 0 120 140"
    role="img"
    :aria-label="`${kind} house, ${state}`"
  >
    <defs>
      <pattern
        v-if="kind === 'straw'"
        :id="patternId"
        width="6"
        height="14"
        patternUnits="userSpaceOnUse"
      >
        <rect width="6" height="14" :fill="look.wall" />
        <path d="M1 0 L2 14 M4 0 L4.5 14" :stroke="look.line" stroke-width="0.8" />
      </pattern>
      <pattern
        v-else-if="kind === 'wood'"
        :id="patternId"
        width="40"
        height="12"
        patternUnits="userSpaceOnUse"
      >
        <rect width="40" height="12" :fill="look.wall" />
        <path d="M0 11.5 H40 M28 0 V12" :stroke="look.line" stroke-width="1.2" />
        <circle cx="10" cy="6" r="1.2" :fill="look.line" />
      </pattern>
      <pattern v-else :id="patternId" width="20" height="12" patternUnits="userSpaceOnUse">
        <rect width="20" height="12" :fill="look.wall" />
        <path d="M0 0.5 H20 M0 6.5 H20 M5 0 V6 M15 6 V12" :stroke="look.line" stroke-width="1" />
      </pattern>
    </defs>

    <!-- Walls, door and window collapse together -->
    <g class="walls">
      <rect x="16" y="64" width="88" height="76" :fill="`url(#${patternId})`" />
      <rect x="16" y="64" width="88" height="76" fill="none" :stroke="look.roofLine" stroke-width="2" />
      <rect x="49" y="100" width="22" height="40" rx="10" ry="10" fill="#3b2412" />
      <rect x="49" y="120" width="22" height="20" fill="#3b2412" />
      <circle cx="66" cy="121" r="1.6" fill="#f0a500" />
      <rect x="80" y="78" width="16" height="16" fill="#ffe7a3" :stroke="look.roofLine" stroke-width="2" />
      <path d="M88 78 V94 M80 86 H96" :stroke="look.roofLine" stroke-width="1.5" />
    </g>

    <!-- Roof flies off -->
    <g class="roof">
      <rect v-if="kind === 'brick'" x="82" y="22" width="12" height="28" fill="#7d2c1d" />
      <polygon points="4,68 60,16 116,68" :fill="look.roof" :stroke="look.roofLine" stroke-width="2" />
      <path
        v-if="kind === 'straw'"
        d="M20 60 L60 22 M34 64 L60 30 M86 64 L60 30 M100 60 L60 22 M60 26 V66"
        :stroke="look.roofLine"
        stroke-width="1.2"
      />
      <path
        v-else-if="kind === 'wood'"
        d="M22 52 H98 M38 38 H82 M52 26 H68"
        :stroke="look.roofLine"
        stroke-width="1.5"
      />
      <path v-else d="M16 57 H104 M28 46 H92 M40 35 H80 M52 24 H68" stroke="#666" stroke-width="1.2" />
    </g>

    <!-- The pig stands by the door and runs off when the house falls -->
    <g class="pig">
      <ellipse cx="32" cy="128" rx="11" ry="9" fill="#f4a6b8" />
      <rect x="25" y="133" width="3.5" height="7" rx="1" fill="#e08aa0" />
      <rect x="35" y="133" width="3.5" height="7" rx="1" fill="#e08aa0" />
      <circle cx="32" cy="113" r="9" fill="#f7b5c5" />
      <polygon points="24,108 25,100 30,105" fill="#e88aa0" />
      <polygon points="40,108 39,100 34,105" fill="#e88aa0" />
      <circle cx="28.5" cy="111" r="1.3" fill="#222" />
      <circle cx="35.5" cy="111" r="1.3" fill="#222" />
      <ellipse cx="32" cy="116.5" rx="4" ry="3" fill="#e88aa0" />
      <circle cx="30.6" cy="116.5" r="0.9" fill="#a24d63" />
      <circle cx="33.4" cy="116.5" r="0.9" fill="#a24d63" />
    </g>

    <!-- Debris shown once the house is down -->
    <g v-if="state === 'fallen'" class="debris">
      <rect x="20" y="132" width="26" height="6" rx="2" :fill="look.wall" transform="rotate(-8 33 135)" />
      <rect x="50" y="131" width="30" height="7" rx="2" :fill="look.roof" transform="rotate(6 65 134)" />
      <rect x="78" y="133" width="22" height="5" rx="2" :fill="look.wall" transform="rotate(-4 89 135)" />
    </g>
  </svg>
</template>

<style scoped>
.house {
  display: block;
  width: 100%;
  height: auto;
  overflow: visible;
  transform-origin: 50% 100%;
  animation: build 0.45s ease-out;
}

@keyframes build {
  from {
    transform: scaleY(0.2);
    opacity: 0;
  }
}

.house g {
  transform-box: fill-box;
}

.house.shaking {
  animation: wobble 0.18s ease-in-out infinite alternate;
}

@keyframes wobble {
  from {
    transform: rotate(-1.5deg) translateX(-1px);
  }
  to {
    transform: rotate(1.8deg) translateX(2px);
  }
}

/* Fallen: roof flies away, walls fold to the ground, pig runs off */
.fallen .roof {
  transform-origin: 50% 100%;
  animation: blow-away 0.9s ease-in forwards;
}

@keyframes blow-away {
  to {
    transform: translate(140%, -120%) rotate(70deg);
    opacity: 0;
  }
}

.fallen .walls {
  transform-origin: 50% 100%;
  animation: collapse 0.7s 0.1s ease-in forwards;
}

@keyframes collapse {
  60% {
    transform: skewX(18deg) scaleY(0.5);
  }
  to {
    transform: skewX(30deg) scaleY(0.04);
    opacity: 0;
  }
}

.fallen .pig {
  animation: run-off 0.9s 0.2s ease-in forwards;
}

@keyframes run-off {
  30% {
    transform: translate(10%, -25%);
  }
  to {
    transform: translate(420%, 0);
    opacity: 0;
  }
}

.debris {
  animation: settle 0.5s 0.5s ease-out backwards;
}

@keyframes settle {
  from {
    opacity: 0;
    transform: translateY(-40%);
  }
}

/* Held: the house stands firm, the pig jumps for joy */
.held .pig {
  animation: cheer 0.45s ease-in-out 3;
}

@keyframes cheer {
  50% {
    transform: translateY(-22%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .house,
  .house *,
  .house.shaking {
    animation-duration: 0.01s !important;
    animation-iteration-count: 1 !important;
  }
}
</style>
