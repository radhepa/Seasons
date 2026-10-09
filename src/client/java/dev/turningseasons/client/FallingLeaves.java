package dev.turningseasons.client;

import dev.turningseasons.api.CalendarDate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Autumn: red, orange and yellow leaves drift down from the undersides of leaf blocks around the
 * player. Strongest in the middle of autumn. Uses vanilla's poplar leaf particles.
 */
final class FallingLeaves {
    private FallingLeaves() {}

    private static final SimpleParticleType[] COLORS = {
            ParticleTypes.RED_POPLAR_LEAVES, ParticleTypes.ORANGE_POPLAR_LEAVES, ParticleTypes.YELLOW_POPLAR_LEAVES};
    static final int RADIUS = 14, TRIES_AT_PEAK = 90;

    /** How many spots to try this tick: peaks mid-autumn, never below a third. */
    static int tries(float progress) {
        float bell = 1 - Math.abs(progress - 0.5F) * 2;
        return Math.round(TRIES_AT_PEAK * (0.35F + 0.65F * bell));
    }

    static void tick(Minecraft client, ClientLevel level, CalendarDate date) {
        if (client.player == null) return;
        RandomSource random = level.getRandom();
        BlockPos center = client.player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int tries = tries(date.progress());
        for (int i = 0; i < tries; i++) {
            pos.set(center.getX() + Mth.nextInt(random, -RADIUS, RADIUS), center.getY() + Mth.nextInt(random, -4, 12),
                    center.getZ() + Mth.nextInt(random, -RADIUS, RADIUS));
            if (!level.getBlockState(pos).is(BlockTags.LEAVES)) continue;
            if (!level.getBlockState(pos.below()).isAir()) continue;
            if (random.nextInt(3) != 0) continue;
            level.addParticle(COLORS[random.nextInt(COLORS.length)],
                    pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }
}
