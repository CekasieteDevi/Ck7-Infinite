package com.ck7infinite.simdistance;

import com.ck7infinite.ultraload.UltraLoadState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Arrays;

/**
 * Adaptive Simulation Distance. Cada evaluationIntervalTicks, calcula la MEDIANA de
 * MinecraftServer#tickTimes (buffer circular vanilla de ~100 ticks, el mismo dato que usan el
 * debug screen y /forge tps) y ajusta la simulation distance real via
 * PlayerList#setSimulationDistance -API publica vanilla, sin mixin, el mismo metodo que usa el
 * comando /simulation-distance-.
 * <p>
 * LA CORRECCION CLAVE sobre el intento anterior de este mecanismo (ver SimDistanceConfig): si
 * UltraLoadState.isActive() (el disparador de carga de chunks de Ultra Carga, Fase 0, esta
 * activo), la evaluacion se saltea COMPLETO -ni cuenta, ni corta, ni crece-. Esto evita
 * exactamente el bug documentado: MSPT alto durante una rafaga de generacion de mundo NO es
 * "el servidor ahogado por simulacion", es esperado, y cortar la sim distance en ese momento
 * solo empeora las cosas (menos chunks visibles + un anillo descargado que hay que regenerar).
 */
public final class SimDistanceHandler {

    private int tickCounter;
    private int ticksSinceLastCut = Integer.MAX_VALUE;

    /** Simulation distance real del jugador/servidor, capturada la primera vez que este modulo
     * corre en una sesion (antes de aplicar cualquier recorte propio). -1 = todavia no capturada. */
    private int ceilingSimulationDistance = -1;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        PlayerList playerList = server.getPlayerList();

        if (!SimDistanceConfig.isEnabled()) {
            restoreCeilingIfNeeded(playerList);
            return;
        }

        ticksSinceLastCut++;

        if (++tickCounter < SimDistanceConfig.evaluationIntervalTicks()) {
            return;
        }
        tickCounter = 0;

        if (UltraLoadState.isActive()) {
            // Rafaga real de carga de chunks en curso (Fase 0): no evaluar nada este ciclo. Ver
            // javadoc de la clase -esto es la correccion sobre el intento anterior.
            return;
        }

        if (ceilingSimulationDistance < 0) {
            ceilingSimulationDistance = playerList.getSimulationDistance();
        }

        double medianTickMillis = medianTickMillis(server.tickTimes);
        int current = playerList.getSimulationDistance();
        int min = SimDistanceConfig.minSimulationDistanceChunks();

        if (medianTickMillis > SimDistanceConfig.highTickMillis() && current > min) {
            playerList.setSimulationDistance(current - 1);
            ticksSinceLastCut = 0;
        } else if (medianTickMillis < SimDistanceConfig.lowTickMillis()
                && current < ceilingSimulationDistance
                && ticksSinceLastCut >= SimDistanceConfig.growthCooldownTicks()) {
            playerList.setSimulationDistance(current + 1);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        restoreCeilingIfNeeded(event.getServer().getPlayerList());
        ceilingSimulationDistance = -1;
        ticksSinceLastCut = Integer.MAX_VALUE;
    }

    private void restoreCeilingIfNeeded(PlayerList playerList) {
        if (ceilingSimulationDistance < 0) {
            return;
        }
        if (playerList.getSimulationDistance() != ceilingSimulationDistance) {
            playerList.setSimulationDistance(ceilingSimulationDistance);
        }
        ceilingSimulationDistance = -1;
    }

    /** Mediana, no promedio: un solo tick lento (worldgen, GC) no la mueve, asi que reacciona a
     * carga SOSTENIDA -donde recortar ayuda- e ignora picos aislados -donde recortar solo agrega
     * churn de chunks-. */
    private static double medianTickMillis(long[] tickTimesNanos) {
        long[] sorted = Arrays.copyOf(tickTimesNanos, tickTimesNanos.length);
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        double medianNanos = (sorted.length % 2 == 0)
                ? (sorted[mid - 1] + sorted[mid]) / 2.0
                : sorted[mid];
        return medianNanos / 1_000_000.0;
    }
}
