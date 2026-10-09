# Ideas: Village Friends with Turning Seasons

Turning Seasons never touches Village Friends (VF) or its add-ons. This page lists what VF *could*
do through the public API (see API.md), always behind
`FabricLoader.getInstance().isModLoaded("turningseasons")` and in a class of its own, so VF keeps
working without it. Nothing here is built yet.

## Harvest festival
- `SeasonChangedCallback` with `to == Season.AUTUMN`: each village holds a harvest festival on
  autumn day 3 or so (`getDayOfSeason`). Reuse the birthday party machinery (party hats, tables in
  the plaza, music) with a harvest theme: pumpkin pie, bread, a hay-bale centrepiece.
- Notice board: "Bring in the harvest" quests in autumn (deliver wheat/carrots/pumpkins), paid in
  friendship. Winter quests could ask for firewood and coal instead.
- A Spring planting day and a Midwinter lantern night would fit the same pattern.

## Seasonal outfits
- The wardrobe pipeline (`tools/wardrobe`) could tag outfits by season: wool cloaks, scarves and
  hoods in winter, straw hats and rolled sleeves in summer. At dawn each resident picks from the
  season's subset (`getSeason`). Biome villages already pick by climate; seasons would be one more
  filter.
- Guards could swap to fur-lined cloaks in winter.

## Seasonal dialogue
- Dialogue lines get an optional `season` condition in `tools/dialogue`, e.g.
  "Mind the ice on the well." (winter), "First lambs of the year!" (spring). The dialogue bank
  already filters by personality and friendship; season is one more key.
- Small talk about the weather can use `getDayOfSeason` and `daysUntilNextSeason()`:
  "Two more days and it's winter. Have you stocked up?"

## Winter tavern stews
- Taverns serve heartier meals in winter: stews and roasts rise in the menu weight, cold drinks
  drop. Hearth & Harvest dishes could carry a `seasonal` tag the tavern reads.
- More residents stay in the tavern through winter evenings (the routine already has an evening
  tavern visit; raise its chance when `getSeason == WINTER`).

## Homesteads and farming
- Homestead farmers sow in spring and harvest in autumn. Turning Seasons already makes outdoor
  crops sleep in winter (VF's own crops count if they're in `#minecraft:crops` or
  `#turningseasons:seasonal_crops`), so a farmer routine that waits for spring looks natural.
- A farmer could ask the player to build a greenhouse (glass roof) as a winter quest.

## Calendar and birthdays
- VF's village calendar could show the season next to the date, and birthdays could mention it
  ("a winter baby").

## Pets
- Cats sleep by the hearth in winter; dogs play in the snow (an existing play clip in a winter biome).

## Optional RPG add-on
- Bestiary entries could note when a mob is most common, read from the seasonal spawn rules.
