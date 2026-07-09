package com.ck7infinite.dynamicres.mixin;

import com.ck7infinite.dynamicres.RenderScaleManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * renderLevel(float, long, PoseStack) dibuja SOLO el mundo 3D (terreno, entidades, clima), sin
 * tocar el HUD, que se dibuja despues por separado en GameRenderer#render(). Por eso es el punto
 * exacto donde envolver "dibujar a una resolucion mas chica y despues estirar": todo lo que pasa
 * dentro de este metodo cae en nuestro render target reducido, y el HUD queda afuera del cambio
 * porque se sigue dibujando directo sobre mainRenderTarget a resolucion nativa.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(
            method = "renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("HEAD")
    )
    private void ck7infinite$beginWorldPass(float partialTick, long finishNanoTime, PoseStack poseStack, CallbackInfo ci) {
        RenderScaleManager.beginWorldPass();
    }

    @Inject(
            method = "renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("TAIL")
    )
    private void ck7infinite$endWorldPass(float partialTick, long finishNanoTime, PoseStack poseStack, CallbackInfo ci) {
        RenderScaleManager.endWorldPass();
    }
}
