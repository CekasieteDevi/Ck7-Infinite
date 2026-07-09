package com.ck7infinite.mobfreeze;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;
import java.util.Set;

import static com.ck7infinite.Ck7Infinite.MODID;

/**
 * Config server-side del modulo Mob AI Freeze. Se guarda en world/serverconfig/ck7infinite-mobfreeze.toml
 * y NO se sincroniza al cliente (no hace falta: el freeze solo corre en el logical server).
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MobFreezeConfig {

    // "namespace:path" con el charset que usa vanilla para ResourceLocation.
    private static final String RESOURCE_LOCATION_REGEX = "[a-z0-9_.-]+:[a-z0-9_./-]+";
    private static final String MODID_REGEX = "[a-z][a-z0-9_-]{0,63}";

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Activa o desactiva por completo el modulo de Mob AI Freeze.")
            .define("enabled", true);

    private static final ForgeConfigSpec.DoubleValue FREEZE_RADIUS = BUILDER
            .comment(
                    "Radio (en bloques) alrededor de cada jugador dentro del cual los mobs hostiles",
                    "mantienen su IA activa. Fuera de este radio se congela pathfinding/goal selector.",
                    "Es un radio propio del mod, independiente de la simulation distance de vanilla."
            )
            .defineInRange("freezeRadiusBlocks", 48.0, 8.0, 1024.0);

    private static final ForgeConfigSpec.IntValue EVALUATION_INTERVAL_TICKS = BUILDER
            .comment(
                    "Cada cuantos ticks se reevalua la distancia jugador-mob para cada mob rastreado.",
                    "Valores mas altos = menos overhead pero reaccion mas lenta al freeze/unfreeze."
            )
            .defineInRange("evaluationIntervalTicks", 10, 1, 200);

    private static final ForgeConfigSpec.IntValue SPAWN_GRACE_TICKS = BUILDER
            .comment(
                    "Ticks de gracia despues de que un mob aparece antes de que sea elegible para",
                    "freeze. Evita congelarlos apenas nacen en spawners densos, antes de que la IA",
                    "tenga chance de dispersarlos (mobs quietos y apilados mueren mas facil por",
                    "'entity cramming' que mobs que ya se movieron un poco)."
            )
            .defineInRange("spawnGraceTicks", 60, 0, 1200);

    private static final ForgeConfigSpec.BooleanValue REQUIRE_HOSTILE = BUILDER
            .comment("Si es true, solo se gestionan mobs que implementan la interfaz vanilla 'Enemy' (hostiles).")
            .define("requireHostile", true);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_TYPE_WHITELIST = BUILDER
            .comment(
                    "Whitelist de entity types (registry name, ej: 'minecraft:zombie').",
                    "Si NO esta vacia, SOLO estos tipos son elegibles para freeze (todo lo demas queda excluido).",
                    "Dejar vacia para que aplique a todos los hostiles (sujeto a entityTypeBlacklist)."
            )
            .defineListAllowEmpty("entityTypeWhitelist", List.of(), MobFreezeConfig::isValidResourceLocation);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_TYPE_BLACKLIST = BUILDER
            .comment(
                    "Entity types que NUNCA se congelan, aunque cumplan el resto de condiciones.",
                    "Util para mobs con mecanicas especiales (ej: 'minecraft:warden')."
            )
            .defineListAllowEmpty("entityTypeBlacklist", List.of("minecraft:warden"), MobFreezeConfig::isValidResourceLocation);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MODID_WHITELIST = BUILDER
            .comment(
                    "Whitelist de mod ids (namespace del entity type, ej: 'minecraft', 'alexsmobs').",
                    "Si NO esta vacia, SOLO mobs de estos mods son elegibles para freeze.",
                    "Dejar vacia para que aplique a todos los mods (sujeto a modIdBlacklist)."
            )
            .defineListAllowEmpty("modIdWhitelist", List.of(), MobFreezeConfig::isValidModId);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MODID_BLACKLIST = BUILDER
            .comment(
                    "Mod ids cuyos mobs NUNCA se congelan. Pensado para excluir mods con IA propia",
                    "o mods de optimizacion que ya gestionan esto (evita pisarse con ellos)."
            )
            .defineListAllowEmpty("modIdBlacklist", List.of(), MobFreezeConfig::isValidModId);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile boolean enabled;
    private static volatile double freezeRadiusBlocks;
    private static volatile int evaluationIntervalTicks;
    private static volatile int spawnGraceTicks;
    private static volatile boolean requireHostile;
    private static volatile Set<String> entityTypeWhitelist = Set.of();
    private static volatile Set<String> entityTypeBlacklist = Set.of();
    private static volatile Set<String> modIdWhitelist = Set.of();
    private static volatile Set<String> modIdBlacklist = Set.of();

    private MobFreezeConfig() {
    }

    private static boolean isValidResourceLocation(Object obj) {
        return obj instanceof String s && s.matches(RESOURCE_LOCATION_REGEX);
    }

    private static boolean isValidModId(Object obj) {
        return obj instanceof String s && s.matches(MODID_REGEX);
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        enabled = ENABLED.get();
        freezeRadiusBlocks = FREEZE_RADIUS.get();
        evaluationIntervalTicks = EVALUATION_INTERVAL_TICKS.get();
        spawnGraceTicks = SPAWN_GRACE_TICKS.get();
        requireHostile = REQUIRE_HOSTILE.get();
        entityTypeWhitelist = Set.copyOf(ENTITY_TYPE_WHITELIST.get());
        entityTypeBlacklist = Set.copyOf(ENTITY_TYPE_BLACKLIST.get());
        modIdWhitelist = Set.copyOf(MODID_WHITELIST.get());
        modIdBlacklist = Set.copyOf(MODID_BLACKLIST.get());
        EntityFreezeClassifier.invalidateCache();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static double freezeRadiusBlocks() {
        return freezeRadiusBlocks;
    }

    public static int evaluationIntervalTicks() {
        return evaluationIntervalTicks;
    }

    public static int spawnGraceTicks() {
        return spawnGraceTicks;
    }

    public static boolean requireHostile() {
        return requireHostile;
    }

    public static Set<String> entityTypeWhitelist() {
        return entityTypeWhitelist;
    }

    public static Set<String> entityTypeBlacklist() {
        return entityTypeBlacklist;
    }

    public static Set<String> modIdWhitelist() {
        return modIdWhitelist;
    }

    public static Set<String> modIdBlacklist() {
        return modIdBlacklist;
    }
}
