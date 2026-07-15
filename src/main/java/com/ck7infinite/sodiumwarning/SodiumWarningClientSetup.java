package com.ck7infinite.sodiumwarning;

import com.ck7infinite.Ck7Infinite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registra el handler del aviso de Sodium/Embeddium solo dentro de FMLClientSetupEvent, que Forge
 * garantiza que corre unicamente en el cliente fisico (mismo patron que UltraLoadClientSetup). El
 * aviso ahora es un mensaje de chat (ver SodiumWarningHandler), asi que no depende de Cloth Config.
 */
@Mod.EventBusSubscriber(modid = Ck7Infinite.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SodiumWarningClientSetup {

    private SodiumWarningClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new SodiumWarningHandler());
    }
}
