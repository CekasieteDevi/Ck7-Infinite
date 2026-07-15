package com.ck7infinite.ultraload;

import com.ck7infinite.Ck7MasterConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import static com.ck7infinite.Ck7Infinite.MODID;

/**
 * Config COMMON del modulo Ultra Carga (Ultra Load).
 * <p>
 * "Modo panico" enfocado en la carga de chunks: cuando se detecta que se estan generando/cargando
 * chunks fuerte (mas de activationChunksPerSecond por segundo), apaga temporalmente todo lo que no
 * ayuda a que los chunks aparezcan -IA de mobs, spawns, random ticks, particulas, render de
 * entidades- para liberar CPU (a los hilos de worldgen/mallado) y GPU (al render del terreno). Al
 * bajar la carga, restaura todo tras un cooldown (para no parpadear). Cada sacrificio tiene su
 * propio toggle. Ver UltraLoadServerHandler / UltraLoadClientHandler.
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class UltraLoadConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Activa o desactiva por completo el modo Ultra Carga.")
            .define("enabled", true);

    public static final ForgeConfigSpec.IntValue ACTIVATION_CHUNKS_PER_SECOND = BUILDER
            .comment(
                    "Cuantos chunks por segundo deben cargarse/generarse para activar Ultra Carga.",
                    "Mas bajo = se activa mas facil (con poco movimiento); mas alto = solo con",
                    "exploracion intensa (volar/correr generando terreno nuevo)."
            )
            .defineInRange("activationChunksPerSecond", 8, 1, 500);

    public static final ForgeConfigSpec.DoubleValue SAFE_RADIUS_BLOCKS = BUILDER
            .comment(
                    "Radio (en bloques) alrededor de CADA jugador donde los sacrificios que afectan",
                    "entidades (congelar todos los mobs, congelar entidades, ocultar entidades) NO se",
                    "aplican, aunque Ultra Carga este activo. Evita el 'susto': un enemigo pegado a vos",
                    "que desaparece/se queda quieto y reaparece de golpe al terminar la carga. Dentro",
                    "de este radio siguen valiendo las reglas normales de cada modulo (distancia/vision)."
            )
            .defineInRange("safeRadiusBlocks", 20.0, 0.0, 256.0);

    public static final ForgeConfigSpec.IntValue COOLDOWN_TICKS = BUILDER
            .comment(
                    "Ticks que Ultra Carga sigue activo despues de que la carga de chunks baja del",
                    "umbral. Evita que prenda/apague en cada micro-pausa (parpadeo). 40 = 2 segundos."
            )
            .defineInRange("cooldownTicks", 40, 0, 400);

    public static final ForgeConfigSpec.BooleanValue FREEZE_ALL_MOBS = BUILDER
            .comment(
                    "Mientras Ultra Carga esta activo, congela la IA de TODOS los mobs (sin importar",
                    "distancia ni linea de vision). Requiere que el modulo Mob AI Freeze este activo.",
                    "Es lo que mas CPU libera."
            )
            .define("freezeAllMobs", true);

    public static final ForgeConfigSpec.BooleanValue PAUSE_SPAWNS = BUILDER
            .comment("Mientras Ultra Carga esta activo, cancela los spawns naturales de mobs.")
            .define("pauseSpawns", true);

    public static final ForgeConfigSpec.BooleanValue PAUSE_RANDOM_TICKS = BUILDER
            .comment(
                    "Mientras Ultra Carga esta activo, pausa los random ticks (crecimiento de",
                    "cultivos/pasto/hojas, fuego, hielo) poniendo randomTickSpeed en 0 y",
                    "restaurandolo despues. Se restaura al desactivarse y al cerrar el servidor."
            )
            .define("pauseRandomTicks", true);

    public static final ForgeConfigSpec.BooleanValue FREEZE_ENTITIES = BUILDER
            .comment(
                    "TIER 2 (agresivo). Mientras Ultra Carga esta activo, congela por completo el tick",
                    "de las entidades -no solo la IA, tambien movimiento/fisica/base tick-, como el",
                    "pause de singleplayer. Libera mucha mas CPU que solo el freeze de IA. No afecta al",
                    "jugador ni a lo que este montando. Retoman al terminar la carga."
            )
            .define("freezeEntities", true);

    public static final ForgeConfigSpec.BooleanValue FREEZE_BLOCK_ENTITIES = BUILDER
            .comment(
                    "TIER 2 (agresivo). Mientras Ultra Carga esta activo, pausa el tick de todos los",
                    "block entities (hoppers, hornos, spawners...). Inofensivo en las rafagas cortas de",
                    "carga; si tenes granjas sensibles al timing y notas hipos, ponelo en false."
            )
            .define("freezeBlockEntities", true);

    public static final ForgeConfigSpec.BooleanValue HIDE_PARTICLES = BUILDER
            .comment("Mientras Ultra Carga esta activo, descarta TODAS las particulas (libera CPU y GPU).")
            .define("hideParticles", true);

    public static final ForgeConfigSpec.BooleanValue HIDE_ENTITIES = BUILDER
            .comment(
                    "Mientras Ultra Carga esta activo, no renderiza las entidades vivas (mobs, otros",
                    "jugadores). Libera GPU para dibujar el terreno nuevo. Reaparecen al terminar."
            )
            .define("hideEntities", true);

    public static final ForgeConfigSpec.BooleanValue PAUSE_SCHEDULED_TICKS = BUILDER
            .comment(
                    "TIER 3 (experimental). Mientras Ultra Carga esta activo, pausa los ticks",
                    "PROGRAMADOS (redstone, flujo de agua/lava; distinto de los random ticks de pasto/",
                    "cultivos, que ya se pausan aparte). Nada se pierde: los ticks pendientes quedan en",
                    "cola y se procesan apenas termina la carga -mismo mecanismo seguro que",
                    "pauseRandomTicks. El costo real: relojes de redstone/pistones se detienen y el",
                    "agua/lava no fluye mientras dura la rafaga de carga."
            )
            .define("pauseScheduledTicks", true);

    public static final ForgeConfigSpec.BooleanValue REDUCE_ENTITY_DISTANCE = BUILDER
            .comment(
                    "TIER 3 (experimental). Mientras Ultra Carga esta activo, baja temporalmente",
                    "Options#entityDistanceScaling (cuan lejos se dibujan las entidades) y lo restaura",
                    "al terminar. A diferencia del render distance -que se probo y se saco por forzar",
                    "a Sodium/Embeddium a re-transmitir chunks-, esta opcion no dispara ninguna",
                    "reconstruccion (callback de cambio vacio en vanilla): es pura ganancia de GPU sin",
                    "tocar el pipeline de chunks."
            )
            .define("reduceEntityDistance", true);

    public static final ForgeConfigSpec.DoubleValue ENTITY_DISTANCE_DURING_LOAD = BUILDER
            .comment(
                    "Multiplicador de distancia de entidades a usar mientras Ultra Carga esta activo.",
                    "Se recorta al valor configurado por el jugador (nunca lo sube). Solo aplica si",
                    "reduceEntityDistance=true."
            )
            .defineInRange("entityDistanceDuringLoad", 0.5, 0.5, 5.0);

    public static final ForgeConfigSpec.BooleanValue LIMIT_FRAMERATE_DURING_LOAD = BUILDER
            .comment(
                    "TIER 3 (experimental). Mientras Ultra Carga esta activo, baja temporalmente el",
                    "limite de FPS (Options#framerateLimit, la misma opcion del menu de Video) y lo",
                    "restaura al terminar. Contraintuitivo pero real en CPUs de pocos nucleos: cada",
                    "frame que el hilo de render NO dibuja es CPU que le queda libre a los hilos de",
                    "generacion/mallado de chunks."
            )
            .define("limitFramerateDuringLoad", true);

    public static final ForgeConfigSpec.IntValue FRAMERATE_LIMIT_DURING_LOAD = BUILDER
            .comment(
                    "Limite de FPS a usar mientras Ultra Carga esta activo. Se recorta al limite",
                    "configurado por el jugador (nunca lo sube). Solo aplica si",
                    "limitFramerateDuringLoad=true."
            )
            .defineInRange("framerateLimitDuringLoad", 30, 10, 260);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile boolean enabled;
    private static volatile int activationChunksPerSecond;
    private static volatile double safeRadiusBlocks;
    private static volatile int cooldownTicks;
    private static volatile boolean freezeAllMobs;
    private static volatile boolean pauseSpawns;
    private static volatile boolean pauseRandomTicks;
    private static volatile boolean freezeEntities;
    private static volatile boolean freezeBlockEntities;
    private static volatile boolean hideParticles;
    private static volatile boolean hideEntities;
    private static volatile boolean pauseScheduledTicks;
    private static volatile boolean reduceEntityDistance;
    private static volatile double entityDistanceDuringLoad;
    private static volatile boolean limitFramerateDuringLoad;
    private static volatile int framerateLimitDuringLoad;

    private UltraLoadConfig() {
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
     * despues de guardar, para que los toggles tengan efecto inmediato.
     */
    public static void refresh() {
        enabled = ENABLED.get();
        activationChunksPerSecond = ACTIVATION_CHUNKS_PER_SECOND.get();
        safeRadiusBlocks = SAFE_RADIUS_BLOCKS.get();
        cooldownTicks = COOLDOWN_TICKS.get();
        freezeAllMobs = FREEZE_ALL_MOBS.get();
        pauseSpawns = PAUSE_SPAWNS.get();
        pauseRandomTicks = PAUSE_RANDOM_TICKS.get();
        freezeEntities = FREEZE_ENTITIES.get();
        freezeBlockEntities = FREEZE_BLOCK_ENTITIES.get();
        hideParticles = HIDE_PARTICLES.get();
        hideEntities = HIDE_ENTITIES.get();
        pauseScheduledTicks = PAUSE_SCHEDULED_TICKS.get();
        reduceEntityDistance = REDUCE_ENTITY_DISTANCE.get();
        entityDistanceDuringLoad = ENTITY_DISTANCE_DURING_LOAD.get();
        limitFramerateDuringLoad = LIMIT_FRAMERATE_DURING_LOAD.get();
        framerateLimitDuringLoad = FRAMERATE_LIMIT_DURING_LOAD.get();
    }

    public static boolean isEnabled() {
        return enabled && Ck7MasterConfig.isEnabled();
    }

    public static int activationChunksPerSecond() {
        return activationChunksPerSecond;
    }

    public static double safeRadiusBlocks() {
        return safeRadiusBlocks;
    }

    public static int cooldownTicks() {
        return cooldownTicks;
    }

    public static boolean freezeAllMobs() {
        return freezeAllMobs;
    }

    public static boolean pauseSpawns() {
        return pauseSpawns;
    }

    public static boolean pauseRandomTicks() {
        return pauseRandomTicks;
    }

    public static boolean freezeEntities() {
        return freezeEntities;
    }

    public static boolean freezeBlockEntities() {
        return freezeBlockEntities;
    }

    public static boolean hideParticles() {
        return hideParticles;
    }

    public static boolean hideEntities() {
        return hideEntities;
    }

    public static boolean pauseScheduledTicks() {
        return pauseScheduledTicks;
    }

    public static boolean reduceEntityDistance() {
        return reduceEntityDistance;
    }

    public static double entityDistanceDuringLoad() {
        return entityDistanceDuringLoad;
    }

    public static boolean limitFramerateDuringLoad() {
        return limitFramerateDuringLoad;
    }

    public static int framerateLimitDuringLoad() {
        return framerateLimitDuringLoad;
    }
}
