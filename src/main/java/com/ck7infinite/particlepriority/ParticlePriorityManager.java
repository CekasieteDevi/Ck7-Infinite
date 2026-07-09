package com.ck7infinite.particlepriority;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Wrapper delgado: junta el estado real de Minecraft (camara, config, contexto del jugador) y se
 * lo pasa a ParticlePriorityCore.evaluate, que es donde vive la logica de decision en si.
 */
public final class ParticlePriorityManager {

    private ParticlePriorityManager() {
    }

    public static ParticleDecision evaluate(ParticleOptions options, double x, double y, double z) {
        boolean recentlyRelevant = PlayerActionContext.isRecentlyRelevant(x, y, z);
        ParticleCategory category = ParticleClassifier.classify(options);
        double radius = radiusFor(category);

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vector3f look = camera.getLookVector();

        return ParticlePriorityCore.evaluate(
                category, recentlyRelevant,
                x, y, z,
                camPos.x, camPos.y, camPos.z,
                look.x, look.y, look.z,
                radius, ParticlePriorityConfig.ambientCloseRadiusBlocks(), ParticlePriorityConfig.viewConeDotThreshold()
        );
    }

    private static double radiusFor(ParticleCategory category) {
        return switch (category) {
            case COMBAT -> ParticlePriorityConfig.combatRadiusBlocks();
            case INTERACTION -> ParticlePriorityConfig.interactionRadiusBlocks();
            case REDSTONE -> ParticlePriorityConfig.redstoneRadiusBlocks();
            case AMBIENT -> ParticlePriorityConfig.ambientRadiusBlocks();
        };
    }
}
