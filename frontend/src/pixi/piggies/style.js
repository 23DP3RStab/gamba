// Shared look so the code-drawn art matches Segel's sprites: thick dark outlines, flat colours.
import { Graphics } from 'pixi.js'

export const INK = 0x1c1c22
export const LINE = 5 // outline width in world units

// World layout (design units). The ground line is where everything stands.
export const WORLD_W = 1600
export const GROUND_Y = 610
export const HOUSE_X = [560, 940, 1320]
export const WOLF_GAP = 250 // wolf stands this far left of the house he's blowing at

// A cartoon blob made of overlapping circles with one outline around the whole shape:
// draw all circles a bit bigger in the outline colour first, then the fill on top.
export function blob(circles, fill, line = LINE, ink = INK) {
  const g = new Graphics()
  for (const [x, y, r] of circles) g.circle(x, y, r + line / 2).fill(ink)
  for (const [x, y, r] of circles) g.circle(x, y, r - line / 2).fill(fill)
  return g
}
