# Curtain Doors

A NeoForge mod for **Minecraft 1.21.1** (NeoForge 21.1.249) that adds curtains you use as doors. Hang one from a ceiling and walk straight through it. You never need to right-click, and the cloth is physically simulated: it parts around whatever passes through and swings back.

- **Wool curtains**, 16 colors: on its own, two panels that part in the middle; placed side by side in a row, one panel per block.
- **Plastic strip curtain**: translucent overlapping strips, like a walk-in freezer door.
- **Beaded curtains**, 16 colors: independent bead strands that swing and clack.

## Using it
- **Placing:** click the underside of any block that a lantern could hang from. The curtain drops 1–4 blocks until it reaches the floor. Clicking the floor under a ceiling also works.
- **Walking through:** players, mobs and items all pass through. Mobs pathfind through curtains.
- **Breaking:** breaking any segment removes the whole curtain and drops one item. A curtain also falls if its ceiling is removed.

## Recipes
| Curtain (×3) | Pattern |
|---|---|
| Wool curtain | `SSS / WWW / WWW`: sticks over 6 wool of one color |
| Beaded curtain | `SSS / D#D / ###`: sticks, 2 dye, 4 string |
| Plastic strip curtain | `III / PSP / PPP`: iron nuggets, glass panes, slime ball |

To change a curtain's color, craft it together with a dye.

## Client config (`config/curtaindoors-client.toml`)
| Key | Default | Meaning |
|---|---|---|
| `simDistance` | 32 | Curtains farther than this hang still |
| `maxActiveSims` | 64 | Maximum curtains simulated at once; the nearest ones win |
| `quality` | MEDIUM | Cloth particle density (LOW / MEDIUM / HIGH) |
| `wind` | true | Gentle wind on curtains open to the sky |
| `contactSounds` | true | Rustle, swish or clack when something passes through |

Server/common config: `maxLength` (1–4) sets how far curtains hang.

The F3 screen shows a `[Curtain Doors]` line with counts of simulated and idle curtains.

## Building
Requires JDK 21.

```bash
./gradlew build                # jar in build/libs, runs unit tests
./gradlew runGameTestServer    # in-game behaviour tests, headless
./gradlew runClient            # dev client
./gradlew runData              # regenerate src/generated (commit the result)
```

Every push triggers CI, which uploads the mod jar as an artifact named `curtaindoors-<sha>` on the workflow run page.

## How it works
- **Server:** curtains are blocks with no collision. Only the top segment carries a block entity. No networking is needed.
- **Client physics:** the cloth is simulated only on the client (`client/sim`):
  - a Verlet solver with distance links and long-range tethers so tall cloth doesn't stretch;
  - entities treated as vertical cylinders;
  - wind on curtains open to the sky;
  - curtains go to sleep when still, and only nearby curtains are simulated.
- **Rendering:** a block entity renderer draws the cloth (`client/render`).

See `docs/PLAN.md` for the full design.
