package dev.turningseasons.test;

import java.util.ArrayList;
import java.util.List;

import dev.turningseasons.Calendar;
import dev.turningseasons.CropGrowth;
import dev.turningseasons.SeasonClockItem;
import dev.turningseasons.SeasonConfig;
import dev.turningseasons.SeasonalSpawns;
import dev.turningseasons.api.CalendarDate;
import dev.turningseasons.api.Season;
import dev.turningseasons.api.SeasonChangedCallback;
import dev.turningseasons.api.TurningSeasonsApi;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

/**
 * Builds a little farm (a tree, an open wheat row, a glass greenhouse and a pond) and walks it
 * through summer, autumn, winter and spring, photographing each season and checking the rules:
 * the API on both sides, colours, snow and ice, winter dormancy versus the greenhouse, the thaw,
 * the spring bloom, breeding cooldowns, spawn weights and the season-changed event.
 * Screenshots land in build/run/clientGameTest/screenshots.
 *   gradlew runClientGameTest -PtestHeap=2560m
 */
@SuppressWarnings("UnstableApiUsage")
public class SeasonsClientTest implements FabricClientGameTest {
    static final int FLOOR = -61;                              // superflat grass layer
    static final BlockPos TREE = new BlockPos(3, -60, 9);
    static final BlockPos OPEN_CROP = new BlockPos(-2, -60, 3), GLASS_CROP = new BlockPos(2, -60, 3);
    static final BlockPos POND = new BlockPos(-7, FLOOR, 5);   // centre of a 3x3 pond
    static final int AREA = 12;                                 // the scene spans -12..12 on x and z

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    static void shoot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(client -> {
            client.gui.toastManager().clear();
            client.gui.hud.getChat().clearMessages(false);
        });
        ctx.takeScreenshot(name);
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1600, 900);
        List<String> changes = new ArrayList<>();
        SeasonChangedCallback.EVENT.register((level, from, to) -> {
            if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) changes.add(from.id() + ">" + to.id());
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            TestServerContext server = world.getServer();
            buildScene(server);
            ctx.waitTicks(20);

            // ---- summer: vanilla colours, crops grow outdoors
            setSeason(ctx, server, Season.SUMMER);
            int summerLeaves = foliageAt(ctx, TREE.above(4));
            int summerGrass = grassAt(ctx, new BlockPos(0, FLOOR, 4));
            checkGrowth(server, Season.SUMMER, true, true);
            photograph(ctx, server, "seasons-01-summer");

            // ---- autumn: orange leaves, falling-leaf particles
            setSeason(ctx, server, Season.AUTUMN);
            int autumnLeaves = foliageAt(ctx, TREE.above(4));
            check(red(autumnLeaves) > green(autumnLeaves), "autumn leaves are orange: " + Integer.toHexString(autumnLeaves));
            check(red(summerLeaves) < green(summerLeaves), "summer leaves are green: " + Integer.toHexString(summerLeaves));
            check(grassAt(ctx, new BlockPos(0, FLOOR, 4)) != summerGrass, "autumn grass is a different colour");
            server.runCommand("tp @a 1.5 -60 4.5 20 -20");      // under the tree, looking up at the falling leaves
            ctx.waitTicks(60);
            shoot(ctx, "seasons-02-autumn-leaves");
            photograph(ctx, server, "seasons-03-autumn");

            // ---- winter: snow, ice, dormant fields, a working greenhouse
            setSeason(ctx, server, Season.WINTER);
            server.runOnServer(srv -> {
                ServerLevel level = srv.overworld();
                BlockPos at = new BlockPos(0, FLOOR + 1, 4);
                check(level.getBiome(at).value().coldEnoughToSnow(at, level.getSeaLevel()), "plains snow in winter");
                // world generation runs on other threads and must not see the chill
                var plains = level.getBiome(at).value();
                int seaLevel = level.getSeaLevel();
                boolean[] workerCold = {true};
                Thread worker = new Thread(() -> workerCold[0] = plains.coldEnoughToSnow(at, seaLevel));
                worker.start();
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
                check(!workerCold[0], "worldgen threads see the natural climate");
            });
            server.runCommand("weather rain");
            ctx.waitTicks(120);   // rain level fades in, the client starts drawing snowfall
            server.runOnServer(srv -> {
                ServerLevel level = srv.overworld();
                for (int pass = 0; pass < 4; pass++) precipitateEverywhere(level);
                check(level.getBlockState(new BlockPos(0, FLOOR + 1, -2)).is(Blocks.SNOW), "snow settles on open grass");
                check(level.getBlockState(GLASS_CROP).getBlock() instanceof CropBlock, "no snow inside the greenhouse");
                check(level.getBlockState(POND).is(Blocks.ICE), "the pond freezes over");
            });
            checkGrowth(server, Season.WINTER, false, true);
            checkBreeding(server, 12000);
            checkSpawnWeights(server);
            photograph(ctx, server, "seasons-04-winter");
            ctx.runOnClient(client -> check(client.level.getBiome(new BlockPos(0, FLOOR + 1, 4)).value()
                    .coldEnoughToSnow(new BlockPos(0, FLOOR + 1, 4), client.level.getSeaLevel()), "the client sees snowfall too"));

            // ---- spring: the thaw, then a bloom
            setSeason(ctx, server, Season.SPRING);
            server.runCommand("weather clear");
            server.runOnServer(srv -> {
                ServerLevel level = srv.overworld();
                float bloom = SeasonConfig.get().springBloomChance;
                SeasonConfig.get().springBloomChance = 0.6F;    // a quick bloom for the picture
                try {
                    for (int pass = 0; pass < 30; pass++) precipitateEverywhere(level);
                } finally {
                    SeasonConfig.get().springBloomChance = bloom;
                }
                check(!level.getBlockState(new BlockPos(0, FLOOR + 1, -2)).is(Blocks.SNOW), "spring melts the snow");
                check(level.getBlockState(POND).is(Blocks.WATER), "spring thaws the pond");
                int flowers = 0;
                for (BlockPos p : BlockPos.betweenClosed(-AREA, FLOOR + 1, -AREA, AREA, FLOOR + 1, AREA)) {
                    if (level.getBlockState(p).is(BlockTags.FLOWERS)) flowers++;
                }
                check(flowers >= 3, "flowers bloom in spring (" + flowers + ")");
            });
            checkGrowth(server, Season.SPRING, true, true);
            checkBreeding(server, 3000);
            ctx.waitTicks(120);   // let the rain fade out for the picture
            photograph(ctx, server, "seasons-05-spring");

            // the HUD and the clock in hand
            server.runCommand("tp @a 0 -60 -6 0 20");
            ctx.waitTicks(10);
            ctx.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
            server.runCommand("item replace entity @a hotbar.0 with turningseasons:season_clock");
            ctx.waitTicks(30);
            server.runOnServer(srv -> check(srv.getPlayerList().getPlayers().getFirst().getMainHandItem()
                    .get(SeasonClockItem.SEASON) == Season.SPRING, "the clock's face follows the season"));
            shoot(ctx, "seasons-06-hud");

            check(changes.equals(List.of("spring>summer", "summer>autumn", "autumn>winter", "winter>spring")),
                    "season-changed event fired once per change: " + changes);
        }
    }

    // ---------------------------------------------------------------------------------------------

    static void buildScene(TestServerContext server) {
        for (String cmd : List.of("gamemode creative @a", "gamerule advance_time false", "gamerule advance_weather false",
                "gamerule spawn_mobs false", "gamerule random_tick_speed 0", "time set 6000", "weather clear",
                "kill @e[type=!player]",
                "fill -12 -60 -12 12 -50 12 air", "fill -12 -61 -12 12 -61 12 grass_block",
                "place feature minecraft:oak 3 -60 9", "place feature minecraft:birch -6 -60 10",
                // open wheat row
                "fill -3 -61 3 -1 -61 3 farmland[moisture=7]",
                // greenhouse: a glass box with one farmland tile inside
                "fill 1 -61 2 3 -58 4 glass", "fill 2 -60 3 2 -59 3 air", "setblock 2 -61 3 farmland[moisture=7]",
                // pond
                "fill -8 -61 4 -6 -61 6 water")) {
            server.runCommand(cmd);
        }
    }

    static void setSeason(ClientGameTestContext ctx, TestServerContext server, Season season) {
        server.runCommand("season set " + season.id() + " 4");
        ctx.waitFor(client -> client.level != null && Calendar.client().date().season() == season);
        ctx.waitTicks(10);
        CalendarDate serverDate = server.computeOnServer(srv -> TurningSeasonsApi.getDate(srv.overworld()));
        CalendarDate clientDate = ctx.computeOnClient(client -> TurningSeasonsApi.getDate(client.level));
        check(serverDate.season() == season && serverDate.dayOfSeason() == 4, "server is on " + season + " day 4: " + serverDate);
        check(clientDate.equals(serverDate), "client calendar matches the server: " + clientDate + " vs " + serverDate);
        check(ctx.computeOnClient(client -> TurningSeasonsApi.hasSeasons(client.level)), "the overworld has seasons");
    }

    static void photograph(ClientGameTestContext ctx, TestServerContext server, String name) {
        server.runCommand("tp @a 0 -59 -8 0 14");
        ctx.waitTicks(20);
        ctx.waitFor(client -> client.levelRenderer.hasRenderedAllSections());
        ctx.waitTicks(10);
        shoot(ctx, name);
    }

    /** Runs vanilla's snow/ice/thaw check (with our hooks) on every column of the scene. */
    static void precipitateEverywhere(ServerLevel level) {
        for (int x = -AREA; x <= AREA; x++) {
            for (int z = -AREA; z <= AREA; z++) level.tickPrecipitation(new BlockPos(x, 0, z));
        }
    }

    /** Replants both crops and gives each 400 random ticks. */
    static void checkGrowth(TestServerContext server, Season season, boolean openGrows, boolean glassGrows) {
        server.runOnServer(srv -> {
            ServerLevel level = srv.overworld();
            check(CropGrowth.exposure(level, OPEN_CROP) == CropGrowth.Exposure.OUTDOOR, "open wheat is outdoors");
            check(CropGrowth.exposure(level, GLASS_CROP) == CropGrowth.Exposure.GREENHOUSE, "glass wheat is in a greenhouse");
            int open = grow(level, OPEN_CROP), glass = grow(level, GLASS_CROP);
            check(openGrows == (open > 0), season + ": open wheat grew " + open + " stages");
            check(glassGrows == (glass > 0), season + ": greenhouse wheat grew " + glass + " stages");
            // leave the open row and the greenhouse planted for the photos
            level.setBlockAndUpdate(OPEN_CROP.west(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, open > 0 ? 7 : 2));
            level.setBlockAndUpdate(OPEN_CROP.east(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, open > 0 ? 5 : 1));
        });
    }

    static int grow(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.WHEAT.defaultBlockState());
        for (int i = 0; i < 400; i++) {
            var state = level.getBlockState(pos);
            state.randomTick(level, pos, level.getRandom());
        }
        return level.getBlockState(pos).getValue(CropBlock.AGE);
    }

    static void checkBreeding(TestServerContext server, int expectedCooldown) {
        server.runOnServer(srv -> {
            ServerLevel level = srv.overworld();
            Animal a = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND), b = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
            a.snapTo(8, -60, -8);
            b.snapTo(9, -60, -8);
            level.addFreshEntity(a);
            level.addFreshEntity(b);
            a.finalizeSpawnChildFromBreeding(level, b, null);
            check(a.getAge() == expectedCooldown && b.getAge() == expectedCooldown,
                    "breeding cooldown is " + expectedCooldown + " (got " + a.getAge() + ")");
            a.discard();
            b.discard();
        });
    }

    static void checkSpawnWeights(TestServerContext server) {
        server.runOnServer(srv -> {
            check(SeasonalSpawns.weightFor(EntityTypes.COW, Season.WINTER) == 0.5F, "fewer cows in winter");
            check(SeasonalSpawns.weightFor(EntityTypes.COW, Season.SPRING) == 1.6F, "more cows in spring");
            check(SeasonalSpawns.weightFor(EntityTypes.SKELETON, Season.AUTUMN) == 1.25F, "skeletons via tag in autumn");
            SpawnerData cow = new SpawnerData(EntityTypes.COW, UniformInt.of(2, 4)), wolf = new SpawnerData(EntityTypes.WOLF, UniformInt.of(1, 2));
            WeightedList<SpawnerData> list = WeightedList.<SpawnerData>builder().add(cow, 10).add(wolf, 10).build();
            var winter = SeasonalSpawns.reweight(list, Season.WINTER).unwrap();
            check(winter.get(0).weight() == 5 && winter.get(1).weight() == 18, "winter weights: " + winter);
            boolean nsv = FabricLoader.getInstance().isModLoaded("nsvmobs");
            EntityType<?> frostbitten = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("nsvmobs", "frostbitten"));
            if (nsv) {
                check(SeasonalSpawns.weightFor(frostbitten, Season.WINTER) == 3F, "Frostbitten lean to winter");
            } else {
                check(SeasonalSpawns.rules().stream().noneMatch(r -> r.types().contains(Identifier.fromNamespaceAndPath("nsvmobs", "frostbitten"))),
                        "NSV rules stay off without NSV");
            }
        });
    }

    static int foliageAt(ClientGameTestContext ctx, BlockPos pos) {
        return ctx.computeOnClient(client -> BiomeColors.getAverageFoliageColor(client.level, pos));
    }

    static int grassAt(ClientGameTestContext ctx, BlockPos pos) {
        return ctx.computeOnClient(client -> BiomeColors.getAverageGrassColor(client.level, pos));
    }

    static int red(int c) { return (c >> 16) & 0xFF; }

    static int green(int c) { return (c >> 8) & 0xFF; }
}
