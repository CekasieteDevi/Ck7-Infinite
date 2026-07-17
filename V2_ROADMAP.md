# Ck7 - Conatus V2 — Roadmap de análisis

Fuente: `Recomendaciones y Gaps para Conatus.pdf` (generado con Kimi.ai, en
`D:\Nacho\Youtube\Devi\Mods\Ck7 - Conatus\`). Todo lo que existe hoy en el repo (6 módulos + toggle
maestro + GUI) es **V1**. Este documento organiza las propuestas del PDF, las evalúa una por una
(factibilidad real en un mod Forge/Java, si ya está cubierto por Sodium/Embeddium/Canary, riesgo de
romper cosas) y propone un orden de trabajo.

**Regla de trabajo acordada:** se implementa **una feature a la vez**. No se pasa a la siguiente
hasta que la actual esté completa y probada con test de FPS (antes/después, mismo escenario). iGPU
laptop es el hardware de validación real ([[project_igpu_testing]] en memoria) — el dev desktop con
GPU dedicada no puede validar el caso de uso que este mod ataca.

---

## Lectura crítica del PDF

Antes de la lista: el documento es un análisis genérico generado por IA, no un audit del código de
Conatus. Varias de sus "gaps" ya están cubiertos por mods que el usuario ya tiene en su stack de
testing (Embeddium hace async chunk meshing, buffer management, batching — ver sección A/B abajo),
y un par de ítems chocan directamente con decisiones que ya tomamos en V1 con evidencia real:

> **Dynamic Resolution Scaling** (sección E) es literalmente el módulo `dynamicres` que **sacamos
> de V1** (commit `cff0fc7`) porque el auto-ajuste de escala corrompía el frame con Embeddium
> instalado — un race confirmado en el pipeline de upload async de Embeddium, no un bug nuestro. El
> `resscale` actual (escala fija, aplicada en vivo pero sin auto-ajuste continuo) es la versión seria
> de esa idea. Si querés retomar el auto-ajuste hay que decidirlo explícitamente sabiendo que ya
> falló una vez por esta razón exacta — no lo puse en la lista de "listo para implementar" sin que
> lo veas primero.

---

## A. Optimización específica para gráficos integrados

| Feature | Factibilidad | Veredicto |
|---|---|---|
| Driver Call Batching | Baja | Es literalmente lo que hace Embeddium (que ya es dependencia recomendada). Reimplementarlo en Conatus es duplicar trabajo y arriesgar conflictos con el pipeline de Embeddium. **Descartar.** |
| Persistent Buffer Mapping (AZDO) | Baja | Mismo problema: toca el mismo buffer management que Embeddium ya gestiona. Hacerlo nosotros sin reemplazar su renderer es, en el mejor caso, redundante y en el peor, un conflicto de quién es dueño del buffer. **Descartar.** |
| Texture Atlas Dinámico | Muy baja | Reimplementar el stitching de atlas en runtime es rehacer una parte central del asset pipeline vanilla. Costo altísimo, beneficio no cuantificado. **Descartar.** |
| Software Vertex Processing (SIMD en Java) | Muy baja | Java no tiene SIMD real hasta el Vector API (incubator), y mover vértices a CPU cuando el objetivo es *iGPU* (que ya comparte esa misma CPU) es contraproducente. **Descartar.** |

## B. Paralelización agresiva del render thread

| Feature | Factibilidad | Veredicto |
|---|---|---|
| Async Chunk Mesh Building | N/A | Ya lo hace Embeddium. **Descartar** (redundante). |
| Parallel Entity Render Prep | Media | Real, pero de alcance grande (tocar matrices/animación de todas las entidades en un thread pool sincronizado con el render thread). Posible V2 tardío, no primer candidato. |
| Job System para Partículas | Media-baja | El módulo 3 (Particle Priority) ya reduce el volumen de partículas por prioridad/contexto; paralelizar el *update* de las que quedan tiene payoff dudoso salvo que perfilemos primero y veamos que realmente pesa. Necesita medición antes de decidir. |
| Command Buffer Multithreading | Inviable en este contexto | Un contexto OpenGL solo puede estar activo en un thread a la vez; generar comandos GL "desde múltiples threads" sin GL 4.5+ AZDO real y sin pelearse con el pipeline de Embeddium es, en la práctica, reescribir el renderer. Fuera de alcance de un mod addon. **Descartar.** |

## C. Gestión de memoria zero-allocation

| Feature | Factibilidad | Veredicto |
|---|---|---|
| Object Pooling Global | Baja (alcance "todo") | Pooling de *nuestras* estructuras (ej. las que ya usa Particle Priority/Ultra Load) es razonable; "reutilizar TODOS los objetos temporales de renderizado" vanilla requiere Mixin invasivo sobre `BufferBuilder`/`VertexConsumer`, terreno de Sodium/Embeddium. Acotar a nuestro propio código si aparece un cuello de botella real medido. |
| Arena Allocators por Frame | Baja | El punto de partida del PDF (GC = micro-stutters) está algo desactualizado: G1/el TLAB de la JVM ya maneja objetos de vida corta barato. Antes de construir esto, medir con `spark`/GC logs si realmente hay presión de GC atribuible a Conatus. Sin ese dato, **no priorizar**. |
| Pre-sized Vertex Buffers | Baja | Terreno de Embeddium (ya lo optimiza). **Descartar.** |
| Memory-mapped Chunk Caches | Baja | Toca I/O de mundo controlado por vanilla/Chunky; alto riesgo, bajo payoff claro. **Descartar.** |

## D. Optimizaciones de CPU-general — la zona con más encaje real

| Feature | Factibilidad | Veredicto |
|---|---|---|
| **Adaptive Simulation Distance** | **Alta** | Encaja directo con lo que el mod ya hace (Ultra Load ya reduce entity distance/framerate bajo carga de chunks). Extender esa misma lógica a bajar `simulationDistance` cuando el TPS cae sostenidamente. Candidato fuerte. |
| Redstone Tick Skipping | Media-baja, riesgo alto | Detectar "circuito estable" sin romper relojes de redstone que la gente ya tiene armados es difícil de garantizar. Si se hace mal, rompe builds existentes de forma silenciosa — el peor tipo de bug para un mod de optimización. Dejar para más adelante, con opt-in explícito. |
| **Entity AI LOD** | **Alta** | Es una extensión natural del módulo 1 (`MobFreezeTickHandler` ya calcula distancia/línea de visión por mob cada `evaluationIntervalTicks`). Hoy es binario (congelado/activo); agregar un tier intermedio (IA a intervalo reducido en vez de 0) es un cambio acotado sobre código que ya entendemos bien. Candidato fuerte. |
| Block Update Coalescing | Baja, riesgo alto | Muchos mods dependen de la semántica exacta de timing de updates vanilla. Alto riesgo de romper compat con mods de tecnología (redstone, tuberías, etc.). **Descartar** salvo pedido específico. |
| JVM Warmup Helper | Baja prioridad | Factible (llamar métodos críticos al boot para forzar JIT temprano) pero el payoff es sobre los primeros segundos de sesión, no sobre FPS sostenido — que es lo que vamos a medir. Baja prioridad. |

## E. Optimizaciones para PCs "Potato"

| Feature | Factibilidad | Veredicto |
|---|---|---|
| Dynamic Resolution Scaling | — | Ver advertencia arriba. **Requiere decisión explícita tuya antes de tocarlo.** |
| Aggressive Mipmapping | Media | `resscale`/`WindowMixin` ya toca LOD bias del sampler; forzar mips más bajos a distancias menores es una extensión chica sobre terreno conocido. Candidato secundario, bajo costo. |
| **Particle Budget System** | **Alta** | Extensión directa del módulo 3: agregar un tope duro de partículas activas con desalojo por prioridad (ya existe la clasificación combate/interacción/redstone/ambiente). Candidato fuerte. |
| Simplified Physics | Media, riesgo medio | Físicas simplificadas para entidades/items lejanos es viable pero hay que acotar bien el alcance (¿solo items dropeados lejos? ¿nunca cerca de jugadores, como el `safeRadiusBlocks` de Ultra Load?) para no romper mecánicas. Diseño necesario antes de codear. |
| Sleepy Mode (bajar polling de input) | Muy baja | El polling de input en GLFW no es un cuello de botella real en Minecraft; el diagnóstico del PDF es incorrecto acá. **Descartar.** |

## Recomendaciones estratégicas del PDF

| Item | Veredicto |
|---|---|
| **Perfil "Potato PC"** (preset automático por detección de hardware) | **Alta prioridad, alto valor.** Encaja perfecto con el toggle maestro que ya existe y con el propósito original del proyecto (laptop iGPU). Detectar ausencia de GPU dedicada vía `GL_RENDERER`/`GL_VENDOR` (buscar "Intel"/"AMD" + ausencia de "NVIDIA"/marcas dedicadas) y aplicar un preset agresivo de todos los módulos. Buen candidato para ir temprano, después de tener 1-2 features nuevas que el preset pueda activar. |
| Launcher wrapper con JVM args óptimos | **No es viable como mod.** Un mod corre *dentro* de una JVM ya iniciada con los flags que sea; no puede reconfigurar retroactivamente `-Xmx`/GC del proceso actual. Lo único realista es un mensaje informativo (mismo patrón que el aviso de Sodium) sugiriendo flags si detectamos algo subóptimo. Alcance mucho menor al que sugiere el PDF. |
| CPU Profiler integrado en F3 | Media prioridad, factible. Ya tenemos `spark` como dependencia dev-only; un overlay liviano (render/tick/IO/GC) via `RenderGuiOverlayEvent` es razonable. Dejar para después de tener 2-3 módulos nuevos que valga la pena diagnosticar. |
| Compatibilidad con Embeddium+Canary+FerriteCore+ModernFix | No es una feature, es QA continuo — aplica a *cada* ítem de esta lista, no es un ticket aparte. |
| Hooks para renderizado modded (TileEntitySpecialRenderer) | Fuera de alcance de V2. Es compromiso de API pública (diseño + mantenimiento a largo plazo). Candidato a V3+ si hay pedidos concretos de otros modders. |

---

## Orden propuesto (a confirmar con vos)

**Fase 1 — extienden módulos que ya entendemos, riesgo bajo, valor claro:**
1. **Entity AI LOD** (extiende Módulo 1 — Mob AI Freeze)
2. **Particle Budget System** (extiende Módulo 3 — Particle Priority)
3. **Adaptive Simulation Distance** (nuevo, mismo patrón que Ultra Load)

**Fase 2 — construye sobre la Fase 1:**
4. **Perfil "Potato PC"** (preset automático — más útil una vez que hay más módulos que activar)
5. Aggressive Mipmapping (extiende `resscale`)

**Decisión pendiente tuya, no programada todavía:**
- Dynamic Resolution Scaling (auto-ajuste) — retomar o no, sabiendo el historial con Embeddium.
- Simplified Physics — necesita spec de alcance antes de estimarlo.
- Redstone Tick Skipping — alto riesgo, dejar para cuando el resto esté sólido.

**Descartados** (redundantes con Embeddium/Sodium, inviables en un mod Forge, o diagnóstico del PDF
incorrecto): Driver Call Batching, Persistent Buffer Mapping, Texture Atlas Dinámico, Software Vertex
Processing, Async Chunk Mesh Building, Command Buffer Multithreading, Pre-sized Vertex Buffers,
Memory-mapped Chunk Caches, Block Update Coalescing, Sleepy Mode, launcher wrapper de JVM (tal como
está planteado), hooks públicos de render modded.

---

## F. Ideas propias (fuera del PDF, verificadas contra el ecosistema actual)

El PDF no es la única fuente posible; busqué qué falta *de verdad* hoy en el ecosistema 1.20.1 antes
de proponer nada, para no duplicar mods que el usuario ya podría tener instalados.

| Idea | Estado de investigación | Veredicto |
|---|---|---|
| **Sound Priority** (módulo nuevo, mismo patrón que Particle Priority: categorías, radio, cap de instancias concurrentes, dedupe de sonidos idénticos disparados en el mismo tick — ej. una horda de zombies gimiendo a la vez) | Busqué mods de "sound culling/priority" para Forge 1.20.1 — no encontré nada equivalente, solo un mod llamado "Sounds" que es config de recursos, no de performance. | **Gap real, candidato fuerte.** El esqueleto ya existe: `ParticlePriorityConfig`/`ParticleClassifier` es un template directo (categorías + radios + contexto reciente) que se puede portar a `OpenAL`/sound engine con cambios acotados. |
| **Cap nativo del thread pool de worldgen/chunk background** (`Util.backgroundExecutor()` en vanilla usa `cpuCount - 1` clamped a 7 según la documentación pública de un agente Java externo que hace justamente esto) | Confirmé que el problema es real y documentado (`saharNooby/minecraft-thread-pool-agent`, un **Java agent externo**, no un mod), y que en CPUs de 4 núcleos/8 hilos el pool default satura el sistema (14 threads entre los dos pools de worldgen). No encontré ningún **mod Forge** que resuelva esto nativamente — hoy la única solución pública es ese agente externo (flag de lanzamiento `-javaagent`), que es justo la fricción que un mod evita. | **Gap real y diferenciado**, pero necesita una prueba de concepto antes de comprometerlo al roadmap: hay que verificar en nuestro propio entorno de dev si `Util`'s pool estático se inicializa antes o después de que nuestro mod pueda actuar (system property `max.bg.threads` seteada por nosotros, o Mixin sobre el cálculo del tamaño). Si el timing no da, esto no es viable sin un coremod/servicio de carga temprana. |
| **Generalizar Ultra Load en una máquina de estados con múltiples disparadores** (hoy solo dispara por carga de chunks; agregar AFK/inactividad y TPS sostenido bajo como disparadores adicionales que reusen exactamente los mismos tiers/efectos/`safeRadiusBlocks`) | Busqué "AFK detection reduce tick rate" — **ya existen varios mods** (Tick Tweaks, FPS Optimizer, AFKStatus) que hacen throttling por inactividad de forma standalone. | El AFK-throttling *standalone* está cubierto, no vale la pena rehacerlo. Pero ninguno de esos mods comparte la sofisticación de nuestro sistema de tiers + radio seguro por jugador. La idea con valor real es la **refactorización arquitectónica**: que `UltraLoadServerHandler` acepte disparadores plugables (carga de chunks, AFK, TPS bajo) en vez de tener el chequeo de chunk-rate hardcodeado, para que el "Adaptive Simulation Distance" de la Fase 1 y un futuro trigger de AFK reusen la misma máquina de tiers en lugar de triplicar lógica. Es más una decisión de diseño para cuando lleguemos a esos ítems que un módulo nuevo en sí. |
| ~~Hopper/container tick throttling~~ | Busqué explícitamente — **ya resuelto** por varios mods maduros y populares (Fast Hoppers, Hopper X-Treme, TickAccelerate). | **Descartado.** No hay gap; si el usuario quiere esto, instala uno de esos. Vale la pena sumarlo a la lista de compatibilidad testeada en vez de reimplementarlo. |

---

## Protocolo de test de FPS (a definir en detalle antes del ítem #1)

Por cada feature: mismo mundo/seed, mismo punto de spawn, misma carga de mobs/chunks, medición
antes/después con el toggle del módulo específico apagado vs. prendido — no comparaciones cruzadas
entre features distintas. Correr en la laptop iGPU (el hardware real objetivo), no en el dev desktop.
