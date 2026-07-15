package com.ck7infinite.ultraload.client;

import com.ck7infinite.Ck7Infinite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registra el handler de efectos de cliente de Ultra Carga solo dentro de FMLClientSetupEvent, que
 * Forge garantiza que corre unicamente en el cliente fisico (mismo patron que
 * ParticlePriorityClientSetup). Asi un dedicated server nunca carga clases de render.
 */
@Mod.EventBusSubscriber(modid = Ck7Infinite.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UltraLoadClientSetup {

    private UltraLoadClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new UltraLoadClientHandler());
    }
}
