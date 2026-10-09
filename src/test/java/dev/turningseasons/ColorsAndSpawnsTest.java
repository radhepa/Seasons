package dev.turningseasons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

class ColorsAndSpawnsTest {
    static final int PLAINS_GRASS = 0x91BD59, FOREST_LEAVES = 0x59AE30;

    static int red(int c) { return (c >> 16) & 0xFF; }
    static int green(int c) { return (c >> 8) & 0xFF; }

    @Test
    void summerIsVanilla() {
        assertEquals(PLAINS_GRASS, SeasonColors.apply(PLAINS_GRASS, SeasonColors.Kind.GRASS, 1.5F));
        assertEquals(FOREST_LEAVES, SeasonColors.apply(FOREST_LEAVES, SeasonColors.Kind.FOLIAGE, 1.5F));
        assertEquals(PLAINS_GRASS, SeasonColors.apply(PLAINS_GRASS, SeasonColors.Kind.GRASS, SeasonColors.OFF));
    }

    @Test
    void autumnLeavesTurnOrange() {
        int autumn = SeasonColors.apply(FOREST_LEAVES, SeasonColors.Kind.FOLIAGE, 2.5F);
        assertTrue(red(autumn) > green(autumn), "red beats green in autumn: " + Integer.toHexString(autumn));
        int summer = SeasonColors.apply(FOREST_LEAVES, SeasonColors.Kind.FOLIAGE, 1.5F);
        assertTrue(red(summer) < green(summer));
    }

    @Test
    void colorsChangeSmoothlyAndWrapAroundTheYear() {
        int prev = SeasonColors.apply(PLAINS_GRASS, SeasonColors.Kind.GRASS, 0);
        for (int i = 1; i <= 400; i++) {
            int now = SeasonColors.apply(PLAINS_GRASS, SeasonColors.Kind.GRASS, i * 0.01F);
            assertTrue(Math.abs(red(now) - red(prev)) <= 4 && Math.abs(green(now) - green(prev)) <= 4, "jump at " + i);
            prev = now;
        }
        assertEquals(SeasonColors.apply(PLAINS_GRASS, SeasonColors.Kind.GRASS, 0F),
                SeasonColors.apply(PLAINS_GRASS, SeasonColors.Kind.GRASS, 4F), "end of winter meets start of spring");
    }

    @Test
    void mixKeepsAlpha() {
        assertEquals(0xFF808080, SeasonColors.mix(0xFF000000, 0x00FFFFFF, 0.5F));
        assertEquals(0x123456, SeasonColors.mix(0x123456, 0xFFFFFF, 0F));
    }

    @Test
    void spawnFilesNeedTheirMod() {
        String json = """
                {"requires_mod": "nsvmobs", "entities": ["nsvmobs:frostbitten", "#minecraft:skeletons"],
                 "weights": {"spring": 0.5, "summer": 0.25, "autumn": 1, "winter": 3}}""";
        assertNull(SeasonalSpawns.parse(JsonParser.parseString(json).getAsJsonObject(), mod -> false));
        SeasonalSpawns.Rule rule = SeasonalSpawns.parse(JsonParser.parseString(json).getAsJsonObject(), mod -> mod.equals("nsvmobs"));
        assertNotNull(rule);
        assertEquals(1, rule.types().size());
        assertEquals(1, rule.tags().size());
        assertEquals(3F, rule.weights()[3]);
    }

    @Test
    void spawnWeightsScale() {
        assertEquals(30, SeasonalSpawns.scaledWeight(10, 3));
        assertEquals(0, SeasonalSpawns.scaledWeight(10, 0), "0 removes the mob this season");
        assertEquals(1, SeasonalSpawns.scaledWeight(1, 0.25F), "a rare mob stays possible");
    }
}
