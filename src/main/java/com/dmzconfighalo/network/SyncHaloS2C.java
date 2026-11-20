package com.dmzconfighalo.network;

import com.dmzconfighalo.HaloSettings;
import com.dmzconfighalo.client.ClientHaloData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Server -> client: the halo settings of the player {@code playerId}. */
public class SyncHaloS2C {

    private final UUID playerId;
    private final float height;
    private final int color;

    public SyncHaloS2C(UUID playerId, float height, int color) {
        this.playerId = playerId;
        this.height = height;
        this.color = color;
    }

    public static void encode(SyncHaloS2C msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeFloat(msg.height);
        buf.writeInt(msg.color);
    }

    public static SyncHaloS2C decode(FriendlyByteBuf buf) {
        return new SyncHaloS2C(buf.readUUID(), buf.readFloat(), buf.readInt());
    }

    public static void handle(SyncHaloS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHaloData.onSync(msg.playerId, new HaloSettings(msg.height, msg.color))));
        ctx.get().setPacketHandled(true);
    }
}
