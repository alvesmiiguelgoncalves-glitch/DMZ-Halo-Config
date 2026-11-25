package com.dmzconfighalo;

import com.dmzconfighalo.network.HaloNetwork;
import com.dmzconfighalo.network.SyncHaloS2C;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/**
 * Server side: stores each player's halo settings and syncs them to the clients.
 * Data lives in the player's persistent data (survives death, relog and restarts).
 */
public final class HaloManager {

    // Forge's "PlayerPersisted" tag is copied by Forge when the player respawns.
    private static final String PERSISTED_TAG = "PlayerPersisted";
    private static final String KEY = "dmzconfighalo";

    private HaloManager() {
    }

    private static Map<String, Float> readTable(CompoundTag parent, String name) {
        Map<String, Float> table = new HashMap<>();
        CompoundTag tag = parent.getCompound(name);
        for (String key : tag.getAllKeys()) {
            table.put(key, tag.getFloat(key));
        }
        return table;
    }

    private static void writeTable(CompoundTag parent, String name, Map<String, Float> table) {
        if (table.isEmpty()) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        table.forEach(tag::putFloat);
        parent.put(name, tag);
    }

    public static HaloSettings get(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(PERSISTED_TAG);
        if (!persisted.contains(KEY, Tag.TAG_COMPOUND)) {
            return HaloSettings.DEFAULT;
        }
        CompoundTag tag = persisted.getCompound(KEY);
        return new HaloSettings(
                tag.getFloat("height"),
                tag.contains("color") ? tag.getInt("color") : -1,
                readTable(tag, "forms"),
                readTable(tag, "hairs"),
                tag.getFloat("rotX"), tag.getFloat("rotY"), tag.getFloat("rotZ"));
    }

    /** @return true if something changed. */
    public static boolean set(ServerPlayer player, HaloSettings settings) {
        if (settings.equals(get(player))) {
            return false;
        }
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(PERSISTED_TAG);
        if (settings.isDefault()) {
            persisted.remove(KEY);
        } else {
            CompoundTag tag = new CompoundTag();
            tag.putFloat("height", settings.height());
            tag.putInt("color", settings.color());
            tag.putFloat("rotX", settings.rotX());
            tag.putFloat("rotY", settings.rotY());
            tag.putFloat("rotZ", settings.rotZ());
            writeTable(tag, "forms", settings.formHeights());
            writeTable(tag, "hairs", settings.hairHeights());
            persisted.put(KEY, tag);
        }
        root.put(PERSISTED_TAG, persisted);

        syncToTrackingAndSelf(player);
        return true;
    }

    public static void syncToTrackingAndSelf(ServerPlayer owner) {
        HaloNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> owner),
                new SyncHaloS2C(owner.getUUID(), get(owner)));
    }

    /** Sends the settings of {@code owner} to a single {@code viewer}. */
    public static void sendTo(ServerPlayer viewer, ServerPlayer owner) {
        HaloNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new SyncHaloS2C(owner.getUUID(), get(owner)));
    }

    public static void copyPersisted(ServerPlayer from, ServerPlayer to) {
        CompoundTag old = from.getPersistentData().getCompound(PERSISTED_TAG);
        if (!old.contains(KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag root = to.getPersistentData();
        CompoundTag persisted = root.getCompound(PERSISTED_TAG);
        persisted.put(KEY, old.getCompound(KEY).copy());
        root.put(PERSISTED_TAG, persisted);
    }
}
