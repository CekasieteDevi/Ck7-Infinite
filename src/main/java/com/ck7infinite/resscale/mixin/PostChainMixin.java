package com.ck7infinite.resscale.mixin;

import com.ck7infinite.resscale.ResScaleManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fix del glowing outline (spectral arrow, mob con Glowing) portado de RenderScale: el post-chain
 * "minecraft:shaders/post/entity_outline.json" tiene sus propios render targets, que NO pasan por
 * nuestro swap de mainRenderTarget -sin este fix, el outline se dibuja a resolucion nativa sobre
 * un mundo dibujado a resolucion reducida y queda desalineado/con offset. Se re-escala su resize()
 * y su matriz ortografica por el inverso de nuestra escala, y SOLO para este post-chain especifico
 * (identificado por nombre), no para otros que puedan estar activos.
 */
@Mixin(PostChain.class)
public abstract class PostChainMixin {

    @Shadow
    private Matrix4f shaderOrthoMatrix;

    @Shadow
    @Final
    private String name;

    private double ck7infinite$inverseScale() {
        if (!name.equals("minecraft:shaders/post/entity_outline.json")) {
            return 1.0;
        }
        return 1.0 / ResScaleManager.appliedScale();
    }

    @Redirect(
            method = "resize",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;resize(IIZ)V")
    )
    private void ck7infinite$resize(RenderTarget instance, int width, int height, boolean clearError) {
        if (!name.equals("minecraft:shaders/post/entity_outline.json")) {
            instance.resize(width, height, clearError);
            return;
        }
        // scaleDim (truncacion), no la division/redondeo de antes -tiene que dar EXACTAMENTE el
        // mismo numero que ResScaleManager#syncOutlineChain para el mismo ancho/alto nativo, si
        // no el chequeo de sync ahi oscila y fuerza un resize todos los frames con dims impares.
        double scale = ResScaleManager.appliedScale();
        instance.resize(ResScaleManager.scaleDim(width, scale), ResScaleManager.scaleDim(height, scale), clearError);
    }

    @Inject(method = "updateOrthoMatrix", at = @At("TAIL"))
    private void ck7infinite$onUpdateOrthoMatrix(CallbackInfo ci) {
        double inv = ck7infinite$inverseScale();
        this.shaderOrthoMatrix = this.shaderOrthoMatrix.scale((float) inv, (float) inv, 1.0F);
    }
}
