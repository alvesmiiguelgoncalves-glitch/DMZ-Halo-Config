package com.dmzconfighalo;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client config file (config/dmzconfighalo-client.toml).
 * It is sent to the server when you join a world, and it is updated automatically when you use /dmzhalo config.
 */
public final class HaloConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue HEIGHT;
    public static final ForgeConfigSpec.ConfigValue<String> COLOR;
    public static final ForgeConfigSpec.BooleanValue APPLY_ON_JOIN;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("halo");

        HEIGHT = b.comment(
                "Vertical offset of YOUR halo, in pixels (1 pixel = 1/16 block).",
                "0 = original DragonMine Z position, positive = up, negative = down.",
                "Decimals are allowed (0.2, -0.3, 1.5, -2.6...)."
        ).defineInRange("height", 0.0D, -HaloSettings.MAX_HEIGHT, HaloSettings.MAX_HEIGHT);

        COLOR = b.comment(
                "Color of YOUR halo as a hexadecimal code, e.g. \"#FFD700\" or \"FFD700\".",
                "Use \"reset\" to keep the original DragonMine Z color."
        ).define("color", "reset", o -> o instanceof String s
                && (s.trim().equalsIgnoreCase("reset") || HaloSettings.parseHex(s).isPresent()));

        APPLY_ON_JOIN = b.comment(
                "If true, the height/color above are sent to the server whenever you join a world",
                "(only when they are different from the default).",
                "Editing this file while the game is running also applies the changes."
        ).define("applyOnJoin", true);

        b.pop();
        SPEC = b.build();
    }

    private HaloConfig() {
    }
}
