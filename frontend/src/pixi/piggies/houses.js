// Straw, wood and brick houses drawn in code, built from separate pieces so they can fall apart.
import { Container, Graphics } from 'pixi.js'
import { INK, LINE, GROUND_Y, HOUSE_X, blob } from './style'
import { rand } from '../particles'
import { ease, tween, wait } from '../tween'

const KINDS = {
  straw: {
    w: 210,
    wallH: 135,
    roofH: 125,
    wall: 0xe2bf55,
    wallLine: 0xb08a2c,
    roof: 0xd8aa3c,
    roofLine: 0x9a7424,
    debris: [0xe2bf55, 0xd8aa3c, 0xc79a2f],
  },
  wood: {
    w: 214,
    wallH: 150,
    roofH: 110,
    wall: 0x9a6233,
    wallLine: 0x5e381a,
    roof: 0x6b3f1f,
    roofLine: 0x43260f,
    debris: [0x9a6233, 0x7a4a24, 0x6b3f1f],
  },
  brick: {
    w: 222,
    wallH: 160,
    roofH: 100,
    wall: 0xb5462f,
    wallLine: 0xe8d2bc,
    roof: 0x575d6b,
    roofLine: 0x353a45,
    debris: [0xb5462f, 0x8e3424, 0x575d6b],
  },
}

export const DEBRIS_COLORS = Object.fromEntries(Object.entries(KINDS).map(([k, v]) => [k, v.debris]))

function walls(k, kind) {
  // Origin at the bottom-right corner, so the walls tip over to the right when they fall
  const g = new Graphics()
  const { w, wallH: h } = k
  g.rect(-w, -h, w, h).fill(k.wall)
  if (kind === 'straw') {
    for (let x = -w + 8; x < -4; x += 9) {
      g.moveTo(x, -h + 4)
        .quadraticCurveTo(x + 3, -h / 2, x - 1, -4)
        .stroke({ width: 2.5, color: k.wallLine, alpha: 0.85 })
    }
    for (const f of [0.36, 0.7]) g.moveTo(-w, -h * f).lineTo(0, -h * f).stroke({ width: 4, color: 0x9a7424 })
  } else if (kind === 'wood') {
    for (let y = -h + 22; y < 0; y += 22) g.moveTo(-w, y).lineTo(0, y).stroke({ width: 3, color: k.wallLine })
    let row = 0
    for (let y = -h; y < 0; y += 22, row++) {
      const x = -w + (row % 2 ? 70 : 140)
      g.moveTo(x, y).lineTo(x, Math.min(y + 22, 0)).stroke({ width: 3, color: k.wallLine })
    }
    for (const [x, y] of [
      [-170, -110],
      [-60, -60],
      [-140, -30],
    ])
      g.circle(x, y, 3).fill(k.wallLine)
  } else {
    let row = 0
    for (let y = -h + 16; y < 0; y += 16, row++) {
      g.moveTo(-w, y).lineTo(0, y).stroke({ width: 2, color: k.wallLine, alpha: 0.9 })
      for (let x = -w + (row % 2 ? 17 : 34); x < 0; x += 34) {
        g.moveTo(x, y - 16).lineTo(x, y).stroke({ width: 2, color: k.wallLine, alpha: 0.9 })
      }
    }
  }
  g.rect(-w * 0.28, -h, w * 0.28, h).fill({ color: 0x000000, alpha: 0.13 }) // cel shade
  g.rect(-w, -h, w, h).stroke({ width: LINE, color: INK, join: 'round' })
  return g
}

function roof(k, kind) {
  // Centred on its own middle so it can spin when it flies off
  const g = new Graphics()
  const half = k.w / 2 + 26
  const top = -k.roofH / 2
  const bottom = k.roofH / 2
  const halfAt = (y) => (half * (y - top)) / k.roofH
  g.poly([-half, bottom, 0, top, half, bottom]).fill(k.roof)
  if (kind === 'straw') {
    for (let x = -half + 14; x < half; x += 16) g.moveTo(0, top + 8).lineTo(x, bottom - 4).stroke({ width: 2.5, color: k.roofLine })
    const teeth = []
    for (let x = -half; x <= half; x += 18) teeth.push(x, bottom - 2, x + 9, bottom + 12)
    teeth.push(half, bottom - 2)
    g.poly(teeth, false).stroke({ width: 4, color: INK, join: 'round' })
  } else {
    for (let y = top + 22; y < bottom; y += 20) {
      const hw = halfAt(y) - 6
      g.moveTo(-hw, y).lineTo(hw, y).stroke({ width: 3, color: k.roofLine })
    }
  }
  g.poly([0, top, half, bottom, half * 0.55, bottom]).fill({ color: 0x000000, alpha: 0.12 })
  g.poly([-half, bottom, 0, top, half, bottom]).stroke({ width: LINE, color: INK, join: 'round' })
  return g
}

function door() {
  const g = new Graphics()
  g.roundRect(-23, -37, 46, 74, 12).fill(0x4a2c14)
  g.moveTo(-8, -32).lineTo(-8, 37).stroke({ width: 2.5, color: 0x2e1a0b })
  g.moveTo(8, -32).lineTo(8, 37).stroke({ width: 2.5, color: 0x2e1a0b })
  g.roundRect(-23, -37, 46, 74, 12).stroke({ width: 4, color: INK })
  g.circle(14, 4, 3.5).fill(0xf0a500).stroke({ width: 1.5, color: INK })
  return g
}

function windowLight() {
  const c = new Container()
  const glow = new Graphics().circle(0, 0, 48).fill(0xffd36b)
  glow.alpha = 0.16
  const pane = new Graphics()
  pane.rect(-21, -21, 42, 42).fill(0xffe08a)
  pane.rect(-21, -21, 42, 18).fill({ color: 0xffffff, alpha: 0.25 })
  pane.moveTo(0, -21).lineTo(0, 21).moveTo(-21, 0).lineTo(21, 0).stroke({ width: 4, color: INK })
  pane.rect(-21, -21, 42, 42).stroke({ width: 4, color: INK })
  c.addChild(glow, pane)
  c.glow = glow
  return c
}

function chimney() {
  const g = new Graphics()
  g.rect(-14, -32, 28, 64).fill(0x8e3424)
  g.moveTo(-14, -8).lineTo(14, -8).moveTo(-14, 14).lineTo(14, 14).stroke({ width: 2, color: 0xe8d2bc })
  g.rect(-14, -32, 28, 64).stroke({ width: 4, color: INK })
  g.rect(-19, -40, 38, 10).fill(0x6e2a1c).stroke({ width: 4, color: INK })
  return g
}

function rubble(k, kind) {
  const g = new Container()
  const c = k.debris
  const pieces =
    kind === 'straw'
      ? [
          [-80, -10, 70, 12, -0.15],
          [-20, -8, 90, 14, 0.08],
          [50, -12, 60, 11, -0.3],
          [-40, -22, 50, 10, 0.25],
        ]
      : kind === 'wood'
        ? [
            [-85, -9, 90, 12, -0.12],
            [-10, -10, 100, 13, 0.1],
            [60, -14, 70, 12, -0.4],
            [-45, -26, 80, 11, 0.3],
          ]
        : [
            [-70, -10, 30, 16, 0.2],
            [-35, -12, 30, 16, -0.1],
            [0, -9, 30, 16, 0.05],
            [35, -13, 30, 16, 0.3],
            [-15, -28, 30, 16, -0.25],
            [20, -27, 30, 16, 0.15],
          ]
  pieces.forEach(([x, y, w, h, r], i) => {
    const p = new Graphics().roundRect(-w / 2, -h / 2, w, h, 3).fill(c[i % c.length]).stroke({ width: 3, color: INK })
    p.position.set(x, y)
    p.rotation = r
    g.addChild(p)
  })
  return g
}

// Builds one house at its slot. Returns the view plus its pieces.
export function createHouse(kind, index) {
  const k = KINDS[kind]
  const view = new Container()
  view.position.set(HOUSE_X[index], GROUND_Y)

  const parts = {}
  if (kind === 'brick') {
    parts.chimney = chimney()
    parts.chimney.position.set(k.w * 0.24, -k.wallH - k.roofH * 0.55)
    view.addChild(parts.chimney)
  }
  parts.walls = walls(k, kind)
  parts.walls.position.set(k.w / 2, 0)
  parts.door = door()
  parts.door.position.set(10, -37)
  parts.window = windowLight()
  parts.window.position.set(68, -k.wallH + 46)
  parts.roof = roof(k, kind)
  parts.roof.position.set(0, -k.wallH - k.roofH / 2 + 12)
  view.addChild(parts.walls, parts.door, parts.window, parts.roof)

  const pile = rubble(k, kind)
  pile.alpha = 0
  view.addChildAt(pile, 0)

  return { kind, index, view, parts, pile, k, shake: 0, fallen: false }
}

// Plays the build-up animation when a new round starts
export function buildUp(house, ticker, delay, animate) {
  if (!animate) return
  house.view.scale.set(1, 0.05)
  house.view.alpha = 0
  wait(ticker, delay).then(() =>
    tween(
      ticker,
      520,
      (t) => {
        house.view.scale.y = 0.05 + 0.95 * t
        house.view.alpha = Math.min(1, t * 3)
      },
      ease.outBack,
      house.view,
    ),
  )
}

// Shows a fallen house instantly (used when the scene mounts mid-round)
export function showFallen(house) {
  for (const part of Object.values(house.parts)) part.visible = false
  house.pile.alpha = 1
  house.fallen = true
}

function smoke(particles, x, y, count) {
  for (let i = 0; i < count; i++) {
    const r = rand(16, 30)
    const puff = blob([[0, 0, r]], 0xd9dde6, 4)
    puff.alpha = 0.95
    puff.scale.set(0.5)
    particles.emit(puff, {
      x: x + rand(-90, 90),
      y: y + rand(-30, 10),
      vx: rand(-260, 260),
      vy: rand(-220, -40),
      drag: 1.8,
      gravity: -40,
      grow: 0.9,
      life: rand(900, 1400),
      fadeFrom: 0.35,
    })
  }
}

function chunks(particles, house, count) {
  const colors = house.k.debris
  for (let i = 0; i < count; i++) {
    const w = rand(10, 26)
    const h = rand(6, 12)
    const c = new Graphics().rect(-w / 2, -h / 2, w, h).fill(colors[i % colors.length]).stroke({ width: 2, color: INK })
    particles.emit(c, {
      x: house.view.x + rand(-90, 90),
      y: GROUND_Y - rand(20, house.k.wallH),
      vx: rand(-120, 520),
      vy: rand(-700, -250),
      gravity: 1500,
      spin: rand(-9, 9),
      floor: GROUND_Y + rand(10, 30),
      life: rand(1500, 2300),
      fadeFrom: 0.7,
    })
  }
}

// The wolf blew it down: roof, door, window and chimney fly off, walls tip over, smoke and bits
export async function collapse(house, { ticker, particles, animate, amount = 1 }) {
  if (house.fallen) return
  house.fallen = true
  house.shake = 0
  house.view.rotation = 0
  house.view.skew.set(0, 0)
  const fly = [
    ['roof', 640, -680, 4],
    ['door', 380, -520, -6],
    ['window', 600, -440, 7],
    ['chimney', 460, -780, 5],
  ]
  for (const [name, vx, vy, spin] of fly) {
    const part = house.parts[name]
    if (!part) continue
    const x = house.view.x + part.x
    const y = house.view.y + part.y
    house.view.removeChild(part)
    delete house.parts[name]
    if (!animate) {
      part.destroy()
      continue
    }
    particles.emit(part, {
      x,
      y,
      vx: vx * rand(0.8, 1.15),
      vy: vy * rand(0.85, 1.1),
      gravity: 1500,
      spin: spin * rand(0.7, 1.2),
      floor: GROUND_Y + 15,
      life: 1900,
      fadeFrom: 0.6,
    })
  }

  const wallsPart = house.parts.walls
  if (animate) {
    smoke(particles, house.view.x, GROUND_Y - 40, Math.round(12 * amount))
    chunks(particles, house, Math.round(20 * amount))
    tween(ticker, 420, (t) => (wallsPart.rotation = 1.5 * t), ease.inCubic, wallsPart).then(() =>
      tween(
        ticker,
        260,
        (t) => (wallsPart.rotation = 1.5 - 0.08 * Math.sin(Math.PI * t)),
        ease.outQuad,
        wallsPart,
      ),
    )
    await wait(ticker, 450)
    tween(ticker, 400, (t) => (house.pile.alpha = t), ease.outCubic, house.pile)
    await wait(ticker, 350)
    await tween(ticker, 500, (t) => (wallsPart.alpha = 1 - t), ease.outCubic, wallsPart)
  } else {
    house.pile.alpha = 1
  }
  if (!wallsPart.destroyed) wallsPart.visible = false
}

// The house held: it stops shaking with a thud and its window flashes
export async function holdFirm(house, { ticker, particles, animate }) {
  house.shake = 0
  house.view.rotation = 0
  house.view.skew.set(0, 0)
  if (!animate) return
  for (const side of [-1, 1]) {
    for (let i = 0; i < 3; i++) {
      const puff = blob([[0, 0, rand(8, 14)]], 0xc9c3b6, 3)
      particles.emit(puff, {
        x: house.view.x + side * (house.k.w / 2 + 6),
        y: GROUND_Y - 8,
        vx: side * rand(60, 160),
        vy: rand(-90, -30),
        drag: 2,
        grow: 0.8,
        life: 700,
        fadeFrom: 0.3,
      })
    }
  }
  await tween(ticker, 160, (t) => (house.view.scale.y = 1 - 0.05 * t), ease.outQuad, house.view)
  await tween(ticker, 380, (t) => (house.view.scale.y = 0.95 + 0.05 * t), ease.outBack, house.view)
  const glow = house.parts.window?.glow
  if (glow && !glow.destroyed) {
    await tween(ticker, 200, (t) => (glow.alpha = 0.16 + 0.44 * t), ease.outCubic, glow)
    await tween(ticker, 500, (t) => (glow.alpha = 0.6 - 0.44 * t), ease.outCubic, glow)
  }
}

// Per-frame wobble while the wolf blows
export function wobble(house, time) {
  if (house.fallen) return
  const s = house.shake
  house.view.rotation = Math.sin(time * 38) * 0.025 * s
  house.view.skew.x = Math.sin(time * 29) * 0.05 * s
}
