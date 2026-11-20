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

    private final float height;
    private final int color;

    public ConfigC2S(float height, int color) {
        this.height = height;
        this.color = color;
    }

    public static void encode(ConfigC2S msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.height);
        buf.writeInt(msg.color);
    }

    public static ConfigC2S decode(FriendlyByteBuf buf) {
        return new ConfigC2S(buf.readFloat(), buf.readInt());
    }

    public static void handle(ConfigC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                // HaloSettings sanitizes (clamps height, validates color) in its constructor.
                HaloManager.set(sender, new HaloSettings(msg.height, msg.color));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
