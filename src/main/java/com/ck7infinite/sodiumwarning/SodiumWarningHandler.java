package com.ck7infinite.sodiumwarning;

import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/**
 * Avisa por chat (no por pantalla) sobre el bug conocido de "Use Fog Occlusion" en Sodium/Embeddium
 * la primera vez que el jugador entra a un mundo, si: no lo tildo "no volver a mostrar" (toggle en
 * Mods -> Ck7 - Conatus -> Config, categoria General) y tiene Sodium, Embeddium o Rubidium cargado.
 * <p>
 * v2 uso una Screen custom y despues una pantalla Cloth Config para esto, interceptando la
 * pantalla de titulo -en AMBOS casos el texto y los widgets salian desproporcionados/cortados.
 * Se investigo a fondo (Embeddium si/no, Resolucion de Render Dinamica si/no, interceptar antes o
 * despues de que la TitleScreen se inicialice) y el bug persistio identico en TODOS los casos,
 * incluso abriendo la pantalla de configuracion NORMAL del mod (Ck7ConfigScreen, ya probada y
 * andando bien DENTRO de una partida) en el menu principal -confirmando que es un problema de
 * abrir CUALQUIER pantalla de Cloth Config/vanilla en el menu principal en este entorno, no algo
 * arreglable desde el codigo de esta pantalla en particular. Por eso el aviso ahora es un mensaje
 * de chat (el chat renderiza y wrappea texto correctamente sin depender del mismo mecanismo de
 * escala de GUI), enviado una vez que el jugador ya esta DENTRO de una partida.
 */
public final class SodiumWarningHandler {

    private static boolean shownThisSession;

    @SubscribeEvent
    public void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (shownThisSession || SodiumWarningConfig.DISMISSED.get()) {
            return;
        }
        ModList mods = ModList.get();
        if (!mods.isLoaded("sodium") && !mods.isLoaded("embeddium") && !mods.isLoaded("rubidium")) {
            return;
        }
        shownThisSession = true;
        event.getPlayer().sendSystemMessage(Component.translatable("ck7infinite.sodiumwarning.chat"));
    }
}
