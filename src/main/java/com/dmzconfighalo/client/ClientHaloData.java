package com.dmzconfighalo.client;

import com.dmzconfighalo.HaloSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client side: halo settings of every player currently known (including yourself). */
public final class ClientHaloData {

    private static final Map<UUID, HaloSettings> SETTINGS = new ConcurrentHashMap<>();

    private ClientHaloData() {
    }

    /** @return the custom settings, or null if that player uses the original halo. */
    public static HaloSettings get(UUID playerId) {
        return SETTINGS.get(playerId);
    }

    public static void onSync(UUID playerId, HaloSettings settings) {
        if (settings.isDefault()) {
            SETTINGS.remove(playerId);
        } else {
            SETTINGS.put(playerId, settings);
        }

        LocalPlayer self = Minecraft.getInstance().player;
        if (self != null && self.getUUID().equals(playerId)) {
            ClientConfigBridge.onServerSync(settings);
        }
    }

    public static void clear() {
        SETTINGS.clear();
        ClientConfigBridge.onLogout();
    }
}
