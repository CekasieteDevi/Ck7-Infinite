package com.ck7infinite.ultraload;

/**
 * Estado compartido de Ultra Carga: un nivel de severidad graduado (0 = inactivo) que el detector
 * del lado servidor (UltraLoadServerHandler) agrega a partir de sus disparadores plugables, y que
 * leen tanto los efectos de servidor (freeze de mobs, spawns, random ticks) como los de cliente
 * (particulas, render de entidades). isActive() (nivel > 0) es el punto de lectura de todos los
 * consumidores hoy -ninguno necesita todavia el nivel exacto, solo on/off-.
 * <p>
 * En singleplayer el servidor integrado y el cliente comparten JVM, asi que este unico estado
 * alcanza para ambos lados. En un servidor dedicado lo maneja el servidor y solo aplican los
 * efectos de servidor; los efectos de cliente en multiplayer remoto necesitarian sincronizar el
 * estado por paquete (pendiente, fuera del alcance de Tier 1).
 */
public final class UltraLoadState {

    private static volatile int level;

    private UltraLoadState() {
    }

    public static int getLevel() {
        return level;
    }

    public static boolean isActive() {
        return level > 0;
    }

    static void setLevel(int value) {
        level = value;
    }
}
