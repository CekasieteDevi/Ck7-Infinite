package com.ck7infinite.ultraload.mixin;

import com.ck7infinite.ultraload.UltraLoadConfig;
import com.ck7infinite.ultraload.UltraLoadState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Corta la iteracion de spawn natural durante Ultra Carga.
 * <p>
 * NaturalSpawner#spawnForChunk es lo que se llama por-chunk cada tick para intentar spawns:
 * recorre categorias y posiciones antes de crear nada, y ESA iteracion es el costo (medido ~5.7%
 * del tick en un pack real). Cancelar el spawn recien en MobSpawnEvent.FinalizeSpawn (como hacia
 * antes el handler) llega tarde: la iteracion ya se pago. Con este @Inject al HEAD cancelamos el
 * metodo entero cuando Ultra Carga esta activo, evitando toda la iteracion. El chequeo en
 * FinalizeSpawn se deja igual, para atrapar spawns naturales que no pasan por spawnForChunk
 * (phantoms, patrullas, etc.).
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(method = "spawnForChunk", at = @At("HEAD"), cancellable = true)
    private static void ck7infinite$skipSpawnDuringUltraLoad(
            ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState spawnState,
            boolean spawnFriendlies, boolean spawnEnemies, boolean spawnPassives, CallbackInfo ci) {
        if (UltraLoadConfig.isEnabled() && UltraLoadConfig.pauseSpawns() && UltraLoadState.isActive()) {
            ci.cancel();
        }
    }
}
