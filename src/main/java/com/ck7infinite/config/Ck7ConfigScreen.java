package com.ck7infinite.config;

import com.ck7infinite.Ck7MasterConfig;
import com.ck7infinite.mobfreeze.MobFreezeConfig;
import com.ck7infinite.particlepriority.ParticlePriorityConfig;
import com.ck7infinite.resscale.ResScaleConfig;
import com.ck7infinite.simdistance.SimDistanceConfig;
import com.ck7infinite.ultraload.UltraLoadConfig;
import com.ck7infinite.villagerthrottle.VillagerThrottleConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de configuracion in-game de CK7, construida con Cloth Config y accesible desde
 * Mods -> Ck7 - Conatus -> Config. Hace de puente entre la GUI y los ForgeConfigSpec: cada
 * control lee el valor actual del ConfigValue y, al guardar, lo escribe de vuelta; el
 * setSavingRunnable persiste los SPEC a disco, y cada .refresh() se llama a mano para efecto
 * inmediato (ver nota en el runnable).
 * <p>
 * Todos los textos son claves de traduccion (assets/ck7infinite/lang/{en_us,es_es,pt_br}.json),
 * no literales. Los nombres/valores de campo (name, tooltip) que reciben los helpers add* son
 * claves de traduccion, no el texto en si.
 * <p>
 * Esta clase importa Cloth Config y clases de cliente, asi que SOLO se referencia (y por ende se
 * carga) cuando el mod corre en cliente fisico y Cloth Config esta presente (ver Ck7Infinite).
 */
public final class Ck7ConfigScreen {
    private Ck7ConfigScreen() {}

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> build(parent))
        );
    }

    public static Screen build(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Ck7 - Conatus"));
        builder.setTransparentBackground(false);
        ConfigEntryBuilder eb = builder.entryBuilder();

        buildGeneral(builder, eb);
        buildUltraLoad(builder, eb);
        buildMobFreeze(builder, eb);
        buildVillagerThrottle(builder, eb);
        buildParticlePriority(builder, eb);
        buildResScale(builder, eb);
        buildSimDistance(builder, eb);

        builder.setSavingRunnable(() -> {
            // SPEC.save() solo escribe el archivo a disco; NO dispara ModConfigEvent.Reloading
            // (eso depende de un file watcher asincrono que en Windows puede fallar/demorarse con
            // el guardado atomico). Por eso cada .refresh() se llama a mano, aca mismo, para que
            // los toggles tengan efecto inmediato sin depender de esa recarga asincrona.
            Ck7MasterConfig.SPEC.save();
            Ck7MasterConfig.refresh();
            UltraLoadConfig.SPEC.save();
            UltraLoadConfig.refresh();
            MobFreezeConfig.SPEC.save();
            MobFreezeConfig.refresh();
            VillagerThrottleConfig.SPEC.save();
            VillagerThrottleConfig.refresh();
            ParticlePriorityConfig.SPEC.save();
            ParticlePriorityConfig.refresh();
            ResScaleConfig.SPEC.save();
            ResScaleConfig.refresh();
            SimDistanceConfig.SPEC.save();
            SimDistanceConfig.refresh();
            com.ck7infinite.sodiumwarning.SodiumWarningConfig.SPEC.save();
            com.ck7infinite.potatoprofile.PotatoProfileConfig.SPEC.save();
        });

        return builder.build();
    }

    private static void buildGeneral(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory c = b.getOrCreateCategory(Component.translatable("ck7infinite.config.category.general"));
        addBool(c, eb, "ck7infinite.config.general.enabled", Ck7MasterConfig.ENABLED, true,
                "ck7infinite.config.general.enabled.tooltip");
        addBool(c, eb, "ck7infinite.config.general.sodium_warning_dismissed",
                com.ck7infinite.sodiumwarning.SodiumWarningConfig.DISMISSED, false,
                "ck7infinite.config.general.sodium_warning_dismissed.tooltip");
        addBool(c, eb, "ck7infinite.config.general.potato_profile_dismissed",
                com.ck7infinite.potatoprofile.PotatoProfileConfig.DISMISSED, false,
                "ck7infinite.config.general.potato_profile_dismissed.tooltip");
    }

    private static void buildUltraLoad(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory c = b.getOrCreateCategory(Component.translatable("ck7infinite.config.category.ultraload"));
        addBool(c, eb, "ck7infinite.config.ultraload.enabled", UltraLoadConfig.ENABLED, true,
                "ck7infinite.config.ultraload.enabled.tooltip");
        addInt(c, eb, "ck7infinite.config.ultraload.activation_rate", UltraLoadConfig.ACTIVATION_CHUNKS_PER_SECOND, 8, 1, 500,
                "ck7infinite.config.ultraload.activation_rate.tooltip");
        addDouble(c, eb, "ck7infinite.config.ultraload.safe_radius", UltraLoadConfig.SAFE_RADIUS_BLOCKS, 20.0, 0.0, 256.0,
                "ck7infinite.config.ultraload.safe_radius.tooltip");
        addInt(c, eb, "ck7infinite.config.ultraload.cooldown", UltraLoadConfig.COOLDOWN_TICKS, 40, 0, 400,
                "ck7infinite.config.ultraload.cooldown.tooltip");

        addSeparator(c, eb, "ck7infinite.config.ultraload.tier1");
        addBool(c, eb, "ck7infinite.config.ultraload.freeze_all_mobs", UltraLoadConfig.FREEZE_ALL_MOBS, true,
                "ck7infinite.config.ultraload.freeze_all_mobs.tooltip");
        addBool(c, eb, "ck7infinite.config.ultraload.pause_spawns", UltraLoadConfig.PAUSE_SPAWNS, true,
                "ck7infinite.config.ultraload.pause_spawns.tooltip");
        addBool(c, eb, "ck7infinite.config.ultraload.pause_random_ticks", UltraLoadConfig.PAUSE_RANDOM_TICKS, true,
                "ck7infinite.config.ultraload.pause_random_ticks.tooltip");
        addBool(c, eb, "ck7infinite.config.ultraload.hide_particles", UltraLoadConfig.HIDE_PARTICLES, true,
                "ck7infinite.config.ultraload.hide_particles.tooltip");
        addBool(c, eb, "ck7infinite.config.ultraload.hide_entities", UltraLoadConfig.HIDE_ENTITIES, true,
                "ck7infinite.config.ultraload.hide_entities.tooltip");

        addSeparator(c, eb, "ck7infinite.config.ultraload.tier2");
        addBool(c, eb, "ck7infinite.config.ultraload.freeze_entities", UltraLoadConfig.FREEZE_ENTITIES, true,
                "ck7infinite.config.ultraload.freeze_entities.tooltip");
        addBool(c, eb, "ck7infinite.config.ultraload.freeze_block_entities", UltraLoadConfig.FREEZE_BLOCK_ENTITIES, true,
                "ck7infinite.config.ultraload.freeze_block_entities.tooltip");

        addSeparator(c, eb, "ck7infinite.config.ultraload.tier3");
        addBool(c, eb, "ck7infinite.config.ultraload.pause_scheduled_ticks", UltraLoadConfig.PAUSE_SCHEDULED_TICKS, true,
                "ck7infinite.config.ultraload.pause_scheduled_ticks.tooltip");
        addBool(c, eb, "ck7infinite.config.ultraload.reduce_entity_distance", UltraLoadConfig.REDUCE_ENTITY_DISTANCE, true,
                "ck7infinite.config.ultraload.reduce_entity_distance.tooltip");
        addDouble(c, eb, "ck7infinite.config.ultraload.entity_distance_during_load", UltraLoadConfig.ENTITY_DISTANCE_DURING_LOAD, 0.5, 0.5, 5.0,
                "ck7infinite.config.ultraload.entity_distance_during_load.tooltip");
        addBool(c, eb, "ck7infinite.config.ultraload.limit_framerate", UltraLoadConfig.LIMIT_FRAMERATE_DURING_LOAD, true,
                "ck7infinite.config.ultraload.limit_framerate.tooltip");
        addInt(c, eb, "ck7infinite.config.ultraload.framerate_limit_value", UltraLoadConfig.FRAMERATE_LIMIT_DURING_LOAD, 30, 10, 260,
                "ck7infinite.config.ultraload.framerate_limit_value.tooltip");
    }

    private static void buildMobFreeze(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory c = b.getOrCreateCategory(Component.translatable("ck7infinite.config.category.mobfreeze"));
        addBool(c, eb, "ck7infinite.config.mobfreeze.enabled", MobFreezeConfig.ENABLED, true,
                "ck7infinite.config.mobfreeze.enabled.tooltip");
        addDouble(c, eb, "ck7infinite.config.mobfreeze.freeze_radius", MobFreezeConfig.FREEZE_RADIUS, 48.0, 8.0, 1024.0,
                "ck7infinite.config.mobfreeze.freeze_radius.tooltip");
        addBool(c, eb, "ck7infinite.config.mobfreeze.require_los", MobFreezeConfig.REQUIRE_LINE_OF_SIGHT, true,
                "ck7infinite.config.mobfreeze.require_los.tooltip");
        addDouble(c, eb, "ck7infinite.config.mobfreeze.always_active_radius", MobFreezeConfig.ALWAYS_ACTIVE_RADIUS, 16.0, 0.0, 1024.0,
                "ck7infinite.config.mobfreeze.always_active_radius.tooltip");
        addInt(c, eb, "ck7infinite.config.mobfreeze.eval_interval", MobFreezeConfig.EVALUATION_INTERVAL_TICKS, 10, 1, 200,
                "ck7infinite.config.mobfreeze.eval_interval.tooltip");
        addInt(c, eb, "ck7infinite.config.mobfreeze.spawn_grace", MobFreezeConfig.SPAWN_GRACE_TICKS, 60, 0, 1200,
                "ck7infinite.config.mobfreeze.spawn_grace.tooltip");
        addBool(c, eb, "ck7infinite.config.mobfreeze.ai_lod_enabled", MobFreezeConfig.AI_LOD_ENABLED, true,
                "ck7infinite.config.mobfreeze.ai_lod_enabled.tooltip");
        addDouble(c, eb, "ck7infinite.config.mobfreeze.ai_lod_close_radius", MobFreezeConfig.AI_LOD_CLOSE_RADIUS, 24.0, 0.0, 1024.0,
                "ck7infinite.config.mobfreeze.ai_lod_close_radius.tooltip");
        addInt(c, eb, "ck7infinite.config.mobfreeze.ai_lod_divisor", MobFreezeConfig.AI_LOD_DIVISOR, 8, 2, 40,
                "ck7infinite.config.mobfreeze.ai_lod_divisor.tooltip");
        addBool(c, eb, "ck7infinite.config.mobfreeze.require_hostile", MobFreezeConfig.REQUIRE_HOSTILE, false,
                "ck7infinite.config.mobfreeze.require_hostile.tooltip");
        addList(c, eb, "ck7infinite.config.mobfreeze.type_whitelist", MobFreezeConfig.ENTITY_TYPE_WHITELIST, List.of(),
                "ck7infinite.config.mobfreeze.type_whitelist.tooltip");
        addList(c, eb, "ck7infinite.config.mobfreeze.type_blacklist", MobFreezeConfig.ENTITY_TYPE_BLACKLIST,
                List.of("minecraft:warden", "minecraft:villager"),
                "ck7infinite.config.mobfreeze.type_blacklist.tooltip");
        addList(c, eb, "ck7infinite.config.mobfreeze.modid_whitelist", MobFreezeConfig.MODID_WHITELIST, List.of(),
                "ck7infinite.config.mobfreeze.modid_whitelist.tooltip");
        addList(c, eb, "ck7infinite.config.mobfreeze.modid_blacklist", MobFreezeConfig.MODID_BLACKLIST, List.of(),
                "ck7infinite.config.mobfreeze.modid_blacklist.tooltip");
    }

    private static void buildVillagerThrottle(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory c = b.getOrCreateCategory(Component.translatable("ck7infinite.config.category.villagerthrottle"));
        addBool(c, eb, "ck7infinite.config.villagerthrottle.enabled", VillagerThrottleConfig.ENABLED, true,
                "ck7infinite.config.villagerthrottle.enabled.tooltip");
        addDouble(c, eb, "ck7infinite.config.villagerthrottle.radius", VillagerThrottleConfig.THROTTLE_RADIUS, 32.0, 4.0, 512.0,
                "ck7infinite.config.villagerthrottle.radius.tooltip");
        addInt(c, eb, "ck7infinite.config.villagerthrottle.eval_interval", VillagerThrottleConfig.EVALUATION_INTERVAL_TICKS, 10, 1, 200,
                "ck7infinite.config.villagerthrottle.eval_interval.tooltip");
        addBool(c, eb, "ck7infinite.config.villagerthrottle.include_wandering_trader", VillagerThrottleConfig.INCLUDE_WANDERING_TRADER, false,
                "ck7infinite.config.villagerthrottle.include_wandering_trader.tooltip");
        addBool(c, eb, "ck7infinite.config.villagerthrottle.unfreeze_on_interact", VillagerThrottleConfig.UNFREEZE_ON_INTERACT, true,
                "ck7infinite.config.villagerthrottle.unfreeze_on_interact.tooltip");
    }

    private static void buildParticlePriority(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory c = b.getOrCreateCategory(Component.translatable("ck7infinite.config.category.particlepriority"));
        addBool(c, eb, "ck7infinite.config.particlepriority.enabled", ParticlePriorityConfig.ENABLED, true,
                "ck7infinite.config.particlepriority.enabled.tooltip");
        addDouble(c, eb, "ck7infinite.config.particlepriority.combat_radius", ParticlePriorityConfig.COMBAT_RADIUS, 48.0, 4.0, 256.0,
                "ck7infinite.config.particlepriority.combat_radius.tooltip");
        addDouble(c, eb, "ck7infinite.config.particlepriority.interaction_radius", ParticlePriorityConfig.INTERACTION_RADIUS, 24.0, 4.0, 256.0,
                "ck7infinite.config.particlepriority.interaction_radius.tooltip");
        addDouble(c, eb, "ck7infinite.config.particlepriority.redstone_radius", ParticlePriorityConfig.REDSTONE_RADIUS, 24.0, 4.0, 256.0,
                "ck7infinite.config.particlepriority.redstone_radius.tooltip");
        addDouble(c, eb, "ck7infinite.config.particlepriority.ambient_radius", ParticlePriorityConfig.AMBIENT_RADIUS, 16.0, 2.0, 128.0,
                "ck7infinite.config.particlepriority.ambient_radius.tooltip");
        addDouble(c, eb, "ck7infinite.config.particlepriority.ambient_close_radius", ParticlePriorityConfig.AMBIENT_CLOSE_RADIUS, 8.0, 1.0, 64.0,
                "ck7infinite.config.particlepriority.ambient_close_radius.tooltip");
        addDouble(c, eb, "ck7infinite.config.particlepriority.view_cone_threshold", ParticlePriorityConfig.VIEW_CONE_DOT_THRESHOLD, 0.0, -1.0, 1.0,
                "ck7infinite.config.particlepriority.view_cone_threshold.tooltip");
        addDouble(c, eb, "ck7infinite.config.particlepriority.context_radius", ParticlePriorityConfig.CONTEXT_RADIUS, 6.0, 1.0, 64.0,
                "ck7infinite.config.particlepriority.context_radius.tooltip");
        addInt(c, eb, "ck7infinite.config.particlepriority.context_recency", ParticlePriorityConfig.CONTEXT_RECENCY_TICKS, 40, 0, 1200,
                "ck7infinite.config.particlepriority.context_recency.tooltip");
        addBool(c, eb, "ck7infinite.config.particlepriority.ambient_coverage_budget_enabled",
                ParticlePriorityConfig.AMBIENT_COVERAGE_BUDGET_ENABLED, true,
                "ck7infinite.config.particlepriority.ambient_coverage_budget_enabled.tooltip");
        addDouble(c, eb, "ck7infinite.config.particlepriority.ambient_coverage_budget",
                ParticlePriorityConfig.AMBIENT_COVERAGE_BUDGET, 30.0, 1.0, 500.0,
                "ck7infinite.config.particlepriority.ambient_coverage_budget.tooltip");
        addList(c, eb, "ck7infinite.config.particlepriority.combat_ids", ParticlePriorityConfig.COMBAT_PARTICLE_IDS,
                List.of("crit", "enchanted_hit", "sweep_attack", "damage_indicator"),
                "ck7infinite.config.particlepriority.combat_ids.tooltip");
        addList(c, eb, "ck7infinite.config.particlepriority.interaction_ids", ParticlePriorityConfig.INTERACTION_PARTICLE_IDS,
                List.of("happy_villager", "enchant", "note", "heart", "smoke"),
                "ck7infinite.config.particlepriority.interaction_ids.tooltip");
        addList(c, eb, "ck7infinite.config.particlepriority.redstone_ids", ParticlePriorityConfig.REDSTONE_PARTICLE_IDS,
                List.of("dust", "dust_color_transition", "dust_pillar"),
                "ck7infinite.config.particlepriority.redstone_ids.tooltip");
    }

    private static void buildResScale(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory c = b.getOrCreateCategory(Component.translatable("ck7infinite.config.category.resscale"));
        addBool(c, eb, "ck7infinite.config.resscale.enabled", ResScaleConfig.ENABLED, false,
                "ck7infinite.config.resscale.enabled.tooltip");
        addDouble(c, eb, "ck7infinite.config.resscale.scale", ResScaleConfig.SCALE, 1.0, 0.25, 2.0,
                "ck7infinite.config.resscale.scale.tooltip");
        addBool(c, eb, "ck7infinite.config.resscale.linear_filter", ResScaleConfig.LINEAR_FILTER, true,
                "ck7infinite.config.resscale.linear_filter.tooltip");
        addBool(c, eb, "ck7infinite.config.resscale.aggressive_mipmapping", ResScaleConfig.AGGRESSIVE_MIPMAPPING, false,
                "ck7infinite.config.resscale.aggressive_mipmapping.tooltip");
        addDouble(c, eb, "ck7infinite.config.resscale.aggressive_mipmapping_bias", ResScaleConfig.AGGRESSIVE_MIPMAPPING_BIAS,
                0.5, 0.0, 2.0, "ck7infinite.config.resscale.aggressive_mipmapping_bias.tooltip");
        addBool(c, eb, "ck7infinite.config.resscale.renderscale_warning_dismissed",
                ResScaleConfig.RENDERSCALE_WARNING_DISMISSED, false,
                "ck7infinite.config.resscale.renderscale_warning_dismissed.tooltip");
    }

    private static void buildSimDistance(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory c = b.getOrCreateCategory(Component.translatable("ck7infinite.config.category.simdistance"));
        addBool(c, eb, "ck7infinite.config.simdistance.enabled", SimDistanceConfig.ENABLED, false,
                "ck7infinite.config.simdistance.enabled.tooltip");
        addInt(c, eb, "ck7infinite.config.simdistance.min_simulation_distance", SimDistanceConfig.MIN_SIMULATION_DISTANCE, 5, 2, 32,
                "ck7infinite.config.simdistance.min_simulation_distance.tooltip");
        addDouble(c, eb, "ck7infinite.config.simdistance.high_tick_millis", SimDistanceConfig.HIGH_TICK_MILLIS, 45.0, 10.0, 500.0,
                "ck7infinite.config.simdistance.high_tick_millis.tooltip");
        addDouble(c, eb, "ck7infinite.config.simdistance.low_tick_millis", SimDistanceConfig.LOW_TICK_MILLIS, 35.0, 5.0, 500.0,
                "ck7infinite.config.simdistance.low_tick_millis.tooltip");
        addInt(c, eb, "ck7infinite.config.simdistance.eval_interval", SimDistanceConfig.EVALUATION_INTERVAL_TICKS, 20, 1, 200,
                "ck7infinite.config.simdistance.eval_interval.tooltip");
        addInt(c, eb, "ck7infinite.config.simdistance.growth_cooldown", SimDistanceConfig.GROWTH_COOLDOWN_TICKS, 100, 0, 1200,
                "ck7infinite.config.simdistance.growth_cooldown.tooltip");
    }

    // ---- helpers ----
    // name/tooltip son CLAVES de traduccion (ver assets/ck7infinite/lang/*.json), no el texto.

    private static void addSeparator(ConfigCategory cat, ConfigEntryBuilder eb, String textKey) {
        cat.addEntry(eb.startTextDescription(Component.translatable(textKey)).build());
    }

    private static void addBool(ConfigCategory cat, ConfigEntryBuilder eb, String nameKey,
                                ForgeConfigSpec.BooleanValue cv, boolean def, String tooltipKey) {
        cat.addEntry(eb.startBooleanToggle(Component.translatable(nameKey), cv.get())
                .setDefaultValue(def)
                .setTooltip(tip(tooltipKey))
                .setSaveConsumer(cv::set)
                .build());
    }

    private static void addInt(ConfigCategory cat, ConfigEntryBuilder eb, String nameKey,
                               ForgeConfigSpec.IntValue cv, int def, int min, int max, String tooltipKey) {
        cat.addEntry(eb.startIntField(Component.translatable(nameKey), cv.get())
                .setDefaultValue(def)
                .setMin(min)
                .setMax(max)
                .setTooltip(tip(tooltipKey))
                .setSaveConsumer(cv::set)
                .build());
    }

    private static void addDouble(ConfigCategory cat, ConfigEntryBuilder eb, String nameKey,
                                  ForgeConfigSpec.DoubleValue cv, double def, double min, double max, String tooltipKey) {
        cat.addEntry(eb.startDoubleField(Component.translatable(nameKey), cv.get())
                .setDefaultValue(def)
                .setMin(min)
                .setMax(max)
                .setTooltip(tip(tooltipKey))
                .setSaveConsumer(cv::set)
                .build());
    }

    private static void addList(ConfigCategory cat, ConfigEntryBuilder eb, String nameKey,
                                ForgeConfigSpec.ConfigValue<List<? extends String>> cv, List<String> def, String tooltipKey) {
        cat.addEntry(eb.startStrList(Component.translatable(nameKey), new ArrayList<>(cv.get()))
                .setDefaultValue(def)
                .setTooltip(tip(tooltipKey))
                .setSaveConsumer(cv::set)
                .build());
    }

    private static Component[] tip(String tooltipKey) {
        String text = Component.translatable(tooltipKey).getString();
        String[] lines = text.split("\n");
        Component[] comps = new Component[lines.length];
        for (int i = 0; i < lines.length; i++) {
            comps[i] = Component.literal(lines[i]);
        }
        return comps;
    }
}
