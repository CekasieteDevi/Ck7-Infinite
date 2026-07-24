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

## Verificación contra el código real (2026-07-24)

Las estimaciones de esfuerzo de la primera versión de este documento se escribieron desde la
descripción de los módulos, no leyendo el código. Al verificarlas archivo por archivo, **cuatro
afirmaciones de "esto ya existe, es extensión barata" resultaron falsas**. Quedan corregidas en las
tablas de abajo; se listan acá juntas porque cambian el tamaño y el orden del trabajo:

1. **El "sistema de tiers" de Ultra Carga no existe.** `UltraLoadServerHandler:29` tiene un único
   `boolean currentlyActive`, y `UltraLoadState.isActive()` es un booleano global. Los "Tier 1" /
   "Tier 3" son **etiquetas en comentarios** (`UltraLoadClientHandler:15,21`) que describen toggles
   de config independientes (`hideEntities`, `reduceEntityDistance`, `limitFramerateDuringLoad`), y
   los tres disparan *a la vez* contra el mismo booleano. No hay escalonamiento. Todo lo que este
   documento decía sobre "reusar los tiers existentes" es **construirlos por primera vez**.

2. **Entity AI LOD no es una extensión acotada del Módulo 1.** El javadoc de
   `MobFreezeTickHandler:15-21` documenta que el módulo toca **solo el flag NoAI**, y por qué se
   rechazó `LivingTickEvent` (cancelarlo saltea el tick entero: fuego, ahogo, efectos, caída →
   rompe drops/farms). `NoAI` es binario por naturaleza; no hay flag vanilla de "IA a ritmo
   reducido". Un tier intermedio exige un hook **nuevo** (Mixin sobre `GoalSelector.tick` /
   `customServerAiStep`, salteando N de cada M), o sea maquinaria distinta sobre justo la superficie
   que el módulo evitó a propósito.

3. **El desalojo de partículas está del lado equivocado del hook.** El mixin actual
   (`LevelRendererMixin:36-40`) está en `addParticleInternal` HEAD: es un **veto en spawn**.
   Desalojar partículas ya vivas exige tocar las colas internas de `ParticleEngine` y almacenar la
   categoría por instancia viva (hoy `ParticleDecision` se calcula y se descarta). El cap con
   *admisión* por prioridad sí es extensión pura del hook actual; el desalojo es otra feature.

4. **No hay infraestructura de presets, y la heurística de detección propuesta es incorrecta.**
   `Ck7MasterConfig` es un único booleano y cada módulo tiene su `ForgeConfigSpec` independiente; un
   preset implica escribir across 6 specs más el baile de `refresh()` que el propio código documenta
   como frágil (`Ck7MasterConfig:47-53`). Y la heurística `"Intel"/"AMD"` + ausencia de NVIDIA
   **falla con AMD**, que fabrica dedicadas e integradas: una APU moderna reporta
   `"AMD Radeon(TM) Graphics"` y quedaría clasificada igual que una RX dedicada.

Lo único cuya estimación de bajo costo **sobrevivió** la verificación: Aggressive Mipmapping. El LOD
bias existe y funciona (`ResScaleManager:187-199`, DSA sobre el atlas compartido).

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

## D. Optimizaciones de CPU-general

> Nota posterior: esta sección estaba titulada "la zona con más encaje real". Sigue siendo la zona
> donde el PDF acierta más seguido, pero **no es la de mayor retorno en el hardware objetivo** — ver
> sección G: en iGPU el cuello de botella es fillrate/bandwidth, no CPU. Estas features valen por
> stutter y TPS, no por FPS.

| Feature | Factibilidad | Veredicto |
|---|---|---|
| **Adaptive Simulation Distance** | **Alta** | Encaja directo con lo que el mod ya hace (Ultra Load ya reduce entity distance/framerate bajo carga de chunks). Extender esa misma lógica a bajar `simulationDistance` cuando el TPS cae sostenidamente. Candidato fuerte. |
| Redstone Tick Skipping | Media-baja, riesgo alto | Detectar "circuito estable" sin romper relojes de redstone que la gente ya tiene armados es difícil de garantizar. Si se hace mal, rompe builds existentes de forma silenciosa — el peor tipo de bug para un mod de optimización. Dejar para más adelante, con opt-in explícito. |
| **Entity AI LOD** | **Media** (corregido, era "Alta") | Reusa la *selección* de mobs del módulo 1 (`MobFreezeTickHandler` ya calcula distancia/LOS por mob cada `evaluationIntervalTicks`), pero **no su mecanismo de aplicación**: hoy solo escribe el flag `NoAI`, que es binario. El tier intermedio necesita un Mixin nuevo sobre `GoalSelector.tick`/`customServerAiStep` — ver corrección #2 arriba. Riesgo de diseño no trivial: saltear goal ticks con navegación activa produce pathing a tirones y mobs que se pasan de largo del objetivo; hay que decidir si el skip aplica a goals pero no a `navigation.tick()`. Sigue siendo buena feature, pero no es "cambio acotado". |
| Block Update Coalescing | Baja, riesgo alto | Muchos mods dependen de la semántica exacta de timing de updates vanilla. Alto riesgo de romper compat con mods de tecnología (redstone, tuberías, etc.). **Descartar** salvo pedido específico. |
| JVM Warmup Helper | Baja prioridad | Factible (llamar métodos críticos al boot para forzar JIT temprano) pero el payoff es sobre los primeros segundos de sesión, no sobre FPS sostenido — que es lo que vamos a medir. Baja prioridad. |

## E. Optimizaciones para PCs "Potato"

| Feature | Factibilidad | Veredicto |
|---|---|---|
| Dynamic Resolution Scaling | — | Ver advertencia arriba. **Requiere decisión explícita tuya antes de tocarlo.** |
| Aggressive Mipmapping | Media | `resscale`/`WindowMixin` ya toca LOD bias del sampler; forzar mips más bajos a distancias menores es una extensión chica sobre terreno conocido. Candidato secundario, bajo costo. |
| **Particle Budget System** | **Alta** (la mitad barata) | Partir en dos: (a) **cap duro con admisión por prioridad** —contar vivas, rechazar nuevas de baja prioridad sobre el tope— es extensión pura del hook que ya existe, factibilidad Alta real; (b) **desalojo** de partículas ya vivas es otra feature, más cara (ver corrección #3). Hacer (a) solamente. Ojo con la métrica: en iGPU el costo de partículas es **fillrate por overdraw**, no conteo — ver sección G.1. |
| Simplified Physics | Media, riesgo medio | Físicas simplificadas para entidades/items lejanos es viable pero hay que acotar bien el alcance (¿solo items dropeados lejos? ¿nunca cerca de jugadores, como el `safeRadiusBlocks` de Ultra Load?) para no romper mecánicas. Diseño necesario antes de codear. |
| Sleepy Mode (bajar polling de input) | Muy baja | El polling de input en GLFW no es un cuello de botella real en Minecraft; el diagnóstico del PDF es incorrecto acá. **Descartar.** |

## Recomendaciones estratégicas del PDF

| Item | Veredicto |
|---|---|
| **Perfil "Potato PC"** (preset automático por detección de hardware) | **Alto valor, pero más caro de lo estimado y con la heurística mal planteada** (corrección #4). No hay infraestructura de presets: hace falta una vía cross-módulo de "aplicar valores + refrescar los 6 specs" que no existe. Y la detección por string falla con AMD (fabrica dedicadas e integradas; una APU reporta `"AMD Radeon(TM) Graphics"`). Recomendación: **no auto-aplicar**. Usar el patrón que ya existe en `sodiumwarning` — *sugerir* el preset por mensaje de chat con un botón/comando para aplicarlo. Auto-cambiar settings que alteran el feel del juego a partir de un match de string es la receta para reportes de "el mod me rompió la partida", y encima con un falso positivo en toda una marca de GPU. |
| Launcher wrapper con JVM args óptimos | **No es viable como mod.** Un mod corre *dentro* de una JVM ya iniciada con los flags que sea; no puede reconfigurar retroactivamente `-Xmx`/GC del proceso actual. Lo único realista es un mensaje informativo (mismo patrón que el aviso de Sodium) sugiriendo flags si detectamos algo subóptimo. Alcance mucho menor al que sugiere el PDF. |
| CPU Profiler integrado en F3 | Media prioridad, factible. Ya tenemos `spark` como dependencia dev-only; un overlay liviano (render/tick/IO/GC) via `RenderGuiOverlayEvent` es razonable. Dejar para después de tener 2-3 módulos nuevos que valga la pena diagnosticar. |
| Compatibilidad con Embeddium+Canary+FerriteCore+ModernFix | No es una feature, es QA continuo — aplica a *cada* ítem de esta lista, no es un ticket aparte. |
| Hooks para renderizado modded (TileEntitySpecialRenderer) | Fuera de alcance de V2. Es compromiso de API pública (diseño + mantenimiento a largo plazo). Candidato a V3+ si hay pedidos concretos de otros modders. |

---

## Orden propuesto (revisado — ver sección G para el razonamiento)

El orden anterior (AI LOD → Particle Budget → Adaptive Sim Distance) tenía **dos problemas
estructurales**:

- **Dependencia invertida.** Los ítems 1 y 3 necesitan ambos un estado de severidad graduada. La
  sección F ya lo notaba pero lo archivaba como "decisión de diseño para cuando lleguemos". Es al
  revés: hacer AI LOD con su propio tiering ad-hoc y después Adaptive Sim Distance con otro
  construye exactamente la lógica triplicada que esa nota advertía.
- **Eje equivocado para el hardware objetivo.** Los tres ítems de la Fase 1 vieja son trabajo de
  **tick de servidor** (CPU). El cuello de botella de una iGPU es **ancho de banda de memoria y
  fillrate** (sección G). Optimizar TPS en una máquina cuyo problema es fillrate puede dar cero
  cambio de FPS y parecer un fracaso.

**Fase 0 — desbloquea todo lo demás:**
0. **Refactor de Ultra Carga: booleano → nivel graduado con disparadores plugables.** Chico,
   autocontenido, y verificable contra el comportamiento actual (nivel 0/1 debe reproducir el
   on/off de hoy *exactamente* — ese es el criterio de aceptación). Sin esto, los ítems 3 y 5
   duplican máquina de estados.

**Fase 1 — atacan el cuello de botella real de la iGPU (fillrate/bandwidth):**
1. **Aggressive Mipmapping** (extiende `resscale`) — el único ítem cuya estimación de bajo costo
   sobrevivió la verificación de código, y pega justo en el eje correcto. Empezar por acá.
2. **Cap de partículas por presupuesto de overdraw** (extiende Módulo 3) — ver G.1: el presupuesto
   debería medirse en cobertura de pantalla, no en conteo.
3. **Completar el LOD bias a los atlas que hoy quedan afuera** (ver G.2) — bug latente, no feature.

**Fase 2 — CPU/tick: valen la pena por stutter y TPS, no por FPS:**
4. **Entity AI LOD** (extiende Módulo 1, con el hook nuevo de la corrección #2)
5. **Adaptive Simulation Distance** (encima del nivel graduado de la Fase 0)

**Fase 3:**
6. **Perfil "Potato PC"**, como *sugerencia* por chat, no auto-aplicado (ver fila corregida arriba).

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
de proponer nada, para no duplicar mods que el usuario ya podría tener instalados. (Una segunda
tanda de ideas, ya filtrada por el criterio de iGPU, está en **G.3**.)

| Idea | Estado de investigación | Veredicto |
|---|---|---|
| **Sound Priority** (módulo nuevo, mismo patrón que Particle Priority: categorías, radio, cap de instancias concurrentes, dedupe de sonidos idénticos disparados en el mismo tick — ej. una horda de zombies gimiendo a la vez) | Busqué mods de "sound culling/priority" para Forge 1.20.1 — no encontré nada equivalente, solo un mod llamado "Sounds" que es config de recursos, no de performance. | **Gap real, candidato fuerte.** El esqueleto ya existe: `ParticlePriorityConfig`/`ParticleClassifier` es un template directo (categorías + radios + contexto reciente) que se puede portar a `OpenAL`/sound engine con cambios acotados. |
| **Cap nativo del thread pool de worldgen/chunk background** (`Util.backgroundExecutor()` en vanilla usa `cpuCount - 1` clamped a 7 según la documentación pública de un agente Java externo que hace justamente esto) | Confirmé que el problema es real y documentado (`saharNooby/minecraft-thread-pool-agent`, un **Java agent externo**, no un mod), y que en CPUs de 4 núcleos/8 hilos el pool default satura el sistema (14 threads entre los dos pools de worldgen). No encontré ningún **mod Forge** que resuelva esto nativamente — hoy la única solución pública es ese agente externo (flag de lanzamiento `-javaagent`), que es justo la fricción que un mod evita. | **Gap real y diferenciado**, pero necesita una prueba de concepto antes de comprometerlo al roadmap: hay que verificar en nuestro propio entorno de dev si `Util`'s pool estático se inicializa antes o después de que nuestro mod pueda actuar (system property `max.bg.threads` seteada por nosotros, o Mixin sobre el cálculo del tamaño). Si el timing no da, esto no es viable sin un coremod/servicio de carga temprana. |
| **Generalizar Ultra Load en una máquina de estados con múltiples disparadores** (hoy solo dispara por carga de chunks; agregar AFK/inactividad y TPS sostenido bajo como disparadores adicionales) | Busqué "AFK detection reduce tick rate" — **ya existen varios mods** (Tick Tweaks, FPS Optimizer, AFKStatus) que hacen throttling por inactividad de forma standalone. | El AFK-throttling *standalone* está cubierto, no vale la pena rehacerlo. La idea con valor real es la **refactorización arquitectónica**: que `UltraLoadServerHandler` acepte disparadores plugables (carga de chunks, AFK, TPS bajo) y exponga un **nivel** en vez de un booleano. **Corrección importante:** la versión anterior de esta fila decía "reusen los mismos tiers" — esos tiers no existen (corrección #1), hay que construirlos. Eso *sube* el costo del ítem pero también su prioridad: es el ítem #0 del orden nuevo, porque tres features de Fase 1 dependen de él y hacerlas antes garantiza la lógica triplicada que esta fila advertía. |
| ~~Hopper/container tick throttling~~ | Busqué explícitamente — **ya resuelto** por varios mods maduros y populares (Fast Hoppers, Hopper X-Treme, TickAccelerate). | **Descartado.** No hay gap; si el usuario quiere esto, instala uno de esos. Vale la pena sumarlo a la lista de compatibilidad testeada en vez de reimplementarlo. |

---

## G. Replanteo estratégico: qué optimiza de verdad una iGPU

Esta sección es nueva y es, probablemente, lo más importante del documento. El PDF fuente y la
primera versión de este roadmap comparten un supuesto tácito: que "optimizar" significa reducir
trabajo de CPU (menos ticks, menos IA, menos entidades). Para una máquina **sin GPU dedicada** ese
supuesto es en buena medida incorrecto.

### El cuello de botella real

Una iGPU no tiene VRAM propia: usa un recorte de la RAM del sistema (UMA), por el mismo bus que la
CPU. Las consecuencias, que cambian qué features valen la pena:

1. **El ancho de banda de memoria satura mucho antes que la capacidad.** Una Iris Xe o una Radeon
   780M se quedan sin bandwidth mucho antes de quedarse sin "VRAM compartida" disponible. O sea: el
   límite no es cuánta memoria usás, es cuántos bytes por segundo movés.
2. **El fillrate es el recurso escaso.** Overdraw —píxeles dibujados varias veces, típicamente por
   quads alpha-blended superpuestos— consume fillrate *y* bandwidth a la vez. Es el peor patrón
   posible en iGPU.
3. **La RAM que consume el juego le sale del presupuesto a la GPU.** En un sistema con memoria
   compartida, menos RAM usada por Java = más headroom para la porción de la GPU. Este link no
   existe en una máquina con GPU dedicada.

### Qué implica para el mod

- **`resscale` es el módulo de mayor palanca del mod**, y está infra-explotado. Es el único que
  ataca el eje correcto directamente: menos píxeles = menos fillrate y menos bandwidth, lineal. Todo
  lo que lo extienda tiene mejor retorno esperado que una feature de tick nueva.
- **Los módulos de tick (1, 2, 6) siguen valiendo**, pero por otra razón: reducen *stutter* y
  mejoran TPS. No van a mover el FPS sostenido en iGPU. Hay que dejar de venderlos (y de medirlos)
  como si lo hicieran.
- **No conviene competir en el eje de RAM**: FerriteCore y ModernFix ya liberan 300-500 MB y hasta
  1 GB respectivamente. Ese trabajo está hecho y hecho mejor de lo que lo haríamos nosotros.

### G.1 — El presupuesto de partículas debería medirse en píxeles, no en unidades

`ParticlePriorityCore` hoy decide por **distancia en el mundo** + cono de visión, y trata "cerca" como
"conservar". Pero en fillrate el peor caso es exactamente ese: una partícula a 0.5 bloques de la
cámara cubre una fracción enorme de la pantalla, y si hay veinte superpuestas, cada píxel se blendea
veinte veces. Una partícula lejana cuesta casi nada aunque el conteo la penalice igual.

La refinación con valor real no es un cap de conteo, es un **presupuesto de cobertura de pantalla
estimada**: aproximar el área en pantalla de cada partícula (tamaño / distancia²) y acumular contra
un tope. Mantiene intacta la clasificación por prioridad que ya existe, cambia solo la unidad de
medida. Es un cambio acotado a `ParticlePriorityCore` — que además ya está escrito como función pura
con parámetros primitivos, o sea testeable sin arrancar el juego.

### G.2 — El LOD bias sólo cubre el atlas de bloques (bug latente)

`ResScaleManager:195` aplica el bias sobre `TextureAtlas.LOCATION_BLOCKS` únicamente. Entidades,
partículas, banderas/escudos y otras texturas viven en atlas distintos y **no reciben la
compensación**. El javadoc del propio método describe el síntoma como "el agua y las entidades se ven
borrosas" — o sea que el fix se diseñó para cubrir entidades pero se aplica a un atlas que no las
contiene. Vale confirmarlo visualmente en la laptop antes de tratarlo como bug, pero el código dice
que la cobertura es parcial.

### G.3 — Ideas nuevas, filtradas contra lo que ya existe

Busqué cada una antes de proponerla, para no duplicar mods que el usuario podría ya tener:

| Idea | Estado de investigación | Veredicto |
|---|---|---|
| **Aviso de `-Xmx` excesivo en iGPU** | No encontré ningún mod que haga esto. El consejo popular universal es "dale más RAM a Minecraft", que en un sistema de memoria compartida es **activamente contraproducente**: cada GB que se reserva Java es un GB que no puede usar la iGPU. | **Gap real, costo casi nulo, y diferenciado.** `Runtime.maxMemory()` + `GL_RENDERER` son ambos legibles en runtime, y el patrón de entrega ya existe (`sodiumwarning` manda un mensaje de chat). Es la mejor relación valor/esfuerzo de toda la lista. Candidato a colarse temprano. |
| Bajar resolución de texturas en runtime | **Ya existe: TexTweaks** (downscale en runtime con algoritmo configurable). | **Descartar.** Cubierto, y mejor. Sumarlo a la lista de compat testeada. |
| Optimizar render de block entities (cofres, carteles) | **Ya existe: Optimized Block Entities / Better Block Entities.** | **Descartar.** |
| Culling de entidades por oclusión | **Ya existe: EntityCulling.** | **Descartar.** |
| Reducir overdraw de clima (lluvia/nieve) | La lluvia es exactamente el patrón caro en iGPU (quads alpha-blended cubriendo la pantalla). Hay mods (No Weather Effects, Weather Refined, Raindance) pero **la cobertura de Forge 1.20.1 no me quedó clara** — varios son solo Fabric. | **Investigar antes de decidir.** Si no hay opción Forge decente, es un candidato razonable y encaja en el eje correcto (fillrate). Ojo: "sacar el clima" es un cambio de gameplay, no una optimización transparente — tendría que ser opt-in y graduable (densidad/radio), no un on/off. |
| **Sound Priority** (ver sección F) | Sin cambios respecto al análisis original: gap real. | Sigue siendo candidato, pero **bajarlo de prioridad**: el audio no toca ni fillrate ni bandwidth de GPU. Es una mejora de CPU, o sea Fase 2 en el marco nuevo. |

---

## Protocolo de test (revisado — la versión anterior medía la variable equivocada)

> **El protocolo completo y ejecutable vive en [`TESTING.md`](TESTING.md)** — preparación de la
> máquina, mundo plantilla, escenarios por feature, orden ABBA contra el throttling térmico, umbral
> de significancia, planilla de registro y las trampas conocidas del proyecto. Lo de abajo es el
> resumen del criterio; para correr un test, usar ese archivo.

**El error de la versión anterior:** decía "medición antes/después de FPS" para *todas* las features.
Los ítems de tick de servidor (Entity AI LOD, Adaptive Simulation Distance) pueden mejorar
sustancialmente y dar **cero cambio de FPS**, con lo cual el protocolo los declararía fracasos.

Por eso, la métrica depende del eje que ataca la feature:

| Tipo de feature | Métrica | Herramienta |
|---|---|---|
| Cliente / render (resscale, mipmapping, partículas) | FPS sostenido **y** percentil 1% (el stutter no aparece en el promedio) | FPS Overlay (ya es dependencia dev) |
| Servidor / tick (AI LOD, Adaptive Sim Distance, Ultra Carga) | **MSPT / TPS**, no FPS | `spark` (ya es dependencia dev) |
| Footprint de memoria | RAM del proceso — relevante en iGPU por G.3 | `spark` |

Condiciones constantes por cada test: mismo mundo/seed, mismo punto de spawn, misma carga de
mobs/chunks, toggle del módulo específico apagado vs. prendido — no comparaciones cruzadas entre
features distintas. Correr en la laptop iGPU (el hardware real objetivo), no en el dev desktop.
