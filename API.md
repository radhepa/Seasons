# Turning Seasons API

Turning Seasons (mod id `turningseasons`) exposes a small, stable API in the package
`dev.turningseasons.api`. Use it to make your mod react to the seasons **without depending on it**:
the player may or may not have Turning Seasons installed.

## Detecting the mod

```java
if (FabricLoader.getInstance().isModLoaded("turningseasons")) {
    MySeasonHooks.register();   // only this class touches dev.turningseasons.api
}
```

Keep every call to the API inside a class of its own (like `MySeasonHooks` above) that is only
loaded behind that check. Then nothing tries to load Turning Seasons classes when the mod is absent.
In `fabric.mod.json`, list it under `suggests` (never `depends`).

Compile against it with `modCompileOnly` / `compileOnly` on the jar, or copy the four small API
classes' signatures into a stub; you don't need to ship it.

## Reading the season

All methods are static on `TurningSeasonsApi` and work on both sides: on the server they read the
saved calendar, on a client the copy the server last synced.

| Method | Returns |
|---|---|
| `hasSeasons(Level)` | true if this dimension has seasons (by default only the Overworld) |
| `getSeason(Level)` | `Season.SPRING`, `SUMMER`, `AUTUMN` or `WINTER` |
| `getDayOfSeason(Level)` | 1 .. days per season |
| `getDaysPerSeason(Level)` | season length in Minecraft days (default 30; can change in game with `/season length`) |
| `getSeasonProgress(Level)` | 0.0 at the start of the season, approaching 1.0 at its end |
| `getYear(Level)` | 1, 2, 3, ... |
| `getDate(Level)` | all of the above in one `CalendarDate` record |

Dimensions without seasons (the Nether, the End) report the season the calendar is in; check
`hasSeasons` if that matters to you.

`Season` has `id()` (`"spring"` ...), `next()`, `previous()`, `displayName()` (a translated
`Component`) and `Season.byId(String)`, which also accepts `"fall"`.

`CalendarDate` also has `daysUntilNextSeason()`, `yearPosition()` (0 to 4 across the year) and
`dayNumber()` (changes once per calendar day; handy for "did the day roll over?" checks).

## Season-changed event

```java
SeasonChangedCallback.EVENT.register((ServerLevel level, Season from, Season to) -> {
    if (to == Season.AUTUMN) startHarvestFestival(level);
});
```

Fires on the server, once for each seasonal dimension, whenever the season turns: naturally, by
sleeping through the last night, or by `/season set` / `/season advance`. It does not fire when a
world is loaded.

## Tags you can add to

Data packs and other mods can extend these tags (see `SeasonTags` for the keys):

| Tag | What it does |
|---|---|
| `#turningseasons:seasonal_crops` (block) | random ticks follow the season's growth speed; outdoor ones sleep in winter. Includes `#minecraft:crops`, `#minecraft:saplings`, sweet berries, cocoa and `#c:crops` |
| `#turningseasons:greenhouse_glass` (block) | a crop with one of these as its first roof keeps growing in winter |
| `#turningseasons:not_a_roof` (block) | the roof check looks through these (leaves by default) |
| `#turningseasons:spring_bloom` (block) | flowers that can appear on open grass in spring |
| `#turningseasons:temperate` (biome) | biomes that snow in winter, thaw in spring and change colour |

## Seasonal spawns (data-driven)

Put a JSON file in `data/<your namespace>/seasonal_spawns/`:

```json
{
  "requires_mod": "nsvmobs",
  "entities": ["nsvmobs:frostbitten", "#minecraft:skeletons"],
  "weights": { "spring": 0.6, "summer": 0.25, "autumn": 1.0, "winter": 3.0 }
}
```

Each natural spawn picks a mob from its biome's weighted list; Turning Seasons multiplies each
entry's weight by the season's number first. `0` means "not this season". `requires_mod` is
optional: the file is skipped when that mod isn't installed. Several rules that match the same mob
multiply together.

## Example: seasonal dialogue

```java
public final class MySeasonHooks {
    public static String greeting(Level level) {
        if (!TurningSeasonsApi.hasSeasons(level)) return "Good day!";
        return switch (TurningSeasonsApi.getSeason(level)) {
            case SPRING -> "The blossoms are out!";
            case SUMMER -> "Hot one today.";
            case AUTUMN -> "Harvest's nearly in.";
            case WINTER -> "Stay by the fire, friend.";
        };
    }
}
```
