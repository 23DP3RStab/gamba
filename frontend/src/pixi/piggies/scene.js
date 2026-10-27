// The Three Piggies scene. Vue (PiggiesScene.vue) calls these actions; this file only draws.
import { Assets, Container, Graphics, Sprite, Texture } from 'pixi.js'
import { createApp } from '../stage'
import { Particles, rand } from '../particles'
import { ease, tween } from '../tween'
import { createScenery } from './scenery'
import { createHouse, buildUp, collapse, holdFirm, showFallen, wobble, DEBRIS_COLORS } from './houses'
import { createPig } from './pigs'
import { createWolf, MOUTH } from './wolf'
import { GROUND_Y, HOUSE_X, WOLF_GAP, WORLD_W } from './style'

const ATLAS = `${import.meta.env.BASE_URL}assets/piggies/piggies.json`
const KINDS = ['straw', 'wood', 'brick']
const NARROW_VIEW = 820 // world units shown on phones; the camera follows the wolf
const WIDE_SCREEN = 700 // css px

const wolfX = (i) => HOUSE_X[i] - WOLF_GAP

function coin() {
  const g = new Graphics()
  g.circle(0, 0, 13).fill(0xf0b400).stroke({ width: 3, color: 0x6b4700 })
  g.circle(0, 0, 8).stroke({ width: 2, color: 0xffd65a })
  return g
}

export async function createPiggiesScene(host, { reducedMotion }) {
  const animate = !reducedMotion
  const fx = animate ? 1 : 0.3 // particle amount
  const [{ app, destroy: destroyApp }, sheet] = await Promise.all([createApp(host), Assets.load(ATLAS)])
  const { ticker } = app

  const world = new Container()
  const camera = new Container()
  world.addChild(camera)
  app.stage.addChild(world)

  const scenery = createScenery(sheet, { animate })
  const houseLayer = new Container()
  const pigLayer = new Container()
  const wolfLayer = new Container()
  const fxLayer = new Container()
  camera.addChild(scenery.back, houseLayer, pigLayer, wolfLayer, scenery.front, fxLayer)

  const particles = new Particles(ticker, fxLayer, animate ? 260 : 60)
  const wolf = createWolf(sheet)
  wolfLayer.addChild(wolf.view)

  let houses = []
  let pigs = []
  let wolfAt = 0
  let target = 0 // house the wind is aimed at
  let windOn = false
  let windClock = 0
  let zoom = 1
  let shakeLeft = 0
  let camX = 0
  let time = 0

  // --- layout: wide screens see the whole street, phones follow the wolf ---
  const view = () => (app.screen.width >= WIDE_SCREEN ? WORLD_W : NARROW_VIEW)
  const focusX = () => (view() === WORLD_W ? WORLD_W / 2 : HOUSE_X[target] - WOLF_GAP / 2)
  function layout() {
    const scale = app.screen.width / view()
    world.scale.set(scale)
    world.y = app.screen.height - (GROUND_Y + 90) * scale
  }
  app.renderer.on('resize', layout)
  layout()
  camX = focusX()

  function placeWolf(i) {
    wolfAt = i
    target = i
    wolf.view.position.set(wolfX(i), GROUND_Y + 6)
  }

  function rebuild(states = KINDS.map(() => 'standing'), withAnimation = true) {
    for (const h of houses) h.view.destroy({ children: true })
    for (const p of pigs) p.view.destroy({ children: true })
    houses = KINDS.map((kind, i) => createHouse(kind, i))
    pigs = KINDS.map((kind, i) => {
      const pig = createPig()
      pig.view.position.set(HOUSE_X[i] - houses[i].k.w / 2 + 40, GROUND_Y + 4)
      return pig
    })
    houses.forEach((h, i) => {
      houseLayer.addChild(h.view)
      pigLayer.addChild(pigs[i].view)
      if (states[i] === 'fallen') {
        showFallen(h)
        pigs[i].view.visible = false
      } else if (withAnimation) {
        buildUp(h, ticker, i * 130, animate)
      }
    })
  }

  // --- per-frame updates ---
  function emitWind() {
    const dir = wolf.facing
    const mx = wolf.view.x + MOUTH.x * dir
    const my = wolf.view.y + MOUTH.y
    // Gust streaks
    const streak = new Sprite(Texture.WHITE)
    streak.anchor.set(0, 0.5)
    streak.width = rand(70, 140)
    streak.height = rand(3, 5)
    streak.alpha = 0.75
    streak.blendMode = 'add'
    particles.emit(streak, {
      x: mx,
      y: my + rand(-40, 45),
      vx: rand(700, 950),
      vy: rand(-30, 30),
      life: rand(320, 480),
      fadeFrom: 0.2,
    })
    // Bits torn off the house the wolf is blowing at
    if (Math.random() < 0.55 * fx) {
      const h = houses[target]
      const colors = DEBRIS_COLORS[h.kind]
      const size = h.kind === 'straw' ? [rand(10, 18), 3] : h.kind === 'wood' ? [rand(8, 16), 4] : [rand(5, 8), rand(5, 8)]
      const bit = new Graphics()
        .rect(-size[0] / 2, -size[1] / 2, size[0], size[1])
        .fill(colors[Math.floor(Math.random() * colors.length)])
      particles.emit(bit, {
        x: HOUSE_X[target] + rand(-90, 60),
        y: GROUND_Y - rand(40, h.k.wallH + h.k.roofH * 0.6),
        vx: rand(350, 650),
        vy: rand(-160, 40),
        gravity: 180,
        spin: rand(-10, 10),
        life: rand(700, 1100),
      })
    }
  }

  ticker.add((t) => {
    const dt = Math.min(t.deltaMS, 50) / 1000
    time += dt
    scenery.update(dt)
    wolf.update(dt)
    for (const p of pigs) p.update(dt)
    for (const h of houses) wobble(h, time)

    if (windOn) {
      windClock += dt
      while (windClock > 0.045 / fx) {
        windClock -= 0.045 / fx
        emitWind()
      }
    }

    // Camera: follow on phones, zoom towards the target house, shake on impact
    camX += (focusX() - camX) * Math.min(1, dt * 4)
    const scale = world.scale.x
    world.x = app.screen.width / 2 - camX * scale
    const fx0 = HOUSE_X[target] - 60
    const fy0 = GROUND_Y - 120
    camera.pivot.set(fx0, fy0)
    camera.scale.set(zoom)
    let sx = 0
    let sy = 0
    if (shakeLeft > 0) {
      shakeLeft -= dt
      const k = Math.max(0, shakeLeft / 0.45) * 9
      sx = rand(-k, k)
      sy = rand(-k, k) * 0.6
    }
    camera.position.set(fx0 + sx, fy0 + sy)
  })

  // --- actions called from Vue ---
  async function walkTo(i, ms = 650) {
    target = i
    if (wolfAt === i) return
    const from = wolf.view.x
    const to = wolfX(i)
    wolf.face(to < from ? -1 : 1)
    wolf.play('run')
    wolfAt = i
    await tween(ticker, animate ? ms : 1, (t) => (wolf.view.x = from + (to - from) * t), ease.inOutCubic)
    wolf.face(1)
    wolf.play('idle')
  }

  function huffStart(i) {
    target = i
    windOn = true
    wolf.setPuffing(true)
    houses[i].shake = 1
    if (animate) tween(ticker, 600, (t) => (zoom = 1 + 0.06 * t), ease.inOutCubic)
  }

  function huffStop() {
    windOn = false
    wolf.setPuffing(false)
    for (const h of houses) h.shake = 0
    if (animate && zoom !== 1) {
      const z = zoom
      tween(ticker, 500, (t) => (zoom = z + (1 - z) * t), ease.inOutCubic)
    }
  }

  function houseFell(i) {
    if (animate) shakeLeft = 0.45
    collapse(houses[i], { ticker, particles, animate, amount: fx })
    pigs[i].flee(ticker, animate)
  }

  function houseHeld(i) {
    holdFirm(houses[i], { ticker, particles, animate })
    pigs[i].cheer(ticker, animate)
  }

  function setScared(index) {
    pigs.forEach((p, i) => p.setMood(i === index ? 'scared' : 'idle'))
  }

  function coinBurst(x, y, count = 16) {
    for (let i = 0; i < Math.round(count * fx); i++) {
      const c = coin()
      const a = (i / count) * Math.PI * 2
      particles.emit(c, {
        x,
        y,
        vx: Math.cos(a) * rand(200, 420),
        vy: Math.sin(a) * rand(200, 420) - 380,
        gravity: 1300,
        floor: GROUND_Y + 20,
        life: 1500,
        fadeFrom: 0.6,
        onUpdate: (p) => (p.display.scale.x = Math.cos(p.age / 70)),
      })
    }
  }

  function coinShower(count = 46) {
    const scale = world.scale.x
    const half = app.screen.width / scale / 2
    const top = -world.y / scale
    for (let i = 0; i < Math.round(count * fx); i++) {
      const c = coin()
      particles.emit(c, {
        x: camX + rand(-half, half),
        y: top - rand(20, 400),
        vx: rand(-40, 40),
        vy: rand(80, 220),
        gravity: 900,
        floor: GROUND_Y + rand(10, 40),
        bounce: 0.4,
        life: rand(1800, 2600),
        fadeFrom: 0.75,
        onUpdate: (p) => (p.display.scale.x = Math.cos(p.age / 80)),
      })
    }
  }

  function celebrate(kind) {
    if (kind === 'fell') coinBurst(HOUSE_X[target], GROUND_Y - 200)
    else if (kind === 'cashed') coinBurst(camX, GROUND_Y - 280, 22)
    else if (kind === 'won') coinShower()
  }

  return {
    rebuild,
    placeWolf,
    walkTo,
    huffStart,
    huffStop,
    houseFell,
    houseHeld,
    setScared,
    setBeaten: (on) => wolf.setBeaten(on),
    celebrate,
    destroy() {
      particles.destroy()
      destroyApp()
    },
  }
}
