package com.ck7infinite.villagerthrottle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantiene, por ServerLevel, el set de aldeanos elegibles para throttling. Se actualiza solo en
 * join/leave del entity (spawn, muerte, descarga de chunk, cambio de dimension), igual que
 * MobFreezeTracker del modulo 1.
 */
public final class VillagerThrottleTracker {

    private static final Map<ServerLevel, Set<Mob>> TRACKED_BY_LEVEL = new ConcurrentHashMap<>();

    static Map<ServerLevel, Set<Mob>> trackedByLevel() {
        return TRACKED_BY_LEVEL;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!VillagerEligibility.isEligible(mob)) {
            // Modulo apagado (o ya no incluye Wandering Trader, etc.): si quedo congelado por
            // nosotros de una sesion anterior, descongelarlo aca para que no quede con NoAI=true
            // para siempre (nunca vuelve a entrar al tracking para reevaluarse).
            if (VillagerFreeze.isFrozenByUs(mob)) {
                VillagerFreeze.apply(mob, false);
            }
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        TRACKED_BY_LEVEL.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet()).add(mob);
    }

    @SubscribeEvent
    public void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        Set<Mob> tracked = TRACKED_BY_LEVEL.get(event.getLevel());
        if (tracked != null) {
            tracked.remove(mob);
        }
        VillagerFreeze.clear(mob);
    }
}
