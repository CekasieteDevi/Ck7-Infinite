package com.ck7infinite.ultraload.trigger;

import net.minecraft.server.MinecraftServer;

/**
 * Disparador plugable de Ultra Carga: evaluado una vez por tick de servidor, devuelve el nivel de
 * severidad que este disparador en particular quiere pedir (0 = no pide nada). El nivel agregado
 * de Ultra Carga es el maximo entre todos los disparadores registrados -ver
 * UltraLoadServerHandler-, asi que sumar un disparador nuevo (TPS sostenido bajo, AFK, etc.) no
 * requiere tocar la maquina de estados de activacion/desactivacion.
 */
public interface UltraLoadTrigger {

    int desiredLevel(MinecraftServer server);

    /**
     * Llamado cuando Ultra Carga esta deshabilitado por config: descarta cualquier acumulacion en
     * curso sin evaluar. Cada disparador decide que parte de su estado interno resetear.
     */
    default void reset() {
    }
}
