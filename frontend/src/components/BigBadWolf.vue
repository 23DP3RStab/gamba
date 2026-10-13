<script setup>
defineProps({
  // Cheeks blown up and wind coming out
  puffing: { type: Boolean, default: false },
  // Out of breath after a house held
  beaten: { type: Boolean, default: false },
})
</script>

<template>
  <div class="wolf" :class="{ puffing, beaten }">
    <svg viewBox="0 0 120 140" role="img" aria-label="The big bad wolf">
      <!-- Tail -->
      <path d="M22 96 Q2 84 6 62 Q18 76 30 84 Z" fill="#5d6470" />
      <!-- Body and legs -->
      <ellipse cx="44" cy="100" rx="24" ry="22" fill="#7a828f" />
      <ellipse cx="48" cy="106" rx="12" ry="13" fill="#b9bec6" />
      <rect x="30" y="116" width="9" height="24" rx="3" fill="#5d6470" />
      <rect x="50" y="116" width="9" height="24" rx="3" fill="#5d6470" />
      <!-- Arm -->
      <path d="M56 92 Q70 98 74 110" stroke="#5d6470" stroke-width="7" stroke-linecap="round" fill="none" />

      <g class="head">
        <polygon points="46,40 50,14 62,34" fill="#5d6470" />
        <polygon points="64,34 74,12 80,38" fill="#5d6470" />
        <polygon points="50,20 54,32 59,32" fill="#c98a96" />
        <circle cx="62" cy="54" r="22" fill="#7a828f" />
        <!-- Snout pointing at the house -->
        <path d="M74 44 L106 52 Q110 58 104 62 L76 68 Z" fill="#8c94a0" />
        <circle cx="106" cy="56" r="4.5" fill="#1d1d1d" />
        <ellipse class="cheek" cx="76" cy="62" rx="7" ry="6" fill="#9aa1ac" />
        <circle cx="68" cy="46" r="5" fill="#fff" />
        <circle class="pupil" cx="70" cy="46" r="2.4" fill="#1d1d1d" />
        <path d="M62 38 L74 41" stroke="#2e3239" stroke-width="2.4" stroke-linecap="round" />
        <ellipse class="mouth" cx="98" cy="64" rx="3" ry="1.2" fill="#2a1414" />
      </g>
    </svg>

    <div v-if="puffing" class="wind" aria-hidden="true">
      <span></span>
      <span></span>
      <span></span>
    </div>
    <div v-if="beaten" class="stars" aria-hidden="true">✦ ✧ ✦</div>
  </div>
</template>

<style scoped>
.wolf {
  position: relative;
  transition: transform 0.4s ease;
  transform-origin: 30% 100%;
}

svg {
  display: block;
  width: 100%;
  height: auto;
  overflow: visible;
}

svg g,
svg ellipse {
  transform-box: fill-box;
  transform-origin: center;
}

/* Huff and puff */
.puffing .head {
  animation: huff 0.5s ease-in-out infinite alternate;
}

@keyframes huff {
  from {
    transform: rotate(-4deg) translateY(-3%);
  }
  to {
    transform: rotate(3deg) translateX(3%);
  }
}

.puffing .cheek {
  animation: puff-cheek 0.5s ease-in-out infinite alternate;
}

@keyframes puff-cheek {
  to {
    transform: scale(1.9);
  }
}

.puffing .mouth {
  transform: scale(1.4, 3.4);
}

.wind {
  position: absolute;
  top: 30%;
  left: 88%;
  width: 90%;
  height: 25%;
  pointer-events: none;
}

.wind span {
  position: absolute;
  left: 0;
  width: 70%;
  height: 3px;
  border-radius: 3px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0));
  animation: gust 0.6s linear infinite;
}

.wind span:nth-child(1) {
  top: 10%;
}

.wind span:nth-child(2) {
  top: 50%;
  animation-delay: 0.2s;
}

.wind span:nth-child(3) {
  top: 90%;
  animation-delay: 0.4s;
}

@keyframes gust {
  from {
    transform: translateX(0) scaleX(0.4);
    opacity: 0;
  }
  30% {
    opacity: 1;
  }
  to {
    transform: translateX(80%) scaleX(1);
    opacity: 0;
  }
}

/* Out of breath */
.beaten {
  transform: rotate(-10deg) translateX(-8%);
}

.beaten .pupil {
  transform: translate(-1.5px, 1.5px);
}

.stars {
  position: absolute;
  top: -6%;
  left: 38%;
  color: var(--gold);
  font-size: 0.9rem;
  letter-spacing: 0.2em;
  animation: dizzy 1.2s linear infinite;
}

@keyframes dizzy {
  50% {
    transform: translateX(10%) rotate(8deg);
    opacity: 0.6;
  }
}

@media (prefers-reduced-motion: reduce) {
  .wolf *,
  .wolf {
    animation: none !important;
    transition: none !important;
  }
}
</style>
