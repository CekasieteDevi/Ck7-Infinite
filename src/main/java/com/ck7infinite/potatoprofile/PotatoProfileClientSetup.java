package com.ck7infinite.potatoprofile;

import com.ck7infinite.Ck7Infinite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Registra el handler de cliente del perfil Potato PC solo dentro de un evento que Forge garantiza
 * que corre unicamente en el cliente fisico, mismo patron que SodiumWarningClientSetup/
 * ParticlePriorityClientSetup. El comando (/ck7potato) se registra aparte, en PotatoProfileCommand,
 * via @Mod.EventBusSubscriber de bus FORGE -RegisterClientCommandsEvent no implementa
 * IModBusEvent, asi que necesita ese bus, y la anotacion de clase (no un registro manual desde
 * aca) evita depender del orden relativo entre FMLClientSetupEvent y RegisterClientCommandsEvent.
 */
@Mod.EventBusSubscriber(modid = Ck7Infinite.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PotatoProfileClientSetup {

    private PotatoProfileClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new PotatoProfileHandler());
    }
}
