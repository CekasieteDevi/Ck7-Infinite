package com.ck7infinite.resscale.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Minecraft#mainRenderTarget no tiene setter vanilla. Lo necesitamos porque codigo interno de
 * LevelRenderer (compositing de Fabulous graphics: translucent/weather/particles/clouds/item
 * entity targets) referencia Minecraft#getMainRenderTarget() directamente en varios puntos
 * durante el render del mundo, no "lo que este bindeado en ese momento". Si solo bindeamos
 * nuestro propio render target mas chico sin que este accessor tambien apunte ahi, esos pasos
 * de compositing terminan escribiendo en el mainRenderTarget real (tamaño completo) mientras el
 * resto del mundo se dibuja en el nuestro (mas chico) -> el resultado final (nuestro blit) pisa
 * ese contenido y esos elementos "desaparecen". Fabuloso igual se bypassea por config (ver
 * ResScaleConfig#bypassWithShaderpack), pero el accessor en si es inofensivo tenerlo siempre.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessorMixin {

    @Accessor("mainRenderTarget")
    @Mutable
    void ck7infinite$setMainRenderTarget(RenderTarget target);
}
