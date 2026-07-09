package com.ck7infinite.villagerthrottle;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;

/**
 * Decide si un mob es un aldeano gestionable por este modulo. A diferencia de Mob AI Freeze,
 * no hace falta whitelist/blacklist por mod id: el modulo esta pensado especificamente para
 * Villager (y opcionalmente Wandering Trader), no para NPCs genericos de otros mods.
 */
final class VillagerEligibility {

    private VillagerEligibility() {
    }

    static boolean isEligible(Mob mob) {
        if (!VillagerThrottleConfig.isEnabled()) {
            return false;
        }
        if (mob instanceof Villager) {
            return true;
        }
        return VillagerThrottleConfig.includeWanderingTrader() && mob instanceof WanderingTrader;
    }
}
