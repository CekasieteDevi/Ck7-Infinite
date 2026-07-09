package com.ck7infinite.particlepriority;

/**
 * Logica pura de decision, sin ninguna dependencia de Minecraft/Forge/JOML: solo primitivos y
 * nuestros propios enums. Separada de ParticlePriorityManager exclusivamente para poder
 * compilarla y testearla con javac plano, sin necesitar todo el toolchain de Forge/Mixin.
 */
final class ParticlePriorityCore {

    private ParticlePriorityCore() {
    }

    static ParticleDecision evaluate(
            ParticleCategory category,
            boolean recentlyRelevant,
            double px, double py, double pz,
            double camX, double camY, double camZ,
            double lookX, double lookY, double lookZ,
            double radius, double closeRadius, double viewConeDotThreshold
    ) {
        if (recentlyRelevant) {
            return ParticleDecision.FORCE_ACCEPT;
        }

        double dx = px - camX;
        double dy = py - camY;
        double dz = pz - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > radius * radius) {
            return ParticleDecision.REJECT;
        }

        if (category != ParticleCategory.AMBIENT) {
            return ParticleDecision.FORCE_ACCEPT;
        }

        if (distSq <= closeRadius * closeRadius) {
            return ParticleDecision.FORCE_ACCEPT;
        }

        double dist = Math.sqrt(distSq);
        if (dist < 1.0E-4) {
            return ParticleDecision.FORCE_ACCEPT;
        }

        double dot = (lookX * dx + lookY * dy + lookZ * dz) / dist;
        return dot >= viewConeDotThreshold ? ParticleDecision.FORCE_ACCEPT : ParticleDecision.REJECT;
    }
}
