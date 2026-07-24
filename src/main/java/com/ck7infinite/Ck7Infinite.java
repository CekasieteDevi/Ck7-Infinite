package com.ck7infinite;

import com.ck7infinite.mobfreeze.MobFreezeConfig;
import com.ck7infinite.mobfreeze.MobFreezeTickHandler;
import com.ck7infinite.mobfreeze.MobFreezeTracker;
import com.ck7infinite.particlepriority.ParticlePriorityConfig;
import com.ck7infinite.resscale.ResScaleConfig;
import com.ck7infinite.ultraload.UltraLoadConfig;
import com.ck7infinite.ultraload.UltraLoadServerHandler;
import com.ck7infinite.villagerthrottle.VillagerInteractHandler;
import com.ck7infinite.villagerthrottle.VillagerThrottleConfig;
import com.ck7infinite.villagerthrottle.VillagerThrottleTickHandler;
import com.ck7infinite.villagerthrottle.VillagerThrottleTracker;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Ck7Infinite.MODID)
public class Ck7Infinite {

    public static final String MODID = "ck7infinite";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Ck7Infinite(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Interruptor general: un solo toggle que desactiva TODO el mod. Config COMMON, se
        // registra primero para que este disponible antes que los modulos individuales lo lean.
        context.registerConfig(ModConfig.Type.COMMON, Ck7MasterConfig.SPEC, MODID + "-general.toml");

        // Modulo 1: Mob AI Freeze. Config COMMON (global en config/, no per-world): asi es
        // editable desde el menu principal via la pantalla de Cloth Config, y la logica corre
        // igual en el logical server. Cada modulo tiene su propio archivo para editarlos aparte.
        context.registerConfig(ModConfig.Type.COMMON, MobFreezeConfig.SPEC, MODID + "-mobfreeze.toml");
        MinecraftForge.EVENT_BUS.register(new MobFreezeTracker());
        MinecraftForge.EVENT_BUS.register(new MobFreezeTickHandler());

        // Modulo 2: Villager Throttling. Config COMMON (misma razon que Mob Freeze).
        context.registerConfig(ModConfig.Type.COMMON, VillagerThrottleConfig.SPEC, MODID + "-villagerthrottle.toml");
        MinecraftForge.EVENT_BUS.register(new VillagerThrottleTracker());
        MinecraftForge.EVENT_BUS.register(new VillagerThrottleTickHandler());
        MinecraftForge.EVENT_BUS.register(new VillagerInteractHandler());

        // Modulo 3: Particulas por Prioridad. Config CLIENT (decision puramente visual/local).
        // El mixin y el tracker de contexto del jugador se registran solo en el cliente
        // (ver ParticlePriorityClientSetup); esta linea es segura en dedicated server porque
        // ParticlePriorityConfig no referencia ninguna clase de net.minecraft.client.
        context.registerConfig(ModConfig.Type.CLIENT, ParticlePriorityConfig.SPEC, MODID + "-particlepriority.toml");

        // Modulo 7: Escala de Resolucion. Config CLIENT (decision puramente visual/local, igual
        // que Particulas por Prioridad). Los mixins solo tocan clases de render del cliente
        // fisico (ver ck7infinite-resscale.mixins.json, array "client").
        context.registerConfig(ModConfig.Type.CLIENT, ResScaleConfig.SPEC, MODID + "-resscale.toml");

        // Modulo 6: Ultra Carga (modo panico durante carga de chunks). Config COMMON. El detector
        // y los efectos de servidor van en el handler; los de cliente (render de entidades) se
        // registran solo en cliente via UltraLoadClientSetup, y el de particulas via su mixin.
        context.registerConfig(ModConfig.Type.COMMON, UltraLoadConfig.SPEC, MODID + "-ultraload.toml");
        MinecraftForge.EVENT_BUS.register(new UltraLoadServerHandler());

        // Modulo 8: Adaptive Simulation Distance. Config COMMON, server-only (misma razon que
        // Ultra Carga). Lee UltraLoadState (Fase 0) para saltear la evaluacion durante rafagas
        // reales de carga de chunks -ver SimDistanceHandler/SimDistanceConfig para el porque.
        context.registerConfig(ModConfig.Type.COMMON, com.ck7infinite.simdistance.SimDistanceConfig.SPEC,
                MODID + "-simdistance.toml");
        MinecraftForge.EVENT_BUS.register(new com.ck7infinite.simdistance.SimDistanceHandler());

        // Aviso de Sodium/Embeddium: config CLIENT minima (solo guarda "no volver a mostrar").
        // El handler que muestra la pantalla se registra aparte, solo en cliente, via
        // SodiumWarningClientSetup (@EventBusSubscriber con Dist.CLIENT).
        context.registerConfig(ModConfig.Type.CLIENT, com.ck7infinite.sodiumwarning.SodiumWarningConfig.SPEC,
                MODID + "-sodiumwarning.toml");

        // Pantalla de configuracion in-game (Mods -> Ck7 - Conatus -> Config), solo en el cliente
        // fisico y solo si Cloth Config esta presente. La clase Ck7ConfigScreen importa Cloth
        // Config y clases de cliente, asi que solo se carga cuando ambos gates se cumplen (si no
        // esta Cloth Config, nunca se referencia -> el mod carga igual, sin GUI).
        if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("cloth_config")) {
            com.ck7infinite.config.Ck7ConfigScreen.register();
        }
    }
}
