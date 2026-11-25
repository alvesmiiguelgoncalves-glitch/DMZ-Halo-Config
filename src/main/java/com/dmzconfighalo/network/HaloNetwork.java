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

    private static final String PROTOCOL = "3";

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

    private static void writeTable(FriendlyByteBuf buf, Map<String, Float> table) {
        buf.writeVarInt(table.size());
        table.forEach((name, value) -> {
            buf.writeUtf(name, 64);
            buf.writeFloat(value);
        });
    }

    private static Map<String, Float> readTable(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<String, Float> table = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf(64);
            float value = buf.readFloat();
            if (i < HaloSettings.MAX_FORMS) {
                table.put(name, value);
            }
        }
        return table;
    }

    static void writeSettings(FriendlyByteBuf buf, HaloSettings s) {
        buf.writeFloat(s.height());
        buf.writeInt(s.color());
        buf.writeFloat(s.rotX());
        buf.writeFloat(s.rotY());
        buf.writeFloat(s.rotZ());
        writeTable(buf, s.formHeights());
        writeTable(buf, s.hairHeights());
    }

    /** HaloSettings sanitizes everything (clamps, validates names, caps the entry count). */
    static HaloSettings readSettings(FriendlyByteBuf buf) {
        float height = buf.readFloat();
        int color = buf.readInt();
        float rotX = buf.readFloat();
        float rotY = buf.readFloat();
        float rotZ = buf.readFloat();
        Map<String, Float> forms = readTable(buf);
        Map<String, Float> hairs = readTable(buf);
        return new HaloSettings(height, color, forms, hairs, rotX, rotY, rotZ);
    }
}
