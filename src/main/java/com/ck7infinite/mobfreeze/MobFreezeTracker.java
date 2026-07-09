package com.ck7infinite.mobfreeze;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantiene, por ServerLevel, el set de mobs elegibles para freeze de IA. Se actualiza solo
 * en join/leave del entity (spawn, despawn, muerte, descarga de chunk, cambio de dimension),
 * que son eventos poco frecuentes comparados con el tick — asi el tick handler no tiene que
 * volver a clasificar ni iterar sobre entities no elegibles cada vez que corre.
 */
public final class MobFreezeTracker {

    private static final Map<ServerLevel, Set<Mob>> TRACKED_BY_LEVEL = new ConcurrentHashMap<>();

    public static Map<ServerLevel, Set<Mob>> trackedByLevel() {
        return TRACKED_BY_LEVEL;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!EntityFreezeClassifier.isEligible(mob)) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        TRACKED_BY_LEVEL.computeIfAbsent(level, l -> ConcurrentHashMap.newKeySet()).add(mob);
        if (!event.loadedFromDisk()) {
            // Solo mobs recien nacidos arrancan el periodo de gracia; uno que vuelve a cargar
            // desde un chunk ya existia antes, no tiene sentido tratarlo como "recien spawneado".
            SpawnGraceTracker.markSpawnTick(mob, level.getGameTime());
        }
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
        FreezeState.clear(mob);
    }
}
