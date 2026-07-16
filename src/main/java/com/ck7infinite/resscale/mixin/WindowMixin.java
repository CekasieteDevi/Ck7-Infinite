package com.ck7infinite.resscale.mixin;

import com.mojang.blaze3d.platform.Window;
import com.ck7infinite.resscale.ResScaleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * La pieza que "Resolucion de Render Dinamica" (v1/v2, eliminada en v2.2.0) nunca tuvo: mientras
 * dura el world pass, Window#getWidth()/getHeight() devuelven las dimensiones ESCALADAS en vez de
 * las nativas. Sin esto, cualquier codigo que lea el tamaño de ventana a mitad del pass (Embeddium/
 * Sodium, fog, proyeccion, particulas) ve un numero que no coincide con el framebuffer realmente
 * bindeado -exactamente el tipo de inconsistencia que se sospecha detras del bug de agua/entidades
 * desalineadas con Embeddium documentado en la memoria del proyecto (ver investigacion del mod
 * "RenderScale" de Zolo101, que tiene esta misma pieza y no tiene ese bug con escala fija).
 * <p>
 * A proposito NO tocamos getGuiScale() aca (a diferencia de RenderScale): en esta arquitectura el
 * HUD/GUI se dibuja DESPUES de restaurar el render target real (ver ResScaleManager#endWorldPass),
 * siempre a resolucion nativa, asi que no hay ningun consumidor de guiScale durante el world pass
 * que necesite verlo escalado.
 */
@Mixin(Window.class)
public abstract class WindowMixin {

    @Inject(method = "getWidth", at = @At("RETURN"), cancellable = true)
    private void ck7infinite$scaleWidth(CallbackInfoReturnable<Integer> cir) {
        if (ResScaleManager.isWorldPassActive()) {
            cir.setReturnValue(ck7infinite$scale(cir.getReturnValueI()));
        }
    }

    @Inject(method = "getHeight", at = @At("RETURN"), cancellable = true)
    private void ck7infinite$scaleHeight(CallbackInfoReturnable<Integer> cir) {
        if (ResScaleManager.isWorldPassActive()) {
            cir.setReturnValue(ck7infinite$scale(cir.getReturnValueI()));
        }
    }

    private static int ck7infinite$scale(int value) {
        return ResScaleManager.scaleDim(value, ResScaleManager.appliedScale());
    }
}
