package com.ck7infinite.villagerthrottle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Cada evaluationIntervalTicks, recorre los aldeanos rastreados por VillagerThrottleTracker y
 * congela/descongela su IA segun la distancia al jugador mas cercano. Igual que en Mob AI Freeze,
 * el freeze usa NoAI (Mob#setNoAi), que en Villager tambien salta el tick del Brain (pathfinding,
 * lookup de trading/POI, restock), ya que Mob#aiStep() invoca customServerAiStep() -> brain.tick()
 * dentro del mismo bloque "if (isEffectiveAi())" que sensing/targetSelector/goalSelector/navigation.
 */
public final class VillagerThrottleTickHandler {

    private int tickCounter;
    private boolean wasEnabled = true;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!VillagerThrottleConfig.isEnabled()) {
            // Igual que en Mob AI Freeze: si solo dejamos de evaluar, un aldeano que haya
            // quedado con NoAI=true nunca se toca de nuevo y queda roto para siempre.
            if (wasEnabled) {
                forceUnfreezeAllTracked();
                wasEnabled = false;
            }
            return;
        }
        wasEnabled = true;

        int interval = VillagerThrottleConfig.evaluationIntervalTicks();
        if (++tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        double radius = VillagerThrottleConfig.throttleRadiusBlocks();

        for (Map.Entry<ServerLevel, Set<Mob>> entry : VillagerThrottleTracker.trackedByLevel().entrySet()) {
            ServerLevel level = entry.getKey();
            Iterator<Mob> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                Mob mob = iterator.next();
                if (!mob.isAlive() || mob.level() != level) {
                    iterator.remove();
                    VillagerFreeze.clear(mob);
                    continue;
                }
                Player nearest = level.getNearestPlayer(mob, radius);
                VillagerFreeze.apply(mob, nearest == null);
            }
        }
    }

    private static void forceUnfreezeAllTracked() {
        for (Set<Mob> mobs : VillagerThrottleTracker.trackedByLevel().values()) {
            for (Mob mob : mobs) {
                VillagerFreeze.apply(mob, false);
            }
        }
    }
}
