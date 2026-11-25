package com.dmzconfighalo.compat;

import com.dmzconfighalo.DMZConfigHalo;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only bridge to DragonMine Z's player stats (via reflection, so this addon does not need the DMZ jar to
 * compile). Used to know the player's active forms / hair type and to follow DMZ's own form-transition blending
 * (hold-G charge, instant transformation and revert), so the halo height moves together with the body.
 *
 * If anything in DMZ changes and a lookup fails, the feature switches itself off (halo keeps working
 * with the base height only) and a single warning is logged.
 */
public final class DmzStats {

    private static final String P = "com.dragonminez.common.";

    private static volatile int state = 0; // 0 = not resolved yet, 1 = ok, -1 = unavailable

    private static Object capability;
    private static Method providerGet;
    private static Method getCharacter;
    private static Method getFormTransition;
    private static Method getActiveForm;
    private static Method getActiveStackForm;
    private static Method ftFrameTime;
    private static Method ftCharge;
    private static Method ftRevert;
    private static Method lookCurrent;
    private static Method lookForm;
    private static Method lookStack;
    private static Method lookHasForm;
    private static Method lookHasStack;
    private static Method lookFormData;
    private static Method lookStackData;
    private static Method fdHasDefinedHairType;
    private static Method fdGetHairType;
    private static Method fdHasHairCodeOverride;
    private static Method chargeLook;
    private static Method chargeFactor;
    private static Method revertLook;
    private static Method revertFactor;

    private DmzStats() {
    }

    private static boolean ready() {
        if (state == 0) {
            resolve();
        }
        return state == 1;
    }

    private static synchronized void resolve() {
        if (state != 0) {
            return;
        }
        try {
            ClassLoader cl = DmzStats.class.getClassLoader();
            Class<?> capCls = Class.forName(P + "stats.StatsCapability", true, cl);
            capability = capCls.getField("INSTANCE").get(null);
            Class<?> provCls = Class.forName(P + "stats.StatsProvider", true, cl);
            providerGet = provCls.getMethod("get", Capability.class, Entity.class);

            Class<?> statsCls = Class.forName(P + "stats.StatsData", true, cl);
            Class<?> charCls = Class.forName(P + "stats.character.Character", true, cl);
            getCharacter = statsCls.getMethod("getCharacter");
            getFormTransition = statsCls.getMethod("getFormTransition");
            getActiveForm = charCls.getMethod("getActiveForm");
            getActiveStackForm = charCls.getMethod("getActiveStackForm");

            Class<?> ftCls = Class.forName(P + "stats.FormTransition", true, cl);
            ftFrameTime = ftCls.getMethod("frameTime", float.class);
            ftCharge = ftCls.getMethod("charge", charCls, double.class);
            ftRevert = ftCls.getMethod("revert", double.class);

            Class<?> lookCls = Class.forName(P + "stats.FormTransition$Look", true, cl);
            lookCurrent = lookCls.getMethod("current", charCls);
            lookForm = lookCls.getMethod("form");
            lookStack = lookCls.getMethod("stack");
            lookHasForm = lookCls.getMethod("hasForm");
            lookHasStack = lookCls.getMethod("hasStack");
            lookFormData = lookCls.getMethod("formData");
            lookStackData = lookCls.getMethod("stackData");

            Class<?> fdCls = Class.forName(P + "config.FormConfig$FormData", true, cl);
            fdHasDefinedHairType = fdCls.getMethod("hasDefinedHairType");
            fdGetHairType = fdCls.getMethod("getHairType");
            fdHasHairCodeOverride = fdCls.getMethod("hasHairCodeOverride");

            Class<?> chargeCls = Class.forName(P + "stats.FormTransition$Charge", true, cl);
            chargeLook = chargeCls.getMethod("look");
            chargeFactor = chargeCls.getMethod("factor");

            Class<?> revertCls = Class.forName(P + "stats.FormTransition$Revert", true, cl);
            revertLook = revertCls.getMethod("look");
            revertFactor = revertCls.getMethod("factor");

            state = 1;
        } catch (Throwable t) {
            state = -1;
            DMZConfigHalo.LOGGER.warn("[DMZ Config Halo] Could not hook DMZ form data; per-form and per-hair halo heights are disabled.", t);
        }
    }

    private static void fail(Throwable t) {
        if (state != -1) {
            state = -1;
            DMZConfigHalo.LOGGER.warn("[DMZ Config Halo] DMZ form lookup failed; per-form and per-hair halo heights are disabled.", t);
        }
    }

    private static Object stats(Entity entity) throws ReflectiveOperationException {
        Object lazy = providerGet.invoke(null, capability, entity);
        if (lazy instanceof LazyOptional<?> optional) {
            return optional.resolve().orElse(null);
        }
        return null;
    }

    private static String clean(Object value) {
        if (!(value instanceof String s)) {
            return null;
        }
        s = s.trim().toLowerCase(Locale.ROOT);
        return s.isEmpty() ? null : s;
    }

    /** Lowercase name of the active main form (e.g. "supersaiyan"), or null. Works on server and client. */
    public static String activeFormName(Entity entity) {
        if (!ready()) {
            return null;
        }
        try {
            Object stats = stats(entity);
            return stats == null ? null : clean(getActiveForm.invoke(getCharacter.invoke(stats)));
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    /** Lowercase name of the active stack form (e.g. "kaioken"), or null. */
    public static String activeStackFormName(Entity entity) {
        if (!ready()) {
            return null;
        }
        try {
            Object stats = stats(entity);
            return stats == null ? null : clean(getActiveStackForm.invoke(getCharacter.invoke(stats)));
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    /** Hair type the player has RIGHT NOW ("base", "ssj", "ssj2", "ssj3", "ssj4", "custom", "none"), or null. */
    public static String currentHairKey(Entity entity) {
        if (!ready()) {
            return null;
        }
        try {
            Object stats = stats(entity);
            if (stats == null) {
                return null;
            }
            return hairKey(lookCurrent.invoke(null, getCharacter.invoke(stats)));
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    /**
     * Extra halo height (in pixels) coming from the player's transformation and hair type RIGHT NOW
     * (client, per frame). Mirrors what DMZ does for the model scale: start from the current look, and blend
     * towards the form being charged (holding G), or from the previous look while a change/revert settles.
     */
    public static float haloHeightOffset(Entity entity, Map<String, Float> forms, Map<String, Float> hairs,
                                         float partialTick) {
        if ((forms.isEmpty() && hairs.isEmpty()) || !ready()) {
            return 0.0F;
        }
        try {
            Object stats = stats(entity);
            if (stats == null) {
                return 0.0F;
            }
            Object character = getCharacter.invoke(stats);
            Object transition = getFormTransition.invoke(stats);
            double time = (double) ftFrameTime.invoke(transition, partialTick);

            float result = heightOf(lookCurrent.invoke(null, character), forms, hairs);

            Object charge = ftCharge.invoke(transition, character, time);
            if (charge != null) {
                float target = heightOf(chargeLook.invoke(charge), forms, hairs);
                return lerp(result, target, (float) chargeFactor.invoke(charge));
            }
            Object revert = ftRevert.invoke(transition, time);
            if (revert != null) {
                float from = heightOf(revertLook.invoke(revert), forms, hairs);
                return lerp(result, from, (float) revertFactor.invoke(revert));
            }
            return result;
        } catch (Throwable t) {
            fail(t);
            return 0.0F;
        }
    }

    private static float heightOf(Object look, Map<String, Float> forms, Map<String, Float> hairs)
            throws ReflectiveOperationException {
        if (look == null) {
            return 0.0F;
        }
        float total = 0.0F;
        if (!forms.isEmpty()) {
            String form = clean(lookForm.invoke(look));
            if (form != null) {
                total += forms.getOrDefault(form, 0.0F);
            }
            String stack = clean(lookStack.invoke(look));
            if (stack != null) {
                total += forms.getOrDefault(stack, 0.0F);
            }
        }
        if (!hairs.isEmpty()) {
            total += hairs.getOrDefault(hairKey(look), 0.0F);
        }
        return total;
    }

    /**
     * Same rule DMZ uses to pick a hair for a look: start from the base hair, then the form can change it,
     * then the stack form can change it again. A defined hairType wins (an unknown/blank one means "no hair");
     * otherwise a forced custom hair code means "custom"; otherwise the hair is inherited.
     */
    private static String hairKey(Object look) throws ReflectiveOperationException {
        String key = "base";
        if (Boolean.TRUE.equals(lookHasForm.invoke(look))) {
            key = hairKeyOf(lookFormData.invoke(look), key);
        }
        if (Boolean.TRUE.equals(lookHasStack.invoke(look))) {
            key = hairKeyOf(lookStackData.invoke(look), key);
        }
        return key;
    }

    private static String hairKeyOf(Object formData, String inherited) throws ReflectiveOperationException {
        if (formData == null) {
            return inherited;
        }
        if (Boolean.TRUE.equals(fdHasDefinedHairType.invoke(formData))) {
            String type = clean(fdGetHairType.invoke(formData));
            return type == null ? "none" : type;
        }
        if (Boolean.TRUE.equals(fdHasHairCodeOverride.invoke(formData))) {
            return "custom";
        }
        return inherited;
    }

    private static float lerp(float a, float b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return a + (b - a) * t;
    }
}
