package com.ck7infinite.mobfreeze;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Cada evaluationIntervalTicks, recorre los mobs rastreados por MobFreezeTracker y congela/
 * descongela su IA segun la distancia al jugador mas cercano. No se engancha a LivingTickEvent
 * (eso corre por-entity todos los ticks y ademas cancelarlo saltea tick() entero: fuego, ahogo,
 * efectos, caida... rompiendo drops/farms). Este handler solo toca el flag NoAI.
 */
public final class MobFreezeTickHandler {

    private int tickCounter;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !MobFreezeConfig.isEnabled()) {
            return;
        }

        int interval = MobFreezeConfig.evaluationIntervalTicks();
        if (++tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        double radius = MobFreezeConfig.freezeRadiusBlocks();
        int graceTicks = MobFreezeConfig.spawnGraceTicks();

        for (Map.Entry<ServerLevel, Set<Mob>> entry : MobFreezeTracker.trackedByLevel().entrySet()) {
            ServerLevel level = entry.getKey();
            Iterator<Mob> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                Mob mob = iterator.next();
                if (!mob.isAlive() || mob.level() != level) {
                    iterator.remove();
                    FreezeState.clear(mob);
                    continue;
                }
                if (SpawnGraceTracker.isInGracePeriod(mob, level.getGameTime(), graceTicks)) {
                    continue;
                }
                Player nearest = level.getNearestPlayer(mob, radius);
                FreezeState.apply(mob, nearest == null);
            }
        }
    }
}
