package dev.turningseasons.api;

import dev.turningseasons.Calendar;

import net.minecraft.world.level.Level;

/**
 * The public face of Turning Seasons. Works on both sides: on the server it reads the saved
 * calendar, on a client the copy the server last synced.
 *
 * <p>Other mods should only touch this class after checking
 * {@code FabricLoader.getInstance().isModLoaded("turningseasons")}, and keep the calls in a class
 * of their own so nothing loads it when the mod is absent. See API.md.
 */
public final class TurningSeasonsApi {
    private TurningSeasonsApi() {}

    /** True if {@code level} has seasons (by default only the Overworld does). */
    public static boolean hasSeasons(Level level) {
        return Calendar.isSeasonal(level);
    }

    /** The current season. Dimensions without seasons report the season the Overworld is in. */
    public static Season getSeason(Level level) {
        return date(level).season();
    }

    /** Day of the current season, starting at 1 and going up to {@link #getDaysPerSeason}. */
    public static int getDayOfSeason(Level level) {
        return date(level).dayOfSeason();
    }

    /** How many Minecraft days each season lasts (7 unless the config changes it). */
    public static int getDaysPerSeason(Level level) {
        return date(level).daysPerSeason();
    }

    /** How far through the current season we are, from 0.0 (first tick) to just under 1.0. */
    public static float getSeasonProgress(Level level) {
        return date(level).progress();
    }

    /** Which year of the calendar it is, starting at 1. */
    public static int getYear(Level level) {
        return date(level).year();
    }

    /** Everything above in one snapshot. */
    public static CalendarDate getDate(Level level) {
        return date(level);
    }

    private static CalendarDate date(Level level) {
        return Calendar.forSide(level.isClientSide()).date();
    }
}
