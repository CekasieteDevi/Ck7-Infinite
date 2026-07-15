package com.ck7infinite.ultraload;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Detector y efectos de servidor de Ultra Carga.
 * <p>
 * Cuenta los ChunkEvent.Load del lado servidor en una ventana movil de ~1s (20 ticks); si superan
 * el umbral, activa Ultra Carga y arranca un cooldown. Al activarse/desactivarse maneja los
 * efectos de servidor: pausar random ticks (gamerule randomTickSpeed) y cancelar spawns naturales.
 * El freeze de mobs lo aplica MobFreezeTickHandler leyendo UltraLoadState; los efectos de cliente
 * (particulas, render de entidades) los aplican el mixin de particulas y UltraLoadClientHandler.
 */
public final class UltraLoadServerHandler {

    // Ventana movil de cargas de chunk por tick (20 ticks ~= 1 segundo a 20 TPS).
    private final int[] loadsRing = new int[20];
    private int ringIndex;
    private int loadsThisTick;
    private int cooldownRemaining;
    private boolean currentlyActive;

    // Valor original de randomTickSpeed guardado mientras lo tenemos en 0. null = no lo tocamos.
    private Integer savedRandomTick;

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() != null && !event.getLevel().isClientSide()) {
            loadsThisTick++;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        if (!UltraLoadConfig.isEnabled()) {
            if (currentlyActive) {
                deactivate(server);
            }
            loadsThisTick = 0;
            return;
        }

        loadsRing[ringIndex] = loadsThisTick;
        ringIndex = (ringIndex + 1) % loadsRing.length;
        loadsThisTick = 0;

        int windowSum = 0;
        for (int v : loadsRing) {
            windowSum += v;
        }

        boolean shouldBeActive;
        if (windowSum >= UltraLoadConfig.activationChunksPerSecond()) {
            cooldownRemaining = UltraLoadConfig.cooldownTicks();
            shouldBeActive = true;
        } else if (cooldownRemaining > 0) {
            cooldownRemaining--;
            shouldBeActive = true;
        } else {
            shouldBeActive = false;
        }

        if (shouldBeActive != currentlyActive) {
            if (shouldBeActive) {
                activate(server);
            } else {
                deactivate(server);
            }
        }
    }

    @SubscribeEvent
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!UltraLoadConfig.isEnabled() || !UltraLoadConfig.pauseSpawns() || !UltraLoadState.isActive()) {
            return;
        }
        MobSpawnType type = event.getSpawnType();
        if (type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Nunca dejar randomTickSpeed en 0 persistido en el save.
        restoreRandomTicks(event.getServer());
        currentlyActive = false;
        UltraLoadState.setActive(false);
    }

    private void activate(MinecraftServer server) {
        currentlyActive = true;
        UltraLoadState.setActive(true);
        pauseRandomTicks(server);
    }

    private void deactivate(MinecraftServer server) {
        currentlyActive = false;
        UltraLoadState.setActive(false);
        restoreRandomTicks(server);
    }

    private void pauseRandomTicks(MinecraftServer server) {
        if (!UltraLoadConfig.pauseRandomTicks() || savedRandomTick != null) {
            return;
        }
        GameRules.IntegerValue rule = server.getGameRules().getRule(GameRules.RULE_RANDOMTICKING);
        savedRandomTick = rule.get();
        rule.set(0, server);
    }

    private void restoreRandomTicks(MinecraftServer server) {
        if (savedRandomTick == null) {
            return;
        }
        server.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(savedRandomTick, server);
        savedRandomTick = null;
    }
}
