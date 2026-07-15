package com.ck7infinite;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import static com.ck7infinite.Ck7Infinite.MODID;

/**
 * Interruptor general del mod: un solo toggle que desactiva TODOS los modulos de una, sin tener
 * que ir modulo por modulo. Cada modulo ya expone su propio isEnabled() (su config individual);
 * este interruptor se aplica ahi mismo (ver el AND con Ck7MasterConfig.isEnabled() en cada
 * Config.isEnabled()), asi que apagarlo reusa exactamente la misma logica de "apagado seguro"
 * -incluido el descongelamiento forzado de mobs/aldeanos- que cada modulo ya tiene para su propio
 * toggle individual. No hace falta duplicar nada en los tick handlers.
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Ck7MasterConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment(
                    "Interruptor general: desactiva TODO el mod de una (los 6 modulos), sin tener que",
                    "apagar cada uno por separado. Los toggles individuales de cada modulo se respetan",
                    "igual: para que un modulo funcione, hacen falta AMBOS -este interruptor general Y",
                    "el propio del modulo- en 'activado'."
            )
            .define("enabled", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile boolean enabled;

    private Ck7MasterConfig() {
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
     * no dispara ModConfigEvent.Reloading (eso depende de un file watcher asincrono que en Windows
     * puede no detectar el guardado atomico "escribir temp + rename" a tiempo, o nunca). Por eso
     * la pantalla de Cloth Config llama esto explicitamente despues de guardar, para que el toggle
     * tenga efecto inmediato en vez de depender de esa recarga asincrona.
     */
    public static void refresh() {
        enabled = ENABLED.get();
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
