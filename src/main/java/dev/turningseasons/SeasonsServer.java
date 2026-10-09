package dev.turningseasons;

import dev.turningseasons.api.CalendarDate;
import dev.turningseasons.api.Season;
import dev.turningseasons.api.SeasonChangedCallback;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Runs the calendar on the server: follows the clock, fires events, syncs clients, shapes weather. */
public final class SeasonsServer {
    private SeasonsServer() {}

    private static SeasonState state;
    private static long lastDay = -1;
    private static Season lastSeason;

    public static void start(MinecraftServer server) {
        SeasonClimate.setServerThread(Thread.currentThread());
        state = server.overworld().getDataStorage().computeIfAbsent(SeasonState.TYPE);
        lastDay = -1;
        lastSeason = null;
        publish(server, false);
    }

    public static void stop() {
        state = null;
        SeasonClimate.setServerThread(null);
        Calendar.server().reset();
        SeasonClimate.setTemperatureShift(0);
    }

    public static void tick(MinecraftServer server) {
        if (state == null) return;
        ServerLevel overworld = server.overworld();
        state.follow(overworld.getOverworldClockTime());
        publish(server, true);
        Season season = Calendar.server().date().season();
        for (ServerLevel level : server.getAllLevels()) {
            if (Calendar.isSeasonal(level)) SeasonalWeather.tick(level, season);
        }
    }

    /** Moves the calendar to a new elapsed time (commands, tests) and tells everyone. */
    public static void setElapsed(MinecraftServer server, long elapsed) {
        if (state == null) start(server);
        state.setElapsed(elapsed);
        lastDay = -1;   // always resync after a jump
        publish(server, true);
    }

    /** Copies the saved state into {@link Calendar#server()}; on a new day syncs clients, on a new season fires the event. */
    private static void publish(MinecraftServer server, boolean announce) {
        SeasonConfig config = SeasonConfig.get();
        Calendar.server().update(state.elapsed(), config.daysPerSeason, config.seasonalDimensions);
        CalendarDate date = Calendar.server().date();
        SeasonClimate.setTemperatureShift(date.season() == Season.WINTER ? -config.winterChill : 0);
        if (date.dayNumber() != lastDay) {
            lastDay = date.dayNumber();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player);
        }
        if (date.season() != lastSeason) {
            Season from = lastSeason;
            lastSeason = date.season();
            if (announce && from != null) {
                for (ServerLevel level : server.getAllLevels()) {
                    if (Calendar.isSeasonal(level)) SeasonChangedCallback.EVENT.invoker().onSeasonChanged(level, from, date.season());
                }
            }
        }
    }

    public static void sync(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, SeasonPayload.TYPE)) return;
        Calendar c = Calendar.server();
        ServerPlayNetworking.send(player, new SeasonPayload(c.elapsed(), c.daysPerSeason(), SeasonConfig.get().winterChill, c.dimensions()));
    }
}
