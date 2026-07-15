package com.ck7infinite.particlepriority.mixin;

import com.ck7infinite.particlepriority.ParticleDecision;
import com.ck7infinite.particlepriority.ParticlePriorityConfig;
import com.ck7infinite.particlepriority.ParticlePriorityManager;
import com.ck7infinite.ultraload.UltraLoadConfig;
import com.ck7infinite.ultraload.UltraLoadState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * addParticleInternal(ParticleOptions, boolean overrideLimiter, boolean minimizeLevel, double x,
 * y, z, dx, dy, dz) es el unico punto por el que pasan TODAS las particulas antes de crearse
 * (tanto las que spawnea el cliente por su cuenta -bloques, entidades- como las que llegan por
 * paquete del servidor). Ahi mismo vive hoy el chequeo de distancia>1024 y el settings ALL/
 * DECREASED/MINIMAL que este modulo reemplaza por prioridad real.
 * <p>
 * Si overrideLimiter es true dejamos que vanilla decida (son particulas que el juego ya marca
 * como "siempre mostrar", ej: rotura de bloque bajo el cursor del jugador).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ck7infinite$onAddParticleInternal(
            ParticleOptions options, boolean overrideLimiter, boolean minimizeLevel,
            double x, double y, double z, double dx, double dy, double dz,
            CallbackInfoReturnable<Particle> cir
    ) {
        // Ultra Carga: mientras esta activo, se descartan TODAS las particulas (incluso las
        // forzadas), independientemente del modulo de prioridad, para liberar CPU/GPU al terreno.
        if (UltraLoadConfig.isEnabled() && UltraLoadConfig.hideParticles() && UltraLoadState.isActive()) {
            cir.setReturnValue(null);
            return;
        }
        if (!ParticlePriorityConfig.isEnabled() || overrideLimiter) {
            return;
        }

        ParticleDecision decision = ParticlePriorityManager.evaluate(options, x, y, z);
        if (decision == ParticleDecision.REJECT) {
            cir.setReturnValue(null);
        } else if (decision == ParticleDecision.FORCE_ACCEPT) {
            cir.setReturnValue(this.minecraft.particleEngine.createParticle(options, x, y, z, dx, dy, dz));
        }
    }
}
