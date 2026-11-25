package com.dmzconfighalo.client;

import com.dmzconfighalo.HaloSettings;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Moves / rotates DMZ's "halo" bone for one frame, and puts it back afterwards.
 *
 * Rotation note: the halo bone's pivot (y=24) is NOT at the ring (y=36.5), so a plain bone rotation would swing the
 * ring around the neck. GeckoLib draws a bone as  T(pos) * T(pivot) * R * T(-pivot)  with R = Rz * Ry * Rx.
 * To rotate around the ring's own center instead, the bone position is shifted by (R_old*r - R_new*r), where
 * r is the vector from the pivot to the ring center. For a single axis the order of Rz/Ry/Rx does not matter.
 */
public final class HaloBoneTweaks {

    /** Y (in model pixels) of the center of DMZ's halo ring (cubes at y=36..37, x/z centered). */
    private static final float RING_CENTER_Y = 36.5F;

    private HaloBoneTweaks() {
    }

    /** @return the original bone values, to give to {@link #restore}. */
    public static float[] apply(GeoBone bone, HaloSettings settings, float extraHeight) {
        float[] original = {
                bone.getPosX(), bone.getPosY(), bone.getPosZ(),
                bone.getRotX(), bone.getRotY(), bone.getRotZ()
        };

        float posX = original[0];
        float posY = original[1] + extraHeight;
        float posZ = original[2];

        if (settings.hasRotation()) {
            float rotX = original[3] + (float) Math.toRadians(settings.rotX());
            float rotY = original[4] + (float) Math.toRadians(settings.rotY());
            float rotZ = original[5] + (float) Math.toRadians(settings.rotZ());

            Vector3f r = new Vector3f(-bone.getPivotX(), RING_CENTER_Y - bone.getPivotY(), -bone.getPivotZ());
            Vector3f before = new Quaternionf().rotationZYX(original[5], original[4], original[3]).transform(new Vector3f(r));
            Vector3f after = new Quaternionf().rotationZYX(rotZ, rotY, rotX).transform(new Vector3f(r));

            // GeckoLib translates by (-posX, posY, posZ) / 16, so X is mirrored.
            posX -= before.x - after.x;
            posY += before.y - after.y;
            posZ += before.z - after.z;

            bone.setRotX(rotX);
            bone.setRotY(rotY);
            bone.setRotZ(rotZ);
        }

        bone.setPosX(posX);
        bone.setPosY(posY);
        bone.setPosZ(posZ);
        return original;
    }

    public static void restore(GeoBone bone, float[] original) {
        bone.setPosX(original[0]);
        bone.setPosY(original[1]);
        bone.setPosZ(original[2]);
        bone.setRotX(original[3]);
        bone.setRotY(original[4]);
        bone.setRotZ(original[5]);
    }
}
