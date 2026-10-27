import { Application } from 'pixi.js'

export const reducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches

// Thrown when the page was left before the scene finished starting; callers ignore it
export class SceneCancelled extends Error {}

// Creates a transparent PixiJS app that fills `host` and follows its size.
// The ticker pauses while the tab is hidden. Call destroy() when the component unmounts.
export async function createApp(host) {
  const app = new Application()
  await app.init({
    resizeTo: host,
    backgroundAlpha: 0,
    antialias: true,
    autoDensity: true,
    resolution: Math.min(window.devicePixelRatio || 1, 2),
  })
  if (!host || !host.isConnected) {
    app.destroy({ removeView: true })
    throw new SceneCancelled()
  }
  host.appendChild(app.canvas)

  const onVisibility = () => (document.hidden ? app.ticker.stop() : app.ticker.start())
  document.addEventListener('visibilitychange', onVisibility)

  return {
    app,
    destroy() {
      document.removeEventListener('visibilitychange', onVisibility)
      // Draw one empty frame so the renderer lets go of the scene's textures, then free the
      // scene and the renderer. Shared textures (the spritesheet) stay cached for the next visit.
      app.ticker.stop()
      const scene = app.stage.removeChildren()
      app.renderer.render(app.stage)
      for (const child of scene) child.destroy({ children: true, texture: false, textureSource: false })
      app.destroy({ removeView: true })
    },
  }
}
