package com.ck7infinite.particlepriority;

import com.ck7infinite.Ck7Infinite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registra las clases que tocan API de cliente (PlayerActionContext) unicamente dentro de un
 * evento que Forge garantiza que solo corre en el cliente fisico, para que un dedicated server
 * nunca intente cargar/instanciar nada que referencie net.minecraft.client.
 */
@Mod.EventBusSubscriber(modid = Ck7Infinite.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ParticlePriorityClientSetup {

    private ParticlePriorityClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new PlayerActionContext());
        MinecraftForge.EVENT_BUS.register(new ParticleCoverageBudget());
    }
}
