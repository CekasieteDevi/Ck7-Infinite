package com.ck7infinite.dynamicres;

import com.ck7infinite.dynamicres.mixin.MinecraftAccessor;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

/**
 * Maneja un render target propio (mas chico que la ventana) donde se dibuja el mundo 3D. Durante
 * GameRenderer#renderLevel, apuntamos Minecraft#mainRenderTarget hacia este target (via
 * MinecraftAccessor) para que TODO lo que el mundo dibuja -incluidos los sub-targets de
 * compositing de Fabulous graphics, que referencian getMainRenderTarget() directamente- caiga
 * ahi. Al terminar, restauramos el mainRenderTarget real y lo bliteamos (upscale) con el
 * contenido ya renderizado. El HUD se dibuja despues, directo sobre el mainRenderTarget real a
 * resolucion nativa, sin pasar por este target.
 */
public final class RenderScaleManager {

    // Solo re-crear el render target si el tamaño pedido cambia mas de este umbral relativo.
    // Sin esto, el ajuste dinamico (que fluctua un poco cada frame) dispara un resize -es decir,
    // destruir y recrear el framebuffer entero- casi todos los frames, lo que además de ser caro
    // provoca parpadeos/perdida puntual de contenido (el bug que reporto el usuario: cielo/agua/
    // particulas "no se renderizan" de forma intermitente).
    private static final double RESIZE_THRESHOLD = 0.03;

    private static MainTarget worldTarget;
    private static RenderTarget realMainTarget;
    private static double currentScale = 1.0;
    private static long lastFrameNanos;
    private static double avgFrameMs;
    private static boolean activeThisFrame;

    private RenderScaleManager() {
    }

    public static void beginWorldPass() {
        activeThisFrame = false;
        if (!DynamicResConfig.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!(mc instanceof MinecraftAccessor accessor)) {
            return;
        }

        updateScale();

        Window window = mc.getWindow();
        int desiredWidth = Math.max(1, Math.round((float) (window.getWidth() * currentScale)));
        int desiredHeight = Math.max(1, Math.round((float) (window.getHeight() * currentScale)));

        if (worldTarget == null) {
            worldTarget = new MainTarget(desiredWidth, desiredHeight);
        } else if (sizeChangedBeyondThreshold(desiredWidth, desiredHeight)) {
            worldTarget.resize(desiredWidth, desiredHeight, Minecraft.ON_OSX);
        }

        realMainTarget = mc.getMainRenderTarget();
        accessor.ck7infinite$setMainRenderTarget(worldTarget);
        worldTarget.bindWrite(true);
        activeThisFrame = true;
    }

    public static void endWorldPass() {
        if (!activeThisFrame || worldTarget == null || realMainTarget == null) {
            activeThisFrame = false;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc instanceof MinecraftAccessor accessor) {
            accessor.ck7infinite$setMainRenderTarget(realMainTarget);
        }

        realMainTarget.bindWrite(true);
        worldTarget.blitToScreen(realMainTarget.width, realMainTarget.height);

        realMainTarget = null;
        activeThisFrame = false;
    }

    private static boolean sizeChangedBeyondThreshold(int desiredWidth, int desiredHeight) {
        double dw = Math.abs(desiredWidth - worldTarget.width) / (double) Math.max(1, worldTarget.width);
        double dh = Math.abs(desiredHeight - worldTarget.height) / (double) Math.max(1, worldTarget.height);
        return dw > RESIZE_THRESHOLD || dh > RESIZE_THRESHOLD;
    }

    private static void updateScale() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0L) {
            double frameMs = (now - lastFrameNanos) / 1_000_000.0;
            avgFrameMs = avgFrameMs <= 0.0 ? frameMs : (avgFrameMs * 0.9 + frameMs * 0.1);
        }
        lastFrameNanos = now;

        double min = DynamicResConfig.minRenderScale();
        double max = DynamicResConfig.maxRenderScale();

        if (!DynamicResConfig.isDynamicEnabled()) {
            currentScale = clamp(DynamicResConfig.fixedRenderScale(), min, max);
            return;
        }

        if (currentScale < min || currentScale > max) {
            currentScale = clamp(currentScale, min, max);
        }

        if (avgFrameMs <= 0.0) {
            return;
        }

        double targetMs = 1000.0 / DynamicResConfig.targetFps();
        double step = DynamicResConfig.adjustmentStep();
        double error = avgFrameMs - targetMs;

        if (error > targetMs * 0.1) {
            currentScale = clamp(currentScale - step, min, max);
        } else if (error < -targetMs * 0.2) {
            currentScale = clamp(currentScale + step * 0.5, min, max);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
