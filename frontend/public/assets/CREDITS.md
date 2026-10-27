# Art credits

All third-party art here is public domain (CC0). Credit is not required, but given anyway.

| File | Source | Author | Licence |
|---|---|---|---|
| `piggies/piggies.png` — wolf frames (idle, run, attack, hurt) | [Wolf Game Character](https://opengameart.org/node/93984) | Segel (Segel2D) | CC0 |
| `piggies/piggies.png` — trees, bushes, grass, rock, stump | [2D Simple Building](https://opengameart.org/node/111871) | Segel | CC0 |

The original frames were trimmed, scaled to 50 % and packed into one texture atlas
(`piggies.png` + `piggies.json`, PixiJS spritesheet format) so the game loads one
image instead of 43.

Everything else in the games (houses, pigs, roulette wheel, sky, effects) is drawn in
code in `frontend/src/pixi/`, styled to match Segel's art.
