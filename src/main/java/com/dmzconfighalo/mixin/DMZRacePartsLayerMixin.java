package com.dmzconfighalo.mixin;

import com.dmzconfighalo.HaloSettings;
import com.dmzconfighalo.client.ClientHaloData;
import com.dmzconfighalo.client.HaloBoneTweaks;
import com.dmzconfighalo.client.HaloDebug;
import com.dmzconfighalo.compat.DmzStats;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Hooks DragonMine Z's DMZRacePartsLayer#renderTargetedBone (the method that draws the "halo" bone).
 *
 * When the bone being drawn is the halo and the player has custom settings, we draw it ourselves exactly like
 * DMZ does, but with the bone raised/lowered by the configured height and the configured color.
 * In every other case the original method runs untouched.
 *
 * The target is DMZ's own (non-obfuscated) method, so remap = false everywhere.
 */
@Mixin(targets = "com.dragonminez.client.render.layer.DMZRacePartsLayer", remap = false)
public abstract class DMZRacePartsLayerMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "renderTargetedBone", at = @At("HEAD"), cancellable = true, remap = false)
    private void dmzconfighalo$renderCustomHalo(GeoBone bone, PoseStack poseStack, MultiBufferSource buffer,
                                                AbstractClientPlayer player, RenderType renderType,
                                                float red, float green, float blue, float alpha,
                                                float partialTick, int packedLight, CallbackInfo ci) {
        if (!"halo".equals(bone.getName())) {
            return;
        }
        HaloSettings settings = ClientHaloData.get(player.getUUID());
        HaloDebug.haloBoneDrawn(player.getUUID(), settings);
        if (settings == null || settings.isDefault()) {
            return;
        }

        if (settings.color() >= 0) {
            int rgb = settings.color();
            red = ((rgb >> 16) & 0xFF) / 255.0F;
            green = ((rgb >> 8) & 0xFF) / 255.0F;
            blue = (rgb & 0xFF) / 255.0F;
        }

        // GeckoLib bone positions are in pixels (1/16 block); +Y = up. DMZ re-syncs this bone every frame,
        // and we restore the value anyway.
        // Base height + per-transformation / per-hair height (blended by DMZ's own form-transition factors).
        float extraHeight = settings.height() + DmzStats.haloHeightOffset(player, settings.formHeights(),
                settings.hairHeights(), partialTick);
        float[] original = HaloBoneTweaks.apply(bone, settings, extraHeight);
        try {
            VertexConsumer consumer = buffer.getBuffer(renderType);
            GeoRenderer renderer = ((GeoRenderLayer<?>) (Object) this).getRenderer();
            renderer.renderRecursively(poseStack, (GeoAnimatable) (Object) player, bone, renderType, buffer,
                    consumer, true, partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                    red, green, blue, alpha);
        } finally {
            HaloBoneTweaks.restore(bone, original);
        }
        ci.cancel();
    }
}
