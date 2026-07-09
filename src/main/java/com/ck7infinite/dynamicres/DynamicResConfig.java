package com.ck7infinite.dynamicres;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import static com.ck7infinite.Ck7Infinite.MODID;

/**
 * Config CLIENT-side del modulo Resolucion de Render Dinamica. No importa ninguna clase de
 * net.minecraft.client a proposito, por la misma razon que ParticlePriorityConfig: se registra
 * desde el constructor comun (corre en ambos sides) y no debe poder romper un dedicated server.
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DynamicResConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Activa o desactiva por completo el modulo de Resolucion de Render Dinamica.")
            .define("enabled", true);

    private static final ForgeConfigSpec.BooleanValue DYNAMIC_ENABLED = BUILDER
            .comment(
                    "Si es true, la escala se ajusta sola tratando de mantener targetFps.",
                    "Si es false, se usa siempre fixedRenderScale."
            )
            .define("dynamicEnabled", true);

    private static final ForgeConfigSpec.DoubleValue FIXED_RENDER_SCALE = BUILDER
            .comment("Escala fija del mundo 3D (1.0 = resolucion nativa). Solo aplica si dynamicEnabled=false.")
            .defineInRange("fixedRenderScale", 0.75, 0.25, 1.0);

    private static final ForgeConfigSpec.DoubleValue MIN_RENDER_SCALE = BUILDER
            .comment("Piso de escala que puede alcanzar el ajuste automatico.")
            .defineInRange("minRenderScale", 0.5, 0.25, 1.0);

    private static final ForgeConfigSpec.DoubleValue MAX_RENDER_SCALE = BUILDER
            .comment("Techo de escala que puede alcanzar el ajuste automatico (1.0 = nunca sobre-escalar).")
            .defineInRange("maxRenderScale", 1.0, 0.25, 1.0);

    private static final ForgeConfigSpec.IntValue TARGET_FPS = BUILDER
            .comment("FPS objetivo del ajuste automatico de escala.")
            .defineInRange("targetFps", 60, 10, 240);

    private static final ForgeConfigSpec.DoubleValue ADJUSTMENT_STEP = BUILDER
            .comment(
                    "Cuanto se mueve la escala por frame cuando el ajuste automatico decide subir o bajar.",
                    "Valores mas altos reaccionan mas rapido pero se notan mas (menos suave)."
            )
            .defineInRange("adjustmentStep", 0.01, 0.001, 0.2);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile boolean enabled;
    private static volatile boolean dynamicEnabled;
    private static volatile double fixedRenderScale;
    private static volatile double minRenderScale;
    private static volatile double maxRenderScale;
    private static volatile int targetFps;
    private static volatile double adjustmentStep;

    private DynamicResConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        enabled = ENABLED.get();
        dynamicEnabled = DYNAMIC_ENABLED.get();
        fixedRenderScale = FIXED_RENDER_SCALE.get();
        minRenderScale = MIN_RENDER_SCALE.get();
        maxRenderScale = MAX_RENDER_SCALE.get();
        targetFps = TARGET_FPS.get();
        adjustmentStep = ADJUSTMENT_STEP.get();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isDynamicEnabled() {
        return dynamicEnabled;
    }

    public static double fixedRenderScale() {
        return fixedRenderScale;
    }

    public static double minRenderScale() {
        return minRenderScale;
    }

    public static double maxRenderScale() {
        return maxRenderScale;
    }

    public static int targetFps() {
        return targetFps;
    }

    public static double adjustmentStep() {
        return adjustmentStep;
    }
}
