// Small tween helper driven by a Pixi ticker. `await tween(...)` resolves when done.

export const ease = {
  linear: (t) => t,
  inCubic: (t) => t ** 3,
  outCubic: (t) => 1 - (1 - t) ** 3,
  inOutCubic: (t) => (t < 0.5 ? 4 * t ** 3 : 1 - (-2 * t + 2) ** 3 / 2),
  outQuad: (t) => 1 - (1 - t) ** 2,
  outBack: (t) => {
    const c1 = 1.70158
    const c3 = c1 + 1
    return 1 + c3 * (t - 1) ** 3 + c1 * (t - 1) ** 2
  },
  outBounce: (t) => {
    const n = 7.5625
    const d = 2.75
    if (t < 1 / d) return n * t * t
    if (t < 2 / d) return n * (t -= 1.5 / d) * t + 0.75
    if (t < 2.5 / d) return n * (t -= 2.25 / d) * t + 0.9375
    return n * (t -= 2.625 / d) * t + 0.984375
  },
}

// Calls onUpdate(easedProgress, rawProgress) every frame for `duration` ms.
// Pass `guard` (a display object) to stop quietly if it gets destroyed mid-tween.
export function tween(ticker, duration, onUpdate, easing = ease.outCubic, guard = null) {
  return new Promise((resolve) => {
    if (duration <= 0) {
      onUpdate(1, 1)
      resolve()
      return
    }
    let elapsed = 0
    const step = (t) => {
      if (guard?.destroyed) {
        ticker.remove(step)
        resolve()
        return
      }
      elapsed += t.deltaMS
      const p = Math.min(elapsed / duration, 1)
      onUpdate(easing(p), p)
      if (p >= 1) {
        ticker.remove(step)
        resolve()
      }
    }
    ticker.add(step)
  })
}

export const lerp = (a, b, t) => a + (b - a) * t
export const wait = (ticker, ms) => tween(ticker, ms, () => {})
