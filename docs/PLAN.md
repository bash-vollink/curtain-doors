# Curtain Doors — NeoForge mod plan (MC 1.21.1, NeoForge 21.1.249)

## Context
The `bash-vollink/curtain-doors` repo is empty, so this is a new mod. The goal is curtains that work as doorways. They hang from a ceiling and fill an opening. Every entity walks straight through them, with no right-clicking. The cloth is simulated, so it parts around whatever passes through and swings back. There are three families:
- **Wool curtains**: 16 dye colors. A full cloth sheet.
- **Clear plastic strip curtains**: translucent vertical PVC strips, like a walk-in freezer door.
- **Beaded curtains**: 16 dye colors. Independent hanging strands of beads.

Decisions confirmed with you: all entities pass through; curtains hang from a ceiling and extend downward automatically, 1 to 4 blocks; beaded curtains come in 16 dye colors.

## Tech baseline
- Java 21, Gradle with **ModDevGradle 2.x** (`net.neoforged.moddev`), `minecraft_version=1.21.1`, `neo_version=21.1.249`, Parchment mappings for 1.21.1.
- Mod id `curtaindoors`, package `com.bashvollink.curtaindoors`.
- The physics is client-only and visual. The server only knows about blocks with no collision, so **no networking** is needed.

## Project layout
```
build.gradle, settings.gradle, gradle.properties, gradlew(+wrapper)
src/main/templates/META-INF/neoforge.mods.toml
src/main/java/com/bashvollink/curtaindoors/
  CurtainDoors.java                 // @Mod entry; registers DeferredRegisters + configs
  registry/ModBlocks, ModItems, ModBlockEntities, ModSounds, ModCreativeTabs
  block/CurtainType.java            // enum WOOL, PLASTIC, BEADED (+ sim params, sounds, render mode)
  block/CurtainBlock.java           // the one block class, configured by type + optional DyeColor
  block/CurtainItem.java            // BlockItem handling ceiling placement + auto-length
  block/entity/CurtainBlockEntity.java  // only on the top segment; holds client sim handle
  config/ClientConfig.java          // sim distance, quality, wind, max active sims, sounds
  client/CurtainDoorsClient.java    // client mod events: BER, item/block colors, tick hook
  client/sim/ClothSim.java          // PURE JAVA Verlet solver (no MC imports) → unit-testable
  client/sim/ClothLayout.java       // builds particles/constraints: SHEET / STRIPS / STRANDS
  client/sim/SimParams.java         // record: gravity, damping, iterations, stiffness, bead mass…
  client/sim/CylinderCollider.java  // entity → vertical cylinder in curtain-local space
  client/sim/SimManager.java        // tracks live sims, ticks at 20 Hz, sleep/wake, LOD, sounds
  client/render/CurtainRenderer.java // BlockEntityRenderer; dispatches to meshers
  client/render/SheetMesher.java    // wool + plastic strips → double-sided quads
  client/render/BeadMesher.java     // beads as small cubes + thin string quads
  datagen/  (DataGenerators, BlockStates, ItemModels, Recipes, LootTables, Lang, BlockTags, Sounds)
src/main/resources/assets/curtaindoors/textures/{item,block}/…, sounds.json
src/test/java/…/client/sim/ClothSimTest.java
```

## Blocks & placement
**Registration.** Loop over `DyeColor` to register `<color>_curtain` (wool, 16) and `<color>_beaded_curtain` (16), plus one `plastic_strip_curtain`. That is 33 blocks, each with its own BlockItem. All of them use the same `CurtainBlock(CurtainType, @Nullable DyeColor, Properties)`.

**Block properties.**
- `noCollission()`, `noOcclusion()`, `isViewBlocking/isSuffocating = never`, `pushReaction(DESTROY)`.
- Wool and beads use `instabreak`-ish strength, are fast with shears, and use the wool and chain sound types. Plastic is a little harder.
- `isPathfindable(...) → true`, and override NeoForge's `getBlockPathType` to return `PathType.OPEN`, so mobs path through.
- `getShape` (the selection outline) is a thin 16×16×2 px panel centered on the axis. `getCollisionShape` is `Shapes.empty()`.
- `RenderShape.ENTITYBLOCK_ANIMATED`. The static model is empty and only provides a particle texture. The BER draws everything.

**Blockstate.**
- `AXIS` (X/Z): the plane of the curtain.
- `SEGMENT` (0–3, 0 = top).
- `LENGTH` (1–4).

That is up to 32 states per block. Only `SEGMENT=0` gets the block entity (`newBlockEntity` returns null otherwise).

**Placement** (`CurtainItem#place` / `CurtainBlock#getStateForPlacement`):
1. Placement requires `Block.canSupportCenter(level, target.above(), DOWN)`, the same check lanterns use. This also allows clicking the bottom face of a block.
2. Measure downward from the target: count air or replaceable blocks, stopping at the first non-replaceable block or at the max length (config, default 4). This gives `LENGTH`.
3. Place the top segment, then fill segments 1..n-1 with the same block and the matching `SEGMENT`/`LENGTH`.
4. `AXIS` comes from the player's horizontal facing, so the curtain spans the opening in front of them.

**Integrity** works like doors:
- `updateShape` on any segment checks that the segment above is either the same curtain or a valid support for segment 0. Otherwise the segment becomes air, which cascades down.
- `playerWillDestroy` removes all segments.
- The loot table drops one item, from segment 0 only (`LootItemBlockStatePropertyCondition`).

## Physics (client-only, `client/sim`)
`ClothSim` is a pure-Java Verlet solver with no Minecraft classes, so it can be unit tested:
- Data is stored in `float[] pos, prev, invMass`. Constraints are `int[] a,b` + `float[] rest, stiffness`.
- Each step:
  1. Integrate: `x += (x - prev) * damping + g·dt²`.
  2. Apply external forces (wind).
  3. Run N iterations of distance-constraint projection.
  4. Collide with the colliders.
  5. Clamp to the floor plane and the pins.
- Fixed 20 Hz tick with 2 substeps. The renderer interpolates `prev→pos` using `partialTick` for smooth frames.

`ClothLayout` builds the three topologies in curtain-local space. The width is 1 block, the height is `LENGTH` blocks, the top row is pinned just under the ceiling, and particle spacing is set by the quality config.

| Type | Topology | Constraints | Feel |
|---|---|---|---|
| Wool | one grid, 9 × (8·L+1) | structural + shear + bend (skip-one) | soft, heavy damping |
| Plastic | 4 strips of 3 × (8·L+1), slight overlap, no links between strips | structural + strong bend | stiff, springy, slides apart |
| Beaded | 8 strands × (6·L) beads | structural only, plus a weak bend | each strand swings freely, quick settle |

**Entity interaction** (`CylinderCollider`):
- Each tick, `SimManager` runs `level.getEntities(null, curtainAABB.inflate(0.5))` and converts each entity to a vertical cylinder in local coordinates: radius = bbWidth/2 + margin, with y-range = the entity's box.
- Particles inside the cylinder are projected radially out to its surface in the horizontal plane. A particle almost exactly on the center axis falls back to the entity's velocity direction.
- Push-out velocity: blend a fraction of the entity's velocity into the particle (`prev -= v·k`). This makes the cloth trail behind the body and part around it naturally.
- This covers every entity type, including other players and mobs seen by the client.

**Wind** (config toggle): a gentle sinusoidal gust field keyed on time and position. It is applied only if `level.canSeeSky(top)`, and is stronger when raining.

**Performance** (`SimManager`):
- **Sleep**: when there is no entity in range, no wind, and kinetic energy has stayed below a threshold for 20 ticks, stop stepping and keep rendering the last pose.
- **Wake**: an entity enters the inflated AABB, or a neighbor update occurs.
- **LOD**:
  - Beyond the sim distance (config, default 32 blocks), the sim is frozen and rendered at its rest pose.
  - The number of active sims is capped (config, default 64). When over the cap, the nearest curtains win.
- Sims are created lazily when first rendered and released in `setRemoved`/chunk unload.

**Sounds**:
- When an entity *newly* touches a curtain (`SimManager` keeps a per-sim set of entity IDs), it plays a client-local sound with `level.playLocalSound`.
- The sound events are `curtain.wool.brush`, `curtain.plastic.swish`, and `curtain.beads.clack`.
- `sounds.json` maps these to vanilla sound files for now (cloth dig sounds, chain or bamboo sounds), so no audio assets are needed yet. Real `.ogg` files can replace them later without code changes.

## Rendering (`client/render`)
`CurtainRenderer implements BlockEntityRenderer<CurtainBlockEntity>`:
- Override `getRenderBoundingBox` to cover all `LENGTH` blocks inflated by 0.75, so swinging cloth isn't culled.
- Set `shouldRenderOffScreen` to false.
- Get the sim from `SimManager`, or use the rest pose if there is none. Interpolate the particle positions.
- **Lighting**: sample `LevelRenderer.getLightColor` once per block row (cached per frame), so a tall curtain in a dark hallway is lit correctly.

`SheetMesher` (wool, plastic):
- Emits quads per grid cell with smooth per-vertex normals (cross product of neighbor differences).
- Draws both sides: emits the back face with a flipped normal and reversed winding.
- Wool:
  - Render type `RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)`.
  - Texture: the vanilla `minecraft:block/<color>_wool` sprite, with UVs mapped into the sprite's u0..u1/v0..v1 and tiled per block.
- Plastic: render type `entityTranslucent(LOCATION_BLOCKS)`. Texture: our own `block/plastic_strip` sprite (pale, about 35% alpha, with a slight edge highlight).

`BeadMesher`:
- Each bead particle is a small cube (about 1.5 px) textured with our `block/bead` sprite and tinted with the dye color's `getTextureDiffuseColor()`.
- A thin dark string quad pair joins consecutive beads.

**Items and particles**:
- Item models are flat sprites. A single grayscale `item/curtain` and `item/beaded_curtain` texture is tinted per dye color by an `ItemColor` registered in `RegisterColorHandlersEvent.Item`, so wool and beads need only 2 textures for 32 items. Plastic gets its own sprite.
- Block break particles use the wool sprite (wool), our plastic sprite (plastic), or the bead sprite (beads), set in each block's static model.

## Recipes (datagen)
- **Wool curtain** ×3: `SSS / WWW / WWW`, where S = stick and W = matching-color wool.
- **Beaded curtain** ×3: `SSS / D#D / ###`, where # = string and D = matching-color dye.
- **Plastic strip curtain** ×3: `III / PSP / PPP`, where I = iron nugget, P = glass pane, S = slime ball. This stands in for "plastic", which has no vanilla equivalent.
- **Re-dye**: shapeless recipes taking any wool curtain + dye → that color (and the same for beads), following the vanilla dyed-bed pattern.

## Datagen (`gradlew runData`)
- Blockstates: every state points to the empty particle model.
- Item models and lang (`en_us`).
- Loot tables (segment 0 only).
- Block tags: `minecraft:mineable/axe` for plastic. Wool and beads are fast to break with shears through `CurtainBlock#getDestroyProgress`, the same way vanilla wool does it.
- Recipes, a sound definitions provider, and a creative tab listing all 33 curtains.

## Config (`ClientConfig`, ModConfigSpec)
| Key | Default |
|---|---|
| `simDistance` | 32 |
| `maxActiveSims` | 64 |
| `quality` | LOW / MEDIUM / HIGH (particle density), MEDIUM |
| `wind` | true |
| `contactSounds` | true |

Common config: `maxLength` (1–4), default 4.

## Build order
1. Gradle/MDG scaffold, mods.toml, main class, and a `gradlew build` that passes.
2. `CurtainBlock`, `CurtainItem`, ceiling placement, multi-segment integrity, loot, and pathfinding. At this point the curtains are invisible but placeable and walk-through (use a placeholder static model so they can be seen).
3. `ClothSim` + `ClothLayout` + unit tests.
4. `SimManager` + `CurtainRenderer` + `SheetMesher` for wool, then plastic.
5. `BeadMesher` + the bead layout.
6. Entity collider, wind, sleep/LOD, and contact sounds.
7. Datagen for everything, textures, creative tab, recipes, and config.
8. Polish: tuning sim params per type, README with screenshots.

## Verification
- **Unit tests** (`gradlew test`, JUnit 5, pure Java):
  - Pinned particles never move.
  - A curtain settles from a perturbed pose: energy drops below the threshold within N steps and nothing becomes NaN.
  - Constraint error stays under 5% after settling.
  - A cylinder pushed through the middle leaves no particle inside it and opens a gap.
  - Strip and strand layouts have no links between strips or strands.
- **GameTests** (`gradlew runGameTestServer`, NeoForge `@GameTest`):
  - Placing under a ceiling with 3 air blocks below produces `LENGTH=3`.
  - Breaking the support removes all segments and drops exactly 1 item.
  - A zombie told to path to the other side of a curtain gets there.
  - The collision shape is empty.
- **Manual** (`gradlew runClient`):
  - Hang each type in a doorway, walk through slowly and quickly, and check it parts and swings back.
  - Watch a villager or another player pass through.
  - Check that plastic looks translucent and sorts correctly in front of water and glass.
  - Check that beads clack.
  - Confirm sims sleep (F3 / debug counter) and that frame time is fine with 50+ curtains.
- Note: the cloud sandbox may not be able to reach `maven.neoforged.net`. If the build is blocked, ask to allow that host or run the build locally.
