package com.ck7infinite.potatoprofile;

import com.ck7infinite.resscale.ResScaleConfig;
import com.mojang.blaze3d.platform.GlUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Sugiere por chat (mismo patron que SodiumWarningHandler/RenderScale conflict: primera vez por
 * sesion, con toggle de "no volver a sugerir") activar `resscale` -el modulo de mayor palanca en
 * iGPU segun V2_ROADMAP.md seccion G- si detecta hardware integrado Y el modulo todavia no esta
 * habilitado (no molestar a alguien que ya lo configuro). NO se auto-aplica -deliberado, ver
 * V2_ROADMAP.md fila "Perfil Potato PC": auto-cambiar settings que alteran el feel del juego a
 * partir de un match de string es la receta para "el mod me rompio la partida", con el agravante
 * de un posible falso positivo en toda una marca de GPU. El mensaje incluye un componente
 * clickeable (ClickEvent.RUN_COMMAND) que corre /ck7potato apply -ver PotatoProfileCommand-.
 */
public final class PotatoProfileHandler {

    private static boolean shownThisSession;

    @SubscribeEvent
    public void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (shownThisSession || PotatoProfileConfig.DISMISSED.get()) {
            return;
        }
        if (ResScaleConfig.ENABLED.get()) {
            // Ya lo tiene prendido (a mano o por una sugerencia anterior): no insistir.
            return;
        }
        if (!HardwareProfile.isLikelyIntegratedGpu(GlUtil.getRenderer())) {
            return;
        }
        shownThisSession = true;

        MutableComponent apply = Component.translatable("ck7infinite.potatoprofile.chat.apply_link")
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ck7potato apply")));
        event.getPlayer().sendSystemMessage(Component.translatable("ck7infinite.potatoprofile.chat", apply));
    }
}
