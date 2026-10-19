// A pig drawn in code to match Segel's style, built from parts so it can breathe, tremble and hop.
import { Container, Graphics } from 'pixi.js'
import { INK } from './style'
import { tween, wait } from '../tween'

const SKIN = 0xf6a9bb
const SKIN_LIGHT = 0xf8b8c7
const SKIN_DARK = 0xe58ea3
const SNOUT = 0xec8fa5

function legs(xs, color) {
  const g = new Graphics()
  for (const x of xs) g.roundRect(x, -15, 10, 17, 4).fill(color).stroke({ width: 3, color: INK })
  return g
}

// Feet at (0, 0), facing left (towards the wolf)
export function createPig() {
  const view = new Container()
  const body = new Container()
  view.addChild(body)

  const tail = new Graphics()
  tail
    .moveTo(34, -32)
    .bezierCurveTo(48, -42, 50, -22, 42, -24)
    .bezierCurveTo(35, -26, 39, -36, 46, -35)
    .stroke({ width: 3.5, color: INK, cap: 'round' })
  body.addChild(tail, legs([10, 22], SKIN_DARK))

  const torso = new Graphics()
  torso.ellipse(8, -29, 31, 23).fill(SKIN)
  torso.ellipse(12, -20, 24, 11).fill({ color: 0x000000, alpha: 0.08 })
  torso.ellipse(8, -29, 31, 23).stroke({ width: 4, color: INK })
  body.addChild(torso, legs([-15, -3], SKIN))

  const head = new Container()
  head.position.set(-22, -46)
  const face = new Graphics()
  face.poly([-12, -12, -9, -29, 0, -15]).fill(SKIN_DARK).stroke({ width: 3, color: INK, join: 'round' })
  face.poly([6, -15, 15, -28, 16, -10]).fill(SKIN_DARK).stroke({ width: 3, color: INK, join: 'round' })
  face.circle(0, 0, 19).fill(SKIN_LIGHT).stroke({ width: 4, color: INK })
  face.circle(4, 8, 4.5).fill({ color: 0xff7f9c, alpha: 0.45 })
  face.ellipse(-16, 4, 9.5, 7.5).fill(SNOUT).stroke({ width: 3, color: INK })
  face.circle(-19, 4, 1.8).fill(0x8e3a50)
  face.circle(-13, 4, 1.8).fill(0x8e3a50)
  head.addChild(face)

  const calmEyes = new Graphics()
  calmEyes.circle(-7, -5, 2.8).fill(INK).circle(4, -5, 2.8).fill(INK)
  const scaredEyes = new Graphics()
  scaredEyes
    .circle(-7, -6, 5.5)
    .fill(0xffffff)
    .stroke({ width: 2, color: INK })
    .circle(4, -6, 5.5)
    .fill(0xffffff)
    .stroke({ width: 2, color: INK })
  scaredEyes.circle(-8, -6, 2).fill(INK).circle(3, -6, 2).fill(INK)
  scaredEyes.ellipse(-12, 14, 2.6, 3.4).fill(0x6b1f2f) // little "o" mouth
  scaredEyes
    .moveTo(17, -22)
    .quadraticCurveTo(22, -14, 17, -11)
    .quadraticCurveTo(12, -14, 17, -22)
    .fill(0x9fd3ff)
    .stroke({ width: 2, color: INK })
  scaredEyes.visible = false
  head.addChild(calmEyes, scaredEyes)
  body.addChild(head)

  let mood = 'idle'
  let time = Math.random() * 10
  let hopping = false

  function setMood(next) {
    mood = next
    calmEyes.visible = next !== 'scared'
    scaredEyes.visible = next === 'scared'
  }

  function update(dt) {
    time += dt
    if (hopping) return
    if (mood === 'scared') {
      body.x = Math.sin(time * 70) * 2.2
      body.scale.y = 1
    } else {
      body.x = 0
      body.scale.y = 1 + 0.03 * Math.sin(time * 3)
    }
    head.rotation = mood === 'scared' ? 0 : 0.04 * Math.sin(time * 1.7)
  }

  // Jump for joy
  async function cheer(ticker, animate) {
    setMood('idle')
    if (!animate) return
    hopping = true
    for (let i = 0; i < 3; i++) {
      await tween(
        ticker,
        380,
        (t) => {
          body.y = -34 * Math.sin(Math.PI * t)
          body.scale.y = 1 + 0.08 * Math.sin(Math.PI * t)
        },
        undefined,
        body,
      )
    }
    if (!body.destroyed) body.y = 0
    hopping = false
  }

  // Run away to the right, hopping, then disappear
  async function flee(ticker, animate) {
    setMood('scared')
    if (!animate) {
      view.visible = false
      return
    }
    hopping = true
    await wait(ticker, 120)
    if (view.destroyed) return
    view.scale.x = -1 // turn round
    const startX = view.x
    await tween(
      ticker,
      900,
      (t, raw) => {
        view.x = startX + 300 * t
        body.y = -Math.abs(Math.sin(raw * Math.PI * 5)) * 16
        view.alpha = raw > 0.7 ? 1 - (raw - 0.7) / 0.3 : 1
      },
      undefined,
      view,
    )
    if (!view.destroyed) view.visible = false
    hopping = false
  }

  return { view, setMood, update, cheer, flee }
}
