package dev.turningseasons;

import dev.turningseasons.api.SeasonTags;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

/**
 * The bits of season state that {@link Biome} methods need, since a Biome doesn't know which
 * world it's in. Only temperate biomes (the {@code turningseasons:temperate} tag) are affected.
 *
 * <p>In single-player the integrated server and the client share this class; the server is the
 * one that writes it then. A client on a remote server writes it from the synced calendar.
 */
public final class SeasonClimate {
    private SeasonClimate() {}

    /** Added to temperate biomes' temperature. Negative in winter, 0 otherwise. */
    private static volatile float temperatureShift;
    /** Set while code needs a biome's natural, un-shifted temperature. Server thread only. */
    private static boolean natural;

    /** Implemented on {@link Biome} by a mixin. */
    public interface SeasonalBiome {
        boolean turningseasons$isTemperate();

        void turningseasons$setTemperate(boolean temperate);
    }

    /** Client: where in the year grass and leaf colours are drawn, or {@link SeasonColors#OFF}. */
    private static volatile float colorPosition = SeasonColors.OFF;

    public static float colorPosition() {
        return colorPosition;
    }

    public static void setColorPosition(float yearPosition) {
        colorPosition = yearPosition;
    }

    /**
     * Only the game loops feel the chill: the server thread (snow, ice, the thaw) and the client
     * render thread (drawing snowfall). World generation runs on worker threads and reads biome
     * temperature too (lakes, top-layer snow, surface rules), so new chunks generate the same in
     * every season.
     */
    private static volatile Thread serverThread, clientThread;

    public static void setServerThread(Thread thread) {
        serverThread = thread;
    }

    public static void setClientThread(Thread thread) {
        clientThread = thread;
    }

    public static float temperatureShift() {
        if (natural) return 0;
        Thread current = Thread.currentThread();
        return current == serverThread || current == clientThread ? temperatureShift : 0;
    }

    public static void setTemperatureShift(float shift) {
        temperatureShift = shift;
    }

    public static boolean isTemperate(Biome biome) {
        return ((SeasonalBiome) (Object) biome).turningseasons$isTemperate();
    }

    /** Runs {@code check} with the seasonal temperature shift switched off. */
    public static boolean withNaturalClimate(java.util.function.BooleanSupplier check) {
        boolean was = natural;
        natural = true;
        try {
            return check.getAsBoolean();
        } finally {
            natural = was;
        }
    }

    /** Marks every biome in the registry as temperate or not, from the tag. Call after tags (re)load. */
    public static void tagBiomes(RegistryAccess registries) {
        registries.lookup(Registries.BIOME).ifPresent(biomes -> biomes.listElements().forEach(holder ->
                ((SeasonalBiome) (Object) holder.value()).turningseasons$setTemperate(holder.is(SeasonTags.TEMPERATE))));
    }

    public static boolean isTemperate(Holder<Biome> biome) {
        return biome.is(SeasonTags.TEMPERATE);
    }
}
