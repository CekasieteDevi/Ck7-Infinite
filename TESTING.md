# Protocolo de medición de rendimiento — Ck7 Conatus

Cómo se prueba si una feature de V2 **realmente** mejora algo, en la laptop con gráficos integrados
(el hardware objetivo real — ver `V2_ROADMAP.md` sección G).

El problema que este documento resuelve: en una laptop, la variación entre corridas idénticas puede
ser **más grande que la mejora que estamos buscando**. Sin controlar eso, cualquier número confirma
lo que uno ya quería creer. Todo lo de abajo existe para que un "mejoró un 6%" signifique algo.

---

## 0. Regla de oro: medir la variación ANTES de medir la mejora

**Esto se hace una sola vez, antes del primer test de features, y se repite si cambia el hardware o
el driver.**

Corré el **mismo escenario, con la misma configuración, 3 veces seguidas** (mod apagado en las tres).
Anotá los tres resultados. La diferencia entre el mayor y el menor es tu **ruido de base**.

> Si tu ruido de base es ±8%, una mejora medida de 5% **no existe**. Es ruido.

Regla práctica: **solo aceptar como real una mejora mayor al doble del ruido de base.** Anotá ese
umbral acá cuando lo midas:

```
Ruido de base medido:  ____ %   (fecha: ____)
Umbral de aceptación:  ____ %   (= ruido × 2)
```

Sin este número, el resto del protocolo no sirve.

---

## 1. Preparación de la máquina (una vez por sesión de testing)

| Ítem | Valor | Por qué |
|---|---|---|
| Alimentación | **Enchufada**, plan de energía en Alto rendimiento | A batería el CPU/GPU bajan de frecuencia; los números no son comparables entre corridas |
| Programas abiertos | **Cerrar todo**, sobre todo el navegador | En iGPU la RAM del sistema *es* la memoria de la GPU: Chrome con 2 GB le saca 2 GB a la GPU |
| VSync | **APAGADO** | Con VSync el FPS se topa en 60 y una mejora real queda invisible |
| Límite de FPS (vanilla) | **Ilimitado** | Misma razón |
| Resolución / modo ventana | Fijo, el mismo siempre | Cambia el fillrate, que es justo lo que medimos |
| Driver de video | El mismo en toda la comparación | Un update de driver invalida los baselines viejos |
| `-Xmx` | Fijo, y **no exagerado** (4G es razonable) | En iGPU, cada GB reservado por Java es un GB menos para la GPU (ver `V2_ROADMAP.md` G.3) |

---

## 2. El mundo de test

**Creá un mundo una vez y guardalo como plantilla.** Antes de cada corrida, **copiá la carpeta de la
plantilla** en vez de reusar el mundo — así el estado (mobs, items tirados, chunks guardados) es
idéntico en todas las corridas.

Configuración del mundo, aplicada una vez y guardada en la plantilla:

```
/gamerule doDaylightCycle false
/time set noon
/gamerule doWeatherCycle false
/weather clear
/gamerule randomTickSpeed 3
```

Anotá y respetá siempre:
- **Coordenadas exactas** del punto de medición (F3 las muestra).
- **Dirección de la cámara** exacta (F3 muestra `Facing`). Mirar 10° para el costado cambia cuántos
  chunks entran en pantalla.
- **Render distance** fija.

### Punto de medición ya escaneado (mundo de dev `New World`)

Verificado por teleport real (fuerza generación de chunk, no solo lectura de heightmap): el area
cercana al spawn de `New World` es selva/río densa -no sirve para "vista abierta"-, y el terreno
mas alla de ~150 bloques del spawn no esta generado. Escaneando hacia el este se encontro una zona
de **plains** solida (confirmada en un radio de ~100 bloques):

```
Mundo:        New World
Coordenadas:  X=1357  Y=72  Z=51
Facing:       north   (el oceano empieza ~50 bloques al SUR -Z creciente-, mirar al norte lo evita)
Bioma:        minecraft:plains
Confirmado:   plains solido en X=[1307,1407], Z=[-49,51] -mas alla de Z=101 hacia el sur es oceano-
```

Usar este punto para escenarios de "vista abierta" (Escala de Resolución, Aggressive Mipmapping)
en vez de explorar a ciegas -evita perder tiempo de sesión buscando un lugar seco cada vez-.

---

## 3. Escenarios según qué se está probando

Cada feature ataca un eje distinto; el escenario tiene que estresar ese eje o el test no mide nada.

| Feature | Escenario | Qué tiene que haber en pantalla |
|---|---|---|
| Escala de resolución, **Aggressive Mipmapping** | Vista abierta (cima de montaña / océano), quieto | Mucho terreno visible a distancia — carga de fillrate |
| **Cap de partículas por overdraw** | Parado al lado de una fuente densa de partículas (varias antorchas de alma, poción de daño, lluvia, portal) | Partículas **superpuestas y cerca de la cámara** — ese es el caso caro |
| **Entity AI LOD**, Mob Freeze | Granja de mobs o ~100 mobs spawneados a distancia media | Mobs fuera de vista pero cargados |
| **Adaptive Simulation Distance**, Ultra Carga | Volar con elytra en línea recta, o `/tp` a 5000 bloques | Carga masiva de chunks nuevos |
| Villager Throttle | Aldea grande / puesto de trading | 20+ aldeanos con IA activa |

---

## 4. Cómo se corre una medición

```
1. Copiar la plantilla del mundo → carpeta de trabajo
2. Abrir el juego, entrar al mundo
3. Ir al punto fijo, orientar la cámara
4. ESPERAR 90 SEGUNDOS sin tocar nada          ← warmup, ver abajo
5. Grabar durante 3 MINUTOS exactos
6. Anotar los números, salir del juego
7. Esperar ~2 minutos con el juego cerrado      ← enfriamiento
```

**El warmup de 90s no es opcional.** Al entrar a un mundo, la JVM todavía está compilando código
(JIT) y Embeddium está construyendo mallas de chunks. Los primeros 60-90 segundos son sistemáticamente
peores que el estado estable, y si una corrida arranca a grabar antes que otra, la comparación queda
contaminada.

**El enfriamiento tampoco.** Una laptop se calienta y baja frecuencias (*thermal throttling*). Sin
pausa entre corridas, la segunda siempre sale peor que la primera — parecería que la feature
empeoró todo cuando en realidad solo se calentó el equipo.

### Orden ABBA (importante en laptop)

No corras `A A A` y después `B B B`: el calor acumulado castigaría sistemáticamente a B.

Corré en este orden: **A → B → B → A**

Así, si hay una tendencia térmica a lo largo de la sesión, afecta a las dos condiciones por igual y
se cancela al promediar.

Repetir el bloque ABBA hasta tener **mínimo 3 mediciones por condición**.

### Reportar la MEDIANA, no el promedio

Si una corrida se ensucia (Windows decidió actualizar algo en el medio), el promedio se arruina pero
la mediana aguanta. Anotá las 3 y reportá la del medio.

---

## 5. Qué métrica usar — y por qué el FPS no siempre sirve

**Este es el error que tenía la primera versión del roadmap:** medir todo por FPS. Las features de
tick de servidor pueden mejorar muchísimo y dar **cero cambio de FPS**, con lo cual las declararías
fracasos siendo que funcionan.

| Tipo de feature | Métrica principal | Métrica secundaria |
|---|---|---|
| Cliente / render (resscale, mipmapping, partículas) | **FPS promedio** | **1% low** (donde se ve el stutter) |
| Servidor / tick (AI LOD, Adaptive Sim, Ultra Carga, Villager) | **MSPT** | TPS |
| Footprint de memoria | RAM del proceso | — |

### Dos trampas de lectura

**El 1% low suele importar más que el promedio.** Un cambio de 45→47 FPS promedio no se siente. Un
cambio de 12→28 en el 1% low es la diferencia entre "tironea" y "va fluido", aunque el promedio no
se mueva. Anotá siempre los dos.

**TPS es una métrica techo: se topa en 20 y esconde la mejora.** Si el servidor tarda 30 ms por tick
y lo bajás a 20 ms, el TPS marca **20 antes y 20 después** — parece que no pasó nada. Pero pasaste de
usar el 60% del presupuesto de tick al 40%: ganaste muchísimo margen antes de que empiece a tironear.
**Para features de servidor, el número que manda es MSPT.** Mirá TPS solo si ya estaba por debajo
de 20.

---

## 6. Herramientas (ya están en el entorno de dev)

Ambas vienen como `runtimeOnly` en `build.gradle`, no se publican con el mod.

### FPS Overlay — para medir
Muestra FPS, 1% lows, TPS y MSPT en pantalla, sin abrir F3. **Es la herramienta de medición.**

### spark — para diagnosticar
```
/spark tps                          → TPS y MSPT actuales
/spark healthreport                 → resumen general (TPS, MSPT, memoria, GC)
/spark profiler start --timeout 180 → perfila 3 minutos y da un link con el desglose
/spark gc                           → estadísticas de garbage collection
```

> **No midas FPS mientras el profiler corre.** spark tiene su propio costo y contamina el número.
> Usalo en corridas aparte: el overlay responde *cuánto* mejoró, spark responde *dónde* se va el
> tiempo. Son dos corridas distintas, no una.

---

## 6.1. Método rápido (dev-only, SOLO para features de servidor) — no reemplaza el protocolo completo

Para un primer vistazo rápido de features de servidor/tick (Entity AI LOD, Adaptive Simulation
Distance, Ultra Carga, Villager Throttle) se puede medir MSPT con `/spark tps` **dentro del
entorno de desarrollo** (`gradlew runClient`), sin necesitar una instancia de launcher real -a
diferencia de resscale/partículas (ver trampa #1), que SÍ requieren una instancia real siempre-.
Sirve para una primera impresión rápida (minutos, no una sesión de laptop completa), **nunca
para reportar un número final**.

Cómo se hizo (y qué falló) la primera vez que se probó este método en este proyecto:

1. Cargar la escena (spawnear entities, configurar el escenario) en una sola sesión de cliente dev.
2. Esperar un asentamiento largo antes de la primera medición -**20 segundos no alcanzó** (ver
   trampa #6); probar con 30-60s si se puede.
3. Alternar la feature con `Config.VALOR.set(x); Config.refresh();` en vez de reiniciar el proceso
   -mas rapido, pero comparte el mismo JIT/asentamiento entre condiciones, lo que ES la limitación.
4. Correr `/spark tps` (da mediana, 95%ile y máximo de tick duration de los últimos 10s/1m) despues
   de cada cambio, con al menos 6s de espera desde el cambio de config.
5. Orden ABBA (no ABAB) igual que el protocolo completo, y **calculá la varianza DENTRO de cada
   condición repetida** (comparando las dos mediciones "A" entre sí, y las dos "B" entre sí) antes
   de confiar en la diferencia ENTRE condiciones. Si esa varianza interna es comparable a la
   diferencia externa, el resultado es "dirección probable, magnitud no confirmada" -reportalo
   así, no como un porcentaje limpio-.

**Cuándo escalar al protocolo completo (sección 1-5):** si necesitás un número para decidir algo
importante (ship/no ship, comparar contra otro mod, poner en la descripción de CurseForge), o si
el método rápido no logra una diferencia mayor al doble de su propio ruido interno.

---

## 7. Planilla de registro

Copiá esto por cada feature probada:

```
FEATURE:          ___________________________
FECHA:            ____________  DRIVER GPU: ____________
ESCENARIO:        ___________________________
COORDENADAS:      X____ Y____ Z____  FACING: ________
EJE ESPERADO:     [ ] fillrate/GPU   [ ] tick/CPU   [ ] memoria
UMBRAL ACEPTACIÓN: ____ %  (del punto 0)

              │ corrida 1 │ corrida 2 │ corrida 3 │ MEDIANA
──────────────┼───────────┼───────────┼───────────┼─────────
APAGADO  FPS  │           │           │           │
APAGADO  1%   │           │           │           │
APAGADO  MSPT │           │           │           │
──────────────┼───────────┼───────────┼───────────┼─────────
PRENDIDO FPS  │           │           │           │
PRENDIDO 1%   │           │           │           │
PRENDIDO MSPT │           │           │           │

DIFERENCIA: ____%   ¿SUPERA EL UMBRAL? [ ] SÍ  [ ] NO
REGRESIÓN VISUAL:   [ ] ninguna  [ ] ____________________
VEREDICTO: [ ] se queda  [ ] se ajusta  [ ] se descarta
```

**Declarar el eje esperado ANTES de medir.** Si anotás "esta feature debería mejorar MSPT" antes de
correr, no podés después justificar un resultado malo diciendo "bueno, igual mejoró otra cosa".

---

## 8. Trampas conocidas de ESTE proyecto

Todas salieron de bugs reales que ya nos pasaron. Están documentadas en el código; se repiten acá
porque afectan cómo se mide.

**1. No probar `resscale` en la ventana de desarrollo.**
El javadoc de `ResScaleManager` documenta que la ventana de dev dispara eventos de resize/foco por
accidente, y esos eventos llaman a `PostChain.resize` — que resincroniza `entityTarget` y **tapa**
bugs que en una sesión real (donde eso nunca pasa) sí aparecen. Cualquier cosa de resscale se prueba
en una instancia de launcher real.

**2. Verificá la herramienta antes de creerle a un resultado raro.**
En el módulo `dynamicres` se persiguió durante rondas un "artefacto de frame viejo" que resultó ser
un bug de la forma de capturar (`Screenshot.grab`), no del renderizado. Si un número o una captura
te sorprende mucho, la primera hipótesis es que la medición está mal, no el juego.

**3. Medí la dirección del cambio, no la asumas.**
Se probó bajar el render distance durante Ultra Carga (parecía obviamente bueno) y **empeoró el
lag**: oscilar el render distance cada pocos segundos hacía que Embeddium re-mallara chunks sin
parar. Está documentado en `UltraLoadClientHandler`. Una optimización "obvia" puede ir para el otro
lado.

**4. Nunca compares entre features distintas.**
Cada test es una sola feature, prendida vs apagada, todo lo demás igual. "Con las 3 features nuevas
gané 15 FPS" no dice cuál de las tres sirvió — ni si alguna estaba restando.

**5. Anotá regresiones visuales aunque el FPS mejore.**
Casi todo lo de la Fase 1 cambia lo que se ve (mipmaps, partículas, resolución). Una feature que da
+10 FPS y deja el agua borrosa no es una mejora: es un trade-off, y hay que decidirlo mirándolo, no
mirando el número.

**6. La deriva de "asentamiento" es más grande y dura más de lo que parece -aunque ya hayas
esperado el warmup.** Midiendo Entity AI LOD y Adaptive Simulation Distance con `spark` dentro de
una sola sesión (mismo proceso, alternando la feature con `set()`/`refresh()` en vez de reiniciar
el juego): incluso con 20s de asentamiento antes de la primera medición y mediciones espaciadas 6s,
el MSPT siguió bajando de forma monótona entre mediciones de la MISMA condición repetida (ej. dos
mediciones seguidas con la feature prendida, sin tocar nada, dieron 8.6ms y despues 8.5ms -ok, casi
igual-, pero en otra corrida 28.6ms y 24.3ms -¡16% de diferencia sin cambiar nada!-). La causa mas
probable es una mezcla de JIT calentando y el pathing/IA de las entidades spawneadas
estabilizandose, no bugs del feature. **Consecuencia practica: si la variación DENTRO de una misma
condición (repetida sin cambiar nada) es del mismo orden que la diferencia ENTRE condiciones
(prendido vs apagado), el resultado no es confiable -ni con orden ABBA-.** Esto le paso a los dos
tests de rendimiento hechos así en este proyecto (ver commits de Entity AI LOD/Adaptive Simulation
Distance): la dirección de la mejora fue consistente (~10-12%), pero no se pudo confirmar que la
magnitud superara el ruido. La única forma real de resolverlo es la que ya pide el protocolo
completo de este documento: **procesos separados por corrida** (mata el JIT-warmup compartido
entre condiciones) y **más repeticiones** (mínimo 3 por condición, no 2) -ver sección 6.1 para una
variante rapida de este mismo problema, explícitamente marcada como no-confiable-.

---

## 9. Caso especial: la Fase 0 no se mide por rendimiento

El refactor de Ultra Carga (booleano → nivel graduado) es una **refactorización**, no una
optimización. Su criterio de aceptación no es "mejoró X%", es:

> Con el nivel graduado en 0 y 1, el comportamiento debe ser **idéntico** al de hoy.

Se verifica así:
- Mismo escenario de Ultra Carga (volar/teleportar), antes y después del refactor.
- Ultra Carga tiene que activarse y desactivarse en los mismos momentos.
- Los mismos efectos tienen que aplicarse (mobs congelados, partículas ocultas, entity distance y
  framerate limitados, random ticks pausados).
- **FPS y MSPT deben quedar iguales** — dentro del ruido de base. Si el refactor *mejora* algo,
  sospechá: probablemente dejó de hacer algo que antes hacía.

Recién cuando eso queda verificado se le empiezan a colgar disparadores nuevos.
