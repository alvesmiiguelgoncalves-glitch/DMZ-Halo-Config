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
    private final HaloSettings settings;

    public SyncHaloS2C(UUID playerId, HaloSettings settings) {
        this.playerId = playerId;
        this.settings = settings;
    }

    public static void encode(SyncHaloS2C msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        HaloNetwork.writeSettings(buf, msg.settings);
    }

    public static SyncHaloS2C decode(FriendlyByteBuf buf) {
        return new SyncHaloS2C(buf.readUUID(), HaloNetwork.readSettings(buf));
    }

    public static void handle(SyncHaloS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHaloData.onSync(msg.playerId, msg.settings)));
        ctx.get().setPacketHandled(true);
    }
}
