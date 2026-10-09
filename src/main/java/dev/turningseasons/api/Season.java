package dev.turningseasons.api;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** The four seasons, in calendar order. */
public enum Season implements StringRepresentable {
    SPRING, SUMMER, AUTUMN, WINTER;

    public static final Codec<Season> CODEC = StringRepresentable.fromEnum(Season::values);

    /** Lower-case name used in commands, data files and item models: "spring", "summer", "autumn", "winter". */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    public Season next() {
        return values()[(ordinal() + 1) % 4];
    }

    public Season previous() {
        return values()[(ordinal() + 3) % 4];
    }

    /** "Spring", "Summer", ... (translated). */
    public Component displayName() {
        return Component.translatable("season.turningseasons." + id());
    }

    /** The season named {@code id} ("spring", "fall" and "autumn" both work), or null. */
    public static Season byId(String id) {
        if (id == null) return null;
        String key = id.toLowerCase(Locale.ROOT).trim();
        if (key.equals("fall")) return AUTUMN;
        for (Season s : values()) {
            if (s.id().equals(key)) return s;
        }
        return null;
    }
}
