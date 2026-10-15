# Bitácora de trabajo — Fase 3

## 1. Información general

| Campo | Valor |
|---|---|
| Sistema | SGO-Logic |
| Equipo | Grupo 5 |
| Integrantes | Jairo Gómez, Jorge Pumay, Edson Sosa, Andy Sánchez y Kevin Girón |
| Periodo | 24 de septiembre al 14 de octubre de 2026 (cierre intensivo del 10 al 14 de octubre) |
| Objetivo | Crédito propio "Compra ahora, paga en 30 días" sin degradar PayKlik, más CI/CD con despliegue en la nube, 25 pruebas, cuatro releases y manuales |
| Planificación | `Docs/planificacion/PLANIFICACION_FASE_3.md` |

## 2. Daily Standup

Un bloque por fecha e integrante, sin completar información retroactiva.

> **Criterio de reconstrucción:** las entradas agregadas durante el cierre se
> sustentan en autor, fecha, mensaje y archivos de cada commit. No se asignan
> horas que el integrante no haya registrado. Un trabajo que permanece en una
> rama se distingue expresamente de uno integrado en `develop`.

### Fecha: 2026-10-13 — Integrante: Kevin Girón

- Trabajo realizado (frente DevOps, KEV3-01 a KEV3-08):
  - **KEV3-01:** ADR-017. Google Compute Engine `e2-standard-2` (8 GB) con Docker Compose, imágenes en GHCR, costo aproximado USD 50/mes y alternativas evaluadas (Cloud Run, GKE, Azure, AWS).
  - **KEV3-03:** `FASE 2/deploy/docker-compose.prod.yml`. Usa imágenes inmutables, solo publica la web (80) y los portales, deja ocultas las bases, RabbitMQ y la API, pone topes de memoria por JVM y `restart: always`. Incluye la plantilla `deploy/.env.production.example` sin secretos.
  - **KEV3-04:** `.github/workflows/ci-cd.yml`.
    - Corre las pruebas de los 5 proyectos Java (matriz) y del frontend.
    - Luego construye las 6 imágenes Docker y, solo con un tag `vN`, despliega a GCP por SSH.
    - `images` depende de todas las pruebas y `deploy` de `images`: **si una prueba falla, no se despliega**.
    - Se puede pedir un despliegue manual de un tag ya publicado (rollback).
  - **KEV3-05:** scripts de operación.
    - `deploy/scripts/deploy.sh`: respaldo previo, espera de los 12 contenedores sanos, smoke y **rollback automático**.
    - `smoke.sh`: 10 verificaciones (salud, login JWT, 200/401, catálogo y portales).
    - `backup.sh`: `pg_dump` de las 5 bases.
    - `vm-bootstrap.sh`: Docker y swap en la VM.
  - **KEV3-06 y KEV3-08:** `docs/operacion/RELEASES-GITFLOW.md` (flujo, comandos y contenido de `v1` a `v4`), `RUNBOOK.md` y `GUIA-GCP-Y-PIPELINE.md`.
- Resultado funcional:
  - El pipeline pasa `actionlint`, incluido `shellcheck`.
  - El Compose de producción combinado con el base es válido.
  - `smoke.sh`: 10 de 10 en verde contra el sistema completo local.
  - `deploy.sh` probado con imágenes reales en un registro local: una versión sana se despliega; una versión que no queda sana se **revierte sola** a la anterior y el script termina en error.
  - Backend: 228 pruebas en verde.
- **Actualización 2026-10-14 (madrugada), KEV3-02 y KEV3-07:**
  - Infraestructura creada en GCP: proyecto `sgo-logic-g5-202010844`, VM `e2-standard-2` en `us-central1-a`, IP fija `136.65.180.39` y firewall de los puertos 80, 8082, 8084, 8085 y 8086.
  - En la VM: Docker 29.9, usuario `deploy` con llave exclusiva y 4 GB de swap.
  - Primer despliegue (`v0-manual`): 12 de 12 contenedores sanos y smoke 10 de 10 desde internet.
  - Backend con el crédito de Jairo: 260 pruebas en verde antes de desplegar.
- **Actualización 2026-10-14 (mañana):**
  - Revisión de lo subido por Jorge (historial ERP del crédito, ciclo de la obligación, matriz 401/403, corrección de tracking concurrente, `.gitattributes` LF): backend **306 pruebas** en verde.
  - Nube actualizada a `v0-manual-2` y cargado el historial de crédito de demostración con `scripts/load-credit-demo-history.sh` (29 registros, 5 perfiles).
  - Corregidos dos defectos de operación encontrados al desplegar:
    - `backup.sh` fallaba con valores de `.env` con espacios; ahora lee solo las claves que necesita.
    - Un corte de SSH podía dejar el despliegue a medias; ahora el pipeline lo ejecuta en la VM con `nohup` y solo sigue su log (probado: el código de salida se propaga).
- **Actualización 2026-10-14 (tarde):**
  - Probando la nube se encontró que la tienda del cliente no cargaba sus botones (ni cerrar sesión): `crypto.randomUUID` solo existe en HTTPS o `localhost` y la nube se sirve por `http://<IP>`. Se agregó un respaldo en `session.js`.
  - nginx ahora comprime HTML, CSS, JS y JSON y guarda en caché las imágenes un día (la portada pesa 1.8 MB).
  - `deploy.sh`: si un servicio Java tardaba más que su healthcheck al arrancar, `docker compose up` fallaba y el script terminaba sin rollback. Ahora sigue esperando la salud, reintenta `up` y después decide.
  - Nube actualizada a `v0-manual-3`: 12 de 12 contenedores sanos y smoke 10 de 10.
- **Actualización 2026-10-14 (cierre):**
  - Los secretos y variables de producción quedaron configurados y el workflow
    manual desplegó correctamente el SHA `274ee02` usando la imagen `develop`.
  - La ejecución automática de ese SHA completó pruebas de los cinco proyectos
    Java, validación del frontend y construcción/publicación de las seis
    imágenes: [run 38616631224](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38616631224).
  - El despliegue, health check, rollback preventivo y smoke externo terminaron
    correctamente en el [run 38618417187](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38618417187).
  - El smoke externo ampliado verificó 17 controles, incluidos los enlaces
    portables de los seis portales, sin fallos.
- **Actualización 2026-10-14 (noche), releases y validación integral:**
  - **KEV3-06:** releases `v1` (PR #10) y `v2` (PR #15) publicados desde `main`,
    cada uno desplegado por su tag con pruebas, imágenes, health check y smoke
    en verde. Notas de release con enlace a su run en GitHub → Releases.
  - Revisión de la integración de Edson, Jairo y Andy antes de `v2`: backend
    con **323 pruebas** en verde, JavaScript de los seis portales válido y
    smoke ampliado 17 de 17 en la nube.
  - Prueba manual de punta a punta en un ambiente local limpio
    (`reset-demo-data.sh`): almacén central, folios a POS-ONLINE y POS-01,
    publicación, venta en tienda física, compra web con PayKlik y con crédito,
    retiro en tienda, LogiExpress, consola, ERP e informes, sin hallazgos.
  - Cierre documental (`v3`): matriz de evidencia con los cuatro releases,
    `RELEASES-GITFLOW.md` con el contenido real de cada uno y el ensayo de
    rollback, y referencias al nombre actual del repositorio
    (`AYD2_A_2S2026_PROYECTO_G5`).
- Impedimentos, responsable e impacto:
  - **KEV3-02 y KEV3-07 cerrados:** infraestructura, secretos, variables,
    despliegue por Actions y smoke externo están operativos.
  - **KEV3-06:** `v1` y `v2` publicados; `v3` y `v4`, con el ensayo de
    rollback, se publican con este cierre.
- **Actualización 2026-10-15, `v3` y ensayo de rollback:**
  - `v3` publicado (cierre documental y diagramas) y desplegado por su tag.
  - Ensayo de rollback con *Run workflow*: `v3 → v2`
    ([run 38726847745](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38726847745))
    y regreso `v2 → v3`
    ([run 38727317337](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38727317337)),
    ambos con respaldo previo, 12/12 contenedores sanos y smoke en verde.
    Evidencia en `FASE 2/docs/operacion/RELEASES-GITFLOW.md`.
  - **KEV3-05 y KEV3-06 cerrados** con la publicación de `v4`.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna; Andy ya puede usar la nube (`http://136.65.180.39`) para carga y estrés.

### Fecha: 2026-10-14 — Integrante: Andy Sánchez

- Trabajo realizado (AND3-01, AND3-05, AND3-06 y AND3-07):
  - Integró las actualizaciones de `develop` en
    `feature/201904024-fase3-rendimiento`, contrastó el arnés contra los
    contratos reales y conservó los aportes cruzados de la bitácora.
  - Alineó cinco scripts de carga y cinco de estrés con
    `credit-history-v1`, carritos, opciones de pago y Saga
    `PAYKLIK|OWN_CREDIT`; eliminó las rutas provisionales.
  - El dataset ahora genera cinco cohortes crediticias reproducibles e
    idempotentes por `sourceReference`.
  - Agregó barreras `ALLOW_MUTATIONS` y `ALLOW_STRESS`, `RUN_ID` por corrida y
    configuración opcional de inventario para evitar ejecutar carga destructiva
    por accidente.
  - Actualizó el smoke externo para usar el `customerId` firmado del login y
    recorrer carrito, opciones, checkout, Saga y consulta de orden.
  - Actualizó el Manual Técnico con crédito, variables, GCP, CI/CD, respaldo,
    rollback y los resultados de rendimiento ejecutados.
  - Realizó la revisión estática cruzada del CI/CD en
    `docs/evidence/devops/REV-ANDY-2026-10-14-CICD.md`.
- Validación local y resultados:
  - Instaló y utilizó Docker Desktop 29.8.2; los 12 contenedores del ambiente
    local quedaron activos.
  - Los 12 archivos JavaScript del arnés y su librería pasan comprobación de
    sintaxis con Node; el ejecutor PowerShell pasa el analizador sintáctico.
  - Corrigió tres problemas del arnés encontrados al ejecutar: lectura de
    `locationId` en el dataset, tratamiento de la salida nativa de k6 desde
    PowerShell y aislamiento de `RUN_ID` entre escenarios.
  - El smoke local aprobó al ampliar la espera de la Saga a 90 sondeos. El
    dataset creó 500 historiales sintéticos en cinco cohortes y aplicó un
    incremento de 10,000 unidades al inventario de prueba.
  - Ejecutó los cinco escenarios de carga y los cinco de estrés. Los cinco de
    carga cumplieron sus umbrales; LOAD-02 quedó con observación sobre acciones
    auxiliares y LOAD-05 con una limitación del selector de órdenes.
  - STRESS-01 aprobó la rampa a 160 VUs y STRESS-05 recuperó p95 de 38.10 ms y
    p99 de 62.73 ms, sin errores en la ventana de recuperación.
  - STRESS-03 se repitió con datos aislados: 58,124 comprobaciones correctas,
    0 % de errores y PayKlik con p95 de 1,470.14 ms y p99 de 1,972.71 ms. Frente
    a la primera medición, p95 mejoró 28.39 % y p99 34.91 %.
  - STRESS-04 identificó un cuello de botella abierto: backlog máximo de 5,566,
    28 % de error en creación y drenaje incompleto en dos minutos.
  - La captura posterior a STRESS-05 confirma que los 12 contenedores seguían
    activos y consumían aproximadamente 2.3 GiB; se documenta como recuperación,
    no como consumo máximo del pico.
  - Consolidó métricas, archivos de evidencia, defectos y conclusiones en
    `docs/evidence/performance/FASE3-REPORTE-CARGA-ESTRES.md`.
  - Ejecutó el smoke independiente contra GCP. El primer intento detectó que
    Actuator no se publica mediante nginx; ajustó el arnés a la ruta pública
    `/api/v1/platform/status` y repitió la prueba. El resultado final fue 11/11
    comprobaciones, 0 % de errores, p95 HTTP de 331.21 ms y recorrido completo
    de checkout, Saga PayKlik y consulta de orden en 2.06 s.
- Pendiente:
  - Ajustar la capacidad del flujo de notificaciones y repetir STRESS-04.
  - Si el tiempo lo permite, ampliar el conjunto de órdenes de LOAD-05 y repetir
    el pico de checkout con telemetría de recursos.
  - Los releases `v2` a `v4` permanecen como cierre del equipo y corresponden
    al frente de publicación de Kevin.
- Impedimento actual: no se capturó telemetría de recursos durante el pico y el
  cuello de botella de notificaciones requiere un ajuste del servicio antes de
  una segunda medición. Esto se registra como limitación, sin ocultar que el
  smoke externo y el resto del recorrido quedaron aprobados.
- Horas reales: pendiente de registrar por Andy.

### Fecha: 2026-10-12 — Integrante: Jairo Gómez

- Trabajo registrado:
  - Corrigió los diagramas y cerró documentación heredada de Fase 2 como base
    para comenzar la evolución arquitectónica (`de2ca66`).
  - Integró la corrección mediante `fix/arreglando-diagramas` y actualizó
    `develop` con la línea base de `main` (`6670d44`, `ec64f7a`).
- Resultado: documentación base consolidada antes de introducir el módulo de
  crédito y la Saga financiera dual.
- Horas reales: no registradas en el historial; no se estiman retroactivamente.

### Fecha: 2026-10-13 — Integrante: Jairo Gómez

- Trabajo realizado (J3-02 a J3-06):
  - Implementó el módulo `credit`, línea disponible acumulada, decisiones,
    operaciones idempotentes, bloqueos transaccionales y referencia fiscal
    neutral (`15c2795`).
  - Integró elegibilidad y selección `PAYKLIK|OWN_CREDIT` en checkout, estados
    explícitos de crédito, reanudación de Saga, propiedad cliente–carrito por
    JWT y degradación segura a PayKlik (`e8787fa`).
  - Propagó la clasificación financiera a ERP, consulta y reporte de ventas;
    agregó notificación de resultados de crédito mediante Outbox/Inbox
    (`185cab8`).
  - Incorporó pruebas unitarias, integración PostgreSQL, E2E, seguridad,
    arquitectura modular, conciliación, reporte y ausencia de datos sensibles
    (`3e4b829`).
  - Actualizó DDA, ADR-016, contrato `credit-v1`, cuatro vistas
    arquitectónicas, matriz requisito–evidencia y guion de demostración
    (`5344402`).
  - Creó la estructura integral del Manual de Usuario para cliente, personal
    interno, tienda online y sistemas conectados (`726aab8`).
  - Corrigió el workflow de integración para que utilizara los secretos y
    contratos vigentes sin valores sensibles explícitos (`8f35443`).
- Resultado funcional:
  - Cada cliente registrado recibe una línea inicial individual de Q2,500; la
    alternativa se ofrece mientras su saldo y condiciones vigentes lo permitan.
    PayKlik se conserva ante rechazo, excepción o timeout.
  - Una compra a crédito no crea un intento PayKlik; factura, ERP e informe
    conservan `OWN_CREDIT`.
  - La línea de Q2,500 se consume entre compras y las solicitudes concurrentes
    no pueden sobregirarla.
  - La regresión integrada reportada después de estos cambios quedó en 260
    pruebas verdes; la ampliación de Jorge elevó posteriormente el backend a
    306 pruebas verdes.
- Integración: commits contenidos en `develop` desde el merge `6ba0eac`.
- Horas reales: no registradas; el plan asigna 22 h al frente, pero no se
  presentan como horas ejecutadas.

### Fecha: 2026-10-13 y 2026-10-14 — Integrante: Jorge Pumay

- Trabajo realizado en el dominio de crédito:
  - Implementó la proyección idempotente del historial ERP, cálculo del perfil
    y contratos públicos de ingesta (`933f212`).
  - Agregó dataset demostrativo reproducible, carga por script y limpieza
    controlada (`fb4a5ef`).
  - Conservó la instantánea de entradas de cada decisión y amplió pruebas de
    umbrales y plazo (`4ed827a`).
  - Incorporó el ciclo de vida `GRANTED → OVERDUE|SETTLED|CANCELLED`,
    restitución idempotente de línea y documentación del modelo (`0964d9f`).
  - Cubrió la matriz 401/403 de endpoints financieros (`a565208`).
  - Corrigió portabilidad LF, trazabilidad del origen de despacho y una carrera
    concurrente del tracking logístico (`d435f9e`, `21015f2`, `c096f6b`).
  - Documentó evidencia del temporizador de reservas y revisión cruzada de
    inventario (`ebaa5fe`).
- Resultado: 29 registros históricos y cinco perfiles de demostración cargados
  en nube; backend reportado con 306 pruebas verdes.
- Integración: trabajo contenido en `develop` mediante `2d21d68`, `26f2d05` y
  `23b9b79`.
- Horas reales: no registradas en la bitácora de Fase 3.

### Fecha: 2026-10-14 — Integrante: Jairo Gómez

- Trabajo de estabilización y presentación en nube:
  - Sustituyó enlaces absolutos a `localhost` por navegación portable en login,
    consola interna, tienda online, Almacén Central, POS-01, LogiExpress y
    PayKlik; amplió el smoke para detectarlos (`a854d47`).
  - Preparó y cargó un catálogo demostrativo de 14 productos con fotografías,
    tres categorías y existencias trazables en almacén, Tienda Online y POS-01.
  - Implementó navegación escalable de categorías: cuatro destacadas como
    máximo, panel **Todas las categorías**, conteos, estados accesibles y
    adaptación móvil (`b621cd6`).
  - Dejó el cargador idempotente: creación de artículos, entradas, despachos,
    recepción por folio, cierre logístico, publicación e imágenes.
- Resultado en nube sobre SHA `274ee02`:
  - Hogar: 5 productos; Moda: 5; Tecnología: 4.
  - Tienda Online: 18 unidades por producto; POS-01: 15; almacén central: 27.
  - 28 folios cerrados como `ENTREGADO`, sin mercancía en tránsito.
  - 14 imágenes WebP de 1000×1000 verificadas y catálogo público operativo.
  - CI, seis imágenes, despliegue y smoke externo en verde.
- Integración: `74684ae` y `274ee02` en `develop`.
- **Actualización de cierre:**
  - Separó carrito, pedidos e identidad por cliente registrado (`457446e`).
  - Corrigió la política para entregar Q2,500 iniciales e independientes a cada
    cliente, conservando consumo acumulado, concurrencia y bloqueos por riesgo
    (`7d3b095`).
  - Incorporó 30 capturas al Manual de Usuario, eliminó marcadores editoriales
    y alineó bitácora, planificación y matriz con la evidencia final
    (`fc52d03`, integrado mediante `3ddec7a`).
  - El CI final de `develop` concluyó correctamente en el run `38674105134`.
- Horas reales: no registradas.

### Fecha: 2026-10-14 — Integrante: Edson Sosa

- Trabajo realizado (`224a73c`, EDS3-01 a EDS3-07):
  - Cerró el endpoint financiero de explicación de crédito para reconstruir
    operación, decisión, regla, resultado, entradas y correlación.
  - Agregó integración real de opciones de pago con PostgreSQL, incluyendo
    propiedad cliente–carrito y respuestas 401/403.
  - Cubrió Outbox, RabbitMQ e Inbox para notificaciones de crédito con reintento,
    cola de fallidos y recuperación idempotente.
  - Incorporó E2E de degradación de crédito, repetición sin duplicados y caída
    del proveedor de notificaciones sin reversión de la orden.
  - Amplió seguridad, escaneo de datos sensibles, reporte funcional, matriz de
    evidencia y catálogo de trazabilidad.
- Resultado: **320 pruebas**, 0 fallos, 0 errores y 0 omitidas en el corte de
  integración; 1,273 líneas agregadas y 19 eliminadas en 16 archivos.
- Integración: commit sustancial contenido en `develop` desde `224a73c`.

## 3. Resumen de contribuciones verificadas

| Integrante | Aporte de Fase 3 acreditado por historial | Estado al corte |
|---|---|---|
| Jairo Gómez | Crédito base, checkout/Saga dual, integración ERP/reporte/notificaciones, pruebas, ADR/DDA/diagramas, Manual de Usuario, navegación y catálogo | Integrado y desplegado desde `develop` |
| Jorge Pumay | Historial ERP, dataset, reglas trazables, ciclo de obligación, seguridad y correcciones de logística | Integrado en `develop` |
| Kevin Girón | CI/CD, Compose productivo, GCP, scripts de backup/deploy/rollback, runbook, releases `v1`–`v4` y validación integral | Integrado; pipeline y nube operativos |
| Andy Sánchez | Arnés 5+5, ejecución y análisis k6, Manual Técnico, smoke independiente y revisión estática del CI/CD | Integrado en `develop` |
| Edson Sosa | Explicación financiera, seguridad, integración PostgreSQL/RabbitMQ, notificaciones y E2E de degradación | Integrado en `develop` mediante `224a73c` |

## 4. Evidencia de integración y operación

| Evidencia | Resultado |
|---|---|
| SHA desplegado | `bcc1cf6c41aa1b6edf4e2313db6889a2c2341adf` |
| CI del SHA | [Run 38668140432](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38668140432): CI/CD completo en verde |
| Deploy del SHA | [Run 38669072485](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38669072485): despliegue manual del mismo SHA en verde |
| Manual de Usuario | `fc52d03`, integrado en `develop` mediante `3ddec7a`; 30 capturas incorporadas |
| CI documental final | [Run 38674105134](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38674105134): frontend, cinco proyectos Java y seis imágenes en verde |
| Ambiente | `http://136.65.180.39` y portales `:8082`, `:8084`, `:8085`, `:8086` |
| Releases | `v1`, `v2`, `v3` y `v4` publicados desde `main` y desplegados por tag (ver `FASE 2/docs/operacion/RELEASES-GITFLOW.md`) |
| Ensayo de rollback | `v3 → v2 → v3`: [run 38726847745](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38726847745) y [run 38727317337](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38727317337), ambos en verde |
| Smoke final | 17 verificaciones, 0 fallos |

## 5. Decisiones y pendientes del cierre

- ADR-016 se ratifica como `Accepted` por coherencia entre decisión, contrato,
  migraciones, límites modulares, pruebas automatizadas y comportamiento
  desplegado. Las limitaciones productivas permanecen explícitas.
- ADR-017 se ratifica como `Accepted` porque CI, imágenes, GCP, health y smoke
  operan sobre la topología decidida. El rollback manual entre tags se ensayó
  (`v3 → v2 → v3`) y los releases `v1`–`v4` están publicados; la restauración
  de respaldos queda documentada en el runbook.
- La revisión técnica del frente de Jairo se acepta: no se detectaron accesos a
  internals de `credit`, intentos PayKlik ficticios ni pérdida de
  idempotencia/concurrencia en la evidencia automatizada.
- El proceso de aprobación de PR se considera administrado por el equipo; esta
  bitácora conserva únicamente hechos verificables del historial.
- Los resultados de rendimiento, el aporte de Edson y el Manual de Usuario con
  sus 30 capturas ya están integrados en `develop` y validados por CI.
- El frente funcional de Jairo se declara terminado: implementación,
  estabilización, manual y evidencias están integrados. La bitácora, el guion y
  la actualización final del DDA quedan preparados en la rama documental de
  cierre para su revisión e integración. Después de esa integración, el trabajo
  restante es colectivo y operativo: ensayo de defensa, promoción a `main` y
  publicación de `v2`, `v3` y `v4`.
