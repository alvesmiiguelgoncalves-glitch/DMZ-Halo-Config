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

    public static HaloSettings get(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(PERSISTED_TAG);
        if (!persisted.contains(KEY, Tag.TAG_COMPOUND)) {
            return HaloSettings.DEFAULT;
        }
        CompoundTag tag = persisted.getCompound(KEY);
        Map<String, Float> forms = new HashMap<>();
        CompoundTag formsTag = tag.getCompound("forms");
        for (String name : formsTag.getAllKeys()) {
            forms.put(name, formsTag.getFloat(name));
        }
        return new HaloSettings(tag.getFloat("height"), tag.contains("color") ? tag.getInt("color") : -1, forms);
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
            if (!settings.formHeights().isEmpty()) {
                CompoundTag formsTag = new CompoundTag();
                settings.formHeights().forEach(formsTag::putFloat);
                tag.put("forms", formsTag);
            }
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
