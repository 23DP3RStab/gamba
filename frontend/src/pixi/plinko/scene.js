// The Plinko board: pegs, 13 slots and the falling balls. PlinkoBoard.vue says where each ball
// goes; this file only draws. The path comes from the server, so where a ball lands is decided
// before it starts falling.
import { Container, Graphics, Text } from 'pixi.js'
import { createApp } from '../stage'
import { ROWS } from '../../plinko'

const GAP = 40 // world units between pegs
const PEG_R = 5
const BALL_R = 9
const TOP = 70 // y of the first row of pegs
const SLOT_W = GAP - 5
const SLOT_H = 30
const SLOT_Y = TOP + ROWS * GAP // centre of the slot row
const WORLD_W = (ROWS + 3) * GAP
const WORLD_H = SLOT_Y + SLOT_H / 2 + 14
const CX = WORLD_W / 2

// Bigger multipliers glow hotter; losing slots stay muted
function slotColor(multiplier) {
  if (multiplier >= 10) return 0xff4d4d
  if (multiplier >= 3) return 0xff8a3c
  if (multiplier >= 1.5) return 0xf0b400
  if (multiplier >= 1) return 0xc9a227
  return 0x5b5b68
}

const label = (multiplier) => `${Number(multiplier).toLocaleString('lv-LV')}×`

// Where the ball rests on its way down: on a peg in each row, then in its slot
function stops(path, slot) {
  const points = [{ x: CX, y: TOP - GAP * 1.5 }]
  let rights = 0
  for (let row = 0; row < ROWS; row++) {
    points.push({ x: CX + (rights - row / 2) * GAP, y: TOP + row * GAP - PEG_R - BALL_R })
    if (path[row] === 'R') rights++
  }
  points.push({ x: CX + (slot - ROWS / 2) * GAP, y: SLOT_Y })
  return points
}

export async function createPlinkoScene(host, { reducedMotion }) {
  const stepMs = reducedMotion ? 45 : 120 // time from one peg to the next
  const { app, destroy: destroyApp } = await createApp(host)
  const { ticker } = app

  const world = new Container()
  app.stage.addChild(world)

  const pegs = new Graphics()
  for (let row = 0; row < ROWS; row++) {
    for (let k = 0; k < row + 3; k++) {
      pegs.circle(CX + (k - (row + 2) / 2) * GAP, TOP + row * GAP, PEG_R)
    }
  }
  pegs.fill(0xe8e8f0)
  world.addChild(pegs)

  const slots = []
  for (let slot = 0; slot <= ROWS; slot++) {
    const view = new Container()
    view.position.set(CX + (slot - ROWS / 2) * GAP, SLOT_Y)
    const box = new Graphics()
    const text = new Text({
      text: '',
      style: { fill: 0x111116, fontSize: 12, fontWeight: '800', fontFamily: 'Segoe UI, Roboto, sans-serif' },
    })
    text.anchor.set(0.5)
    view.addChild(box, text)
    world.addChild(view)
    slots.push({ view, box, text, glow: 0 })
  }

  const ballLayer = new Container()
  world.addChild(ballLayer)
  const balls = new Set()

  function setMultipliers(list) {
    slots.forEach((slot, i) => {
      const multiplier = Number(list?.[i] ?? 0)
      slot.box.clear()
      slot.box.roundRect(-SLOT_W / 2, -SLOT_H / 2, SLOT_W, SLOT_H, 6).fill(slotColor(multiplier))
      slot.text.text = list?.length ? label(multiplier) : ''
      slot.text.style.fill = multiplier < 1 ? 0xd8d8e0 : 0x111116
    })
  }

  // Fits the board into the host, centred
  function layout() {
    const scale = Math.min(app.screen.width / WORLD_W, app.screen.height / WORLD_H)
    world.scale.set(scale)
    world.position.set((app.screen.width - WORLD_W * scale) / 2, (app.screen.height - WORLD_H * scale) / 2)
  }

  function land(ball) {
    balls.delete(ball)
    ball.view.destroy()
    slots[ball.slot].glow = 1
    ball.resolve()
  }

  ticker.add((t) => {
    layout()
    for (const ball of balls) {
      ball.t += t.deltaMS / stepMs
      const last = ball.points.length - 1
      const seg = Math.min(Math.floor(ball.t), last - 1)
      const local = Math.min(ball.t - seg, 1)
      const a = ball.points[seg]
      const b = ball.points[seg + 1]
      // A straight drop onto the first peg, then a little hop from peg to peg
      const hop = seg === 0 ? 0 : GAP * 0.35 * Math.sin(Math.PI * local)
      const fall = seg === 0 ? local * local : local
      ball.view.position.set(a.x + (b.x - a.x) * local, a.y + (b.y - a.y) * fall - hop)
      if (ball.t >= last) land(ball)
    }
    for (const slot of slots) {
      if (slot.glow <= 0) continue
      slot.glow = Math.max(0, slot.glow - t.deltaMS / 400)
      slot.view.scale.set(1 + 0.18 * slot.glow)
    }
  })

  return {
    setMultipliers,
    // Animates one ball along `path` ('LRRL…'); resolves when it lands in `slot`
    drop(path, slot) {
      return new Promise((resolve) => {
        const view = new Graphics().circle(0, 0, BALL_R).fill(0xffd65a).stroke({ width: 2, color: 0x8a6a00 })
        const points = stops(path, slot)
        view.position.set(points[0].x, points[0].y)
        ballLayer.addChild(view)
        balls.add({ view, points, slot, t: 0, resolve })
      })
    },
    destroy() {
      // Balls still falling count as landed, so nobody waits on them
      for (const ball of balls) ball.resolve()
      balls.clear()
      destroyApp()
    },
  }
}
