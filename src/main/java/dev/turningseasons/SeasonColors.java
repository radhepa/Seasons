package dev.turningseasons;

/**
 * Grass and leaf colours through the year. Pure maths, so it is unit tested.
 *
 * <p>Each season has a target colour and a strength, reached at the middle of the season; between
 * two middles the colour slides smoothly from one to the next. Summer has strength 0, so summer
 * looks exactly like vanilla.
 */
public final class SeasonColors {
    private SeasonColors() {}

    public enum Kind { GRASS, FOLIAGE }

    //                                     spring    summer    autumn    winter
    private static final int[] GRASS_TINT = {0x79CF45, 0x000000, 0xC4A24A, 0x948E6E};
    private static final float[] GRASS_MIX = {0.30F, 0.0F, 0.50F, 0.60F};
    private static final int[] LEAF_TINT = {0x67CB38, 0x000000, 0xDB6E22, 0x8B6640};
    private static final float[] LEAF_MIX = {0.25F, 0.0F, 0.78F, 0.62F};

    /** No colour change: used when a biome isn't temperate or seasonal colours are off. */
    public static final float OFF = -1;

    /**
     * {@code base} as it looks at {@code yearPosition} (0 = start of spring, 4 = end of winter).
     * A negative position means "don't touch".
     */
    public static int apply(int base, Kind kind, float yearPosition) {
        if (yearPosition < 0) return base;
        int[] tints = kind == Kind.GRASS ? GRASS_TINT : LEAF_TINT;
        float[] mixes = kind == Kind.GRASS ? GRASS_MIX : LEAF_MIX;
        // keyframes sit at 0.5, 1.5, 2.5, 3.5; find the one at or before us
        float t = ((yearPosition - 0.5F) % 4 + 4) % 4;
        int a = (int) t, b = (a + 1) % 4;
        float f = smooth(t - a);
        int colorA = mix(base, tints[a], mixes[a]);
        int colorB = mix(base, tints[b], mixes[b]);
        return mix(colorA, colorB, f);
    }

    /** Linear blend of two 0xRRGGBB colours; keeps {@code from}'s alpha byte. */
    public static int mix(int from, int to, float amount) {
        if (amount <= 0) return from;
        float k = Math.min(1, amount);
        int r = Math.round(((from >> 16) & 0xFF) * (1 - k) + ((to >> 16) & 0xFF) * k);
        int g = Math.round(((from >> 8) & 0xFF) * (1 - k) + ((to >> 8) & 0xFF) * k);
        int bl = Math.round((from & 0xFF) * (1 - k) + (to & 0xFF) * k);
        return (from & 0xFF000000) | r << 16 | g << 8 | bl;
    }

    private static float smooth(float x) {
        return x * x * (3 - 2 * x);
    }
}
