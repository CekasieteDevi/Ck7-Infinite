package com.ck7infinite.particlepriority;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Acumulador del presupuesto de cobertura de particulas AMBIENT del frame actual. Se resetea al
 * comienzo de cada frame renderizado; ParticlePriorityManager consulta/suma contra el mismo
 * acumulador cada vez que decide aceptar una particula ambiental (ver evaluate()).
 */
final class ParticleCoverageBudget {

    private static volatile double used;

    static double used() {
        return used;
    }

    static void add(double weight) {
        used += weight;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            used = 0.0;
        }
    }
}
