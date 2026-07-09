package com.ck7infinite.villagerthrottle;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Descongela de inmediato a un aldeano cuando un jugador lo interactua, sin esperar al proximo
 * tick de evaluacion del throttling. Cierra el caso borde de "el jugador llego justo ahora y le
 * hace click antes de que corra la proxima evaluacion" y permite que el restock/trading respondan
 * en tiempo real durante la interaccion (el restock depende del Brain, que solo tickea sin NoAI).
 */
public final class VillagerInteractHandler {

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide
                || !VillagerThrottleConfig.isEnabled()
                || !VillagerThrottleConfig.unfreezeOnInteract()) {
            return;
        }
        if (!(event.getTarget() instanceof Mob mob) || !VillagerEligibility.isEligible(mob)) {
            return;
        }
        if (VillagerFreeze.isFrozenByUs(mob)) {
            VillagerFreeze.apply(mob, false);
        }
    }
}
