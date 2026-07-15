package com.ck7infinite.ultraload.client;

import com.ck7infinite.ultraload.UltraLoadConfig;
import com.ck7infinite.ultraload.UltraLoadState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Efectos de cliente de Ultra Carga.
 * <p>
 * hideEntities (Tier 1): mientras esta activo, cancela el render de las entidades vivas (mobs,
 * otros jugadores) para liberar GPU al dibujo del terreno nuevo. Usa RenderLivingEvent.Pre, un
 * hook de Forge que no toca el pipeline de chunks de Sodium/Embeddium. No oculta entidades dentro
 * de safeRadiusBlocks del jugador local (evita el "susto" de que un enemigo pegado a vos
 * desaparezca y reaparezca de golpe).
 * <p>
 * reduceEntityDistance / limitFramerateDuringLoad (Tier 3): bajan y restauran
 * Options#entityDistanceScaling y Options#framerateLimit -misma API vanilla que el menu de Video.
 * A diferencia del render distance (Tier 3 viejo, sacado -ver nota abajo), estas dos opciones
 * tienen callback de cambio vacio/directo en vanilla (verificado por decompile): no fuerzan
 * ninguna reconstruccion de mallas, asi que alternarlas cada pocos segundos (cada ciclo de Ultra
 * Carga) es seguro. ClientPlayerNetworkEvent.LoggingOut es la red de seguridad para no persistir
 * el valor recortado en options.txt si el jugador sale del mundo con Ultra Carga activo.
 * <p>
 * Solo se registra en el cliente fisico (ver Ck7Infinite / gate de Dist), asi que importar clases
 * de render/opciones de cliente aca es seguro.
 * <p>
 * Nota: se probo (y se saco) un Tier 3 que bajaba/restauraba Options#renderDistance. Como Ultra
 * Carga entra/sale cada pocos segundos (cooldownTicks), oscilar el render distance a esa
 * frecuencia hacia que Sodium/Embeddium re-transmitiera/mallara chunks constantemente,
 * empeorando el lag en vez de aliviarlo. No reintentar esa via sin medir con spark en pack real.
 */
public final class UltraLoadClientHandler {

    private Double savedEntityDistance;
    private Integer savedFramerateLimit;

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!UltraLoadConfig.isEnabled() || !UltraLoadConfig.hideEntities() || !UltraLoadState.isActive()) {
            return;
        }
        Player localPlayer = Minecraft.getInstance().player;
        double safeRadius = UltraLoadConfig.safeRadiusBlocks();
        if (localPlayer != null && safeRadius > 0.0
                && localPlayer.distanceToSqr(event.getEntity()) <= safeRadius * safeRadius) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        boolean ultraActive = UltraLoadConfig.isEnabled() && UltraLoadState.isActive();

        if (ultraActive && UltraLoadConfig.reduceEntityDistance()) {
            applyReducedEntityDistance();
        } else {
            restoreEntityDistance();
        }

        if (ultraActive && UltraLoadConfig.limitFramerateDuringLoad()) {
            applyLimitedFramerate();
        } else {
            restoreFramerate();
        }
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // Salir del mundo/servidor con Ultra Carga activo no debe dejar estos valores recortados
        // guardados en options.txt.
        restoreEntityDistance();
        restoreFramerate();
    }

    private void applyReducedEntityDistance() {
        if (savedEntityDistance != null) {
            return;
        }
        var options = Minecraft.getInstance().options;
        double current = options.entityDistanceScaling().get();
        double target = Math.min(current, UltraLoadConfig.entityDistanceDuringLoad());
        if (target >= current) {
            return;
        }
        savedEntityDistance = current;
        options.entityDistanceScaling().set(target);
    }

    private void restoreEntityDistance() {
        if (savedEntityDistance == null) {
            return;
        }
        Minecraft.getInstance().options.entityDistanceScaling().set(savedEntityDistance);
        savedEntityDistance = null;
    }

    private void applyLimitedFramerate() {
        if (savedFramerateLimit != null) {
            return;
        }
        var options = Minecraft.getInstance().options;
        int current = options.framerateLimit().get();
        int target = Math.min(current, UltraLoadConfig.framerateLimitDuringLoad());
        if (target >= current) {
            return;
        }
        savedFramerateLimit = current;
        options.framerateLimit().set(target);
    }

    private void restoreFramerate() {
        if (savedFramerateLimit == null) {
            return;
        }
        Minecraft.getInstance().options.framerateLimit().set(savedFramerateLimit);
        savedFramerateLimit = null;
    }
}
