package com.ck7infinite.ultraload;

/**
 * Estado compartido de Ultra Carga: un flag que el detector del lado servidor
 * (UltraLoadServerHandler) enciende/apaga, y que leen tanto los efectos de servidor (freeze de
 * mobs, spawns, random ticks) como los de cliente (particulas, render de entidades).
 * <p>
 * En singleplayer el servidor integrado y el cliente comparten JVM, asi que este unico flag
 * alcanza para ambos lados. En un servidor dedicado el flag lo maneja el servidor y solo aplican
 * los efectos de servidor; los efectos de cliente en multiplayer remoto necesitarian sincronizar
 * el estado por paquete (pendiente, fuera del alcance de Tier 1).
 */
public final class UltraLoadState {

    private static volatile boolean active;

    private UltraLoadState() {
    }

    public static boolean isActive() {
        return active;
    }

    static void setActive(boolean value) {
        active = value;
    }
}
