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
 * A proposito NO hay ningun controlador que recalcule la escala cuadro a cuadro: el valor sale
 * directo de {@link ResScaleConfig#scale()}. Una version anterior con ajuste automatico por FPS SI
 * recreaba el framebuffer repetidamente durante el juego y corrompia el frame (geometria rota,
 * confirmado con un self-test coincidiendo con un resize + un stall); sacar el controlador
 * dinamico saca esa fuente de resizes en vivo.
 * <p>
 * <b>La escala se "commitea" (queda fija) al crear el framebuffer por primera vez en una sesion de
 * mundo, y NO se vuelve a tocar hasta salir del mundo</b> -aunque el usuario cambie el valor en la
 * config mientras juega, ese cambio recien se aplica la proxima vez que entra a un mundo. Esto no
 * es una limitacion arbitraria: se probo activamente permitir el cambio en caliente (recrear el
 * MainTarget con el nuevo tamaño en el momento) y se confirmo en vivo, en las dos direcciones
 * (achicar y agrandar), que deja el frame roto -geometria/agua desplazada- de forma consistente,
 * mientras que CUALQUIER arranque fresco a cualquier escala (incluida una sesion nueva tras salir
 * y volver a entrar) siempre funciono limpio en todas las pruebas. La causa exacta del cambio en
 * caliente no se aislo del todo (se descarto que fuera solo el resize() heredado de RenderTarget,
 * ver commit history); en vez de seguir cazandola, se opto por la garantia estructural: la unica
 * forma soportada de aplicar un cambio de escala es una construccion fresca. Dos disparadores
 * cuentan como "construccion fresca": salir del mundo ({@link #resetSession()}, via
 * ClientPlayerNetworkEvent.LoggingOut) y apagar+prender el toggle general del modulo SIN salir del
 * mundo (deteccion de transicion en {@link #beginWorldPass()}) -este segundo disparador se agrego
 * porque, sin el, apagar/prender el modulo parecia "no hacer nada" igual que cambiar la escala
 * sola, lo cual confundia sobre cual era el mecanismo real para aplicar un cambio.
 * <p>
 * <b>Causa raiz real encontrada (investigacion con Fable 5, verificada por bytecode) del bug de
 * agua/entidades desplazadas que sobrevivio a todos los fixes anteriores</b>:
 * {@code LevelRenderer#renderLevel} hace, TODOS los frames (con o sin mobs glowing),
 * {@code entityTarget.clear()} (deja el viewport GL en las dimensiones de {@code entityTarget})
 * seguido de {@code mainRenderTarget.bindWrite(false)} (que NO restaura el viewport), justo
 * ANTES de dibujar entidades, agua (translucent), particulas, nubes y clima -vanilla asume que
 * {@code entityTarget} siempre tiene el MISMO tamaño que el main target activo. Nuestro swap
 * cambia el tamaño del main target activo pero nunca redimensionaba {@code entityTarget}: ese
 * chain (el post-chain del entity outline/glowing) solo se resincroniza cuando VANILLA llama a
 * {@code PostChain.resize} (resize real de ventana/F11/cambio de GUI scale), asi que en cuanto
 * la escala cambiaba sin que pasara eso, el viewport de toda la segunda mitad del frame quedaba
 * desincronizado -exactamente el patron reportado (terreno bien, agua/entidades siempre mal,
 * "esquinado" en una direccion o slabs/bandas diagonales en la otra segun cual de los dos targets
 * quedara mas grande). Por eso los self-tests en la ventana de dev SIEMPRE salian limpios: la
 * ventana recibe eventos de resize/foco que casualmente reparan el invariante, algo que la sesion
 * real del usuario (cambia la escala, nunca toca la ventana) nunca dispara. Fix: sincronizar
 * {@code entityTarget} a mano, cada frame (comparacion de enteros, gratis), en
 * {@link #syncOutlineChain}.
 */
public final class ResScaleManager {

    private static MainTarget scaledTarget;
    private static RenderTarget realTarget;
    private static boolean worldPassActive = false;
    private static double committedScale = 1.0;
    private static boolean wasEnabled = false;
    private static boolean lodBiasApplied = false;

    /**
     * Escala EFECTIVA del world pass de este frame: committedScale cuando el swap esta activo,
     * 1.0 en cualquier bypass (modulo apagado, escala 1.0, shaderpack/Fabuloso). Es lo que
     * PostChainMixin/syncOutlineChain deben usar -el chain del outline tiene que seguir el
     * tamaño REAL del pass, no el valor crudo de la config.
     */
    private static double activeScale = 1.0;

    private ResScaleManager() {
    }

    public static void beginWorldPass() {
        worldPassActive = false;
        Minecraft mc = Minecraft.getInstance();
        boolean enabled = ResScaleConfig.isEnabled();
        if (!enabled) {
            wasEnabled = false;
            resetLodBias();
            activeScale = 1.0;
            syncOutlineChain(mc);
            return;
        }
        if (!wasEnabled) {
            // Transicion apagado->prendido (el toggle general de la GUI, no solo salir del
            // mundo): tambien cuenta como punto valido para recomprometer la escala -sin esto,
            // apagar y prender el modulo sin salir del mundo pareceria "no hacer nada" igual que
            // cambiar el valor de escala solo, lo cual confunde (reportado por el usuario).
            resetSession();
        }
        wasEnabled = true;

        if (scaledTarget == null) {
            // Recien ahora, al (re)crear el framebuffer, se lee el valor de config -queda
            // "commiteado" para el resto de la sesion de mundo. Ver el resetSession() llamado al
            // salir del mundo, que es lo unico que permite que un cambio de escala tenga efecto.
            committedScale = ResScaleConfig.scale();
        }

        if (committedScale == 1.0) {
            // Bypass: el mundo se dibuja directo sobre el mainRenderTarget real, sin swap.
            resetLodBias();
            activeScale = 1.0;
            syncOutlineChain(mc);
            return;
        }

        if (!(mc instanceof MinecraftAccessorMixin accessor)) {
            return;
        }

        if (isBypassedByShaderpackOrFabulous()) {
            resetLodBias();
            activeScale = 1.0;
            syncOutlineChain(mc);
            return;
        }

        Window window = mc.getWindow();
        int desiredWidth = scaleDim(window.getWidth(), committedScale);
        int desiredHeight = scaleDim(window.getHeight(), committedScale);

        if (scaledTarget == null || scaledTarget.width != desiredWidth || scaledTarget.height != desiredHeight) {
            // Reconstruir de cero (no resize()) ante cualquier cambio de tamaño -incluido un
            // resize real de la ventana nativa, que con la escala commiteada sin cambiar igual
            // puede pedir un tamaño de pixeles distinto. MainTarget tiene su propia logica de
            // asignacion con fallback (allocateAttachments/Dimension.listWithFallback, confirmado
            // por decompile) que solo corre en el constructor; resize() la saltea.
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

        activeScale = committedScale;
        syncOutlineChain(mc);

        applyLodBias(committedScale);

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
     * Compensa la seleccion automatica de mipmap de la GPU: con el viewport achicado, las
     * derivadas de pantalla hacen que se elija un mip mas grueso/borroso del que corresponde al
     * tamaño final en pantalla (despues del estirado). El sintoma es "el agua y las entidades se
     * ven borrosas/planas, los bloques solidos cercanos apenas se notan" -ya diagnosticado y
     * arreglado una vez en el modulo viejo "Resolucion de Render Dinamica" (ver memoria del
     * proyecto, v2.0.8), y reaparece aca porque este modulo es una reescritura desde cero que
     * nunca heredo ese fix. Bias NEGATIVO a mitad de fuerza (0.5, no 1.0 completo -la fuerza
     * completa arregla el mip pero mete shimmer/aliasing visible sin TAA que lo disimule)
     * aplicado via Direct State Access sobre la textura COMPARTIDA del atlas de bloques -sin
     * bindear nada, sin mixinear Sodium/Embeddium, que samplean esa misma textura de vanilla.
     */
    private static void applyLodBias(double scale) {
        if (lodBiasApplied) {
            return;
        }
        float bias = (float) (0.5 * (Math.log(scale) / Math.log(2.0)));
        int textureId = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId();
        GL45.glTextureParameterf(textureId, GL14.GL_TEXTURE_LOD_BIAS, bias);
        lodBiasApplied = true;
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
     * Llamado al salir de un mundo (ClientPlayerNetworkEvent.LoggingOut). Es la UNICA forma en que
     * un cambio de escala hecho en la config mientras se jugaba tiene efecto: se libera el
     * framebuffer actual, y la proxima vez que se entra a un mundo beginWorldPass() lo reconstruye
     * de cero leyendo el valor de config vigente en ese momento.
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

    /** Usado por WindowMixin. Devuelve la escala COMMITEADA (ver clase), no el valor de config en vivo. */
    public static double appliedScale() {
        return committedScale;
    }

    /** Usado por PostChainMixin: la escala EFECTIVA del world pass de este frame (1.0 en cualquier bypass). */
    public static double appliedScaleForPostChain() {
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
