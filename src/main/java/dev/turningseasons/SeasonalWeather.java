package dev.turningseasons;

import dev.turningseasons.api.Season;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;

/**
 * Makes rain more or less common by season. Vanilla counts {@code rainTime} down by one every
 * tick and flips the weather at zero; we speed that countdown up or slow it down. A frequency of
 * 1.5 makes dry spells shorter and showers longer, 0.6 does the opposite.
 */
public final class SeasonalWeather {
    private SeasonalWeather() {}

    /** How fast the rain countdown should run compared to vanilla. */
    public static float countdownSpeed(float frequency, boolean raining) {
        float f = Math.max(0.05F, frequency);
        return raining ? 1 / f : f;
    }

    /**
     * Extra ticks to take off the countdown this tick (negative = add back). Vanilla already took
     * one off; {@code roll} is uniform in [0, 1).
     */
    public static int adjustment(float speed, float roll) {
        float extra = speed - 1;
        if (extra >= 0) return (int) extra + (roll < extra - (int) extra ? 1 : 0);
        return roll < -extra ? -1 : 0;
    }

    public static void tick(ServerLevel level, Season season) {
        if (!level.getGameRules().get(GameRules.ADVANCE_WEATHER)) return;
        WeatherData weather = level.getWeatherData();
        if (weather.getClearWeatherTime() > 0) return;   // /weather clear <time> is in charge
        int rainTime = weather.getRainTime();
        if (rainTime <= 1) return;
        RandomSource random = level.getRandom();
        float speed = countdownSpeed(SeasonConfig.get().rainFrequency.get(season), weather.isRaining());
        int change = adjustment(speed, random.nextFloat());
        if (change != 0) weather.setRainTime(Math.max(1, rainTime - change));
    }
}
