package com.dmzconfighalo.network;

import com.dmzconfighalo.DMZConfigHalo;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class HaloNetwork {

    private static final String PROTOCOL = "1";

    // acceptMissingOr: players/servers without this addon can still connect.
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DMZConfigHalo.MOD_ID, "main"),
            () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL)
    );

    private HaloNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SyncHaloS2C.class, SyncHaloS2C::encode, SyncHaloS2C::decode,
                SyncHaloS2C::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, ConfigC2S.class, ConfigC2S::encode, ConfigC2S::decode,
                ConfigC2S::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
