package com.ck7infinite.particlepriority;

/**
 * Resultado de evaluar si una particula deberia spawnear.
 * REJECT: se cancela, no se crea la particula.
 * FORCE_ACCEPT: se crea sin pasar por el chequeo de settings/distancia de vanilla (particula relevante).
 * DEFER_TO_VANILLA: no tenemos opinion fuerte, dejar que la logica original de LevelRenderer decida.
 */
public enum ParticleDecision {
    REJECT,
    FORCE_ACCEPT,
    DEFER_TO_VANILLA
}
