package com.ck7infinite.particlepriority;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Rastrea la ultima accion "relevante" del jugador local (romper/colocar/usar bloque, interactuar
 * o atacar una entidad) para poder priorizar particulas relacionadas a lo que esta haciendo ahora,
 * no solo a la distancia. Solo se registra en el cliente (ver ParticlePriorityClientSetup).
 */
public final class PlayerActionContext {

    private static volatile Vec3 lastActionPos;
    private static volatile long lastActionTick = Long.MIN_VALUE;

    static boolean isRecentlyRelevant(double x, double y, double z) {
        Vec3 pos = lastActionPos;
        if (pos == null) {
            return false;
        }
        long ticksSince = currentTick() - lastActionTick;
        if (ticksSince < 0 || ticksSince > ParticlePriorityConfig.contextRecencyTicks()) {
            return false;
        }
        double radius = ParticlePriorityConfig.contextRadiusBlocks();
        return pos.distanceToSqr(x, y, z) <= radius * radius;
    }

    private static long currentTick() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }

    private static void markAction(Vec3 pos) {
        lastActionPos = pos;
        lastActionTick = currentTick();
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        markAction(blockCenter(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()));
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        markAction(blockCenter(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()));
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        markAction(event.getTarget().position());
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        markAction(event.getTarget().position());
    }

    private static Vec3 blockCenter(int x, int y, int z) {
        return new Vec3(x + 0.5D, y + 0.5D, z + 0.5D);
    }
}
