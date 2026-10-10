# Changelog

## 1.1.0
- Seasons now last **30 days** by default (a year is 120 days), up from 7.
- `/season length` shows the season length; `/season length <days>` changes it for this world
  (operators, or cheats on in single player). The current season and day are kept, and clients
  update straight away.
- The season length is saved in each world. `daysPerSeason` in the config is now the length for
  new worlds.
- Config files written by 1.0.0 that still have the old automatic `7` move to `30`; any other value
  you chose is kept.

## 1.0.0
First release, for Minecraft 26.3 (Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25).

- Season calendar: spring, summer, autumn, winter; 7 days each by default; server-authoritative,
  saved in the world, synced to clients; follows the Overworld clock (sleeping counts, rewinding
  time doesn't).
- `/season`, `/season set <season> [day]`, `/season advance <days>`.
- Season Clock item with a face per season, and an optional season HUD card.
- Seasonal grass and leaf colours in temperate biomes; falling autumn leaves.
- Winter snowfall, snow cover and frozen water in temperate biomes; spring thaw; spring flower bloom.
- Seasonal crop growth through `#turningseasons:seasonal_crops`; outdoor crops dormant in winter
  unless under `#turningseasons:greenhouse_glass`.
- Seasonal rain frequency and breeding cooldowns.
- Data-driven seasonal spawn weights (`seasonal_spawns/*.json`), with optional Not-So-Vanilla Mobs
  rules for Frostbitten, Penguins, Glowmoths and Scarecrows.
- Public API: `TurningSeasonsApi`, `Season`, `CalendarDate`, `SeasonChangedCallback`, `SeasonTags`.
