package com.ck7infinite.particlepriority;

/**
 * Logica pura de decision, sin ninguna dependencia de Minecraft/Forge/JOML: solo primitivos y
 * nuestros propios enums. Separada de ParticlePriorityManager exclusivamente para poder
 * compilarla y testearla con javac plano, sin necesitar todo el toolchain de Forge/Mixin.
 */
final class ParticlePriorityCore {

    /** Tope al peso de una sola particula contra el presupuesto de cobertura -sin esto, una
     * particula pegada a la camara (distSq -> 0) monopolizaria el presupuesto entero. */
    private static final double MAX_WEIGHT_PER_PARTICLE = 4.0;

    private ParticlePriorityCore() {
    }

    /**
     * Estima el peso de cobertura de pantalla de una particula relativo a una a
     * referenceDistanceBlocks (esa distancia pesa 1.0). Ley de cuadrado inverso: el area aparente
     * de un objeto de tamaño fijo cae con distancia^2, asi que refDistSq/distSq es la razon de
     * cobertura esperada sin necesitar el tamaño real de la particula (que no se conoce en el
     * punto donde se decide esto -antes de crear el objeto Particle-).
     */
    static double coverageWeight(double distSq, double referenceDistanceBlocks) {
        double refDistSq = referenceDistanceBlocks * referenceDistanceBlocks;
        double flooredDistSq = Math.max(distSq, 1.0);
        return Math.min(refDistSq / flooredDistSq, MAX_WEIGHT_PER_PARTICLE);
    }

    static boolean fitsInCoverageBudget(double weight, double budgetUsed, double budgetMax) {
        return budgetUsed + weight <= budgetMax;
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
