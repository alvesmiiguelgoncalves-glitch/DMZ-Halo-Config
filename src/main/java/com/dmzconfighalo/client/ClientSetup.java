package com.dmzconfighalo.client;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Only ever loaded on the client (see DMZConfigHalo). */
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        modBus.addListener(ClientEvents::onConfigReloading);
    }
}
