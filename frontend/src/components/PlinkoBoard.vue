<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { reducedMotion, SceneCancelled } from '../pixi/stage'

const props = defineProps({
  // Payout per slot, left to right, for the chosen risk level
  multipliers: { type: Array, default: () => [] },
})

// --- PixiJS scene (drawing lives in src/pixi/plinko) ---
const host = ref(null)
const failed = ref(false)
let scene = null
let unmounted = false

onMounted(async () => {
  try {
    // Loaded on demand so PixiJS only downloads when someone opens this game
    const { createPlinkoScene } = await import('../pixi/plinko/scene')
    const created = await createPlinkoScene(host.value, { reducedMotion: reducedMotion() })
    if (unmounted) {
      created.destroy()
      return
    }
    scene = created
    scene.setMultipliers(props.multipliers)
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

watch(
  () => props.multipliers,
  (list) => scene?.setMultipliers(list),
)

// Animates one ball along the server's path; resolves when it lands (straight away without a board)
function drop(path, slot) {
  return scene ? scene.drop(path, slot) : Promise.resolve()
}

defineExpose({ drop })
</script>

<template>
  <div ref="host" class="stage" aria-hidden="true"></div>
  <p v-if="failed" class="fallback">The game board could not start in this browser.</p>
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
