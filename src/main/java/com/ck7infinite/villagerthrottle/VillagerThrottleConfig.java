package com.ck7infinite.villagerthrottle;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import static com.ck7infinite.Ck7Infinite.MODID;

/**
 * Config server-side del modulo Villager Throttling. Se guarda en
 * world/serverconfig/ck7infinite-villagerthrottle.toml y NO se sincroniza al cliente.
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VillagerThrottleConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Activa o desactiva por completo el modulo de Villager Throttling.")
            .define("enabled", true);

    private static final ForgeConfigSpec.DoubleValue THROTTLE_RADIUS = BUILDER
            .comment(
                    "Radio (en bloques) alrededor de cada jugador dentro del cual los aldeanos",
                    "mantienen su IA activa (pathfinding, lookup de trading/POI, brain). Fuera de",
                    "este radio se congelan. Pensado para villager halls grandes."
            )
            .defineInRange("throttleRadiusBlocks", 32.0, 4.0, 512.0);

    private static final ForgeConfigSpec.IntValue EVALUATION_INTERVAL_TICKS = BUILDER
            .comment(
                    "Cada cuantos ticks se reevalua la distancia jugador-aldeano para cada aldeano rastreado.",
                    "Valores mas altos = menos overhead pero reaccion mas lenta al freeze/unfreeze."
            )
            .defineInRange("evaluationIntervalTicks", 10, 1, 200);

    private static final ForgeConfigSpec.BooleanValue INCLUDE_WANDERING_TRADER = BUILDER
            .comment("Si es true, tambien gestiona Wandering Traders ademas de Villagers normales.")
            .define("includeWanderingTrader", false);

    private static final ForgeConfigSpec.BooleanValue UNFREEZE_ON_INTERACT = BUILDER
            .comment(
                    "Si es true, un aldeano se descongela de inmediato cuando un jugador lo interactua",
                    "(click derecho), incluso si sigue fuera del radio, para que el trading/restock",
                    "respondan al instante en vez de esperar al proximo tick de evaluacion."
            )
            .define("unfreezeOnInteract", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile boolean enabled;
    private static volatile double throttleRadiusBlocks;
    private static volatile int evaluationIntervalTicks;
    private static volatile boolean includeWanderingTrader;
    private static volatile boolean unfreezeOnInteract;

    private VillagerThrottleConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        enabled = ENABLED.get();
        throttleRadiusBlocks = THROTTLE_RADIUS.get();
        evaluationIntervalTicks = EVALUATION_INTERVAL_TICKS.get();
        includeWanderingTrader = INCLUDE_WANDERING_TRADER.get();
        unfreezeOnInteract = UNFREEZE_ON_INTERACT.get();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static double throttleRadiusBlocks() {
        return throttleRadiusBlocks;
    }

    public static int evaluationIntervalTicks() {
        return evaluationIntervalTicks;
    }

    public static boolean includeWanderingTrader() {
        return includeWanderingTrader;
    }

    public static boolean unfreezeOnInteract() {
        return unfreezeOnInteract;
    }
}
