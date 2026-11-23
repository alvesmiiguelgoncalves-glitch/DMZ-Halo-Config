package com.dmzconfighalo.network;

import com.dmzconfighalo.HaloManager;
import com.dmzconfighalo.HaloSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: the player's config file changed (or they just joined).
 * It only ever affects the SENDER's own halo, and values are validated here.
 */
public class ConfigC2S {

    private final HaloSettings settings;

    public ConfigC2S(HaloSettings settings) {
        this.settings = settings;
    }

    public static void encode(ConfigC2S msg, FriendlyByteBuf buf) {
        HaloNetwork.writeSettings(buf, msg.settings);
    }

    public static ConfigC2S decode(FriendlyByteBuf buf) {
        return new ConfigC2S(HaloNetwork.readSettings(buf));
    }

    public static void handle(ConfigC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                HaloManager.set(sender, msg.settings);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
