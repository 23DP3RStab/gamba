<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { msUntil } from '../crash'
import { reducedMotion, SceneCancelled } from '../pixi/stage'

const props = defineProps({
  // BETTING, RUNNING or CRASHED (null before the first update)
  phase: { type: String, default: null },
  // When the round took off, in this browser's clock (ms)
  takeoff: { type: Number, default: null },
  // Where it crashed, once it has
  crashPoint: { type: Number, default: null },
})

// --- PixiJS scene (drawing lives in src/pixi/crash) ---
const host = ref(null)
const failed = ref(false)
let scene = null
let unmounted = false

function show() {
  if (!scene) return
  if (props.phase === 'RUNNING') scene.fly(() => Date.now() - props.takeoff)
  else if (props.phase === 'CRASHED' && props.crashPoint) scene.crash(msUntil(props.crashPoint))
  else scene.reset()
}

onMounted(async () => {
  try {
    // Loaded on demand so PixiJS only downloads when someone opens this game
    const { createCrashScene } = await import('../pixi/crash/scene')
    const created = await createCrashScene(host.value, { reducedMotion: reducedMotion() })
    if (unmounted) {
      created.destroy()
      return
    }
    scene = created
    show()
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
})

watch(() => [props.phase, props.takeoff], show)
</script>

<template>
  <div ref="host" class="stage" aria-hidden="true"></div>
  <p v-if="failed" class="fallback">The game scene could not start in this browser.</p>
</template>

<style scoped>
.stage {
  position: absolute;
  inset: 0;
}

.stage :deep(canvas) {
  display: block;
}

.fallback {
  position: absolute;
  inset: auto 0 1rem;
  text-align: center;
  opacity: 0.7;
}
</style>
