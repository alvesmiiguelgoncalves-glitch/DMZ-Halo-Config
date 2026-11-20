package com.dmzconfighalo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class HaloServerEvents {

    private HaloServerEvents() {
    }

    /** The player gets their own settings when joining. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HaloManager.sendTo(player, player);
        }
    }

    /** When a player starts seeing another one, they receive that player's halo settings. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            HaloManager.sendTo(viewer, target);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer old && event.getEntity() instanceof ServerPlayer now) {
            HaloManager.copyPersisted(old, now);
        }
    }
}
