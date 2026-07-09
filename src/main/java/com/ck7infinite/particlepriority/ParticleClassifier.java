package com.ck7infinite.particlepriority;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clasifica un ParticleType en una ParticleCategory segun las listas configurables. El resultado
 * depende solo del tipo (no de la instancia puntual), asi que se cachea por ParticleType.
 */
final class ParticleClassifier {

    private static final Map<ParticleType<?>, ParticleCategory> CACHE = new ConcurrentHashMap<>();

    private ParticleClassifier() {
    }

    static void invalidateCache() {
        CACHE.clear();
    }

    static ParticleCategory classify(ParticleOptions options) {
        return CACHE.computeIfAbsent(options.getType(), ParticleClassifier::compute);
    }

    private static ParticleCategory compute(ParticleType<?> type) {
        ResourceLocation id = ForgeRegistries.PARTICLE_TYPES.getKey(type);
        String path = id == null ? "" : id.getPath();

        if (ParticlePriorityConfig.combatParticleIds().contains(path)) {
            return ParticleCategory.COMBAT;
        }
        if (ParticlePriorityConfig.interactionParticleIds().contains(path)) {
            return ParticleCategory.INTERACTION;
        }
        if (ParticlePriorityConfig.redstoneParticleIds().contains(path)) {
            return ParticleCategory.REDSTONE;
        }
        return ParticleCategory.AMBIENT;
    }
}
