package com.dmzconfighalo.client;

import com.dmzconfighalo.HaloConfig;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class ClientEvents {

    private ClientEvents() {
    }

    // Forge event bus
    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientConfigBridge.onLogin();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientHaloData.clear();
    }

    // Mod event bus (registered in ClientSetup)
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == HaloConfig.SPEC) {
            ClientConfigBridge.onFileReloaded();
        }
    }
}
