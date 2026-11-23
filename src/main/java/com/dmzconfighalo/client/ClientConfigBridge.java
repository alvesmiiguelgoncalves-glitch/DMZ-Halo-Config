package com.dmzconfighalo.client;

import com.dmzconfighalo.DMZConfigHalo;
import com.dmzconfighalo.HaloConfig;
import com.dmzconfighalo.HaloSettings;
import com.dmzconfighalo.network.ConfigC2S;
import com.dmzconfighalo.network.HaloNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/** Keeps the config file (dmzconfighalo-client.toml) and the server in sync. */
public final class ClientConfigBridge {

    /** Last settings the server told us about for ourselves. Used to avoid sync loops. */
    private static volatile HaloSettings lastServerKnown = null;

    private ClientConfigBridge() {
    }

    public static HaloSettings readFile() {
        float height = (float) HaloConfig.HEIGHT.get().doubleValue();
        String color = HaloConfig.COLOR.get().trim();
        int rgb = -1;
        if (!color.equalsIgnoreCase("reset")) {
            OptionalInt parsed = HaloSettings.parseHex(color);
            if (parsed.isPresent()) {
                rgb = parsed.getAsInt();
            }
        }
        Map<String, Float> forms = new HashMap<>();
        for (String line : HaloConfig.FORM_HEIGHTS.get()) {
            Object[] entry = HaloConfig.parseEntry(line);
            if (entry != null) {
                forms.put((String) entry[0], (Float) entry[1]);
            }
        }
        return new HaloSettings(height, rgb, forms);
    }

    private static List<String> formLines(HaloSettings settings) {
        List<String> lines = new ArrayList<>();
        settings.formHeights().forEach((name, value) -> lines.add(name + "=" + Float.toString(value)));
        return lines;
    }

    /** The server changed our settings (command, or applying our file): mirror them in the file. */
    static void onServerSync(HaloSettings settings) {
        lastServerKnown = settings;
        if (!HaloConfig.SPEC.isLoaded()) {
            return;
        }
        try {
            // Double.parseDouble(Float.toString(..)) keeps "0.2" instead of "0.20000000298023224"
            double height = Double.parseDouble(Float.toString(settings.height()));
            String color = HaloSettings.formatColor(settings.color());
            List<String> forms = formLines(settings);
            boolean changed = false;
            if (Double.compare(HaloConfig.HEIGHT.get(), height) != 0) {
                HaloConfig.HEIGHT.set(height);
                changed = true;
            }
            if (!color.equalsIgnoreCase(HaloConfig.COLOR.get().trim())) {
                HaloConfig.COLOR.set(color);
                changed = true;
            }
            if (!forms.equals(new ArrayList<>(HaloConfig.FORM_HEIGHTS.get()))) {
                HaloConfig.FORM_HEIGHTS.set(forms);
                changed = true;
            }
            if (changed) {
                HaloConfig.SPEC.save();
            }
        } catch (RuntimeException e) {
            DMZConfigHalo.LOGGER.warn("[DMZ Config Halo] Could not update the config file", e);
        }
    }

    /** Joined a world: send the file values (only if customized, so we never wipe a value stored on the server). */
    static void onLogin() {
        if (!HaloConfig.SPEC.isLoaded() || !HaloConfig.APPLY_ON_JOIN.get()) {
            return;
        }
        HaloSettings fromFile = readFile();
        if (!fromFile.isDefault()) {
            send(fromFile);
        }
    }

    /** The user edited the file while the game was running. */
    static void onFileReloaded() {
        if (!HaloConfig.SPEC.isLoaded()) {
            return;
        }
        HaloSettings fromFile = readFile();
        // Also fires after our own writes; those match what the server already knows, so nothing is sent.
        if (lastServerKnown != null && fromFile.equals(lastServerKnown)) {
            return;
        }
        send(fromFile);
    }

    static void onLogout() {
        lastServerKnown = null;
    }

    private static void send(HaloSettings settings) {
        Minecraft.getInstance().execute(() -> {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection == null || !HaloNetwork.CHANNEL.isRemotePresent(connection.getConnection())) {
                return; // not in a world, or the server does not have this addon
            }
            HaloNetwork.CHANNEL.sendToServer(new ConfigC2S(settings));
        });
    }
}
