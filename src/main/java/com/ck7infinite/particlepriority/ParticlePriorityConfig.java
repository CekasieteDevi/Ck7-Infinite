package com.ck7infinite.particlepriority;

import com.ck7infinite.Ck7MasterConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;
import java.util.Set;

import static com.ck7infinite.Ck7Infinite.MODID;

/**
 * Config CLIENT-side del modulo Particulas por Prioridad. Es una config de cliente (no server)
 * porque la decision de que partícula renderizar es puramente visual/local a cada jugador.
 * <p>
 * No importa ninguna clase de net.minecraft.client a proposito: esta clase se registra desde el
 * constructor comun del mod (corre en ambos sides), y solo las clases que realmente tocan
 * renderizado (mixin, contexto de jugador) quedan detras de un gate de Dist.CLIENT.
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ParticlePriorityConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Activa o desactiva por completo el modulo de Particulas por Prioridad.")
            .define("enabled", true);

    public static final ForgeConfigSpec.DoubleValue COMBAT_RADIUS = BUILDER
            .comment("Radio (bloques) dentro del cual las particulas de combate se muestran siempre.")
            .defineInRange("combatRadiusBlocks", 48.0, 4.0, 256.0);

    public static final ForgeConfigSpec.DoubleValue INTERACTION_RADIUS = BUILDER
            .comment("Radio (bloques) dentro del cual las particulas de interaccion/crafteo se muestran siempre.")
            .defineInRange("interactionRadiusBlocks", 24.0, 4.0, 256.0);

    public static final ForgeConfigSpec.DoubleValue REDSTONE_RADIUS = BUILDER
            .comment("Radio (bloques) dentro del cual las particulas de redstone se muestran siempre.")
            .defineInRange("redstoneRadiusBlocks", 24.0, 4.0, 256.0);

    public static final ForgeConfigSpec.DoubleValue AMBIENT_RADIUS = BUILDER
            .comment(
                    "Radio (bloques) dentro del cual las particulas ambientales (lluvia, goteo,",
                    "esporas, portal, etc.) se muestran a tasa completa. Mas alla de este radio",
                    "se descartan salvo que esten dentro de ambientCloseRadiusBlocks o en el cono de vision."
            )
            .defineInRange("ambientRadiusBlocks", 16.0, 2.0, 128.0);

    public static final ForgeConfigSpec.DoubleValue AMBIENT_CLOSE_RADIUS = BUILDER
            .comment(
                    "Radio (bloques) muy cercano al jugador donde las particulas ambientales se",
                    "muestran igual aunque esten fuera del cono de vision (evita que algo pegado",
                    "a la camara desaparezca de forma antinatural)."
            )
            .defineInRange("ambientCloseRadiusBlocks", 8.0, 1.0, 64.0);

    public static final ForgeConfigSpec.DoubleValue VIEW_CONE_DOT_THRESHOLD = BUILDER
            .comment(
                    "Umbral del producto punto entre la direccion de camara y el vector hacia la",
                    "particula. 0.0 = cono de 180 grados (todo el hemisferio delantero). Valores",
                    "mas altos (hasta 1.0) acortan el cono; valores negativos lo agrandan."
            )
            .defineInRange("viewConeDotThreshold", 0.0, -1.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue CONTEXT_RADIUS = BUILDER
            .comment(
                    "Radio (bloques) alrededor de la ultima accion del jugador (romper/colocar/usar",
                    "bloque, atacar) dentro del cual las particulas se tratan como maxima prioridad."
            )
            .defineInRange("contextRadiusBlocks", 6.0, 1.0, 64.0);

    public static final ForgeConfigSpec.IntValue CONTEXT_RECENCY_TICKS = BUILDER
            .comment("Cuantos ticks despues de una accion del jugador sigue aplicando el bonus de contexto.")
            .defineInRange("contextRecencyTicks", 40, 0, 1200);

    public static final ForgeConfigSpec.BooleanValue AMBIENT_COVERAGE_BUDGET_ENABLED = BUILDER
            .comment(
                    "Cap de particulas AMBIENT por presupuesto de cobertura de pantalla estimada",
                    "(no por conteo plano): una particula muy cerca de la camara pesa mas contra el",
                    "presupuesto que una lejana. Solo afecta particulas ambientales que ya pasaron el",
                    "filtro de distancia/cono de arriba y no estan ligadas a tu ultima accion",
                    "(romper/colocar/atacar) -esas siguen siendo prioridad maxima."
            )
            .define("ambientCoverageBudgetEnabled", true);

    public static final ForgeConfigSpec.DoubleValue AMBIENT_COVERAGE_BUDGET = BUILDER
            .comment(
                    "Presupuesto de cobertura por frame, en unidades de 'particulas equivalentes a",
                    "la distancia de ambientCloseRadiusBlocks'. Una particula a esa distancia cuesta",
                    "~1.0; mas cerca cuesta mas (hasta 4x), mas lejos cuesta menos. Solo aplica si",
                    "ambientCoverageBudgetEnabled=true."
            )
            .defineInRange("ambientCoverageBudget", 30.0, 1.0, 500.0);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COMBAT_PARTICLE_IDS = BUILDER
            .comment("IDs (path, sin namespace) de ParticleType clasificados como combate.")
            .defineListAllowEmpty(
                    "combatParticleIds",
                    List.of("crit", "enchanted_hit", "sweep_attack", "damage_indicator"),
                    obj -> obj instanceof String
            );

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> INTERACTION_PARTICLE_IDS = BUILDER
            .comment("IDs (path, sin namespace) de ParticleType clasificados como interaccion/crafteo.")
            .defineListAllowEmpty(
                    "interactionParticleIds",
                    List.of("happy_villager", "enchant", "note", "heart", "smoke"),
                    obj -> obj instanceof String
            );

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REDSTONE_PARTICLE_IDS = BUILDER
            .comment("IDs (path, sin namespace) de ParticleType clasificados como redstone.")
            .defineListAllowEmpty(
                    "redstoneParticleIds",
                    List.of("dust", "dust_color_transition", "dust_pillar"),
                    obj -> obj instanceof String
            );

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static volatile boolean enabled;
    private static volatile double combatRadiusBlocks;
    private static volatile double interactionRadiusBlocks;
    private static volatile double redstoneRadiusBlocks;
    private static volatile double ambientRadiusBlocks;
    private static volatile double ambientCloseRadiusBlocks;
    private static volatile double viewConeDotThreshold;
    private static volatile double contextRadiusBlocks;
    private static volatile int contextRecencyTicks;
    private static volatile boolean ambientCoverageBudgetEnabled;
    private static volatile double ambientCoverageBudget;
    private static volatile Set<String> combatParticleIds = Set.of();
    private static volatile Set<String> interactionParticleIds = Set.of();
    private static volatile Set<String> redstoneParticleIds = Set.of();

    private ParticlePriorityConfig() {
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
        combatRadiusBlocks = COMBAT_RADIUS.get();
        interactionRadiusBlocks = INTERACTION_RADIUS.get();
        redstoneRadiusBlocks = REDSTONE_RADIUS.get();
        ambientRadiusBlocks = AMBIENT_RADIUS.get();
        ambientCloseRadiusBlocks = AMBIENT_CLOSE_RADIUS.get();
        viewConeDotThreshold = VIEW_CONE_DOT_THRESHOLD.get();
        contextRadiusBlocks = CONTEXT_RADIUS.get();
        contextRecencyTicks = CONTEXT_RECENCY_TICKS.get();
        ambientCoverageBudgetEnabled = AMBIENT_COVERAGE_BUDGET_ENABLED.get();
        ambientCoverageBudget = AMBIENT_COVERAGE_BUDGET.get();
        combatParticleIds = Set.copyOf(COMBAT_PARTICLE_IDS.get());
        interactionParticleIds = Set.copyOf(INTERACTION_PARTICLE_IDS.get());
        redstoneParticleIds = Set.copyOf(REDSTONE_PARTICLE_IDS.get());
        ParticleClassifier.invalidateCache();
    }

    public static boolean isEnabled() {
        return enabled && Ck7MasterConfig.isEnabled();
    }

    public static double combatRadiusBlocks() {
        return combatRadiusBlocks;
    }

    public static double interactionRadiusBlocks() {
        return interactionRadiusBlocks;
    }

    public static double redstoneRadiusBlocks() {
        return redstoneRadiusBlocks;
    }

    public static double ambientRadiusBlocks() {
        return ambientRadiusBlocks;
    }

    public static double ambientCloseRadiusBlocks() {
        return ambientCloseRadiusBlocks;
    }

    public static double viewConeDotThreshold() {
        return viewConeDotThreshold;
    }

    public static double contextRadiusBlocks() {
        return contextRadiusBlocks;
    }

    public static int contextRecencyTicks() {
        return contextRecencyTicks;
    }

    public static boolean ambientCoverageBudgetEnabled() {
        return ambientCoverageBudgetEnabled;
    }

    public static double ambientCoverageBudget() {
        return ambientCoverageBudget;
    }

    public static Set<String> combatParticleIds() {
        return combatParticleIds;
    }

    public static Set<String> interactionParticleIds() {
        return interactionParticleIds;
    }

    public static Set<String> redstoneParticleIds() {
        return redstoneParticleIds;
    }
}
