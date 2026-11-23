package com.dmzconfighalo.network;

import com.dmzconfighalo.DMZConfigHalo;
import com.dmzconfighalo.HaloSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class HaloNetwork {

    private static final String PROTOCOL = "2";

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

    static void writeSettings(FriendlyByteBuf buf, HaloSettings s) {
        buf.writeFloat(s.height());
        buf.writeInt(s.color());
        buf.writeVarInt(s.formHeights().size());
        s.formHeights().forEach((name, value) -> {
            buf.writeUtf(name, 64);
            buf.writeFloat(value);
        });
    }

    /** HaloSettings sanitizes everything (clamps, validates names, caps the entry count). */
    static HaloSettings readSettings(FriendlyByteBuf buf) {
        float height = buf.readFloat();
        int color = buf.readInt();
        int count = buf.readVarInt();
        Map<String, Float> forms = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf(64);
            float value = buf.readFloat();
            if (i < HaloSettings.MAX_FORMS) {
                forms.put(name, value);
            }
        }
        return new HaloSettings(height, color, forms);
    }
}
