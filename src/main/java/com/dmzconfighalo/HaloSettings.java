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
 * @param hairHeights extra height in pixels per hair type ("base", "ssj", "ssj2", "ssj3", "ssj4", "custom", "none").
 * @param rotX        extra rotation in degrees around the X axis (tilt forward/back).
 * @param rotY        extra rotation in degrees around the Y axis (spin, mostly invisible on a ring).
 * @param rotZ        extra rotation in degrees around the Z axis (tilt sideways).
 */
public record HaloSettings(float height, int color, Map<String, Float> formHeights, Map<String, Float> hairHeights,
                           float rotX, float rotY, float rotZ) {

    public static final HaloSettings DEFAULT = new HaloSettings(0.0F, -1, Map.of(), Map.of(), 0.0F, 0.0F, 0.0F);
    public static final float MAX_HEIGHT = 64.0F;
    public static final float MAX_ROTATION = 180.0F;
    /** Max entries in each of the form / hair tables. */
    public static final int MAX_FORMS = 64;

    private static final Pattern HEX = Pattern.compile("^(?:#|0[xX])?([0-9a-fA-F]{6})$");
    private static final Pattern KEY = Pattern.compile("[a-z0-9_.\\-]{1,48}");

    public HaloSettings {
        height = clampHeight(height);
        color = sanitizeColor(color);
        formHeights = sanitizeTable(formHeights);
        hairHeights = sanitizeTable(hairHeights);
        rotX = clampRotation(rotX);
        rotY = clampRotation(rotY);
        rotZ = clampRotation(rotZ);
    }

    public HaloSettings(float height, int color) {
        this(height, color, Map.of(), Map.of(), 0.0F, 0.0F, 0.0F);
    }

    public boolean isDefault() {
        return height == 0.0F && color < 0 && formHeights.isEmpty() && hairHeights.isEmpty() && !hasRotation();
    }

    public boolean hasRotation() {
        return rotX != 0.0F || rotY != 0.0F || rotZ != 0.0F;
    }

    public HaloSettings withHeight(float v) {
        return new HaloSettings(v, color, formHeights, hairHeights, rotX, rotY, rotZ);
    }

    public HaloSettings withColor(int c) {
        return new HaloSettings(height, c, formHeights, hairHeights, rotX, rotY, rotZ);
    }

    /** Sets (or removes, when 0) the extra height of one form. {@code key} must already be normalized. */
    public HaloSettings withFormHeight(String key, float value) {
        return new HaloSettings(height, color, putEntry(formHeights, key, value), hairHeights, rotX, rotY, rotZ);
    }

    public HaloSettings withHairHeight(String key, float value) {
        return new HaloSettings(height, color, formHeights, putEntry(hairHeights, key, value), rotX, rotY, rotZ);
    }

    public HaloSettings withoutFormHeights() {
        return new HaloSettings(height, color, Map.of(), hairHeights, rotX, rotY, rotZ);
    }

    public HaloSettings withoutHairHeights() {
        return new HaloSettings(height, color, formHeights, Map.of(), rotX, rotY, rotZ);
    }

    /** @param axis 'x', 'y' or 'z' */
    public HaloSettings withRotation(char axis, float degrees) {
        return switch (axis) {
            case 'x' -> new HaloSettings(height, color, formHeights, hairHeights, degrees, rotY, rotZ);
            case 'y' -> new HaloSettings(height, color, formHeights, hairHeights, rotX, degrees, rotZ);
            default -> new HaloSettings(height, color, formHeights, hairHeights, rotX, rotY, degrees);
        };
    }

    public HaloSettings withoutRotation() {
        return new HaloSettings(height, color, formHeights, hairHeights, 0.0F, 0.0F, 0.0F);
    }

    private static Map<String, Float> putEntry(Map<String, Float> table, String key, float value) {
        TreeMap<String, Float> copy = new TreeMap<>(table);
        if (value == 0.0F) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }
        return copy;
    }

    public static float clampHeight(float h) {
        if (Float.isNaN(h) || Float.isInfinite(h) || h == 0.0F) {
            return 0.0F;
        }
        return Math.max(-MAX_HEIGHT, Math.min(MAX_HEIGHT, h));
    }

    public static float clampRotation(float r) {
        if (Float.isNaN(r) || Float.isInfinite(r) || r == 0.0F) {
            return 0.0F;
        }
        return Math.max(-MAX_ROTATION, Math.min(MAX_ROTATION, r));
    }

    public static int sanitizeColor(int c) {
        return (c >= 0 && c <= 0xFFFFFF) ? c : -1;
    }

    /** Lowercases/trims a form or hair name and validates it. @return the key, or null if invalid. */
    public static String normalizeFormKey(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return KEY.matcher(key).matches() ? key : null;
    }

    private static Map<String, Float> sanitizeTable(Map<String, Float> in) {
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
