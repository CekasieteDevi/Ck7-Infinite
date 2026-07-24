package com.ck7infinite.resscale;

import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/**
 * Tres responsabilidades de cliente:
 * <p>
 * 1) Red de seguridad: RenderTickEvent END corre SIEMPRE, pase lo que pase durante el frame (a
 * diferencia del RETURN de GameRendererMixin, que una excepcion a mitad de renderLevel podria
 * saltear). Si ResScaleManager quedo con el render target real swapeado, lo restaura aca.
 * <p>
 * 2) Al salir de un mundo, libera el framebuffer escalado. Un cambio de escala ya se aplica solo,
 * en vivo, cada frame (ver ResScaleManager) -esto es solo limpieza de memoria de GPU mientras no
 * hay mundo cargado.
 * <p>
 * 3) Aviso por chat (mismo patron que SodiumWarningHandler) si detecta el mod RenderScale
 * (modid "renderscale", confirmado por codigo fuente -ver memoria del proyecto) cargado junto a
 * este: ambos hacen swap propio de {@code Minecraft#mainRenderTarget} cada frame, sin coordinarse
 * entre si, asi que tener los dos activos a la vez con una escala distinta de 1.0 corrompe el
 * frame (el ultimo en swapear pisa al otro). Recomendacion: sacar RenderScale o dejarlo en
 * escala 1.0 mientras se usa este modulo.
 */
public final class ResScaleClientHandler {

    private static boolean renderScaleWarningShown;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ResScaleManager.safetyNetRestore();
    }

    @SubscribeEvent
    public void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (renderScaleWarningShown || ResScaleConfig.RENDERSCALE_WARNING_DISMISSED.get()) {
            return;
        }
        if (!ModList.get().isLoaded("renderscale")) {
            return;
        }
        renderScaleWarningShown = true;
        event.getPlayer().sendSystemMessage(Component.translatable("ck7infinite.resscale.renderscale_conflict.chat"));
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ResScaleManager.resetSession();
    }
}
