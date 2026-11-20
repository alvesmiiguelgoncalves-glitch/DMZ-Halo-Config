package com.dmzconfighalo;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Halo settings of ONE player.
 *
 * @param height offset in pixels (1 pixel = 1/16 block). 0 = original DMZ position, positive = up, negative = down.
 * @param color  RGB color (0xRRGGBB), or -1 to use the original DMZ color.
 */
public record HaloSettings(float height, int color) {

    public static final HaloSettings DEFAULT = new HaloSettings(0.0F, -1);
    public static final float MAX_HEIGHT = 64.0F;

    private static final Pattern HEX = Pattern.compile("^(?:#|0[xX])?([0-9a-fA-F]{6})$");

    public HaloSettings {
        height = clampHeight(height);
        color = sanitizeColor(color);
    }

    public boolean isDefault() {
        return height == 0.0F && color < 0;
    }

    public HaloSettings withHeight(float newHeight) {
        return new HaloSettings(newHeight, color);
    }

    public HaloSettings withColor(int newColor) {
        return new HaloSettings(height, newColor);
    }

    public static float clampHeight(float h) {
        if (Float.isNaN(h) || Float.isInfinite(h)) {
            return 0.0F;
        }
        return Math.max(-MAX_HEIGHT, Math.min(MAX_HEIGHT, h));
    }

    public static int sanitizeColor(int c) {
        return (c >= 0 && c <= 0xFFFFFF) ? c : -1;
    }

    /** Accepts "FFD700", "#FFD700" and "0xFFD700". */
    public static OptionalInt parseHex(String raw) {
        if (raw == null) {
            return OptionalInt.empty();
        }
        Matcher m = HEX.matcher(raw.trim());
        if (!m.matches()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(m.group(1), 16));
    }

    public static String formatColor(int color) {
        return color < 0 ? "reset" : String.format(Locale.ROOT, "#%06X", color);
    }

    public static String formatHeight(float h) {
        // 0.2F -> "0.2" (instead of 0.20000000298023224)
        return Float.toString(h);
    }
}
