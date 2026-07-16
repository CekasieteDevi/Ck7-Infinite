package com.ck7infinite.resscale.mixin;

import com.ck7infinite.resscale.ResScaleConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dos fixes portados de RenderScale (mod de Zolo101, ver investigacion en la memoria del proyecto)
 * que aplican al blit final de CUALQUIER RenderTarget, no solo el nuestro -por eso ambos se gatean
 * explicitamente en ResScaleConfig#isEnabled(), para que el modulo apagado sea 100% inerte, como
 * el resto de los modulos de este mod.
 * <p>
 * Prioridad alta (99999) a proposito: RenderScale la usa asi para asegurarse de aplicar despues de
 * cualquier mixin de Sodium/Embeddium sobre la misma clase.
 */
@Mixin(value = RenderTarget.class, priority = 99999)
public abstract class RenderTargetMixin {

    /**
     * Fuerza el filtro de textura elegido por config (lineal o nearest) en cualquier llamada a
     * setFilterMode, en vez del que pida el codigo original. Sin esto el filtro de upscale que el
     * usuario elige en la config puede perderse en cualquier resize real de ventana (RenderTarget
     * siempre recrea con GL_NEAREST internamente).
     */
    @Redirect(
            method = "setFilterMode",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texParameter(III)V")
    )
    private void ck7infinite$forceFilterMode(int target, int pname, int param) {
        if (!ResScaleConfig.isEnabled()) {
            GlStateManager._texParameter(target, pname, param);
            return;
        }
        GlStateManager._texParameter(target, pname, ResScaleConfig.linearFilter() ? GL11.GL_LINEAR : GL11.GL_NEAREST);
    }

    /**
     * "Sodium fix" (nombre original de RenderScale): blitToScreen(width, height, disableBlend)
     * recibe un booleano que, con Sodium/Embeddium instalado, rompe el estirado final si queda en
     * true. RenderScale lo fuerza a false siempre que su modulo esta activo; hacemos lo mismo, y
     * aplica igual para Embeddium (comparte el mismo blit path que Sodium, del que es un fork).
     */
    @ModifyVariable(method = "blitToScreen(IIZ)V", at = @At("HEAD"), index = 3)
    private boolean ck7infinite$disableBlendFalse(boolean disableBlend) {
        return ResScaleConfig.isEnabled() ? false : disableBlend;
    }
}
