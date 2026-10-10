package dev.turningseasons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.turningseasons.api.CalendarDate;
import dev.turningseasons.api.Season;

class CalendarTest {
    static final int DAY = CalendarDate.TICKS_PER_DAY;

    @Test
    void startsOnTheFirstDayOfSpring() {
        CalendarDate d = CalendarDate.of(0, 7);
        assertEquals(Season.SPRING, d.season());
        assertEquals(1, d.dayOfSeason());
        assertEquals(1, d.year());
        assertEquals(0F, d.progress());
        assertEquals(7, d.daysUntilNextSeason());
    }

    @Test
    void walksThroughTheYear() {
        assertEquals(Season.SPRING, CalendarDate.of(7L * DAY - 1, 7).season());
        assertEquals(7, CalendarDate.of(7L * DAY - 1, 7).dayOfSeason());
        assertEquals(Season.SUMMER, CalendarDate.of(7L * DAY, 7).season());
        assertEquals(Season.AUTUMN, CalendarDate.of(14L * DAY + 5, 7).season());
        assertEquals(Season.WINTER, CalendarDate.of(27L * DAY, 7).season());
        assertEquals(7, CalendarDate.of(27L * DAY, 7).dayOfSeason());
        CalendarDate nextYear = CalendarDate.of(28L * DAY, 7);
        assertEquals(Season.SPRING, nextYear.season());
        assertEquals(2, nextYear.year());
    }

    @Test
    void seasonLengthIsConfigurable() {
        assertEquals(Season.SUMMER, CalendarDate.of(3L * DAY, 3).season());
        assertEquals(Season.WINTER, CalendarDate.of(3L * DAY * 3, 3).season());
        // a broken length of 0 is treated as 1 day
        assertEquals(Season.SUMMER, CalendarDate.of(DAY, 0).season());
    }

    @Test
    void progressAndYearPosition() {
        CalendarDate mid = CalendarDate.of(14L * DAY + 7L * DAY / 2, 7);
        assertEquals(Season.AUTUMN, mid.season());
        assertEquals(0.5F, mid.progress(), 1e-6);
        assertEquals(2.5F, mid.yearPosition(), 1e-6);
    }

    @Test
    void ticksUntilAlwaysGoesForward() {
        long now = 3L * DAY + 100;   // spring day 4
        long toSummer = CalendarDate.ticksUntil(now, 7, Season.SUMMER, 1);
        assertEquals(Season.SUMMER, CalendarDate.of(now + toSummer, 7).season());
        assertEquals(1, CalendarDate.of(now + toSummer, 7).dayOfSeason());
        long toSpring = CalendarDate.ticksUntil(now, 7, Season.SPRING, 1);
        assertTrue(toSpring > 0, "spring day 1 has passed this year, so it is next year's");
        assertEquals(2, CalendarDate.of(now + toSpring, 7).year());
        long winter5 = CalendarDate.ticksUntil(now, 7, Season.WINTER, 5);
        assertEquals(5, CalendarDate.of(now + winter5, 7).dayOfSeason());
        // a day past the end is clamped to the last day
        assertEquals(7, CalendarDate.of(now + CalendarDate.ticksUntil(now, 7, Season.WINTER, 99), 7).dayOfSeason());
    }

    @Test
    void savedCalendarFollowsTheClockForwardOnly() {
        assertEquals(0, SeasonState.step(-1, 5000), "first tick only records the clock");
        assertEquals(1, SeasonState.step(5000, 5001));
        assertEquals(11000, SeasonState.step(13000, 24000), "sleeping moves the seasons");
        assertEquals(0, SeasonState.step(24000, 1000), "/time set back never rewinds");
    }

    @Test
    void changingTheLengthKeepsTheDate() {
        long autumnDay3 = 2L * 7 * DAY + 2L * DAY + 9000;          // year 1, autumn day 3 of 7, mid-afternoon
        long longer = SeasonState.rescale(autumnDay3, 7, 30);
        CalendarDate d = CalendarDate.of(longer, 30);
        assertEquals(Season.AUTUMN, d.season());
        assertEquals(3, d.dayOfSeason());
        assertEquals(1, d.year());
        assertEquals(9000, longer % DAY, "same time of day");

        long year3WinterDay20 = 2L * 4 * 30 * DAY + 3L * 30 * DAY + 19L * DAY;
        CalendarDate shorter = CalendarDate.of(SeasonState.rescale(year3WinterDay20, 30, 7), 7);
        assertEquals(Season.WINTER, shorter.season());
        assertEquals(7, shorter.dayOfSeason(), "day 20 doesn't exist in a 7-day season, so it's the last day");
        assertEquals(3, shorter.year());
    }

    @Test
    void oldConfigFilesMoveToThirtyDaySeasons() {
        SeasonConfig untouched = new SeasonConfig();
        untouched.daysPerSeason = 7;                                 // what 1.0.0 wrote
        assertTrue(SeasonConfig.upgrade(untouched));
        assertEquals(30, untouched.daysPerSeason);

        SeasonConfig edited = new SeasonConfig();
        edited.daysPerSeason = 12;                                   // somebody chose this, keep it
        assertTrue(SeasonConfig.upgrade(edited));
        assertEquals(12, edited.daysPerSeason);

        SeasonConfig current = new SeasonConfig();
        current.configVersion = SeasonConfig.CONFIG_VERSION;
        current.daysPerSeason = 7;                                   // chosen after the upgrade, keep it
        assertFalse(SeasonConfig.upgrade(current));
        assertEquals(7, current.daysPerSeason);
    }

    @Test
    void seasonNames() {
        assertEquals(Season.AUTUMN, Season.byId("fall"));
        assertEquals(Season.AUTUMN, Season.byId("Autumn"));
        assertNull(Season.byId("monsoon"));
        assertEquals(Season.SPRING, Season.WINTER.next());
        assertEquals(Season.WINTER, Season.SPRING.previous());
    }
}
