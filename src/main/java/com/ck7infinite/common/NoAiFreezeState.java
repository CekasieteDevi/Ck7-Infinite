package com.ck7infinite.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Aplica/retira freeze de IA usando el flag vanilla NoAI (Mob#setNoAi), compartido por todos los
 * modulos que necesiten pausar pathfinding/goal selector/brain de un mob sin bytecode injection.
 * <p>
 * Cada modulo pasa su propia tagKey para marcar, en la persistent data del entity, que fue ESE
 * modulo el que lo congelo. Asi evitamos tocar mobs cuyo NoAI ya haya sido puesto en true por
 * otra fuente (datapacks, comandos, otros mods, mapmakers) y evitamos que dos modulos se pisen
 * entre si si algun dia un mismo entity fuera elegible para mas de uno.
 */
public final class NoAiFreezeState {

    private NoAiFreezeState() {
    }

    public static void apply(Mob mob, boolean shouldFreeze, String tagKey) {
        CompoundTag data = mob.getPersistentData();
        boolean frozenByUs = data.getBoolean(tagKey);

        if (shouldFreeze) {
            if (!frozenByUs && !mob.isNoAi()) {
                mob.setNoAi(true);
                data.putBoolean(tagKey, true);
            }
        } else if (frozenByUs) {
            mob.setNoAi(false);
            data.remove(tagKey);
        }
    }

    public static void clear(Mob mob, String tagKey) {
        CompoundTag data = mob.getPersistentData();
        if (data.contains(tagKey)) {
            data.remove(tagKey);
        }
    }

    public static boolean isFrozenByUs(Mob mob, String tagKey) {
        return mob.getPersistentData().getBoolean(tagKey);
    }
}
