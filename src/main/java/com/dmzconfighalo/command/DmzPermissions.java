package com.dmzconfighalo.command;

import com.dmzconfighalo.DMZConfigHalo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.server.permission.nodes.PermissionNode;

import java.lang.reflect.Method;

/**
 * Reuses DragonMine Z's own permission check for the halo on/off commands (so the behavior stays exactly
 * the same as in the original mod: OP, or a permission-API grant). Uses reflection so this addon does not
 * need the DMZ jar to compile. Falls back to "OP level 2" if DMZ changes.
 */
final class DmzPermissions {

    private static boolean resolved;
    private static Method check;
    private static Object haloSelf;
    private static Object haloOthers;

    private DmzPermissions() {
    }

    static boolean canToggleHalo(CommandSourceStack source) {
        resolve();
        if (check != null) {
            try {
                return (boolean) check.invoke(null, source, haloSelf, haloOthers);
            } catch (ReflectiveOperationException | RuntimeException e) {
                DMZConfigHalo.LOGGER.warn("[DMZ Config Halo] DMZ permission check failed, using OP level 2", e);
            }
        }
        return source.hasPermission(2);
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> c = Class.forName("com.dragonminez.server.commands.DMZPermissions", true,
                    DmzPermissions.class.getClassLoader());
            haloSelf = c.getField("HALO_SELF").get(null);
            haloOthers = c.getField("HALO_OTHERS").get(null);
            check = c.getMethod("check", CommandSourceStack.class, PermissionNode.class, PermissionNode.class);
        } catch (Throwable t) {
            DMZConfigHalo.LOGGER.warn("[DMZ Config Halo] Could not hook DMZPermissions, /dmzhalo on|off will require OP level 2", t);
            check = null;
        }
    }
}
