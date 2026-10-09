package dev.turningseasons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTabs;

/** Turning Seasons: spring, summer, autumn and winter for Minecraft. */
public final class TurningSeasons implements ModInitializer {
    public static final String MOD_ID = "turningseasons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        SeasonConfig.load(FabricLoader.getInstance().getConfigDir().resolve("turningseasons.json"));
        Calendar.server().reset();
        Calendar.client().reset();
        SeasonClockItem.init();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(SeasonClockItem.ITEM));

        PayloadTypeRegistry.clientboundPlay().register(SeasonPayload.TYPE, SeasonPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(SeasonsServer::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SeasonsServer.stop());
        ServerTickEvents.END_SERVER_TICK.register(SeasonsServer::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SeasonsServer.sync(handler.getPlayer()));
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> SeasonClimate.tagBiomes(registries));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> SeasonCommand.register(dispatcher));
        SeasonalSpawns.register();
        LOGGER.info("Turning Seasons ready: {} days per season", SeasonConfig.get().daysPerSeason);
    }
}
