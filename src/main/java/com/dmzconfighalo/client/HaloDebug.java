package com.dmzconfighalo.client;

import com.dmzconfighalo.DMZConfigHalo;
import com.dmzconfighalo.HaloSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Diagnostic helper. Writes INFO lines to latest.log AND a short message in the chat (client side only),
 * never every frame. Set CHAT to false to silence the chat messages.
 */
public final class HaloDebug {

    private static final boolean CHAT = true;

    private static volatile boolean hookSeen = false;
    private static volatile HaloSettings lastApplied = null;

    private HaloDebug() {
    }

    /** Called by the Mixin every time DMZ draws a halo bone. Reports only the first time and when settings change. */
    public static void haloBoneDrawn(UUID playerId, HaloSettings settings) {
        if (!hookSeen) {
            hookSeen = true;
            say("Mixin hook ACTIVE (DMZ drew a halo bone)");
        }
        if (settings != null && !settings.equals(lastApplied)) {
            lastApplied = settings;
            say("Drawing halo with " + settings);
        }
    }

    public static void settingsReceived(UUID playerId, HaloSettings settings) {
        say("Client received settings: " + settings);
    }

    private static void say(String msg) {
        DMZConfigHalo.LOGGER.info("[DMZ Config Halo] {}", msg);
        if (CHAT) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("[Halo debug] " + msg));
            }
        }
    }
}
