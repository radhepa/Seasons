package dev.turningseasons.client;

import dev.turningseasons.Calendar;
import dev.turningseasons.SeasonClimate;
import dev.turningseasons.SeasonConfig;
import dev.turningseasons.SeasonPayload;
import dev.turningseasons.api.CalendarDate;
import dev.turningseasons.api.Season;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Client side: keeps the synced calendar, recolours grass and leaves, drops autumn leaves, draws the HUD. */
public final class TurningSeasonsClient implements ClientModInitializer {
    /** The year position the world's colours were last drawn at (one value per calendar day). */
    private static float drawnColor = Float.NaN;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SeasonPayload.TYPE, (payload, context) -> receive(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(TurningSeasonsClient::clear));
        ClientTickEvents.END_CLIENT_TICK.register(TurningSeasonsClient::tick);
        SeasonHud.register();
        SeasonClimate.setClientThread(Thread.currentThread());   // the render thread runs client init
    }

    static void receive(SeasonPayload payload) {
        Calendar.client().update(payload.elapsed(), payload.daysPerSeason(), payload.dimensions());
        // in single-player the integrated server already set the climate for this JVM
        if (!Minecraft.getInstance().hasSingleplayerServer()) {
            boolean winter = Calendar.client().date().season() == Season.WINTER;
            SeasonClimate.setTemperatureShift(winter ? -payload.winterChill() : 0);
        }
    }

    static void clear() {
        Calendar.client().reset();
        SeasonClimate.setColorPosition(dev.turningseasons.SeasonColors.OFF);
        if (!Minecraft.getInstance().hasSingleplayerServer()) SeasonClimate.setTemperatureShift(0);
        drawnColor = Float.NaN;
    }

    /** The year position colours use: the middle of the current calendar day, so they change once a day. */
    public static float colorPosition(CalendarDate date) {
        return date.season().ordinal() + (date.dayOfSeason() - 0.5F) / date.daysPerSeason();
    }

    private static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null) return;
        boolean seasonal = Calendar.isSeasonal(level);
        CalendarDate date = Calendar.client().date();
        float wanted = seasonal && SeasonConfig.get().seasonalColors ? colorPosition(date) : dev.turningseasons.SeasonColors.OFF;
        if (wanted != drawnColor) {
            drawnColor = wanted;
            SeasonClimate.setColorPosition(wanted);
            level.clearTintCaches();
            client.levelExtractor.allChanged();
        }
        if (seasonal && date.season() == Season.AUTUMN && SeasonConfig.get().autumnLeaves && !client.isPaused()) {
            FallingLeaves.tick(client, level, date);
        }
    }

    /** Forces the next tick to redraw colours (tests use this after jumping the calendar). */
    public static void redrawColors() {
        drawnColor = Float.NaN;
    }
}
