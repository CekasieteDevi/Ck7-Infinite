package com.ck7infinite.ultraload.mixin;

import com.ck7infinite.ultraload.UltraLoadConfig;
import com.ck7infinite.ultraload.UltraLoadState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tier 2 de Ultra Carga: pausa el tick de todos los block entities del servidor (hoppers, hornos,
 * spawners, etc.) mientras Ultra Carga esta activo, salteando Level#tickBlockEntities. Solo del
 * lado servidor (el tick de block entities del cliente son animaciones baratas). Al terminar,
 * retoman solos. Durante los ~2s de una rafaga de carga es inofensivo (un hopper que pausa un
 * instante), y libera la CPU de todos ellos.
 */
@Mixin(Level.class)
public abstract class LevelBlockEntityMixin {

    @Shadow
    @Final
    public boolean isClientSide;

    @Inject(method = "tickBlockEntities", at = @At("HEAD"), cancellable = true)
    private void ck7infinite$freezeBlockEntitiesDuringUltraLoad(CallbackInfo ci) {
        if (!this.isClientSide
                && UltraLoadConfig.isEnabled()
                && UltraLoadConfig.freezeBlockEntities()
                && UltraLoadState.isActive()) {
            ci.cancel();
        }
    }
}
