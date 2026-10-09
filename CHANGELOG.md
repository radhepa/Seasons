# Changelog

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
