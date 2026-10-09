package dev.turningseasons;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.turningseasons.api.Season;

/**
 * Settings from {@code config/turningseasons.json}. The file is written with these defaults the
 * first time the game starts; edit it and restart to change them. Every per-season value is a
 * multiplier where 1.0 means "like vanilla".
 */
public final class SeasonConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** One number per season. */
    public static final class PerSeason {
        public float spring, summer, autumn, winter;

        public PerSeason() {}

        public PerSeason(float spring, float summer, float autumn, float winter) {
            this.spring = spring;
            this.summer = summer;
            this.autumn = autumn;
            this.winter = winter;
        }

        public float get(Season season) {
            return switch (season) {
                case SPRING -> spring;
                case SUMMER -> summer;
                case AUTUMN -> autumn;
                case WINTER -> winter;
            };
        }
    }

    /** Minecraft days per season (a full year is four times this). */
    public int daysPerSeason = 7;
    /** Dimensions that have seasons. The calendar itself always follows the Overworld clock. */
    public List<String> seasonalDimensions = new ArrayList<>(List.of("minecraft:overworld"));

    /** Crop growth speed (blocks tagged turningseasons:seasonal_crops). */
    public PerSeason cropGrowth = new PerSeason(1.25F, 1.5F, 1.0F, 0.5F);
    /** In winter, crops open to the sky stop growing altogether unless glass is overhead. */
    public boolean winterDormancy = true;
    /** Crops under glass (turningseasons:greenhouse_glass) grow at least this fast, any season. */
    public float greenhouseGrowth = 1.0F;

    /** How often it rains (and how long rain lasts). */
    public PerSeason rainFrequency = new PerSeason(1.5F, 0.6F, 1.2F, 1.4F);
    /** How long parents wait before breeding again (vanilla is 5 minutes). */
    public PerSeason breedingCooldown = new PerSeason(0.5F, 1.0F, 1.0F, 2.0F);

    /** How much colder temperate biomes get in winter; 0.7 is enough to make plains snow. */
    public float winterChill = 0.7F;
    /** Chance per weather check that a patch of snow or ice melts outside winter. */
    public float thawChance = 0.35F;
    /** Chance per weather check in spring that a flower blooms on open grass. */
    public float springBloomChance = 0.04F;

    /** Client: grass and leaf colours follow the season. */
    public boolean seasonalColors = true;
    /** Client: falling leaves under trees in autumn. */
    public boolean autumnLeaves = true;
    /** Client: when the small season HUD shows: "clock" (holding a season clock), "always" or "never". */
    public String hud = "clock";

    private static SeasonConfig current = new SeasonConfig();

    public static SeasonConfig get() {
        return current;
    }

    /** Tests and commands can swap settings in at runtime. */
    public static void set(SeasonConfig config) {
        current = config;
    }

    /** Loads the file, writing the defaults first if it doesn't exist. Bad files fall back to defaults. */
    public static SeasonConfig load(Path file) {
        SeasonConfig loaded = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                loaded = GSON.fromJson(reader, SeasonConfig.class);
            } catch (Exception e) {
                TurningSeasons.LOGGER.warn("Couldn't read {}, using defaults", file, e);
            }
        }
        if (loaded == null) {
            loaded = new SeasonConfig();
            try {
                Files.createDirectories(file.getParent());
                try (Writer writer = Files.newBufferedWriter(file)) {
                    GSON.toJson(loaded, writer);
                }
            } catch (IOException e) {
                TurningSeasons.LOGGER.warn("Couldn't write {}", file, e);
            }
        }
        loaded.daysPerSeason = Math.max(1, loaded.daysPerSeason);
        if (loaded.seasonalDimensions == null) loaded.seasonalDimensions = new ArrayList<>(List.of("minecraft:overworld"));
        current = loaded;
        return loaded;
    }
}
