package dev.turningseasons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
 */
public final class SeasonState extends SavedData {
    public static final Codec<SeasonState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("elapsed").forGetter(s -> s.elapsed),
            Codec.LONG.fieldOf("last_clock").forGetter(s -> s.lastClock)
    ).apply(i, SeasonState::new));

    public static final SavedDataType<SeasonState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TurningSeasons.MOD_ID, "calendar"), SeasonState::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private long elapsed;
    private long lastClock = -1;

    public SeasonState() {}

    private SeasonState(long elapsed, long lastClock) {
        this.elapsed = elapsed;
        this.lastClock = lastClock;
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
}
