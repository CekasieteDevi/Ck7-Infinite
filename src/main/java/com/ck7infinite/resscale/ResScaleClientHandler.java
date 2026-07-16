package com.ck7infinite.resscale;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Tres responsabilidades de cliente:
 * <p>
 * 1) Red de seguridad: RenderTickEvent END corre SIEMPRE, pase lo que pase durante el frame (a
 * diferencia del RETURN de GameRendererMixin, que una excepcion a mitad de renderLevel podria
 * saltear). Si ResScaleManager quedo con el render target real swapeado, lo restaura aca.
 * <p>
 * 2) Al salir de un mundo, libera el framebuffer escalado -es la unica forma en que un cambio de
 * escala hecho en la config mientras se jugaba llega a aplicarse (ver ResScaleManager, cambiar la
 * escala EN CALIENTE sin pasar por esto rompe el frame, confirmado en vivo en ambas direcciones).
 * <p>
 * 3) Aviso por chat si un cambio de "Escala" guardado sigue sin aplicarse despues de un ratito.
 * A proposito NO se chequea inmediatamente al guardar (eso tenia una condicion de carrera: el
 * chequeo corria antes de que beginWorldPass() de la proxima renderizada procesara la transicion
 * de un toggle apagado->prendido, dando un falso positivo aunque el cambio fuera a aplicar bien un
 * instante despues). Esperar unos frames de "asentamiento" evita ese falso positivo.
 */
public final class ResScaleClientHandler {

    private static final int SETTLE_TICKS = 10;

    private int mismatchTicks = 0;
    private boolean warned = false;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ResScaleManager.safetyNetRestore();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !ResScaleConfig.isEnabled()) {
            mismatchTicks = 0;
            warned = false;
            return;
        }
        if (ResScaleManager.appliedScale() == ResScaleConfig.scale()) {
            mismatchTicks = 0;
            warned = false;
            return;
        }
        mismatchTicks++;
        if (mismatchTicks == SETTLE_TICKS && !warned) {
            warned = true;
            mc.player.sendSystemMessage(Component.translatable("ck7infinite.config.resscale.pending_restart"));
        }
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ResScaleManager.resetSession();
        mismatchTicks = 0;
        warned = false;
    }
}
