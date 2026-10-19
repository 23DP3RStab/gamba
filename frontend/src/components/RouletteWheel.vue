<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { reducedMotion, SceneCancelled } from '../pixi/stage'

// The wheel is drawn with PixiJS (src/pixi/roulette/wheel.js); this component only hosts it.
const host = ref(null)
const failed = ref(false)
let wheel = null
let unmounted = false
let markReady
const ready = new Promise((resolve) => (markReady = resolve))

onMounted(async () => {
  try {
    // Loaded on demand so PixiJS only downloads when someone opens this game
    const { createRouletteWheel } = await import('../pixi/roulette/wheel')
    const created = await createRouletteWheel(host.value, { reducedMotion: reducedMotion() })
    if (unmounted) {
      created.destroy()
      return
    }
    wheel = created
  } catch (e) {
    if (!(e instanceof SceneCancelled)) {
      console.error('Could not start the roulette wheel', e)
      failed.value = true
    }
  }
  markReady()
})

onBeforeUnmount(() => {
  unmounted = true
  wheel?.destroy()
  wheel = null
})

// Spin until `number`'s pocket holds the ball; resolves when the wheel stops
async function spinTo(number) {
  await ready
  if (wheel) await wheel.spinTo(number)
}

defineExpose({ spinTo })
</script>

<template>
  <div class="wheel-wrap" role="img" aria-label="Roulette wheel">
    <div ref="host" class="stage"></div>
    <p v-if="failed" class="fallback">The wheel could not be drawn in this browser.</p>
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
  padding: 2rem;
  text-align: center;
}
</style>
