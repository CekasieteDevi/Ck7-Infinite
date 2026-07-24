package com.ck7infinite.resscale;

import com.ck7infinite.Ck7Infinite;
import com.ck7infinite.resscale.mixin.MinecraftAccessorMixin;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraftforge.fml.ModList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL45;

/**
 * Estado y logica del modulo Escala de Resolucion. Arquitectura tomada de RenderScale (Zolo101,
 * ver memoria del proyecto): un MainTarget propio, del tamaño ya escalado (viewport siempre =
 * tamaño completo de ESTE target, nunca un sub-rectangulo de uno mas grande), swapeado dentro de
 * GameRenderer#renderLevel via MinecraftAccessorMixin, y bliteado (estirado) contra la ventana
 * real al terminar.
 * <p>
 * A proposito NO hay ningun controlador que recalcule la escala con ajuste automatico por FPS
 * (una version anterior si lo tenia y recreaba el framebuffer repetidamente durante el juego,
 * corrompiendo el frame -geometria rota, confirmado con un self-test coincidiendo con un resize +
 * un stall). Pero el VALOR de {@link ResScaleConfig#scale()} SI se relee todos los frames y se
 * aplica de inmediato si cambio -ver mas abajo por que esto es seguro ahora y no lo era antes.
 * <p>
 * <b>Causa raiz real (investigacion con Fable 5, verificada por bytecode) del bug de agua/
 * entidades desplazadas que hacia que cualquier cambio de escala en caliente rompiera el frame</b>:
 * {@code LevelRenderer#renderLevel} hace, TODOS los frames (con o sin mobs glowing),
 * {@code entityTarget.clear()} (deja el viewport GL en las dimensiones de {@code entityTarget})
 * seguido de {@code mainRenderTarget.bindWrite(false)} (que NO restaura el viewport), justo
 * ANTES de dibujar entidades, agua (translucent), particulas, nubes y clima -vanilla asume que
 * {@code entityTarget} siempre tiene el MISMO tamaño que el main target activo. Nuestro swap
 * cambia el tamaño del main target activo; si nada mas redimensiona {@code entityTarget} para que
 * coincida, el viewport de toda la segunda mitad del frame queda desincronizado -exactamente el
 * patron reportado (terreno bien, agua/entidades siempre mal, "esquinado" en una direccion o
 * slabs/bandas diagonales en la otra segun cual de los dos targets quedara mas grande). Antes de
 * este fix, la unica forma en que {@code entityTarget} se resincronizaba era que VANILLA llamara a
 * {@code PostChain.resize} (resize real de ventana/F11/cambio de GUI scale) -algo que la ventana
 * de dev dispara por accidente (eventos de resize/foco) pero una sesion real que solo cambia la
 * escala desde la GUI nunca. Por eso durante un tiempo se penso que "cambiar la escala en vivo"
 * era estructuralmente inseguro y se opto por exigir salir del mundo o apagar/prender el modulo
 * para aplicar un cambio -perooo la causa real no era el cambio en si, era que nada resincronizaba
 * {@code entityTarget}. Con {@link #syncOutlineChain} corrigiendo eso EN CADA FRAME (no solo en
 * una transicion puntual), leer la escala en vivo cada frame volvio a ser seguro: si cambia, el
 * bloque de abajo recrea {@code scaledTarget} al nuevo tamaño Y {@code syncOutlineChain} lo nota y
 * repara {@code entityTarget} en el MISMO frame, sin ventana de inconsistencia.
 */
public final class ResScaleManager {

    private static MainTarget scaledTarget;
    private static RenderTarget realTarget;
    private static boolean worldPassActive = false;
    private static boolean lodBiasApplied = false;
    private static float lastAppliedBias = Float.NaN;

    /**
     * Escala EFECTIVA del world pass de este frame: el valor de config cuando el swap esta activo,
     * 1.0 en cualquier bypass (modulo apagado, escala 1.0, shaderpack/Fabuloso). Es lo que
     * WindowMixin/PostChainMixin/syncOutlineChain deben usar -el chain del outline y el tamaño de
     * ventana reportado tienen que seguir el tamaño REAL del pass de este frame.
     */
    private static double activeScale = 1.0;

    private ResScaleManager() {
    }

    public static void beginWorldPass() {
        worldPassActive = false;
        Minecraft mc = Minecraft.getInstance();
        if (!ResScaleConfig.isEnabled()) {
            resetLodBias();
            activeScale = 1.0;
            syncOutlineChain(mc);
            return;
        }

        if (isBypassedByShaderpackOrFabulous()) {
            // Terreno no probado (shaders custom podrian no respetar el bias de mipmap de la
            // misma forma que vanilla/Sodium/Embeddium): ni la compensacion ni Aggressive
            // Mipmapping se aplican aca, mismo criterio cauteloso que ya regia para el swap.
            resetLodBias();
            activeScale = 1.0;
            syncOutlineChain(mc);
            return;
        }

        double scale = ResScaleConfig.scale();

        if (scale == 1.0) {
            // Sin reduccion de viewport, pero Aggressive Mipmapping puede seguir aplicando: la
            // compensacion de applyLodBias da 0 en scale=1.0, asi que esto es un no-op si
            // Aggressive Mipmapping tambien esta apagado.
            activeScale = 1.0;
            syncOutlineChain(mc);
            applyLodBias(1.0);
            return;
        }

        if (!(mc instanceof MinecraftAccessorMixin accessor)) {
            return;
        }

        Window window = mc.getWindow();
        int desiredWidth = scaleDim(window.getWidth(), scale);
        int desiredHeight = scaleDim(window.getHeight(), scale);

        if (scaledTarget == null || scaledTarget.width != desiredWidth || scaledTarget.height != desiredHeight) {
            // Reconstruir de cero (no resize()) ante cualquier cambio de tamaño -sea porque el
            // usuario cambio el valor de escala, o porque la ventana nativa se redimensiono.
            // MainTarget tiene su propia logica de asignacion con fallback
            // (allocateAttachments/Dimension.listWithFallback, confirmado por decompile) que solo
            // corre en el constructor; resize() la saltea.
            if (scaledTarget != null) {
                scaledTarget.destroyBuffers();
            }
            scaledTarget = new MainTarget(desiredWidth, desiredHeight);
            // El constructor de MainTarget deja el filtro en GL_NEAREST hardcodeado y nunca pasa
            // por setFilterMode (confirmado por decompile) -sin esta llamada, linearFilter de la
            // config era letra muerta para un target recien creado. El redirect de
            // RenderTargetMixin fuerza el valor de config real dentro de setFilterMode.
            scaledTarget.setFilterMode(ResScaleConfig.linearFilter() ? GL11.GL_LINEAR : GL11.GL_NEAREST);
            if (scaledTarget.width != desiredWidth || scaledTarget.height != desiredHeight) {
                // Dimension.listWithFallback podria degradar silenciosamente el tamaño si la GPU
                // rechazara la asignacion pedida -casi imposible (pedimos <= tamaño nativo ya
                // asignado), pero si pasara hace falta saberlo en vez de fallar en silencio.
                Ck7Infinite.LOGGER.warn("[resscale] MainTarget con tamaño distinto al pedido: pedido {}x{}, obtenido {}x{}",
                        desiredWidth, desiredHeight, scaledTarget.width, scaledTarget.height);
            }
        }

        activeScale = scale;
        syncOutlineChain(mc);

        applyLodBias(scale);

        realTarget = mc.getMainRenderTarget();
        accessor.ck7infinite$setMainRenderTarget(scaledTarget);
        worldPassActive = true;
        scaledTarget.bindWrite(true);
    }

    /** Truncacion, no redondeo -tiene que dar el MISMO numero que WindowMixin/PostChainMixin
     * para el mismo input, si no el chequeo de sync de {@link #syncOutlineChain} oscila y
     * redimensiona framebuffers todos los frames con anchos/altos nativos impares. */
    public static int scaleDim(int value, double scale) {
        return Math.max(1, (int) (value * scale));
    }

    /**
     * EL FIX CENTRAL (ver javadoc de la clase). Compara el tamaño actual de
     * {@code LevelRenderer#entityTarget()} contra el tamaño esperado para la escala EFECTIVA de
     * este frame, y solo si difieren llama a {@code LevelRenderer#resize(int,int)} -el mismo
     * camino publico que usa vanilla en cada resize de ventana, que redimensiona el chain
     * COMPLETO (final+swap+matriz orto) a traves del redirect que ya existe en PostChainMixin.
     * Se chequea cada frame (comparacion de 4 enteros, gratis); el resize real solo ocurre en
     * transiciones genuinas, con el mismo costo que un resize de ventana vanilla (incluido su
     * needsUpdate() de re-evaluar el grafo de chunks) -aceptable por lo infrecuente que es.
     */
    private static void syncOutlineChain(Minecraft mc) {
        RenderTarget outline = mc.levelRenderer.entityTarget();
        if (outline == null) {
            return;
        }
        Window window = mc.getWindow();
        int expectedWidth = scaleDim(window.getWidth(), activeScale);
        int expectedHeight = scaleDim(window.getHeight(), activeScale);
        if (outline.width == expectedWidth && outline.height == expectedHeight) {
            return;
        }
        mc.levelRenderer.resize(window.getWidth(), window.getHeight());
        // LevelRenderer#resize deja el FBO 0 bindeado al terminar. En el camino donde vamos a
        // activar el swap esto lo pisa el bindWrite(true) del scaledTarget que sigue; en los
        // caminos de bypass, restaurar aca evita que el resto del frame se dibuje contra el
        // framebuffer 0 en vez del mainRenderTarget real.
        mc.getMainRenderTarget().bindWrite(true);
    }

    /**
     * Combina DOS biases de mipmap independientes en una sola escritura GL (GL_TEXTURE_LOD_BIAS
     * es un unico valor de sampler, no se puede aplicar dos veces):
     * <p>
     * 1) Compensacion (NEGATIVA): con el viewport achicado por resscale, las derivadas de
     * pantalla hacen que la GPU elija un mip mas grueso/borroso del que corresponde al tamaño
     * final en pantalla (despues del estirado). El sintoma es "el agua y las entidades se ven
     * borrosas/planas, los bloques solidos cercanos apenas se notan" -ya diagnosticado y
     * arreglado una vez en el modulo viejo "Resolucion de Render Dinamica" (ver memoria del
     * proyecto, v2.0.8). Bias a mitad de fuerza (0.5, no 1.0 completo -la fuerza completa arregla
     * el mip pero mete shimmer/aliasing visible sin TAA que lo disimule). Da 0 cuando scale=1.0.
     * <p>
     * 2) Aggressive Mipmapping (POSITIVA, opt-in): fuerza mips mas gruesos A PROPOSITO para
     * ahorrar ancho de banda de textura -el eje correcto en una iGPU limitada por bandwidth/
     * fillrate (ver V2_ROADMAP.md seccion G)-, independiente de si resscale esta reduciendo el
     * viewport. Es la contrapartida exacta de (1): mientras (1) busca nitidez, (2) la sacrifica
     * a proposito por rendimiento.
     * <p>
     * Ambas se aplican via Direct State Access sobre la textura COMPARTIDA del atlas de bloques
     * -sin bindear nada, sin mixinear Sodium/Embeddium, que samplean esa misma textura de vanilla.
     */
    private static void applyLodBias(double scale) {
        float compensationBias = scale == 1.0 ? 0f : (float) (0.5 * (Math.log(scale) / Math.log(2.0)));
        float aggressiveBias = ResScaleConfig.aggressiveMipmapping() ? (float) ResScaleConfig.aggressiveMipmappingBias() : 0f;
        float bias = compensationBias + aggressiveBias;

        // Con los valores en vivo (no commiteados), un cambio en cualquiera de los dos config
        // tiene que re-emitir el bias en el mismo frame -no alcanza con el flag de "ya aplicado
        // alguna vez", si no un cambio de bias sin pasar por un bypass intermedio dejaria el
        // valor viejo pegado.
        if (lodBiasApplied && lastAppliedBias == bias) {
            return;
        }
        int textureId = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId();
        GL45.glTextureParameterf(textureId, GL14.GL_TEXTURE_LOD_BIAS, bias);
        lodBiasApplied = true;
        lastAppliedBias = bias;
    }

    private static void resetLodBias() {
        if (!lodBiasApplied) {
            return;
        }
        int textureId = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId();
        GL45.glTextureParameterf(textureId, GL14.GL_TEXTURE_LOD_BIAS, 0.0f);
        lodBiasApplied = false;
    }

    /**
     * Llamado al salir de un mundo (ClientPlayerNetworkEvent.LoggingOut). Ya no hace falta para que
     * un cambio de escala tenga efecto (eso ahora se aplica solo, en vivo, ver javadoc de la
     * clase) -esto es solo limpieza de memoria de GPU: libera el framebuffer mientras no hay mundo
     * cargado, en vez de dejarlo reservado sin uso hasta la proxima vez que entre a un mundo.
     */
    public static void resetSession() {
        if (scaledTarget != null) {
            scaledTarget.destroyBuffers();
            scaledTarget = null;
        }
        realTarget = null;
        worldPassActive = false;
        activeScale = 1.0;
        resetLodBias();
    }

    public static void endWorldPass() {
        if (!worldPassActive) {
            worldPassActive = false;
            return;
        }
        // Bajar el flag ANTES de restaurar/blitear: blitToScreen debe recibir dimensiones
        // NATIVAS (realTarget.width/height, no window.getWidth()), y con el flag todavia arriba
        // WindowMixin seguiria devolviendo el tamaño escalado.
        worldPassActive = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc instanceof MinecraftAccessorMixin accessor && realTarget != null) {
            accessor.ck7infinite$setMainRenderTarget(realTarget);
        }
        if (realTarget != null) {
            realTarget.bindWrite(true);
            scaledTarget.blitToScreen(realTarget.width, realTarget.height);
        }
        realTarget = null;
    }

    /**
     * Red de seguridad: si una excepcion corta renderLevel a mitad de camino (antes de llegar al
     * RETURN donde vive endWorldPass), el render target real quedaria swapeado para siempre y el
     * juego se rompe visualmente por completo. Llamado desde ResScaleClientHandler en
     * RenderTickEvent END, que corre pase lo que pase.
     */
    public static void safetyNetRestore() {
        if (!worldPassActive) {
            return;
        }
        Ck7Infinite.LOGGER.warn("[resscale] worldPassActive seguia activo al final del frame (posible excepcion a mitad de renderLevel); restaurando el render target real.");
        endWorldPass();
    }

    /** Usado por WindowMixin: solo escala Window#getWidth()/getHeight() mientras esto es true. */
    public static boolean isWorldPassActive() {
        return worldPassActive;
    }

    /** Usado por WindowMixin/PostChainMixin: la escala EFECTIVA del world pass de este frame (1.0 en cualquier bypass). */
    public static double appliedScale() {
        return activeScale;
    }

    private static boolean isBypassedByShaderpackOrFabulous() {
        if (Minecraft.useShaderTransparency()) {
            return true;
        }
        return isShaderpackActive();
    }

    /**
     * Deteccion por reflexion, sin dependencia de compilacion contra Iris/Oculus: ambos exponen
     * la misma API IrisApi (Oculus es un fork de Iris para Forge). Si el jar no esta presente,
     * cualquier excepcion de reflexion se traga y se asume que no hay shaderpack activo.
     */
    private static boolean isShaderpackActive() {
        if (!ModList.get().isLoaded("oculus") && !ModList.get().isLoaded("iris")) {
            return false;
        }
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApiClass.getMethod("getInstance").invoke(null);
            Object result = irisApiClass.getMethod("isShaderPackInUse").invoke(instance);
            return Boolean.TRUE.equals(result);
        } catch (Throwable t) {
            return false;
        }
    }
}
