package dev.turningseasons;

import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.turningseasons.api.Season;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

/**
 * Seasonal spawn weights, read from {@code data/<namespace>/seasonal_spawns/*.json}:
 * <pre>
 * {
 *   "requires_mod": "nsvmobs",                       (optional: skip the file without this mod)
 *   "entities": ["nsvmobs:frostbitten", "#minecraft:skeletons"],
 *   "weights": {"spring": 0.5, "summer": 0.25, "autumn": 1.0, "winter": 3.0}
 * }
 * </pre>
 * Natural spawning picks a mob from a weighted list; we multiply each entry's weight by its
 * season's number before the pick. 0 means "not this season". Rules for the same mob multiply.
 */
public final class SeasonalSpawns {
    private SeasonalSpawns() {}

    private static final FileToIdConverter FILES = FileToIdConverter.json("seasonal_spawns");

    /** One file. {@code types} and {@code tags} are what "entities" listed. */
    public record Rule(List<Identifier> types, List<TagKey<EntityType<?>>> tags, float[] weights) {
        boolean matches(EntityType<?> type) {
            if (types.contains(EntityType.getKey(type))) return true;
            for (TagKey<EntityType<?>> tag : tags) {
                if (type.builtInRegistryHolder().is(tag)) return true;
            }
            return false;
        }
    }

    private static volatile List<Rule> rules = List.of();
    private static final Map<WeightedList<SpawnerData>, WeightedList<SpawnerData>> CACHE = new IdentityHashMap<>();
    private static Season cachedSeason;

    public static void register() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Identifier.fromNamespaceAndPath(TurningSeasons.MOD_ID, "seasonal_spawns"),
                new SimplePreparableReloadListener<List<Rule>>() {
                    @Override
                    protected List<Rule> prepare(ResourceManager resources, ProfilerFiller profiler) {
                        List<Rule> loaded = new ArrayList<>();
                        FILES.listMatchingResources(resources).forEach((file, resource) -> {
                            try (Reader reader = resource.openAsReader()) {
                                Rule rule = parse(JsonParser.parseReader(reader).getAsJsonObject(), FabricLoader.getInstance()::isModLoaded);
                                if (rule != null) loaded.add(rule);
                            } catch (Exception e) {
                                TurningSeasons.LOGGER.warn("Skipping bad seasonal spawn file {}", file, e);
                            }
                        });
                        return List.copyOf(loaded);
                    }

                    @Override
                    protected void apply(List<Rule> loaded, ResourceManager resources, ProfilerFiller profiler) {
                        setRules(loaded);
                    }
                });
    }

    /** Reads one file; null if it needs a mod that isn't installed. */
    public static Rule parse(JsonObject json, java.util.function.Predicate<String> modLoaded) {
        if (json.has("requires_mod") && !modLoaded.test(json.get("requires_mod").getAsString())) return null;
        List<Identifier> types = new ArrayList<>();
        List<TagKey<EntityType<?>>> tags = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("entities")) {
            String id = e.getAsString();
            if (id.startsWith("#")) tags.add(TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(id.substring(1))));
            else types.add(Identifier.parse(id));
        }
        SeasonConfig.PerSeason w = new Gson().fromJson(json.get("weights"), SeasonConfig.PerSeason.class);
        return new Rule(List.copyOf(types), List.copyOf(tags), new float[]{w.spring, w.summer, w.autumn, w.winter});
    }

    public static synchronized void setRules(List<Rule> loaded) {
        rules = loaded;
        CACHE.clear();
    }

    public static List<Rule> rules() {
        return rules;
    }

    /** The combined multiplier for one mob this season (1 if no rule mentions it). */
    public static float weightFor(EntityType<?> type, Season season) {
        float m = 1;
        for (Rule rule : rules) {
            if (rule.matches(type)) m *= Math.max(0, rule.weights()[season.ordinal()]);
        }
        return m;
    }

    /** Scales an entry's weight. A tiny but positive result still keeps the mob possible (weight 1). */
    public static int scaledWeight(int weight, float multiplier) {
        if (multiplier <= 0) return 0;
        return Math.max(1, Math.round(weight * multiplier));
    }

    /** The spawn list natural spawning should use right now. */
    public static synchronized WeightedList<SpawnerData> reweight(WeightedList<SpawnerData> list, Season season) {
        if (rules.isEmpty() || list.isEmpty()) return list;
        if (season != cachedSeason) {
            CACHE.clear();
            cachedSeason = season;
        }
        return CACHE.computeIfAbsent(list, l -> {
            WeightedList.Builder<SpawnerData> builder = WeightedList.builder();
            boolean changed = false;
            for (Weighted<SpawnerData> entry : l.unwrap()) {
                int weight = scaledWeight(entry.weight(), weightFor(entry.value().type(), season));
                changed |= weight != entry.weight();
                if (weight > 0) builder.add(entry.value(), weight);
            }
            return changed ? builder.build() : l;
        });
    }
}
