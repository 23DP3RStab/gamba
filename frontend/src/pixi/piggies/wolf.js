// Segel's animated wolf (CC0 frames from the atlas) with idle, run, huff and hurt states.
import { AnimatedSprite, Container, Graphics } from 'pixi.js'
import { INK } from './style'

const SCALE = 1.3
// Frame rates relative to 60 fps (AnimatedSprite.animationSpeed)
const SPEED = { idle: 0.18, run: 0.38, attack: 0.3, hurt: 0.22 }

// Where the snout is, relative to the wolf's feet, when facing right (for the wind effect)
export const MOUTH = { x: 120, y: -96 }

function star() {
  const g = new Graphics()
  const pts = []
  for (let i = 0; i < 10; i++) {
    const r = i % 2 ? 4 : 10
    const a = (i / 10) * Math.PI * 2 - Math.PI / 2
    pts.push(Math.cos(a) * r, Math.sin(a) * r)
  }
  return g.poly(pts).fill(0xf0c419).stroke({ width: 2.5, color: INK, join: 'round' })
}

export function createWolf(sheet) {
  const view = new Container()
  const body = new Container()
  view.addChild(body)

  const sprite = new AnimatedSprite(sheet.animations.wolf_idle)
  sprite.anchor.set(0.5, 0.97)
  sprite.scale.set(SCALE)
  body.addChild(sprite)

  // Dizzy stars over his head after a house holds
  const stars = new Container()
  stars.position.set(50, -175)
  for (let i = 0; i < 3; i++) stars.addChild(star())
  stars.visible = false
  view.addChild(stars)

  let current = ''
  let facing = 1
  let puffing = false
  let time = 0

  function play(name, loop = true) {
    if (current === name) return
    current = name
    sprite.textures = sheet.animations[`wolf_${name}`]
    sprite.animationSpeed = SPEED[name]
    sprite.loop = loop
    sprite.gotoAndPlay(0)
  }

  function face(direction) {
    facing = direction
    sprite.scale.x = SCALE * direction
  }

  function setPuffing(on) {
    puffing = on
    play(on ? 'attack' : 'idle')
    if (!on) body.scale.set(1)
  }

  function setBeaten(on) {
    stars.visible = on
    play(on ? 'hurt' : 'idle', !on)
  }

  function update(dt) {
    time += dt
    if (puffing) {
      // Chest swells and he leans in with each huff
      const huff = Math.sin(time * 9)
      body.scale.set(1 + 0.05 * Math.max(0, huff), 1 + 0.07 * Math.max(0, huff))
      body.rotation = 0.04 * huff * facing
    } else {
      body.rotation = 0
    }
    if (stars.visible) {
      stars.children.forEach((s, i) => {
        const a = time * 4 + (i * Math.PI * 2) / 3
        s.position.set(Math.cos(a) * 34, Math.sin(a) * 9)
        s.rotation = time * 3
      })
    }
  }

  play('idle')
  return {
    view,
    play,
    face,
    setPuffing,
    setBeaten,
    update,
    get facing() {
      return facing
    },
  }
}
