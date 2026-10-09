package dev.turningseasons.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/** The tags Turning Seasons reads. Add your own blocks and biomes to them with a data pack or your mod's data. */
public final class SeasonTags {
    private SeasonTags() {}

    /** Blocks whose random ticks follow the season's growth speed (includes #minecraft:crops and #minecraft:saplings). */
    public static final TagKey<Block> SEASONAL_CROPS = block("seasonal_crops");
    /** Blocks that turn the space under them into a greenhouse. */
    public static final TagKey<Block> GREENHOUSE_GLASS = block("greenhouse_glass");
    /** Blocks the greenhouse check looks straight through, like leaves (a tree is not a roof). */
    public static final TagKey<Block> NOT_A_ROOF = block("not_a_roof");
    /** Flowers that can pop up on open grass in spring. */
    public static final TagKey<Block> SPRING_BLOOM = block("spring_bloom");
    /** Biomes that get snow in winter, thaw in spring and change colour with the seasons. */
    public static final TagKey<Biome> TEMPERATE = TagKey.create(Registries.BIOME, id("temperate"));

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("turningseasons", path);
    }
}
