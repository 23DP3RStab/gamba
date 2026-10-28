// The Crash graph: a curve rising with the multiplier and a rocket at its tip. CrashScene.vue
// says when the round takes off and crashes; this file only draws. The axes stretch as it climbs.
import { Container, Graphics, Text } from 'pixi.js'
import { createApp } from '../stage'
import { Particles, rand } from '../particles'
import { GROWTH_PER_MS } from '../../crash'

const PAD = { left: 56, right: 36, top: 30, bottom: 38 }
const GOLD = 0xd4af37
const RED = 0xff5a5a
const INK = 0x1c1c22
const STEPS = [0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100, 200, 500]
const POINTS = 90 // samples along the curve

// The smooth multiplier for drawing; the number shown on the page is cut to hundredths
const curve = (ms) => Math.exp(GROWTH_PER_MS * ms)
const niceStep = (range, ticks) => STEPS.find((step) => range / step <= ticks) ?? STEPS.at(-1)

function createRocket() {
  const rocket = new Container()
  const flame = new Graphics()
  flame.poly([-14, -6, -34, 0, -14, 6]).fill(0xff9a3c)
  flame.poly([-14, -3, -25, 0, -14, 3]).fill(0xffe28a)
  const body = new Graphics()
  body.poly([-10, -7, -19, -14, -4, -7]).fill(RED).stroke({ width: 2, color: INK })
  body.poly([-10, 7, -19, 14, -4, 7]).fill(RED).stroke({ width: 2, color: INK })
  body.roundRect(-15, -8, 30, 16, 8).fill(0xf1f1f1).stroke({ width: 2, color: INK })
  body.poly([12, -7, 25, 0, 12, 7]).fill(RED).stroke({ width: 2, color: INK })
  body.circle(3, 0, 4).fill(0x4fc3ff).stroke({ width: 2, color: INK })
  rocket.addChild(flame, body)
  return { rocket, flame }
}

export async function createCrashScene(host, { reducedMotion }) {
  const animate = !reducedMotion
  const { app, destroy: destroyApp } = await createApp(host)
  const { ticker } = app

  const grid = new Graphics()
  const labels = new Container()
  const area = new Graphics()
  const line = new Graphics()
  const { rocket, flame } = createRocket()
  const fxLayer = new Container()
  app.stage.addChild(grid, labels, area, line, rocket, fxLayer)
  const particles = new Particles(ticker, fxLayer, animate ? 160 : 40)

  let mode = 'waiting' // 'waiting' | 'flying' | 'crashed'
  let elapsed = () => 0 // ms flown, while flying
  let frozenAt = 0 // ms flown when it crashed
  let clock = 0
  const tip = { x: 0, y: 0 }

  // Axis labels are reused by their text, so a frame only creates labels it hasn't shown before
  const labelPool = new Map()
  function label(text, x, y, anchorX, anchorY, used) {
    let t = labelPool.get(text)
    if (!t) {
      t = new Text({
        text,
        style: { fill: 0x8a8a96, fontSize: 12, fontFamily: 'Segoe UI, Roboto, sans-serif' },
      })
      labels.addChild(t)
      labelPool.set(text, t)
    }
    t.anchor.set(anchorX, anchorY)
    t.position.set(x, y)
    used.add(t)
  }

  function draw(ms) {
    const { width, height } = app.screen
    const w = width - PAD.left - PAD.right
    const h = height - PAD.top - PAD.bottom
    const maxMs = Math.max(10_000, ms * 1.15)
    const maxM = Math.max(2, 1 + (curve(ms) - 1) * 1.2)
    const x = (t) => PAD.left + (t / maxMs) * w
    const y = (m) => PAD.top + h - ((m - 1) / (maxM - 1)) * h
    const used = new Set()

    grid.clear()
    const mStep = niceStep(maxM - 1, 5)
    for (let k = 0; 1 + k * mStep <= maxM; k++) {
      const m = 1 + k * mStep
      grid.moveTo(PAD.left, y(m)).lineTo(PAD.left + w, y(m))
      label(`${Number(m.toFixed(1))}×`, PAD.left - 10, y(m), 1, 0.5, used)
    }
    const sStep = niceStep(maxMs / 1000, 6)
    for (let k = 1; k * sStep * 1000 <= maxMs; k++) {
      const s = k * sStep
      grid.moveTo(x(s * 1000), PAD.top).lineTo(x(s * 1000), PAD.top + h)
      label(`${s}s`, x(s * 1000), PAD.top + h + 10, 0.5, 0, used)
    }
    grid.stroke({ width: 1, color: 0xffffff, alpha: 0.06 })
    grid.moveTo(PAD.left, PAD.top).lineTo(PAD.left, PAD.top + h).lineTo(PAD.left + w, PAD.top + h)
    grid.stroke({ width: 2, color: 0xffffff, alpha: 0.18 })
    for (const t of labelPool.values()) t.visible = used.has(t)

    // The curve from take-off to now, and the glow under it
    const color = mode === 'crashed' ? RED : GOLD
    const points = []
    for (let i = 0; i <= POINTS; i++) {
      const t = (ms * i) / POINTS
      points.push(x(t), y(curve(t)))
    }
    line.clear()
    area.clear()
    if (ms > 0) {
      line.poly(points, false).stroke({ width: 5, color, cap: 'round', join: 'round' })
      area
        .poly([...points, x(ms), PAD.top + h, x(0), PAD.top + h])
        .fill({ color, alpha: 0.12 })
    }

    tip.x = x(ms)
    tip.y = y(curve(ms))
    rocket.position.set(tip.x, tip.y)
    // Point the rocket along the curve
    const slopeX = w / maxMs
    const slopeY = (h / (maxM - 1)) * GROWTH_PER_MS * curve(ms)
    rocket.rotation = -Math.atan2(slopeY, slopeX)
  }

  function explode() {
    const count = animate ? 48 : 12
    const colors = [0xffe28a, 0xff9a3c, RED, 0xffffff]
    for (let i = 0; i < count; i++) {
      const bit = new Graphics().circle(0, 0, rand(2, 6)).fill(colors[i % colors.length])
      const angle = rand(0, Math.PI * 2)
      const speed = rand(80, animate ? 420 : 160)
      particles.emit(bit, {
        x: tip.x,
        y: tip.y,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        drag: 2.2,
        gravity: 140,
        life: rand(500, 1100),
        fadeFrom: 0.3,
      })
    }
  }

  ticker.add((t) => {
    clock += t.deltaMS
    draw(mode === 'flying' ? Math.max(0, elapsed()) : mode === 'crashed' ? frozenAt : 0)
    if (animate) flame.scale.set(0.75 + 0.35 * Math.abs(Math.sin(clock / 70)), 1)
  })

  return {
    // `flown` returns how many ms the round has been flying
    fly(flown) {
      mode = 'flying'
      elapsed = flown
      rocket.visible = true
    },
    crash(ms) {
      mode = 'crashed'
      frozenAt = ms
      draw(ms)
      rocket.visible = false
      explode()
    },
    reset() {
      mode = 'waiting'
      rocket.visible = true
      particles.clear()
    },
    destroy() {
      particles.destroy()
      destroyApp()
    },
  }
}
