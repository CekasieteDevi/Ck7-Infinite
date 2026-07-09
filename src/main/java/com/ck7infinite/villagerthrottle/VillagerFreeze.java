package com.ck7infinite.villagerthrottle;

import com.ck7infinite.common.NoAiFreezeState;
import net.minecraft.world.entity.Mob;

/**
 * Wrapper de NoAiFreezeState con la tagKey propia del modulo Villager Throttling.
 */
final class VillagerFreeze {

    private static final String TAG_KEY = "ck7infinite_villagerthrottle_ai_frozen";

    private VillagerFreeze() {
    }

    static void apply(Mob mob, boolean shouldFreeze) {
        NoAiFreezeState.apply(mob, shouldFreeze, TAG_KEY);
    }

    static void clear(Mob mob) {
        NoAiFreezeState.clear(mob, TAG_KEY);
    }

    static boolean isFrozenByUs(Mob mob) {
        return NoAiFreezeState.isFrozenByUs(mob, TAG_KEY);
    }
}
