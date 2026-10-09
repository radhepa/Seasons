package dev.turningseasons;

import dev.turningseasons.api.Season;
import dev.turningseasons.api.SeasonTags;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Seasonal crop growth. A random tick on a block in {@code turningseasons:seasonal_crops} is
 * skipped, passed through or doubled, so on average the crop grows at the season's speed.
 */
public final class CropGrowth {
    private CropGrowth() {}

    /** Where a crop is, as far as the weather is concerned. */
    public enum Exposure {
        /** Nothing but air (or leaves) between the crop and the sky. */
        OUTDOOR,
        /** The first roof block above is glass. */
        GREENHOUSE,
        /** Under some other roof: a barn, a cave, a tunnel. */
        SHELTERED
    }

    /** How far up we look for a roof before giving up and calling it sheltered. */
    static final int MAX_ROOF_SCAN = 48;

    private static boolean extraTick;

    /** The growth speed for a crop. 0 means dormant. */
    public static float multiplier(Season season, Exposure exposure, SeasonConfig config) {
        float base = Math.max(0, config.cropGrowth.get(season));
        return switch (exposure) {
            case GREENHOUSE -> Math.max(base, config.greenhouseGrowth);
            case OUTDOOR -> season == Season.WINTER && config.winterDormancy ? 0 : base;
            case SHELTERED -> base;
        };
    }

    /**
     * How many times one random tick should really run: 0, 1 or 2. {@code roll} is a uniform
     * random number in [0, 1). A speed of 0.5 runs half the ticks; 1.5 runs every tick and doubles half.
     */
    public static int ticksFor(float multiplier, float roll) {
        if (multiplier <= 0) return 0;
        if (multiplier < 1) return roll < multiplier ? 1 : 0;
        return roll < Math.min(1, multiplier - 1) ? 2 : 1;
    }

    /** Looks straight up from a crop for a roof. */
    public static Exposure exposure(ServerLevel level, BlockPos pos) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        if (pos.getY() + 1 >= top) return Exposure.OUTDOOR;
        BlockPos.MutableBlockPos cursor = pos.mutable();
        int limit = Math.min(top, pos.getY() + 1 + MAX_ROOF_SCAN);
        for (int y = pos.getY() + 1; y < limit; y++) {
            cursor.setY(y);
            BlockState above = level.getBlockState(cursor);
            if (above.is(SeasonTags.GREENHOUSE_GLASS)) return Exposure.GREENHOUSE;
            if (above.isAir() || above.is(SeasonTags.NOT_A_ROOF) || !above.is(BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP)) continue;
            return Exposure.SHELTERED;
        }
        // only see-through blocks (leaves) up to the top, or the roof is too far up to matter
        return limit == top ? Exposure.OUTDOOR : Exposure.SHELTERED;
    }

    /**
     * Called before a block's random tick. Returns true if this method handled the tick (vanilla's
     * should be skipped), false to let vanilla run normally.
     */
    public static boolean onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (extraTick || !Calendar.isSeasonal(level)) return false;
        Season season = Calendar.server().date().season();
        float speed = multiplier(season, exposure(level, pos), SeasonConfig.get());
        int ticks = ticksFor(speed, random.nextFloat());
        if (ticks == 1) return false;
        if (ticks == 2) {
            extraTick = true;
            try {
                state.randomTick(level, pos, random);
                BlockState grown = level.getBlockState(pos);
                if (grown.is(SeasonTags.SEASONAL_CROPS) && grown.isRandomlyTicking()) grown.randomTick(level, pos, random);
            } finally {
                extraTick = false;
            }
        }
        return true;
    }
}
