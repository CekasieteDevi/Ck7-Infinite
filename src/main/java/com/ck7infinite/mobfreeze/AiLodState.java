package com.ck7infinite.mobfreeze;

import net.minecraft.world.entity.Mob;

/**
 * Tag booleano simple (persistent data) que marca si un mob esta en la banda "media" de Entity AI
 * LOD -activo, no congelado, pero fuera de aiLodCloseRadiusBlocks-. A diferencia de FreezeState/
 * NoAiFreezeState no hay ningun flag vanilla compartido en juego aca (no tocamos NoAi), asi que no
 * hace falta la logica de "tomar posesion": es enteramente nuestro. Publico (a diferencia de
 * FreezeState, package-private) porque lo lee el mixin en el subpaquete mobfreeze.mixin -mismo
 * motivo por el que UltraLoadState/UltraLoadConfig son publicos para ultraload.mixin.
 */
public final class AiLodState {

    private static final String TAG_KEY = "ck7infinite_mobfreeze_ai_lod";

    private AiLodState() {
    }

    public static void apply(Mob mob, boolean shouldLod) {
        mob.getPersistentData().putBoolean(TAG_KEY, shouldLod);
    }

    public static void clear(Mob mob) {
        mob.getPersistentData().remove(TAG_KEY);
    }

    public static boolean isLodded(Mob mob) {
        return mob.getPersistentData().getBoolean(TAG_KEY);
    }
}
