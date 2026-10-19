<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { HOUSES } from '../piggies'
import { formatMoney } from '../format'
import { reducedMotion, SceneCancelled } from '../pixi/stage'

const props = defineProps({
  // Per house: 'standing' | 'fallen' | 'held'
  houseStates: { type: Array, required: true },
  // Bumps each round so the houses are rebuilt
  round: { type: Number, required: true },
  // Index of the house the wolf stands at
  wolfAt: { type: Number, required: true },
  puffing: { type: Boolean, default: false },
  walking: { type: Boolean, default: false },
  // Houses blown down this round
  level: { type: Number, default: 0 },
  // Game phase from PiggiesView
  phase: { type: String, required: true },
  // Latest moment to celebrate: { id, kind: 'fell' | 'held' | 'won' | 'cashed', multiplier, amount }
  banner: { type: Object, default: null },
})

const BANNER_MS = 1800

// --- PixiJS scene (drawing lives in src/pixi/piggies) ---
const host = ref(null)
const failed = ref(false)
let scene = null
let unmounted = false

onMounted(async () => {
  try {
    // Loaded on demand so PixiJS only downloads when someone opens this game
    const { createPiggiesScene } = await import('../pixi/piggies/scene')
    const created = await createPiggiesScene(host.value, { reducedMotion: reducedMotion() })
    if (unmounted) {
      created.destroy()
      return
    }
    scene = created
    scene.placeWolf(props.wolfAt)
    scene.rebuild(props.houseStates, false)
    scene.setBeaten(props.phase === 'lost')
  } catch (e) {
    if (e instanceof SceneCancelled) return // page was left while starting
    console.error('Could not start the game scene', e)
    failed.value = true
  }
})

onBeforeUnmount(() => {
  unmounted = true
  scene?.destroy()
  scene = null
  clearTimeout(bannerTimer)
})

// Order matters: stop the huff before a house falls or holds in the same update
watch(
  () => props.puffing,
  (on) => (on ? scene?.huffStart(props.wolfAt) : scene?.huffStop()),
)
watch(
  () => props.walking,
  (on) => on && scene?.walkTo(props.wolfAt),
)
watch(
  () => [...props.houseStates],
  (now, before) =>
    now.forEach((state, i) => {
      if (state === before[i]) return
      if (state === 'fallen') scene?.houseFell(i)
      else if (state === 'held') scene?.houseHeld(i)
    }),
)
watch(
  () => props.round,
  () => scene?.rebuild(),
)
watch(
  () => props.phase === 'lost',
  (beaten) => scene?.setBeaten(beaten),
)
watch(
  () =>
    props.phase === 'blowing' && props.houseStates[props.wolfAt] === 'standing' ? props.wolfAt : -1,
  (index) => scene?.setScared(index),
)

// --- banner (HTML so the text stays crisp and readable) ---
const shownBanner = ref(null)
let bannerTimer = null
watch(
  () => props.banner?.id,
  () => {
    if (!props.banner) return
    shownBanner.value = props.banner
    scene?.celebrate(props.banner.kind)
    clearTimeout(bannerTimer)
    bannerTimer = setTimeout(() => (shownBanner.value = null), BANNER_MS)
  },
)

const bannerText = computed(() => {
  const b = shownBanner.value
  if (!b) return null
  const times = `×${b.multiplier?.toLocaleString('lv-LV')}`
  switch (b.kind) {
    case 'fell':
      return { title: 'Blown down!', sub: times }
    case 'held':
      return { title: 'It held!', sub: 'The pig is safe' }
    case 'won':
      return { title: 'All three!', sub: `${times} · ${formatMoney(b.amount)}` }
    default:
      return { title: 'Cashed out', sub: formatMoney(b.amount) }
  }
})

const playing = computed(() => props.phase === 'blowing' || props.phase === 'decide')
function labelClass(i) {
  return {
    done: props.houseStates[i] === 'fallen',
    failed: props.houseStates[i] === 'held',
    next: playing.value && i === props.level,
  }
}
</script>

<template>
  <div class="board">
    <div class="scene">
      <div ref="host" class="stage" aria-hidden="true"></div>
      <p v-if="failed" class="fallback">The game scene could not start in this browser.</p>

      <Transition name="banner">
        <div
          v-if="bannerText"
          :key="shownBanner.id"
          class="banner"
          :class="shownBanner.kind"
          aria-hidden="true"
        >
          <p class="banner-title">{{ bannerText.title }}</p>
          <p class="banner-sub">{{ bannerText.sub }}</p>
        </div>
      </Transition>
    </div>

    <ol class="house-labels">
      <li v-for="(house, i) in HOUSES" :key="house.key" :class="labelClass(i)">
        <span class="label-name">{{ house.label }}</span>
        <span class="label-mult">×{{ house.multiplier.toLocaleString('lv-LV') }}</span>
      </li>
    </ol>
  </div>
</template>

<style scoped>
.board {
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
}

.scene {
  position: relative;
  aspect-ratio: 16 / 7;
  overflow: hidden;
  border: 1px solid rgba(240, 165, 0, 0.35);
  border-radius: 12px 12px 0 0;
  background: #0a1830;
}

.stage {
  position: absolute;
  inset: 0;
}

.stage :deep(canvas) {
  display: block;
}

.fallback {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 1rem;
  text-align: center;
}

/* Result banner */
.banner {
  position: absolute;
  z-index: 2;
  top: 14%;
  left: 50%;
  translate: -50% 0;
  padding: 0.6rem 2.2rem 0.7rem;
  border: 2px solid var(--gold);
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.78);
  box-shadow: 0 0 30px rgba(240, 165, 0, 0.5);
  text-align: center;
  white-space: nowrap;
  pointer-events: none;
}

.banner-title {
  font-family: var(--font-serif);
  font-size: clamp(1.3rem, 4vw, 2.4rem);
  font-weight: bold;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--gold);
  text-shadow: 0 0 14px rgba(240, 165, 0, 0.6);
}

.banner-sub {
  font-size: clamp(0.85rem, 2vw, 1.1rem);
  color: #fff;
}

.banner.held {
  border-color: #ff6b6b;
  box-shadow: 0 0 30px rgba(255, 107, 107, 0.45);
}

.banner.held .banner-title {
  color: #ff6b6b;
  text-shadow: 0 0 14px rgba(255, 107, 107, 0.6);
}

.banner.won .banner-title {
  font-size: clamp(1.6rem, 5vw, 3rem);
}

.banner-enter-active {
  animation: banner-in 0.45s cubic-bezier(0.2, 1.4, 0.4, 1);
}

.banner-leave-active {
  transition: opacity 0.3s;
}

.banner-leave-to {
  opacity: 0;
}

@keyframes banner-in {
  from {
    transform: translateY(-60%) scale(0.6);
    opacity: 0;
  }
}

/* Labels under the scene */
.house-labels {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0;
  padding: 0.55rem 0 0.55rem 16%;
  list-style: none;
  border: 1px solid rgba(240, 165, 0, 0.35);
  border-top: none;
  border-radius: 0 0 12px 12px;
  background: linear-gradient(180deg, #121212, #070707);
}

.house-labels li {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 0.8rem;
  opacity: 0.75;
  transition:
    color 0.3s,
    opacity 0.3s;
}

.label-name {
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.label-mult {
  font-family: var(--font-serif);
  font-size: 1.1rem;
  color: var(--gold);
}

.house-labels .next,
.house-labels .done,
.house-labels .failed {
  opacity: 1;
}

.house-labels .next .label-name {
  color: var(--gold);
}

.house-labels .done .label-name {
  color: #5fd38d;
}

.house-labels .failed .label-name {
  color: #ff6b6b;
}

@media (max-width: 700px) {
  /* Phones show a closer view that follows the wolf */
  .scene {
    aspect-ratio: 4 / 3;
  }

  .house-labels {
    padding-left: 0;
  }

  .label-name {
    display: none;
  }

  .banner {
    padding: 0.45rem 1.2rem 0.55rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .banner-enter-active {
    animation: none;
  }
}
</style>
