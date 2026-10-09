package dev.turningseasons.api;

/**
 * A point on the season calendar.
 *
 * @param season        the current season
 * @param dayOfSeason   1 .. daysPerSeason
 * @param daysPerSeason length of every season in Minecraft days
 * @param progress      0.0 at the first tick of the season, approaching 1.0 at its end
 * @param year          1 for the first four seasons, then 2, ...
 * @param elapsedTicks  ticks since the calendar began (the raw value everything else comes from)
 */
public record CalendarDate(Season season, int dayOfSeason, int daysPerSeason, float progress, int year, long elapsedTicks) {
    public static final int TICKS_PER_DAY = 24000;

    /** The date {@code elapsedTicks} after the first morning of spring, year 1. */
    public static CalendarDate of(long elapsedTicks, int daysPerSeason) {
        long elapsed = Math.max(0, elapsedTicks);
        int days = Math.max(1, daysPerSeason);
        long seasonTicks = (long) days * TICKS_PER_DAY;
        long yearTicks = seasonTicks * 4;
        long intoYear = elapsed % yearTicks;
        int index = (int) (intoYear / seasonTicks);
        long intoSeason = intoYear % seasonTicks;
        return new CalendarDate(Season.values()[index], (int) (intoSeason / TICKS_PER_DAY) + 1, days,
                (float) ((double) intoSeason / seasonTicks), (int) (elapsed / yearTicks) + 1, elapsed);
    }

    /** Ticks to add to an elapsed count so it lands on the first morning of {@code target} day {@code day}, never going back. */
    public static long ticksUntil(long elapsedTicks, int daysPerSeason, Season target, int day) {
        long seasonTicks = (long) Math.max(1, daysPerSeason) * TICKS_PER_DAY;
        long yearTicks = seasonTicks * 4;
        long yearStart = elapsedTicks - Math.floorMod(elapsedTicks, yearTicks);
        int clampedDay = Math.clamp(day, 1, Math.max(1, daysPerSeason));
        long goal = yearStart + target.ordinal() * seasonTicks + (long) (clampedDay - 1) * TICKS_PER_DAY;
        if (goal < elapsedTicks) goal += yearTicks;
        return goal - elapsedTicks;
    }

    /** Whole days left before the next season begins (1 on the last day). */
    public int daysUntilNextSeason() {
        return daysPerSeason - dayOfSeason + 1;
    }

    /** Position in the year as a number from 0 (start of spring) to just under 4 (end of winter). */
    public float yearPosition() {
        return season.ordinal() + progress;
    }

    /** A number that changes once per calendar day; handy for "did the day roll over?" checks. */
    public long dayNumber() {
        return elapsedTicks / TICKS_PER_DAY;
    }
}
