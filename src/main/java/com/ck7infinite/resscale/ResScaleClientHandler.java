package com.ck7infinite.resscale;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Dos responsabilidades de cliente:
 * <p>
 * 1) Red de seguridad: RenderTickEvent END corre SIEMPRE, pase lo que pase durante el frame (a
 * diferencia del RETURN de GameRendererMixin, que una excepcion a mitad de renderLevel podria
 * saltear). Si ResScaleManager quedo con el render target real swapeado, lo restaura aca.
 * <p>
 * 2) Al salir de un mundo, libera el framebuffer escalado. Un cambio de escala ya se aplica solo,
 * en vivo, cada frame (ver ResScaleManager) -esto es solo limpieza de memoria de GPU mientras no
 * hay mundo cargado.
 */
public final class ResScaleClientHandler {

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ResScaleManager.safetyNetRestore();
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ResScaleManager.resetSession();
    }
}
