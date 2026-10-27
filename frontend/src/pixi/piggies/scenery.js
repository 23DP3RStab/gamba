// Night backdrop: sky, stars, moon, clouds, hills, Segel's trees and bushes, ground.
import { Container, Graphics, Sprite } from 'pixi.js'
import { INK, LINE, GROUND_Y, WORLD_W, blob } from './style'

const SKY_TOP = -900 // the sky reaches far up so tall (phone) views never show its edge
const NIGHT_TINT = 0x8ea3c9 // darkens the daytime sprites for a moonlit scene

function lerpColor(a, b, t) {
  const r = ((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t
  const g = ((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t
  const bl = (a & 255) + ((b & 255) - (a & 255)) * t
  return (Math.round(r) << 16) | (Math.round(g) << 8) | Math.round(bl)
}

function sky() {
  const g = new Graphics()
  const stops = [
    [SKY_TOP, 0x03070f],
    [0, 0x0a1830],
    [380, 0x1c3863],
    [GROUND_Y, 0x35588a],
  ]
  for (let i = 0; i < stops.length - 1; i++) {
    const [y0, c0] = stops[i]
    const [y1, c1] = stops[i + 1]
    const bands = Math.ceil((y1 - y0) / 12)
    for (let b = 0; b < bands; b++) {
      const y = y0 + ((y1 - y0) * b) / bands
      g.rect(-400, y, WORLD_W + 800, (y1 - y0) / bands + 1).fill(lerpColor(c0, c1, b / bands))
    }
  }
  return g
}

function moon() {
  const c = new Container()
  // Soft halo: many faint rings stacked so no single edge shows
  const halo = new Graphics()
  for (let r = 160; r > 54; r -= 6) halo.circle(0, 0, r).fill({ color: 0xfff1c2, alpha: 0.018 })
  c.addChild(halo)
  c.addChild(new Graphics().circle(0, 0, 52).fill(0xf6e7b0).stroke({ width: LINE, color: INK }))
  c.addChild(new Graphics().circle(14, 8, 46).fill({ color: 0xd9c27f, alpha: 0.35 })) // shade side
  for (const [x, y, r] of [
    [-14, -14, 10],
    [16, 12, 7],
    [-6, 22, 5],
  ])
    c.addChild(new Graphics().circle(x, y, r).fill(0xe0cd8e).stroke({ width: 3, color: 0xc4ad66 }))
  return c
}

function cloud(scale) {
  const g = blob(
    [
      [-60, 10, 34],
      [-20, -12, 44],
      [30, -4, 38],
      [70, 14, 28],
      [0, 20, 34],
    ],
    0x33476e,
    LINE,
    0x141b2e,
  )
  g.scale.set(scale)
  g.alpha = 0.9
  return g
}

function hill(points, fill, outline) {
  const g = new Graphics()
  g.moveTo(-400, GROUND_Y + 10)
  for (const [x, y] of points) g.lineTo(x, y)
  g.lineTo(WORLD_W + 400, GROUND_Y + 10).closePath().fill(fill)
  if (outline) {
    g.moveTo(points[0][0], points[0][1])
    for (const [x, y] of points.slice(1)) g.lineTo(x, y)
    g.stroke({ width: LINE, color: INK, join: 'round' })
  }
  return g
}

// Smooth hill outline sampled from a few sine waves
function hillPoints(base, amp, phase) {
  const pts = []
  for (let x = -400; x <= WORLD_W + 400; x += 40) {
    const y = base - amp * (0.6 * Math.sin((x + phase) / 210) + 0.4 * Math.sin((x + phase * 2) / 97))
    pts.push([x, y])
  }
  return pts
}

function sprite(sheet, name, x, scale, { tint = NIGHT_TINT, y = GROUND_Y } = {}) {
  const s = new Sprite(sheet.textures[name])
  s.anchor.set(0.5, 1)
  s.position.set(x, y)
  s.scale.set(scale)
  s.tint = tint
  return s
}

// Returns { back, front, update(dt) }: `back` goes behind the houses, `front` in front of them
export function createScenery(sheet, { animate }) {
  const back = new Container()
  const front = new Container()

  back.addChild(sky())

  const stars = new Container()
  let seed = 7
  const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647
  for (let i = 0; i < 70; i++) {
    const s = new Graphics().circle(0, 0, 1 + rnd() * 2).fill(0xffffff)
    s.position.set(-300 + rnd() * (WORLD_W + 600), SKY_TOP + 100 + rnd() * (340 - SKY_TOP))
    s.phase = rnd() * Math.PI * 2
    s.speed = 0.6 + rnd() * 1.4
    stars.addChild(s)
  }
  back.addChild(stars)

  const theMoon = moon()
  theMoon.position.set(1380, 120)
  back.addChild(theMoon)

  const clouds = new Container()
  for (const [x, y, s, v] of [
    [200, 90, 1, 8],
    [760, 40, 0.75, 5],
    [1150, 190, 0.6, 11],
    [-200, -150, 0.9, 6],
    [1500, -60, 0.7, 9],
  ]) {
    const c = cloud(s)
    c.position.set(x, y)
    c.drift = v
    clouds.addChild(c)
  }
  back.addChild(clouds)

  back.addChild(hill(hillPoints(430, 50, 0), 0x16294a))
  // Trees stand on the near hill, behind the houses
  for (const [name, x, scale] of [
    ['tree1', 120, 0.9],
    ['tree2', 360, 0.75],
    ['tree1', 760, 0.8],
    ['tree2', 1130, 0.85],
    ['tree1', 1510, 0.95],
  ])
    back.addChild(sprite(sheet, name, x, scale, { y: GROUND_Y - 60, tint: 0x6d84ad }))
  back.addChild(hill(hillPoints(540, 28, 400), 0x1e4a33, true))

  // Ground with a dirt path
  const ground = new Graphics()
  ground.rect(-400, GROUND_Y, WORLD_W + 800, 400).fill(0x2f6b34)
  ground.moveTo(-400, GROUND_Y).lineTo(WORLD_W + 400, GROUND_Y).stroke({ width: LINE, color: INK })
  ground.roundRect(-400, GROUND_Y + 30, WORLD_W + 800, 34, 17).fill(0x5d4630)
  ground.rect(-400, GROUND_Y + 110, WORLD_W + 800, 300).fill(0x24552a)
  back.addChild(ground)

  for (const [name, x, scale] of [
    ['bush1', 40, 0.7],
    ['bush2', 740, 0.6],
    ['grass', 1110, 0.9],
    ['bush1', 1520, 0.6],
    ['stump', 420, 0.45],
  ])
    back.addChild(sprite(sheet, name, x, scale))

  for (const [name, x, scale] of [
    ['grass', 205, 1],
    ['rock', 1080, 0.55],
    ['grass', 1460, 0.8],
    ['bush2', 1590, 0.55],
  ])
    front.addChild(sprite(sheet, name, x, scale, { y: GROUND_Y + 34 }))

  let time = 0
  function update(dt) {
    time += dt
    for (const s of stars.children) s.alpha = 0.45 + 0.4 * Math.sin(time * s.speed + s.phase)
    if (!animate) return
    for (const c of clouds.children) {
      c.x += c.drift * dt
      if (c.x > WORLD_W + 300) c.x = -300
    }
  }

  return { back, front, update }
}
