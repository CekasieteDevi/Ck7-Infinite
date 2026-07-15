package com.ck7infinite.mobfreeze;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Cada evaluationIntervalTicks, recorre los mobs rastreados por MobFreezeTracker y congela/
 * descongela su IA segun distancia y (opcionalmente) linea de vision al jugador mas cercano.
 * No se engancha a LivingTickEvent (eso corre por-entity todos los ticks y ademas cancelarlo
 * saltea tick() entero: fuego, ahogo, efectos, caida... rompiendo drops/farms). Este handler
 * solo toca el flag NoAI.
 */
public final class MobFreezeTickHandler {

    private int tickCounter;
    private boolean wasEnabled = true;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!MobFreezeConfig.isEnabled()) {
            // Al pasar de activado a desactivado en vivo (sin recargar el mundo), hay que
            // descongelar todo lo que tengamos marcado: si solo dejamos de evaluar, cualquier
            // mob que haya quedado con NoAI=true nunca se toca de nuevo y queda roto para
            // siempre (el bug de "el caballo no se mueve/no se puede domar").
            if (wasEnabled) {
                forceUnfreezeAllTracked();
                wasEnabled = false;
            }
            return;
        }
        wasEnabled = true;

        int interval = MobFreezeConfig.evaluationIntervalTicks();
        if (++tickCounter < interval) {
            return;
        }
        tickCounter = 0;

        double radius = MobFreezeConfig.freezeRadiusBlocks();
        double radiusSq = radius * radius;
        double alwaysActive = Math.min(MobFreezeConfig.alwaysActiveRadiusBlocks(), radius);
        double alwaysActiveSq = alwaysActive * alwaysActive;
        boolean requireLos = MobFreezeConfig.requireLineOfSight();
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
                boolean hasPlayer = hasActivatingPlayer(level, mob, radiusSq, alwaysActiveSq, requireLos);
                FreezeState.apply(mob, !hasPlayer);
            }
        }
    }

    private static void forceUnfreezeAllTracked() {
        for (Set<Mob> mobs : MobFreezeTracker.trackedByLevel().values()) {
            for (Mob mob : mobs) {
                FreezeState.apply(mob, false);
            }
        }
    }

    /**
     * Un jugador mantiene activo a un mob si esta dentro del radio Y ademas: esta dentro del
     * radio de actividad garantizada, o el chequeo de linea de vision esta apagado, o tiene
     * linea de vision con el mob. La linea de vision es oclusion geometrica (bloques entre las
     * cabezas de ambos, el mismo chequeo que usa la IA vanilla para targetear), NO el cono de
     * vision del jugador: si dependiera de hacia donde mira, darle la espalda a un mob lo
     * congelaria (exploit) y girarse lo despertaria con delay visible. Con oclusion, un mob en
     * una cueva cerrada bajo tus pies queda congelado, pero uno detras tuyo al aire libre sigue
     * activo. El raycast solo se paga para mobs dentro del radio y fuera de la zona garantizada,
     * una vez por intervalo de evaluacion.
     */
    private static boolean hasActivatingPlayer(ServerLevel level, Mob mob, double radiusSq,
                                               double alwaysActiveSq, boolean requireLos) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            double distSq = player.distanceToSqr(mob);
            if (distSq > radiusSq) {
                continue;
            }
            if (!requireLos || distSq <= alwaysActiveSq) {
                return true;
            }
            if (mob.hasLineOfSight(player)) {
                return true;
            }
        }
        return false;
    }
}
