package com.dmzconfighalo;

import com.dmzconfighalo.network.HaloNetwork;
import com.dmzconfighalo.network.SyncHaloS2C;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

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
        return new HaloSettings(tag.getFloat("height"), tag.contains("color") ? tag.getInt("color") : -1);
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
            persisted.put(KEY, tag);
        }
        root.put(PERSISTED_TAG, persisted);

        syncToTrackingAndSelf(player);
        return true;
    }

    public static void syncToTrackingAndSelf(ServerPlayer owner) {
        HaloSettings s = get(owner);
        HaloNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> owner),
                new SyncHaloS2C(owner.getUUID(), s.height(), s.color()));
    }

    /** Sends the settings of {@code owner} to a single {@code viewer}. */
    public static void sendTo(ServerPlayer viewer, ServerPlayer owner) {
        HaloSettings s = get(owner);
        HaloNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new SyncHaloS2C(owner.getUUID(), s.height(), s.color()));
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
