package com.dmzconfighalo;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Halo settings of ONE player.
 *
 * @param height      offset in pixels (1 pixel = 1/16 block). 0 = original DMZ position, positive = up, negative = down.
 * @param color       RGB color (0xRRGGBB), or -1 to use the original DMZ color.
 * @param formHeights extra height in pixels per transformation name (lowercase, e.g. "supersaiyan" -> 2.5).
 *                    Added on top of {@code height} while the player is in that form (blended during transitions).
 */
public record HaloSettings(float height, int color, Map<String, Float> formHeights) {

    public static final HaloSettings DEFAULT = new HaloSettings(0.0F, -1, Map.of());
    public static final float MAX_HEIGHT = 64.0F;
    public static final int MAX_FORMS = 64;

    private static final Pattern HEX = Pattern.compile("^(?:#|0[xX])?([0-9a-fA-F]{6})$");
    private static final Pattern FORM_KEY = Pattern.compile("[a-z0-9_.\\-]{1,48}");

    public HaloSettings {
        height = clampHeight(height);
        color = sanitizeColor(color);
        formHeights = sanitizeForms(formHeights);
    }

    public HaloSettings(float height, int color) {
        this(height, color, Map.of());
    }

    public boolean isDefault() {
        return height == 0.0F && color < 0 && formHeights.isEmpty();
    }

    public HaloSettings withHeight(float newHeight) {
        return new HaloSettings(newHeight, color, formHeights);
    }

    public HaloSettings withColor(int newColor) {
        return new HaloSettings(height, newColor, formHeights);
    }

    /** Sets (or removes, when 0) the extra height of one form. {@code key} must already be normalized. */
    public HaloSettings withFormHeight(String key, float value) {
        TreeMap<String, Float> copy = new TreeMap<>(formHeights);
        if (value == 0.0F) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }
        return new HaloSettings(height, color, copy);
    }

    public HaloSettings withoutFormHeight(String key) {
        return withFormHeight(key, 0.0F);
    }

    public HaloSettings withoutFormHeights() {
        return new HaloSettings(height, color, Map.of());
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

    /** Lowercases/trims a form name and validates it. @return the key, or null if invalid. */
    public static String normalizeFormKey(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return FORM_KEY.matcher(key).matches() ? key : null;
    }

    private static Map<String, Float> sanitizeForms(Map<String, Float> in) {
        if (in == null || in.isEmpty()) {
            return Map.of();
        }
        TreeMap<String, Float> out = new TreeMap<>();
        for (Map.Entry<String, Float> e : in.entrySet()) {
            if (out.size() >= MAX_FORMS) {
                break;
            }
            String key = normalizeFormKey(e.getKey());
            if (key == null || e.getValue() == null) {
                continue;
            }
            float value = clampHeight(e.getValue());
            if (value != 0.0F) {
                out.put(key, value);
            }
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
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
