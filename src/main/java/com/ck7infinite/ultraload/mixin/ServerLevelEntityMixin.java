package com.ck7infinite.ultraload.mixin;

import com.ck7infinite.ultraload.UltraLoadConfig;
import com.ck7infinite.ultraload.UltraLoadState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tier 2 de Ultra Carga: congela por completo el tick de las entidades (no solo la IA) mientras
 * Ultra Carga esta activo, salteando ServerLevel#tickNonPassenger. Asi no corren movimiento,
 * fisica, colisiones ni base tick -toda la carga de EntityTickList (medida ~18-23% del tick) se
 * va durante la carga de chunks. Se comporta como el "pause" de singleplayer (entidades quietas).
 * <p>
 * NO congela al jugador ni a lo que el jugador este montando (sino te quedarias trabado sobre un
 * caballo/bote justo cuando Ultra Carga se activa, que es cuando estas explorando). Tampoco
 * congela entidades dentro de safeRadiusBlocks de un jugador, para evitar el "susto" de un
 * enemigo pegado a vos que se queda quieto de golpe y retoma recien al terminar la carga.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelEntityMixin {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void ck7infinite$freezeEntityDuringUltraLoad(Entity entity, CallbackInfo ci) {
        if ((entity instanceof Player)
                || entity.hasPassenger(passenger -> passenger instanceof Player)
                || !UltraLoadConfig.isEnabled()
                || !UltraLoadConfig.freezeEntities()
                || !UltraLoadState.isActive()) {
            return;
        }
        double safeRadius = UltraLoadConfig.safeRadiusBlocks();
        if (safeRadius > 0.0 && ((ServerLevel) entity.level()).getNearestPlayer(entity, safeRadius) != null) {
            return;
        }
        ci.cancel();
    }
}
