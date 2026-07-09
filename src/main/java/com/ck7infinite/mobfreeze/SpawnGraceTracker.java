package com.ck7infinite.mobfreeze;

import net.minecraft.world.entity.Mob;

/**
 * Guarda el gameTime en el que un mob entro al tracking, para poder darle spawnGraceTicks antes
 * de que sea elegible para freeze (ver MobFreezeConfig#spawnGraceTicks).
 */
final class SpawnGraceTracker {

    private static final String SPAWN_TICK_TAG = "ck7infinite_mobfreeze_spawn_tick";

    private SpawnGraceTracker() {
    }

    static void markSpawnTick(Mob mob, long gameTime) {
        mob.getPersistentData().putLong(SPAWN_TICK_TAG, gameTime);
    }

    static boolean isInGracePeriod(Mob mob, long currentGameTime, int graceTicks) {
        if (graceTicks <= 0 || !mob.getPersistentData().contains(SPAWN_TICK_TAG)) {
            return false;
        }
        long spawnTick = mob.getPersistentData().getLong(SPAWN_TICK_TAG);
        return currentGameTime - spawnTick < graceTicks;
    }
}
