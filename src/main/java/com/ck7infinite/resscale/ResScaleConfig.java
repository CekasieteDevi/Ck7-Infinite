package com.ck7infinite.resscale;

import com.ck7infinite.Ck7MasterConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import static com.ck7infinite.Ck7Infinite.MODID;

/**
 * Config CLIENT del modulo Escala de Resolucion. Deliberadamente minimo -mismo alcance que
 * RenderScale (Zolo101), que no tiene mas que un valor de escala y un filtro: sin ajuste
 * automatico por FPS, sin escala minima/maxima, sin cuantizacion/dwell. Ese controlador dinamico
 * (removido en esta version) resultaba en resizes reales del framebuffer durante el juego, algo
 * que RenderScale nunca hace -solo resizea en eventos puntuales (activar, cambiar el valor,
 * resize de ventana)- y que en la practica corrompio el frame (confirmado con un self-test:
 * geometria rota coincidiendo con un resize + un stall de frame). Sacar el controlador dinamico
 * saca tambien la unica fuente de resizes repetidos durante el juego.
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ResScaleConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment(
                    "Activa o desactiva por completo el modulo de Escala de Resolucion.",
                    "Default false: modulo nuevo, activalo solo despues de probarlo en tu configuracion real."
            )
            .define("enabled", false);

    public static final ForgeConfigSpec.DoubleValue SCALE = BUILDER
            .comment(
                    "Escala del mundo 3D. 1.0 = resolucion nativa (sin efecto). Menos de 1.0 renderiza",
                    "a menor resolucion y estira a pantalla completa (mas pixelado, mejor rendimiento).",
                    "Mas de 1.0 renderiza a mayor resolucion (supersampling/SSAA, mas nitido, peor",
                    "rendimiento). Se aplica al instante al guardar, sin reiniciar ni volver a entrar",
                    "al mundo."
            )
            .defineInRange("scale", 1.0, 0.25, 2.0);

    public static final ForgeConfigSpec.BooleanValue LINEAR_FILTER = BUILDER
            .comment(
                    "Filtro usado al estirar el framebuffer a pantalla completa. true = lineal (mas",
                    "suave), false = vecino mas cercano (mas nitido/pixelado, como un upscale crudo)."
            )
            .define("linearFilter", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile boolean enabled;
    private static volatile double scale;
    private static volatile boolean linearFilter;

    private ResScaleConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        refresh();
    }

    /**
     * Recarga los campos cacheados desde el SPEC. ForgeConfigSpec#save() solo escribe el archivo:
     * no dispara ModConfigEvent.Reloading (eso depende de un file watcher asincrono poco confiable
     * en Windows para el guardado atomico). La pantalla de Cloth Config llama esto explicitamente
     * despues de guardar, para que el cambio de escala tenga efecto inmediato.
     */
    public static void refresh() {
        enabled = ENABLED.get();
        scale = SCALE.get();
        linearFilter = LINEAR_FILTER.get();
    }

    public static boolean isEnabled() {
        return enabled && Ck7MasterConfig.isEnabled();
    }

    public static double scale() {
        return scale;
    }

    public static boolean linearFilter() {
        return linearFilter;
    }
}
