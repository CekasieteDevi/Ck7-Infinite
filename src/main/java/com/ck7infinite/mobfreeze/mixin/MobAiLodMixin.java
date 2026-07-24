package com.ck7infinite.mobfreeze.mixin;

import com.ck7infinite.mobfreeze.AiLodState;
import com.ck7infinite.mobfreeze.MobFreezeConfig;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Entity AI LOD. Decompilado por bytecode (javap -c sobre el jar mapeado de Forge):
 * {@code Mob#serverAiStep()} ya alterna, cada frame, entre reevaluar que goal deberia correr
 * (caro: {@code GoalSelector#tick()}) y solo continuar el goal ya activo (barato:
 * {@code GoalSelector#tickRunningGoals(false)}), decidido por
 * {@code (server.getTickCount() + getId()) % Mob.UPDATE_GOAL_SELECTOR_EVERY_N_TICKS(=2) != 0}
 * -escalonado por id de entity para no sincronizar todos los mobs en el mismo tick. Navigation,
 * moveControl, lookControl, jumpControl y customServerAiStep quedan FUERA de esa rama y corren
 * SIEMPRE, todos los ticks, sin excepcion (confirmado por el mismo bytecode) -por eso este cambio
 * no puede producir el pathing a tirones que preocupaba en el analisis original: nunca toca
 * movimiento/navegacion, solo la frecuencia de reevaluacion de goals.
 * <p>
 * Este mixin reemplaza ese divisor fijo (2) por uno mayor SOLO para mobs marcados por
 * MobFreezeTickHandler como en la banda "media" (activos, no congelados, fuera de
 * aiLodCloseRadiusBlocks) -mismo mecanismo de vanilla, mas agresivo para esos mobs en particular.
 * Con el modulo apagado o el mob no marcado, devuelve el 2 original: comportamiento identico a
 * vanilla, sin excepcion.
 */
@Mixin(Mob.class)
public abstract class MobAiLodMixin {

    @ModifyConstant(method = "serverAiStep", constant = @Constant(intValue = 2))
    private int ck7infinite$aiLodDivisor(int vanillaDivisor) {
        Mob self = (Mob) (Object) this;
        if (MobFreezeConfig.isEnabled() && AiLodState.isLodded(self)) {
            return MobFreezeConfig.aiLodDivisor();
        }
        return vanillaDivisor;
    }
}
