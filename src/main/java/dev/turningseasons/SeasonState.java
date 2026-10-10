package dev.turningseasons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.turningseasons.api.CalendarDate;

import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The saved calendar: how many ticks of Overworld time have passed since spring of year 1.
 *
 * <p>Each tick we add however far the Overworld clock moved forward. Sleeping (which jumps the
 * clock to morning) and {@code /time add} therefore move the seasons too, while {@code /time set}
 * to an earlier time never makes the calendar run backwards.
 *
 * <p>It also keeps the world's season length: a new world takes the config default, and
 * {@code /season length} changes it afterwards.
 */
public final class SeasonState extends SavedData {
    public static final Codec<SeasonState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("elapsed").forGetter(s -> s.elapsed),
            Codec.LONG.fieldOf("last_clock").forGetter(s -> s.lastClock),
            Codec.INT.optionalFieldOf("days_per_season", 0).forGetter(s -> s.daysPerSeason)
    ).apply(i, SeasonState::new));

    public static final SavedDataType<SeasonState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TurningSeasons.MOD_ID, "calendar"), SeasonState::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private long elapsed;
    private long lastClock = -1;
    private int daysPerSeason;   // 0 until the world first loads

    public SeasonState() {}

    private SeasonState(long elapsed, long lastClock, int daysPerSeason) {
        this.elapsed = elapsed;
        this.lastClock = lastClock;
        this.daysPerSeason = daysPerSeason;
    }

    /** Follows the Overworld clock; returns true if the calendar moved. */
    public boolean follow(long clock) {
        long step = step(lastClock, clock);
        if (lastClock != clock) {
            lastClock = clock;
            setDirty();
        }
        if (step <= 0) return false;
        elapsed += step;
        return true;
    }

    /** How far the calendar should move when the clock goes from {@code last} to {@code now}. */
    static long step(long last, long now) {
        if (last < 0) return 0;          // first tick after loading a fresh world
        return Math.max(0, now - last);  // never backwards
    }

    public long elapsed() {
        return elapsed;
    }

    public void setElapsed(long elapsed) {
        this.elapsed = Math.max(0, elapsed);
        setDirty();
    }

    public int daysPerSeason() {
        return daysPerSeason;
    }

    /** Gives a world that has no season length yet the config default. */
    public void initDaysPerSeason(int days) {
        if (daysPerSeason > 0) return;
        daysPerSeason = Math.max(1, days);
        setDirty();
    }

    /** Changes the season length, keeping the season, day (clamped to the new length), year and time of day. */
    public void setDaysPerSeason(int days) {
        int to = Math.max(1, days);
        elapsed = rescale(elapsed, daysPerSeason, to);
        daysPerSeason = to;
        setDirty();
    }

    /** The same date measured with seasons {@code to} days long instead of {@code from}. */
    static long rescale(long elapsed, int from, int to) {
        CalendarDate date = CalendarDate.of(elapsed, from);
        int days = Math.max(1, to);
        long seasonTicks = (long) days * CalendarDate.TICKS_PER_DAY;
        int day = Math.min(date.dayOfSeason(), days);
        return (date.year() - 1L) * 4 * seasonTicks + date.season().ordinal() * seasonTicks
                + (day - 1L) * CalendarDate.TICKS_PER_DAY + Math.floorMod(Math.max(0, elapsed), CalendarDate.TICKS_PER_DAY);
    }
}
