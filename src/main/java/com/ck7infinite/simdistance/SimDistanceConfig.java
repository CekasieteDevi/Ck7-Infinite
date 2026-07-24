package com.ck7infinite.simdistance;

import com.ck7infinite.Ck7MasterConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import static com.ck7infinite.Ck7Infinite.MODID;

/**
 * Config COMMON del modulo Adaptive Simulation Distance (Fase 2, item 5 del roadmap V2). Baja/
 * sube gradualmente la simulation distance del servidor segun la MEDIANA del tick time reciente
 * (server.tickTimes, ~100 ticks / 5s), no el promedio -un pico aislado (worldgen, GC) no mueve la
 * mediana, asi que reacciona a carga SOSTENIDA, no a picos-.
 * <p>
 * Default false: un intento anterior de este mismo mecanismo (config huerfano encontrado en el
 * entorno dev, nunca commiteado a este repo) resulto CONTRAPRODUCENTE en un pack real -no
 * distinguia MSPT alto por GENERACION de mundo (donde recortar la sim distance solo empeora las
 * cosas: "chunks no cargan hasta estar encima" + cada recorte descarga un anillo de chunks que
 * inunda processUnloads) de MSPT alto por simulacion real (donde recortar SI ayuda). Este modulo
 * corrige esa causa raiz leyendo UltraLoadState (Fase 0): mientras el disparador de carga de
 * chunks de Ultra Carga esta activo, la evaluacion se saltea por completo -ver SimDistanceHandler-.
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SimDistanceConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment(
                    "Activa o desactiva por completo el modulo de Adaptive Simulation Distance.",
                    "Default false: modulo nuevo, activalo solo despues de probarlo en tu",
                    "configuracion real -un intento anterior de este mecanismo resulto",
                    "contraproducente en un pack real por no distinguir generacion de simulacion."
            )
            .define("enabled", false);

    public static final ForgeConfigSpec.IntValue MIN_SIMULATION_DISTANCE = BUILDER
            .comment(
                    "Piso de simulation distance (en chunks) que puede alcanzar el ajuste bajo",
                    "carga. El techo es siempre la simulation distance configurada del servidor/",
                    "mundo. Mas bajo = mas alivio bajo carga sostenida, pero granjas/redstone",
                    "lejanas se pausan antes."
            )
            .defineInRange("minSimulationDistanceChunks", 5, 2, 32);

    public static final ForgeConfigSpec.DoubleValue HIGH_TICK_MILLIS = BUILDER
            .comment(
                    "Si la MEDIANA del tick time de los ultimos ~100 ticks (5s) supera este valor,",
                    "se baja la simulation distance un chunk por evaluacion."
            )
            .defineInRange("highTickMillis", 45.0, 10.0, 500.0);

    public static final ForgeConfigSpec.DoubleValue LOW_TICK_MILLIS = BUILDER
            .comment(
                    "Si la mediana de tick time baja de este valor, se sube la simulation distance",
                    "un chunk por evaluacion (hasta el techo), siempre que haya pasado el cooldown",
                    "post-recorte. La franja entre este valor y highTickMillis es zona muerta que",
                    "evita oscilar. Debe ser menor que highTickMillis."
            )
            .defineInRange("lowTickMillis", 35.0, 5.0, 500.0);

    public static final ForgeConfigSpec.IntValue EVALUATION_INTERVAL_TICKS = BUILDER
            .comment(
                    "Cada cuantos ticks se reevalua y se mueve la simulation distance a lo sumo un",
                    "chunk. Cambiarla recalcula tickets de chunk (no es gratis), asi que conviene",
                    "no hacerlo cada tick."
            )
            .defineInRange("evaluationIntervalTicks", 20, 1, 200);

    public static final ForgeConfigSpec.IntValue GROWTH_COOLDOWN_TICKS = BUILDER
            .comment(
                    "Ticks que hay que esperar despues de un recorte antes de permitir que la sim",
                    "distance vuelva a crecer. Rompe el ciclo recortar->descargar chunks->crecer->",
                    "regenerar->recortar que provoca oscilacion."
            )
            .defineInRange("growthCooldownTicks", 100, 0, 1200);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile boolean enabled;
    private static volatile int minSimulationDistanceChunks;
    private static volatile double highTickMillis;
    private static volatile double lowTickMillis;
    private static volatile int evaluationIntervalTicks;
    private static volatile int growthCooldownTicks;

    private SimDistanceConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        refresh();
    }

    public static void refresh() {
        enabled = ENABLED.get();
        minSimulationDistanceChunks = MIN_SIMULATION_DISTANCE.get();
        highTickMillis = HIGH_TICK_MILLIS.get();
        lowTickMillis = LOW_TICK_MILLIS.get();
        evaluationIntervalTicks = EVALUATION_INTERVAL_TICKS.get();
        growthCooldownTicks = GROWTH_COOLDOWN_TICKS.get();
    }

    public static boolean isEnabled() {
        return enabled && Ck7MasterConfig.isEnabled();
    }

    public static int minSimulationDistanceChunks() {
        return minSimulationDistanceChunks;
    }

    public static double highTickMillis() {
        return highTickMillis;
    }

    public static double lowTickMillis() {
        return lowTickMillis;
    }

    public static int evaluationIntervalTicks() {
        return evaluationIntervalTicks;
    }

    public static int growthCooldownTicks() {
        return growthCooldownTicks;
    }
}
