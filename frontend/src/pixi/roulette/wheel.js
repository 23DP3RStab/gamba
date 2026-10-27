// European roulette wheel in WebGL with a physically-shaped ball run.
// The winning number is chosen before the spin (in roulette.js); this only animates to it.
import { BlurFilter, Container, Graphics, Text } from 'pixi.js'
import { createApp } from '../stage'
import { Particles, rand } from '../particles'
import { POCKETS, colorOf } from '../../roulette'

const SIZE = 400
const SEG = (Math.PI * 2) / POCKETS.length
const TAU = Math.PI * 2
const R = {
  rim: 196,
  track: 176,
  trackInner: 150,
  numbersOuter: 148,
  numbersInner: 120,
  pocketsInner: 96,
  ball: 7,
  ballTrack: 163,
  ballPocket: 108,
}
const FILL = { red: 0xb52a1f, black: 0x151515, green: 0x1b7a43 }
const POCKET_FILL = { red: 0x7e1c14, black: 0x0a0a0a, green: 0x125630 }
const GOLD = 0xf0a500
const GOLD_DARK = 0x8a5f00

// Pixi angles start at +x and go clockwise; ours start at the top
const toPixi = (a) => a - Math.PI / 2

function ringSlice(g, centre, inner, outer, color) {
  const a0 = toPixi(centre - SEG / 2)
  const a1 = toPixi(centre + SEG / 2)
  g.moveTo(Math.cos(a0) * outer, Math.sin(a0) * outer)
  g.arc(0, 0, outer, a0, a1)
  g.lineTo(Math.cos(a1) * inner, Math.sin(a1) * inner)
  g.arc(0, 0, inner, a1, a0, true)
  g.closePath().fill(color).stroke({ width: 0.8, color: 0xc9a24a })
}

function bowl() {
  const c = new Container()
  const g = new Graphics()
  // Wooden rim with a few rings to suggest a turned, varnished edge
  g.circle(0, 0, R.rim).fill(0x2a1506)
  g.circle(0, 0, R.rim - 4).fill(0x5a3313)
  g.circle(0, 0, R.rim - 10).fill(0x6b3d16)
  g.circle(0, 0, R.rim - 15).fill(0x3a1f0a)
  g.circle(0, 0, R.rim).stroke({ width: 2.5, color: GOLD })
  // Ball track
  g.circle(0, 0, R.track).fill(0x101010)
  g.circle(0, 0, R.track - 8).fill(0x1a1a1a)
  g.circle(0, 0, R.track - 18).fill(0x141414)
  g.circle(0, 0, R.track).stroke({ width: 1.5, color: GOLD })
  c.addChild(g)
  // Diamond deflectors on the lower track
  for (let i = 0; i < 8; i++) {
    const a = toPixi((i + 0.5) * (TAU / 8))
    const d = new Graphics().poly([0, -6, 3.2, 0, 0, 6, -3.2, 0]).fill(GOLD).stroke({ width: 1, color: GOLD_DARK })
    d.position.set(Math.cos(a) * 156, Math.sin(a) * 156)
    d.rotation = a + Math.PI / 2
    c.addChild(d)
  }
  return c
}

function head() {
  const c = new Container()
  const numbers = new Container()
  const ring = new Graphics()
  POCKETS.forEach((n, i) => ringSlice(ring, i * SEG, R.numbersInner, R.numbersOuter, FILL[colorOf(n)]))
  numbers.addChild(ring)
  POCKETS.forEach((n, i) => {
    const t = new Text({
      text: String(n),
      style: { fontFamily: 'system-ui, Segoe UI, sans-serif', fontSize: 14, fontWeight: '700', fill: 0xffffff },
    })
    t.anchor.set(0.5)
    const a = toPixi(i * SEG)
    t.position.set(Math.cos(a) * 134, Math.sin(a) * 134)
    t.rotation = i * SEG
    numbers.addChild(t)
  })
  const blur = new BlurFilter({ strength: 0, quality: 3 })
  numbers.filters = [blur]
  c.addChild(numbers)

  const pockets = new Graphics()
  POCKETS.forEach((n, i) => ringSlice(pockets, i * SEG, R.pocketsInner, R.numbersInner, POCKET_FILL[colorOf(n)]))
  // Raised frets between pockets
  POCKETS.forEach((_, i) => {
    const a = toPixi(i * SEG + SEG / 2)
    pockets
      .moveTo(Math.cos(a) * R.pocketsInner, Math.sin(a) * R.pocketsInner)
      .lineTo(Math.cos(a) * R.numbersInner, Math.sin(a) * R.numbersInner)
      .stroke({ width: 2, color: 0xd9b25a })
  })
  pockets.circle(0, 0, R.numbersOuter).stroke({ width: 2, color: GOLD })
  c.addChild(pockets)

  // Wooden cone with gold turret
  const cone = new Graphics()
  for (let r = R.pocketsInner, i = 0; r > 20; r -= 4, i++) {
    const t = i / 20
    const shade = Math.round(0x5a + (0x8a - 0x5a) * t)
    cone.circle(0, 0, r).fill((shade << 16) | (Math.round(shade * 0.62) << 8) | Math.round(shade * 0.26))
  }
  cone.circle(0, 0, R.pocketsInner).stroke({ width: 2, color: GOLD })
  cone.circle(0, 0, 48).stroke({ width: 1, color: GOLD, alpha: 0.5 })
  c.addChild(cone)

  const turret = new Graphics()
  for (const a of [0, Math.PI / 2, Math.PI, (3 * Math.PI) / 2]) {
    const x = Math.cos(a)
    const y = Math.sin(a)
    turret.moveTo(0, 0).lineTo(x * 46, y * 46).stroke({ width: 6, color: GOLD_DARK, cap: 'round' })
    turret.moveTo(0, 0).lineTo(x * 46, y * 46).stroke({ width: 3.5, color: GOLD, cap: 'round' })
    turret.circle(x * 48, y * 48, 6).fill(GOLD).stroke({ width: 1.5, color: GOLD_DARK })
  }
  turret.circle(0, 0, 15).fill(GOLD_DARK)
  turret.circle(0, 0, 12).fill(GOLD)
  turret.circle(-3, -4, 5).fill({ color: 0xfff2c4, alpha: 0.8 })
  c.addChild(turret)

  return { view: c, blur }
}

function ball() {
  const c = new Container()
  c.addChild(new Graphics().circle(2, 3, R.ball).fill({ color: 0x000000, alpha: 0.45 }))
  const g = new Graphics()
  g.circle(0, 0, R.ball).fill(0xd8d8d8)
  g.circle(-0.8, -0.8, R.ball - 1.5).fill(0xf2f2f2)
  g.circle(-2.4, -2.6, 2.4).fill(0xffffff)
  c.addChild(g)
  return c
}

export async function createRouletteWheel(host, { reducedMotion }) {
  const { app, destroy: destroyApp } = await createApp(host)
  const world = new Container()
  app.stage.addChild(world)

  const stage = new Container() // centred wheel
  world.addChild(stage)
  stage.addChild(bowl())
  const wheelHead = head()
  stage.addChild(wheelHead.view)
  const theBall = ball()
  stage.addChild(theBall)
  const sparks = new Container()
  stage.addChild(sparks)
  const particles = new Particles(app.ticker, sparks, 60)

  function layout() {
    const scale = Math.min(app.screen.width, app.screen.height) / SIZE
    world.scale.set(scale)
    world.position.set(app.screen.width / 2, app.screen.height / 2)
  }
  app.renderer.on('resize', layout)
  layout()

  // State, in our angle convention (0 = top, clockwise positive)
  let wheelAngle = 0
  let ballAngle = 0
  let ballRadius = R.ballPocket
  function draw() {
    wheelHead.view.rotation = wheelAngle
    const a = toPixi(ballAngle)
    theBall.position.set(Math.cos(a) * ballRadius, Math.sin(a) * ballRadius)
  }
  draw()

  let active = null

  function sparkle() {
    const a = toPixi(ballAngle)
    const x = Math.cos(a) * ballRadius
    const y = Math.sin(a) * ballRadius
    for (let i = 0; i < (reducedMotion ? 6 : 18); i++) {
      const s = new Graphics().poly([0, -5, 1.4, -1.4, 5, 0, 1.4, 1.4, 0, 5, -1.4, 1.4, -5, 0, -1.4, -1.4]).fill(0xffe08a)
      const dir = rand(0, TAU)
      particles.emit(s, {
        x,
        y,
        vx: Math.cos(dir) * rand(60, 170),
        vy: Math.sin(dir) * rand(60, 170),
        drag: 2.5,
        spin: rand(-8, 8),
        life: rand(500, 900),
        fadeFrom: 0.3,
      })
    }
  }

  // Spins so the ball ends in `number`'s pocket. Resolves when everything has stopped.
  function spinTo(number) {
    const index = POCKETS.indexOf(number)
    const pocket = index * SEG
    const T = reducedMotion ? 1200 : 6500 // ms total
    const T1 = T * 0.62 // ball drops off the track into the pockets
    const D = T - T1
    const ballTurns = reducedMotion ? 1 : 5

    // Wheel: turns clockwise, slowing down (ease-out quadratic)
    const W0 = wheelAngle
    const dW = TAU * (reducedMotion ? 0.5 : 2) + rand(0, TAU)
    const wheelAt = (ms) => {
      const u = Math.min(ms / T, 1)
      return W0 + dW * (1 - (1 - u) ** 2)
    }
    const wheelSpeedAt = (ms) => (dW * 2 * (1 - Math.min(ms / T, 1))) / (T / 1000) // rad/s

    // Ball, phase A: runs counter-clockwise on the track, slowing (g(1) = 1, g'(1) = 0.46).
    // Phase B: after dropping it keeps skidding relative to the wheel and settles:
    // rel(u) = rel1 * (1 - u)^2, which matches the ball's speed at the drop and ends at 0.
    // rel1 depends on the run length, so solve the two together (converges in a few steps).
    const g = (u) => (u * (2 - 0.7 * u)) / 1.3
    const gSpeedEnd = (2 - 1.4) / 1.3
    const B0 = ballAngle
    const r0 = ballRadius
    // Whole turns are fixed on the first pass so later passes change dB smoothly
    const first = B0 - (wheelAt(T1) + pocket)
    const turns = Math.ceil(-first / TAU) + ballTurns
    const runLength = (rel) => B0 - (wheelAt(T1) + pocket + rel) + TAU * turns
    let rel1 = 0
    for (let i = 0; i < 8; i++) {
      const ballSpeed = -(runLength(rel1) * gSpeedEnd) / (T1 / 1000) // rad/s, negative = counter-clockwise
      const v1 = ballSpeed - wheelSpeedAt(T1) // relative to the wheel
      rel1 = (-v1 * (D / 1000)) / 2
    }
    const dB = runLength(rel1)

    const start = performance.now()
    return new Promise((resolve) => {
      function frame(elapsed) {
        wheelAngle = wheelAt(elapsed)
        if (elapsed < T1) {
          const u = elapsed / T1
          ballAngle = B0 - dB * g(u)
          const up = Math.min(elapsed / 400, 1)
          const lift = r0 + (R.ballTrack - r0) * (1 - (1 - up) ** 3)
          // Losing speed, it drifts down the track before dropping
          ballRadius = u > 0.75 ? lift - ((u - 0.75) / 0.25) * 7 : lift
        } else {
          const u = Math.min((elapsed - T1) / D, 1)
          const rel = rel1 * (1 - u) ** 2 + Math.sin(u * Math.PI * 9) * (1 - u) ** 3 * 0.06
          ballAngle = wheelAngle + pocket + rel
          const drop = Math.min((elapsed - T1) / 350, 1)
          const fall = R.ballTrack - 7 - (R.ballTrack - 7 - R.ballPocket) * drop ** 2
          // Bounces off the frets, smaller each time
          const hop = Math.abs(Math.sin(u * Math.PI * 7)) * (1 - u) ** 2 * 14
          ballRadius = (drop < 1 ? fall : R.ballPocket) + (drop < 1 ? 0 : hop)
        }
        wheelHead.blur.strength = Math.min(4, Math.abs(wheelSpeedAt(elapsed)) * 0.8)
        draw()
      }

      function finish() {
        if (!active) return
        app.ticker.remove(active.step)
        clearTimeout(active.timer)
        active = null
        frame(T)
        wheelAngle %= TAU
        ballAngle = wheelAngle + pocket
        ballRadius = R.ballPocket
        wheelHead.blur.strength = 0
        draw()
        sparkle()
        resolve()
      }

      const step = () => {
        const elapsed = performance.now() - start
        if (elapsed >= T) finish()
        else frame(elapsed)
      }
      // Wall-clock based, so a hidden tab catches up; the timer finishes if frames stop entirely
      active = { step, timer: setTimeout(finish, T + 2500) }
      app.ticker.add(step)
    })
  }

  return {
    spinTo,
    destroy() {
      if (active) {
        clearTimeout(active.timer)
        active = null
      }
      particles.destroy()
      destroyApp()
    },
  }
}
