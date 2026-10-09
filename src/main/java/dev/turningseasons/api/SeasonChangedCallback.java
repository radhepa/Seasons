package dev.turningseasons.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;

/**
 * Fired on the server, once per seasonal dimension, when the season turns (naturally or through
 * {@code /season set}). {@code level} is the dimension whose season changed.
 */
@FunctionalInterface
public interface SeasonChangedCallback {
    Event<SeasonChangedCallback> EVENT = EventFactory.createArrayBacked(SeasonChangedCallback.class,
            listeners -> (level, from, to) -> {
                for (SeasonChangedCallback l : listeners) l.onSeasonChanged(level, from, to);
            });

    void onSeasonChanged(ServerLevel level, Season from, Season to);
}
