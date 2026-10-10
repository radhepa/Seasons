package dev.turningseasons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.turningseasons.CropGrowth.Exposure;
import dev.turningseasons.api.Season;

class GrowthAndWeatherTest {
    final SeasonConfig config = new SeasonConfig();

    @Test
    void outdoorCropsSleepInWinter() {
        assertEquals(0F, CropGrowth.multiplier(Season.WINTER, Exposure.OUTDOOR, config));
        assertEquals(1.5F, CropGrowth.multiplier(Season.SUMMER, Exposure.OUTDOOR, config));
        assertEquals(1.25F, CropGrowth.multiplier(Season.SPRING, Exposure.OUTDOOR, config));
    }

    @Test
    void greenhousesKeepGrowing() {
        assertEquals(1.0F, CropGrowth.multiplier(Season.WINTER, Exposure.GREENHOUSE, config));
        // a greenhouse never slows a good season down
        assertEquals(1.5F, CropGrowth.multiplier(Season.SUMMER, Exposure.GREENHOUSE, config));
        // a barn roof isn't glass: slow, but not dormant
        assertEquals(0.5F, CropGrowth.multiplier(Season.WINTER, Exposure.SHELTERED, config));
    }

    @Test
    void dormancyCanBeSwitchedOff() {
        SeasonConfig mild = new SeasonConfig();
        mild.winterDormancy = false;
        assertEquals(0.5F, CropGrowth.multiplier(Season.WINTER, Exposure.OUTDOOR, mild));
    }

    @Test
    void tickDecisionsAverageToTheMultiplier() {
        for (float m : new float[]{0F, 0.25F, 0.5F, 1F, 1.25F, 1.5F, 2F}) {
            int n = 10000, total = 0;
            for (int i = 0; i < n; i++) total += CropGrowth.ticksFor(m, (i + 0.5F) / n);
            assertEquals(m, total / (float) n, 0.001, "speed " + m);
        }
        assertEquals(0, CropGrowth.ticksFor(0, 0));
        assertEquals(2, CropGrowth.ticksFor(5, 0.99F), "capped at two ticks");
    }

    @Test
    void rainCountdown() {
        assertEquals(1.5F, SeasonalWeather.countdownSpeed(1.5F, false), "dry spells end sooner");
        assertEquals(1 / 1.5F, SeasonalWeather.countdownSpeed(1.5F, true), 1e-6, "showers last longer");
        assertEquals(0.6F, SeasonalWeather.countdownSpeed(0.6F, false), "summer: dry spells drag on");
        for (float speed : new float[]{0.4F, 0.6F, 1F, 1.5F, 2.5F}) {
            int n = 10000, total = 0;
            for (int i = 0; i < n; i++) total += SeasonalWeather.adjustment(speed, (i + 0.5F) / n);
            // vanilla took one tick off already; adjustments make the average speed - 1
            assertEquals(speed - 1, total / (float) n, 0.001, "speed " + speed);
        }
    }

    @Test
    void defaultsMatchTheDesign() {
        assertTrue(config.rainFrequency.get(Season.SPRING) > 1 && config.rainFrequency.get(Season.SUMMER) < 1);
        assertTrue(config.breedingCooldown.get(Season.SPRING) < 1 && config.breedingCooldown.get(Season.WINTER) > 1);
        assertEquals(30, config.daysPerSeason);
    }
}
