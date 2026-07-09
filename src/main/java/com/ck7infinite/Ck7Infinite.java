package com.ck7infinite;

import com.ck7infinite.dynamicres.DynamicResConfig;
import com.ck7infinite.mobfreeze.MobFreezeConfig;
import com.ck7infinite.mobfreeze.MobFreezeTickHandler;
import com.ck7infinite.mobfreeze.MobFreezeTracker;
import com.ck7infinite.particlepriority.ParticlePriorityConfig;
import com.ck7infinite.villagerthrottle.VillagerInteractHandler;
import com.ck7infinite.villagerthrottle.VillagerThrottleConfig;
import com.ck7infinite.villagerthrottle.VillagerThrottleTickHandler;
import com.ck7infinite.villagerthrottle.VillagerThrottleTracker;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Ck7Infinite.MODID)
public class Ck7Infinite {

    public static final String MODID = "ck7infinite";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Ck7Infinite(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Modulo 1: Mob AI Freeze. Cada modulo registra su propio archivo de config
        // server-side para que puedan activarse/editarse de forma independiente.
        context.registerConfig(ModConfig.Type.SERVER, MobFreezeConfig.SPEC, MODID + "-mobfreeze.toml");
        MinecraftForge.EVENT_BUS.register(new MobFreezeTracker());
        MinecraftForge.EVENT_BUS.register(new MobFreezeTickHandler());

        // Modulo 2: Villager Throttling.
        context.registerConfig(ModConfig.Type.SERVER, VillagerThrottleConfig.SPEC, MODID + "-villagerthrottle.toml");
        MinecraftForge.EVENT_BUS.register(new VillagerThrottleTracker());
        MinecraftForge.EVENT_BUS.register(new VillagerThrottleTickHandler());
        MinecraftForge.EVENT_BUS.register(new VillagerInteractHandler());

        // Modulo 3: Particulas por Prioridad. Config CLIENT (decision puramente visual/local).
        // El mixin y el tracker de contexto del jugador se registran solo en el cliente
        // (ver ParticlePriorityClientSetup); esta linea es segura en dedicated server porque
        // ParticlePriorityConfig no referencia ninguna clase de net.minecraft.client.
        context.registerConfig(ModConfig.Type.CLIENT, ParticlePriorityConfig.SPEC, MODID + "-particlepriority.toml");

        // Modulo 4: Resolucion de Render Dinamica. Config CLIENT. El mixin (GameRendererMixin) y
        // RenderScaleManager solo se tocan durante GameRenderer#renderLevel, que unicamente se
        // ejecuta en el cliente fisico; no hace falta un gate de Dist explicito ademas del que ya
        // provee el mixins.json (array "client").
        context.registerConfig(ModConfig.Type.CLIENT, DynamicResConfig.SPEC, MODID + "-dynamicres.toml");
    }
}
