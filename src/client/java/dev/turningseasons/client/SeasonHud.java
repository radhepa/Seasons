package dev.turningseasons.client;

import dev.turningseasons.Calendar;
import dev.turningseasons.SeasonClockItem;
import dev.turningseasons.SeasonConfig;
import dev.turningseasons.api.CalendarDate;
import dev.turningseasons.api.Season;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A small card in the top-left corner: the season clock's face, the season and the day.
 * Config {@code "hud"}: "clock" (default, while holding a season clock), "always" or "never".
 */
public final class SeasonHud {
    private SeasonHud() {}

    /** Text colour per season: fresh green, warm gold, rust, frost blue. */
    static final int[] COLORS = {0x9BE36D, 0xFFD45C, 0xF08A3C, 0xBFE3FF};

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR,
                Identifier.fromNamespaceAndPath("turningseasons", "season_card"), SeasonHud::draw);
    }

    static boolean visible(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null || !Calendar.isSeasonal(client.level)) return false;
        return switch (SeasonConfig.get().hud) {
            case "always" -> true;
            case "never" -> false;
            default -> player.getMainHandItem().is(SeasonClockItem.ITEM) || player.getOffhandItem().is(SeasonClockItem.ITEM);
        };
    }

    private static void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (!visible(client)) return;
        CalendarDate date = Calendar.client().date();
        Season season = date.season();
        Font font = client.font;
        Component title = season.displayName();
        Component day = Component.translatable("hud.turningseasons.day", date.dayOfSeason(), date.daysPerSeason());
        int x = 4, y = 4, w = 26 + Math.max(font.width(title), font.width(day)) + 6, h = 24;
        g.fill(x, y, x + w, y + h, 0x88000000);
        g.fill(x, y + h - 2, x + Math.max(2, Math.round(w * date.progress())), y + h, 0xFF000000 | COLORS[season.ordinal()]);
        ItemStack face = new ItemStack(SeasonClockItem.ITEM);
        face.set(SeasonClockItem.SEASON, season);
        g.fakeItem(face, x + 4, y + 3);
        g.text(font, title, x + 24, y + 3, 0xFF000000 | COLORS[season.ordinal()], true);
        g.text(font, day, x + 24, y + 12, 0xFFE0E0E0, true);
    }
}
