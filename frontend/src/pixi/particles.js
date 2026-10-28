// Lightweight particle system: each particle is any display object moved by simple physics.

export class Particles {
  constructor(ticker, layer, limit = 220) {
    this.ticker = ticker
    this.layer = layer
    this.limit = limit
    this.list = []
    ticker.add(this.update, this)
  }

  // options: x, y, vx, vy (px/s), gravity (px/s²), drag (0..1 per second), spin (rad/s),
  // life (ms), fadeFrom (0..1 of life when fading starts), grow (scale added per second),
  // floor (y to bounce on), bounce (0..1), onUpdate(particle, dt)
  emit(display, options) {
    if (this.list.length >= this.limit) {
      display.destroy()
      return null
    }
    const p = {
      display,
      vx: 0,
      vy: 0,
      gravity: 0,
      drag: 0,
      spin: 0,
      life: 1000,
      fadeFrom: 0.6,
      grow: 0,
      bounce: 0.35,
      floor: null,
      age: 0,
      ...options,
    }
    display.position.set(options.x ?? 0, options.y ?? 0)
    p.startAlpha = display.alpha
    this.layer.addChild(display)
    this.list.push(p)
    return p
  }

  update(ticker) {
    const dt = Math.min(ticker.deltaMS, 50) / 1000
    for (let i = this.list.length - 1; i >= 0; i--) {
      const p = this.list[i]
      const d = p.display
      p.age += dt * 1000
      p.vx *= 1 - p.drag * dt
      p.vy *= 1 - p.drag * dt
      p.vy += p.gravity * dt
      d.x += p.vx * dt
      d.y += p.vy * dt
      d.rotation += p.spin * dt
      if (p.grow) d.scale.set(d.scale.x + p.grow * dt, d.scale.y + p.grow * dt)
      if (p.floor !== null && d.y > p.floor) {
        d.y = p.floor
        p.vy = -Math.abs(p.vy) * p.bounce
        p.vx *= 0.6
        p.spin *= 0.5
      }
      const t = p.age / p.life
      if (t > p.fadeFrom) d.alpha = p.startAlpha * Math.max(0, 1 - (t - p.fadeFrom) / (1 - p.fadeFrom))
      p.onUpdate?.(p, dt)
      if (t >= 1) {
        d.destroy()
        this.list.splice(i, 1)
      }
    }
  }

  clear() {
    for (const p of this.list) p.display.destroy()
    this.list.length = 0
  }

  destroy() {
    this.ticker.remove(this.update, this)
    this.clear()
  }
}

export const rand = (min, max) => min + Math.random() * (max - min)
