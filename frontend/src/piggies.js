// The Three Piggies and the Wolf: the wolf tries to blow down each house in turn.
// Sturdier houses are harder to blow down, so each one that falls multiplies the
// player's amount by 1 / chance: every step is a fair bet, and going on to the next
// house is worth as much as cashing out and starting over. That makes the bet pay
// 2x, 5x and 16.67x after the straw, wood and brick house; if one stands, it is lost.
// The server rolls each blow (PiggiesService); these numbers are for showing the payouts.
let total = 1
export const HOUSES = [
  { key: 'straw', label: 'Straw house', chance: 0.5 },
  { key: 'wood', label: 'Wood house', chance: 0.4 },
  { key: 'brick', label: 'Brick house', chance: 0.3 },
].map((house) => {
  total /= house.chance
  return { ...house, multiplier: Math.round(total * 100) / 100 }
})
