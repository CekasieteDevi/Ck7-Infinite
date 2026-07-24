package com.ck7infinite.potatoprofile;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Config CLIENT minima: solo guarda si el usuario ya tildo "No volver a sugerir" el perfil Potato
 * PC. Mismo patron que SodiumWarningConfig.
 */
public final class PotatoProfileConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DISMISSED = BUILDER
            .comment("Si esta en true, no se vuelve a sugerir el perfil Potato PC al abrir el juego.")
            .define("dismissed", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private PotatoProfileConfig() {
    }
}
