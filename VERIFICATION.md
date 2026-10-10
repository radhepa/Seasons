# Verification

## 1.1.0 (2026-10-10): 30-day seasons and `/season length`

### Unit tests: `gradlew test`: 21 passed, 0 failed
New in `CalendarTest`: changing the length keeps the season, day, year and time of day (autumn
day 3 of 7 becomes autumn day 3 of 30; winter day 20 of 30 becomes winter day 7 of 7), and a
1.0.0 config file with the automatic `7` moves to `30` while a value someone chose is kept.

### Client game test: `SeasonsClientTest`: **passed** (BUILD SUCCESSFUL)
- The run folder still had the 1.0.0 config (`"daysPerSeason": 7`); it was upgraded on start
  ("30 days per season in new worlds") and the new world had 30-day seasons.
- The four seasons were run on day 16 of 30 (mid-season, as day 4 of 7 was before). All 1.0.0
  checks pass unchanged.
- `/season length 7` on spring day 16 of 30 gave spring day 7 of 7; `/season length 30` then gave
  spring day 7 of 30. The year stayed the same and the client got the new length straight away.
  No extra season-changed events fired (the event list is still exactly the four changes).
- HUD screenshot reads "Spring, Day 16 of 30".

Not run for 1.1.0: the modpack (`-PcompatMods`) run.

## 1.0.0 (2026-10-09)

Environment: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, JDK 25.0.4, Windows 11.

### Unit tests: `gradlew test`: 19 passed, 0 failed
| Class | Tests | Covers |
|---|---|---|
| `CalendarTest` | 7 | season/day/year from elapsed ticks, configurable length, progress, `ticksUntil` only goes forward, the saved calendar never rewinds, season names |
| `GrowthAndWeatherTest` | 6 | growth multipliers (outdoor dormancy, greenhouse floor, sheltered), skip/double tick decisions average to the multiplier, rain countdown speeds |
| `ColorsAndSpawnsTest` | 6 | summer is vanilla, autumn leaves turn orange, colours change smoothly and wrap from winter to spring, spawn files need their mod, weight scaling |

### Client game test: `SeasonsClientTest`
`gradlew runClientGameTest -PtestHeap=2560m`: **passed** (BUILD SUCCESSFUL, 6 screenshots).

The test builds a farm on a superflat plains world (oak and birch tree, open wheat row on farmland,
a 3x3x4 glass greenhouse with wheat inside, a 3x3 pond), then for **summer, autumn, winter, spring**
runs `/season set <season> 4` and checks:

- `TurningSeasonsApi` gives the same `CalendarDate` on the server and the client (sync works), and
  `hasSeasons` is true in the Overworld.
- The crop roof check: open wheat is `OUTDOOR`, greenhouse wheat is `GREENHOUSE`.
- 400 random ticks each: in summer and spring both crops grow; **in winter the open wheat stays at
  age 0 while the greenhouse wheat grows**.
- Autumn: oak foliage colour on the client is orange (red > green), grass colour differs from summer.
- Winter: plains are cold enough to snow on the server and on the client, **but not on a worker
  thread** (world generation keeps the natural climate). After rain and four weather passes, snow
  covers open grass, none gets into the greenhouse, and the pond freezes to ice.
- Breeding cooldown after `finalizeSpawnChildFromBreeding`: 12000 ticks in winter, 3000 in spring.
- Spawn weights: cows 0.5 in winter and 1.6 in spring, skeletons 1.25 in autumn through the
  `#minecraft:skeletons` tag; a cow 10 / wolf 10 list becomes 5 / 18 in winter; without NSV no
  NSV rule is loaded.
- Spring: weather passes melt the snow and thaw the pond back to water; at least 3 flowers bloom.
- The Season Clock in hand shows the spring face (`turningseasons:season` component) and the HUD card.
- `SeasonChangedCallback` fired exactly `spring>summer, summer>autumn, autumn>winter, winter>spring`.

Screenshots (`build/run/clientGameTest/screenshots`, copies in `docs/screenshots`):
`seasons-01-summer`, `-02-autumn-leaves` (falling poplar-leaf particles under the oak),
`-03-autumn`, `-04-winter` (snowfall, snow cover, frozen pond), `-05-spring` (melted, flowers),
`-06-hud` (Season Clock + "Spring, Day 4 of 7" card).

### Modpack run
`gradlew runClientGameTest -PtestHeap=2560m -PcompatMods=%APPDATA%/.minecraft/instances/VillageFriends/mods`
loaded Village Friends 2.26.1, Village Friends RPG 1.0.0 and Not-So-Vanilla Mobs 1.2.0 next to
Turning Seasons: **passed**. In this run the NSV branch of the spawn check ran: Frostbitten have
weight x3 in winter. No mixin or error lines from any of the mods in the log.

### Bugs found by the tests and fixed
- A worldgen-thread check deadlocked when the helper thread loaded a chunk while the server
  thread waited; the helper now only evaluates the already-fetched biome.
- Snow thaw looked only at the heightmap block; it now checks the block below as well, in case a
  snow layer counts towards the heightmap.

### Not covered by automated tests
- Natural spawning over time (the reweighting is tested directly on a list).
- The rain-frequency countdown in a running world (the maths is unit tested).
- A dedicated server (the client-only HUD/colour code is only on the client source set).
