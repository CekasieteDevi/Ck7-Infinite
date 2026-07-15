package com.ck7infinite.sodiumwarning;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Config CLIENT minima: solo guarda si el usuario ya tildo "No volver a mostrar" en el aviso de
 * Sodium/Embeddium. Se lee directamente (sin cache de campo volatile) porque solo se consulta una
 * vez por apertura de pantalla de titulo, no en un hot path como los modulos de tick.
 */
public final class SodiumWarningConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DISMISSED = BUILDER
            .comment("Si esta en true, no se vuelve a mostrar el aviso de Sodium/Embeddium al abrir el juego.")
            .define("dismissed", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SodiumWarningConfig() {
    }
}
