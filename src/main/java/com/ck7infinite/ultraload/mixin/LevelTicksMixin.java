package com.ck7infinite.ultraload.mixin;

import com.ck7infinite.ultraload.UltraLoadConfig;
import com.ck7infinite.ultraload.UltraLoadState;
import net.minecraft.world.ticks.LevelTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tier 3 de Ultra Carga: pausa los ticks PROGRAMADOS (redstone, flujo de agua/lava) mientras Ultra
 * Carga esta activo, salteando LevelTicks#tick. Distinto del random tick (crecimiento de pasto/
 * cultivos, gamerule randomTickSpeed): este es el sistema de ScheduledTick que usan repetidores,
 * pistones y fluidos. ServerLevel mantiene una instancia de LevelTicks para bloques y otra para
 * fluidos; ambas pasan por este mismo metodo, asi que un solo mixin cubre las dos.
 * <p>
 * Nada se pierde al saltear: los ticks pendientes (collectTicks/nextTickForContainer) quedan en
 * su cola por-chunk sin tocar, y se procesan apenas se deja de cancelar -mismo mecanismo seguro
 * que randomTickSpeed=0. El costo real es visible mientras dura la rafaga: relojes de redstone y
 * pistones se detienen, el agua/lava no fluye.
 */
@Mixin(LevelTicks.class)
public abstract class LevelTicksMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ck7infinite$pauseDuringUltraLoad(CallbackInfo ci) {
        if (UltraLoadConfig.isEnabled() && UltraLoadConfig.pauseScheduledTicks() && UltraLoadState.isActive()) {
            ci.cancel();
        }
    }
}
