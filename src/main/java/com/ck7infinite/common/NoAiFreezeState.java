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

        // Bug real encontrado y reproducido (reporte de usuario: "un animal que se congela una
        // vez, nunca mas se descongela, ni parado al lado"): la version anterior de este metodo
        // exigia !mob.isNoAi() ademas de !frozenByUs antes de congelar, para no pisar un NoAi que
        // otra fuente hubiera puesto. Pero si el mob YA estaba NoAi=true por CUALQUIER motivo en
        // el momento en que decidimos congelarlo (persistido de una sesion vieja con una build mas
        // vieja de este mod, otro mod, un comando, lo que sea) esa guarda impedia para siempre que
        // pusieramos NUESTRO tag -y sin el tag, la rama de abajo (frozenByUs) tampoco iba a poder
        // descongelarlo nunca. El mob quedaba con NoAi=true de por vida, sin que ninguna
        // evaluacion futura (por mas que el jugador se parara al lado) pudiera tocarlo. Ahora: si
        // decidimos que el mob debe estar congelado y todavia no es nuestro, tomamos posesion
        // (ponemos el tag) sin importar el NoAi previo -asi el ciclo freeze/unfreeze siempre queda
        // bajo nuestro control y se puede revertir.
        if (shouldFreeze) {
            if (!frozenByUs) {
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
