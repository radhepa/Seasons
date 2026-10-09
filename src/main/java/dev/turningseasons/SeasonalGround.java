package dev.turningseasons;

import java.util.List;

import dev.turningseasons.api.Season;
import dev.turningseasons.api.SeasonTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Runs after vanilla's weather check on a random column (the one that places snow and freezes
 * water): outside winter it melts seasonal snow and ice, and in spring it can grow a flower.
 *
 * <p>Snow in winter needs no code here: a mixin makes temperate biomes cold enough that vanilla
 * snows and freezes on its own.
 */
public final class SeasonalGround {
    private SeasonalGround() {}

    public static void afterPrecipitation(ServerLevel level, BlockPos column) {
        if (!Calendar.isSeasonal(level)) return;
        Season season = Calendar.server().date().season();
        if (season == Season.WINTER) return;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        Holder<Biome> biome = level.getBiome(top);
        if (!SeasonClimate.isTemperate(biome)) return;
        RandomSource random = level.getRandom();
        SeasonConfig config = SeasonConfig.get();
        if (random.nextFloat() < config.thawChance) thaw(level, biome.value(), top);
        if (season == Season.SPRING && random.nextFloat() < config.springBloomChance) bloom(level, top, random);
    }

    /** Melts one layer of snow at {@code top}, or the ice just under it, where the biome is naturally warm. */
    static void thaw(ServerLevel level, Biome biome, BlockPos top) {
        BlockPos below = top.below();
        // high mountain snow stays: only melt what the season brought
        if (!SeasonClimate.withNaturalClimate(() -> biome.warmEnoughToRain(top, level.getSeaLevel()))) return;
        // a thin snow layer may or may not count for the heightmap, so look at both spots
        BlockPos snow = level.getBlockState(top).is(Blocks.SNOW) ? top : level.getBlockState(below).is(Blocks.SNOW) ? below : null;
        if (snow != null) {
            BlockState state = level.getBlockState(snow);
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            level.setBlockAndUpdate(snow, layers > 1 ? state.setValue(SnowLayerBlock.LAYERS, layers - 1) : Blocks.AIR.defaultBlockState());
        } else if (level.getBlockState(below).is(Blocks.ICE)) {
            level.setBlockAndUpdate(below, Blocks.WATER.defaultBlockState());
        }
    }

    /** Plants a random spring flower on open grass, unless there's already one close by. */
    static void bloom(ServerLevel level, BlockPos top, RandomSource random) {
        if (!level.getBlockState(top).isAir() || !level.getBlockState(top.below()).is(Blocks.GRASS_BLOCK)) return;
        for (BlockPos near : BlockPos.betweenClosed(top.offset(-2, -1, -2), top.offset(2, 1, 2))) {
            if (level.getBlockState(near).is(BlockTags.FLOWERS)) return;
        }
        List<Holder<Block>> flowers = toList(BuiltInRegistries.BLOCK.getTagOrEmpty(SeasonTags.SPRING_BLOOM));
        if (flowers.isEmpty()) return;
        BlockState flower = flowers.get(random.nextInt(flowers.size())).value().defaultBlockState();
        if (flower.canSurvive(level, top)) level.setBlockAndUpdate(top, flower);
    }

    private static List<Holder<Block>> toList(Iterable<Holder<Block>> holders) {
        java.util.ArrayList<Holder<Block>> list = new java.util.ArrayList<>();
        holders.forEach(list::add);
        return list;
    }
}
