package com.ck7infinite.ultraload.trigger;

import com.ck7infinite.ultraload.UltraLoadConfig;
import net.minecraft.server.MinecraftServer;

/**
 * Disparador original de Ultra Carga: cuenta ChunkEvent.Load del lado servidor en una ventana
 * movil de 20 ticks (~1s); si superan activationChunksPerSecond pide nivel 1, y lo sostiene
 * durante cooldownTicks despues de que la carga baja del umbral (anti-parpadeo). Logica identica
 * a la que vivia inline en UltraLoadServerHandler antes de este refactor.
 */
public final class ChunkLoadTrigger implements UltraLoadTrigger {

    private final int[] loadsRing = new int[20];
    private int ringIndex;
    private int loadsThisTick;
    private int cooldownRemaining;

    public void onChunkLoad() {
        loadsThisTick++;
    }

    @Override
    public int desiredLevel(MinecraftServer server) {
        loadsRing[ringIndex] = loadsThisTick;
        ringIndex = (ringIndex + 1) % loadsRing.length;
        loadsThisTick = 0;

        int windowSum = 0;
        for (int v : loadsRing) {
            windowSum += v;
        }

        if (windowSum >= UltraLoadConfig.activationChunksPerSecond()) {
            cooldownRemaining = UltraLoadConfig.cooldownTicks();
            return 1;
        }
        if (cooldownRemaining > 0) {
            cooldownRemaining--;
            return 1;
        }
        return 0;
    }

    @Override
    public void reset() {
        loadsThisTick = 0;
    }
}
