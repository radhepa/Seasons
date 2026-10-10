package dev.turningseasons;

import java.util.List;
import java.util.Set;

import dev.turningseasons.api.CalendarDate;

import net.minecraft.world.level.Level;

/**
 * The calendar as one side of the game sees it. The server keeps the authoritative copy (saved in
 * the world, see {@link SeasonState}); clients get a copy through {@link SeasonPayload}. In
 * single-player both live in the same JVM, which is why there are two.
 */
public final class Calendar {
    private static final Calendar SERVER = new Calendar(), CLIENT = new Calendar();

    private volatile long elapsed;
    private volatile int daysPerSeason = SeasonConfig.DEFAULT_DAYS_PER_SEASON;
    private volatile Set<String> dimensions = Set.of("minecraft:overworld");

    public static Calendar server() {
        return SERVER;
    }

    public static Calendar client() {
        return CLIENT;
    }

    public static Calendar forSide(boolean clientSide) {
        return clientSide ? CLIENT : SERVER;
    }

    public static boolean isSeasonal(Level level) {
        return forSide(level.isClientSide()).dimensions.contains(level.dimension().identifier().toString());
    }

    public void update(long elapsed, int daysPerSeason, List<String> dimensions) {
        this.elapsed = elapsed;
        this.daysPerSeason = Math.max(1, daysPerSeason);
        this.dimensions = Set.copyOf(dimensions);
    }

    public void reset() {
        update(0, SeasonConfig.get().daysPerSeason, SeasonConfig.get().seasonalDimensions);
    }

    public CalendarDate date() {
        return CalendarDate.of(elapsed, daysPerSeason);
    }

    public long elapsed() {
        return elapsed;
    }

    public int daysPerSeason() {
        return daysPerSeason;
    }

    public List<String> dimensions() {
        return List.copyOf(dimensions);
    }
}
