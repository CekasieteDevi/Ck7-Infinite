package com.ck7infinite.potatoprofile;

import java.util.Locale;

/**
 * Deteccion de iGPU por el string GL_RENDERER (GlUtil#getRenderer). V2_ROADMAP.md señala que la
 * heuristica naive "Intel o AMD y no NVIDIA -> integrada" falla: AMD fabrica tanto dedicadas como
 * integradas (APUs Ryzen), asi que "es AMD" no alcanza para decidir. Esta version distingue por
 * patrones REALES de nombre:
 * <p>
 * - Intel: casi todas sus GPUs de consumo son integradas: "Intel" sin "Arc" (su unica linea
 * dedicada) -> integrada.
 * - AMD: las APUs integradas reportan el nombre GENERICO "Radeon(TM) Graphics" o
 * "Radeon Vega N Graphics" (N = 3/6/8/11, los Vega integrados de los Ryzen), SIN un modelo
 * especifico (RX, Instinct, etc). Las dedicadas siempre incluyen un modelo especifico.
 * - NVIDIA: no fabrica GPUs integradas relevantes en el mercado de consumo -> nunca integrada.
 * <p>
 * Cualquier string no reconocido devuelve false (no sugerir) a proposito -un falso positivo en
 * toda una marca de GPU es peor que no sugerir nada (ver V2_ROADMAP.md, fila "Perfil Potato PC").
 */
final class HardwareProfile {

    private static final String[] AMD_INTEGRATED_MARKERS = {
            "radeon(tm) graphics",
            "radeon vega",
            "vega 3", "vega 6", "vega 8", "vega 11",
    };

    private HardwareProfile() {
    }

    static boolean isLikelyIntegratedGpu(String renderer) {
        if (renderer == null || renderer.isBlank()) {
            return false;
        }
        String lower = renderer.toLowerCase(Locale.ROOT);
        if (lower.contains("intel") && !lower.contains("arc")) {
            return true;
        }
        for (String marker : AMD_INTEGRATED_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
