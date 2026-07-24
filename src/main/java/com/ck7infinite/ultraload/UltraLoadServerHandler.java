package com.ck7infinite.ultraload;

import com.ck7infinite.ultraload.trigger.ChunkLoadTrigger;
import com.ck7infinite.ultraload.trigger.UltraLoadTrigger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

/**
 * Detector y efectos de servidor de Ultra Carga.
 * <p>
 * El nivel agregado es el maximo entre todos los disparadores plugables (ver
 * com.ck7infinite.ultraload.trigger); hoy el unico es ChunkLoadTrigger (ritmo de carga de chunks),
 * asi que el nivel solo toma 0 o 1 -futuros disparadores (TPS sostenido bajo, AFK) se suman a
 * `triggers` sin tocar esta maquina de estados-. Al activarse/desactivarse maneja los efectos de
 * servidor: pausar random ticks (gamerule randomTickSpeed) y cancelar spawns naturales. El freeze
 * de mobs lo aplica MobFreezeTickHandler leyendo UltraLoadState; los efectos de cliente
 * (particulas, render de entidades) los aplican el mixin de particulas y UltraLoadClientHandler.
 */
public final class UltraLoadServerHandler {

    private final ChunkLoadTrigger chunkLoadTrigger = new ChunkLoadTrigger();
    private final List<UltraLoadTrigger> triggers = List.of(chunkLoadTrigger);

    private int currentLevel;

    // Valor original de randomTickSpeed guardado mientras lo tenemos en 0. null = no lo tocamos.
    private Integer savedRandomTick;

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() != null && !event.getLevel().isClientSide()) {
            chunkLoadTrigger.onChunkLoad();
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
            triggers.forEach(UltraLoadTrigger::reset);
            if (currentLevel > 0) {
                deactivate(server);
            }
            return;
        }

        int newLevel = 0;
        for (UltraLoadTrigger trigger : triggers) {
            newLevel = Math.max(newLevel, trigger.desiredLevel(server));
        }

        if (newLevel != currentLevel) {
            if (newLevel > 0) {
                activate(server, newLevel);
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
        currentLevel = 0;
        UltraLoadState.setLevel(0);
    }

    private void activate(MinecraftServer server, int level) {
        currentLevel = level;
        UltraLoadState.setLevel(level);
        pauseRandomTicks(server);
    }

    private void deactivate(MinecraftServer server) {
        currentLevel = 0;
        UltraLoadState.setLevel(0);
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
