# Bitácora de trabajo arquitectónico - Fase 2

## 1. Información general del sprint

| Campo | Valor |
|---|---|
| Sistema | SGO-Logic |
| Equipo | Grupo 5 |
| Integrantes | Jairo Gómez, Jorge Pumay, Edson Sosa, Andy Sánchez y Kevin Girón |
| Periodo planificado | 10 al 23 de septiembre de 2026 |
| Objetivo del sprint | Construir e integrar una base ejecutable de SGO-Logic para que cada responsable incorpore progresivamente su módulo. Pendiente de ratificación por el equipo. |
| Enlace al Kanban | [MegaStore - Fase 2](https://trello.com/b/uyoJQ4RH/megastore-fase-2) |


## 2. Sprint Backlog

| ID | Descripción | Dueño | Estado | Dependencias | Evidencia |
|---|---|---|---|---|---|
| BB-01 | Crear estructura FASE 2, README, bitácora y plantillas ADR | Jairo | `Ready for Testing` en captura | Ninguna | `FASE 2/README.md`, estructura del backend y tarjeta [DzYqM3ew](https://trello.com/c/DzYqM3ew) |
| BB-02 | Registrar ADR-009 de stack y arquitectura ejecutable | Jairo | `Blocked` en captura; borrador `Proposed` | Gate 0 y aprobación de al menos tres integrantes | `FASE 2/docs/adr/ADR-009-stack-y-topologia-ejecutable.md` y tarjeta [FRHUOgHv](https://trello.com/c/FRHUOgHv) |
| BB-05 | Docker Compose y configuración por ambiente | Jairo | `Ready for Testing` en captura | ADR-009 y componentes compartidos | `FASE 2/docker-compose.yml`, `.env.example` y tarjeta [eu881SUz](https://trello.com/c/eu881SUz) |
| QA-01 | Contrato e integración continua | Jairo | Implementación local preparada; ejecución GitHub pendiente | Repositorio, secretos y stack | `.github/workflows/qa-integracion-base.yml` y `FASE 2/docs/integration/qa-01-ci-contract.md` |
| DOC-03 | Bitácora, capturas Kanban y burndown | Jairo | `In Progress` en captura | Registro diario de todos | Esta bitácora y tarjeta [MnIkXzvZ](https://trello.com/c/MnIkXzvZ) |
| Resto del backlog | Consultar planificación oficial | Según planificación | Pendiente | Contratos e implementaciones del equipo | Pendiente |

Los estados anteriores describen únicamente los artefactos presentes; no equivalen a tarjetas `Done` ni inventan aceptación del equipo.

## 3. Captura inicial del Kanban

- Tablero identificado: `MegaStore - Fase 2`.
- Estado inicial observable: la captura `2026-09-14_14-02.png` muestra 54 tarjetas en `To Do` y las demás columnas vacías.
- Fecha de registro en esta bitácora: 2026-09-14.
- Responsable del registro: Jairo Gómez.
- Captura visual: incorporada.

### Captura inicial disponible

![CapturaInicial](FASE%202/docs/evidence/bitacora/2026-09-14_14-02.png)

## 4. Daily Standup

Copiar un bloque por fecha e integrante sin completar información retroactiva.

### Fecha: 2026-09-14 — Integrante: Jairo Gómez

- Ayer: no existe un registro diario verificable anterior en esta bitácora; se deja pendiente en lugar de reconstruirlo retroactivamente.
- Trabajo realizado hoy:
  - `[BB-01] Crear estructura FASE 2, README, bitácora y plantillas ADR`: se creó la estructura oficial, el proyecto Maven/Spring Boot, la declaración de los doce módulos y la infraestructura HTTP transversal mínima.
  - `[BB-02] Registrar ADR-009 de stack y arquitectura ejecutable`: se redactó el borrador en estado `Proposed`; su aprobación continúa pendiente.
  - `[BB-05] Docker Compose y configuración por ambiente`: se configuraron backend, PostgreSQL y RabbitMQ con imágenes versionadas, volúmenes, red dedicada y health checks.
  - `[DOC-03] Bitácora, capturas Kanban y burndown`: se incorporó el enlace y el estado observable del tablero exportado, además del presente registro diario.
  - `[QA-01] Contrato e integración continua`: se creó el workflow con secretos externos, publicación de reportes y limpieza garantizada de Compose; la ejecución en GitHub queda pendiente hasta configurar los secretos.
  - Preparación de `ORD-01`, `ORD-02`, `ORD-03`, `ORD-05` y `QA-04`: se documentaron casos, estados candidatos, puertos de Saga y plan E2E sin implementar lógica funcional ni código ajeno.
  - Preparación de integración y release: se añadieron checklist de entrega, orden de integración, política de compatibilidad y checklist de release.
  - Validación cruzada de BB-01/BB-05: se preparó el procedimiento reproducible y el espacio de evidencia; ejecución por una persona no autora pendiente.
- Resultado funcional o numérico: el comando único `./tests/smoke/verify-platform.sh` terminó correctamente; `./mvnw clean verify` ejecutó 13 pruebas con 0 fallos, 0 errores y 0 omitidas; los tres servicios alcanzaron `healthy`; Actuator y `/api/v1/platform/status` respondieron correctamente y se verificó `X-Correlation-Id`.
- Trazabilidad: BB-01 establece la base común; BB-02 prepara la decisión relacionada con ADR-001; BB-05 prepara el entorno reproducible; DOC-03 conserva la evidencia académica verificable.
- Impedimentos: faltan aprobación de al menos tres integrantes para ADR-009, actualización manual del estado de las tarjetas en Trello y entregas posteriores de los módulos propietarios para avanzar ORD-01 a ORD-05.
- Horas reales y estimadas restantes: pendientes de registrar por Jairo; no se infieren métricas.
- Personas o tareas desbloqueadas: la base técnica permite que Jorge, Edson, Andy y Kevin comiencen sus módulos sin crear otro backend; la disponibilidad o adopción efectiva por ellos aún no está verificada.

### Evidencia visual del estado actualizado el 2026-09-14

![Captura1](FASE%202/docs/evidence/bitacora/2026-09-14_13-57.png)

La captura muestra `[BB-02]` en `Blocked`, `[DOC-03]` en `In Progress` y `[BB-01]`/`[BB-05]` en `Ready for Testing`. Los nombres de archivo se conservan como fueron entregados y no se usan para inferir el orden cronológico entre ambas capturas.

### Espacio para captura de QA-01

> **INSERTAR AQUÍ LA CAPTURA DE LA EJECUCIÓN DEL WORKFLOW QA-01**
>
> Fecha: pendiente.
>
> Ruta sugerida: `FASE 2/docs/evidence/bitacora/qa-01-ejecucion-verde.png`
>
> Enlace a la ejecución: pendiente.

### Fecha: 2026-09-22 — Integrante: Kevin Girón

- Ayer: no existe un registro diario verificable anterior de Kevin en esta bitácora; el último commit visible en `develop` es del 2026-09-14 (BB-01/BB-05/QA-01 de Jairo). Se deja pendiente en lugar de reconstruir retroactivamente.
- Trabajo realizado hoy:
  - `[LOG-01]` Crear despacho desde el origen asignado: `Shipment`, `ShipmentService.createDispatch`, idempotente por `Idempotency-Key`, `POST /api/v1/shipments`.
  - `[LOG-02]` Guía y tracking mediante LogiExpress simulado: `LogiExpressGatewayPort` + `SimulatedLogiExpressAdapter` (tasa de timeout ~2 %, según §2.3 del DDA), registro de eventos de tracking (`POST/GET /api/v1/shipments/{id}/tracking-events`).
  - `[LOG-03]` Reintentos 2/8/32 s y DLQ: `ShipmentService.requestLabelWithRetry` con `TaskScheduler` propio del módulo; al agotar reintentos, registra en `shipment_dead_letters` (cola local mientras BB-04 no existe).
  - `[NOT-01]` Notificaciones no bloqueantes: `NotificationService` despacha en un `Executor` propio; `POST /api/v1/notifications` responde `202 Accepted` de inmediato; un reintento cubre la indisponibilidad del proveedor simulado (CDU-11, E1).
  - `[QRY-01]` Consulta consolidada: `OrderSummaryProjection` (modelo de lectura propio) + `GET /api/v1/orders/{orderId}/summary`; cada campo expone `asOf` y `stale` para cumplir CDU-09 E1 ("muestra la última información conocida... en vez de fallar toda la consulta"). Punto de ingesta temporal `POST /api/v1/query/order-summary` documentado como sustituible por un consumidor de eventos cuando BB-04 exista.
  - `[INC-01]` Incidencias/devoluciones: `Incident` referencia la venta solo por `orderId`; `POST /api/v1/incidents`, resolución manual vía `PATCH .../resolution` (CDU-10, E1) mientras AUD-01/QRY-01 no están integrables por evento.
  - Migraciones Flyway propias del módulo: `V1001` (logistics), `V1002` (notifications), `V1003` (query), `V1004` (incidents). BB-03 (Jorge) todavía no existe, así que no hay rango de versiones acordado con el equipo; se documentó una numeración provisional en `db/migration/README.md`, pendiente de ratificar.
  - Se amplió `ModularArchitectureTest.hasNoUnapprovedDependenciesBetweenModules`: antes exigía cero dependencias para todo módulo, lo que habría bloqueado cualquier módulo que use `shared.http` (formato común de error y `X-Correlation-Id`, exigido por el checklist de entrega). Ahora cada módulo de negocio solo puede depender de `shared`; ningún módulo de negocio puede depender de otro. **Este cambio toca un archivo compartido y necesita revisión de Jairo (arquitecto integrador) antes de integrar a `develop`.**
  - `shared` se declaró `@ApplicationModule(type = OPEN)` por el mismo motivo: por defecto Spring Modulith trata los subpaquetes como internos, y `shared.http` necesita ser accesible desde cualquier módulo. **También necesita revisión de Jairo.**
  - `shared.SharedClockConfig`: único bean `Clock` de toda la aplicación (ver hallazgo de Moments abajo). **También toca código compartido.**
  - `[UI-01]` Base de frontend (`FASE 2/frontend`): página estática (HTML/CSS/JS sin build, "mínima" a propósito) servida por nginx con proxy inverso a `/api/`, nuevo servicio `frontend` en `docker-compose.yml` (`FRONTEND_PORT`). Pantallas funcionales: despacho (LOG-01/02/03), notificaciones (NOT-01), consulta consolidada (QRY-01, con insignia "desactualizado" por campo) y devoluciones/incidencias (INC-01). La pestaña "Checkout" queda explícitamente bloqueada con una nota: depende de `ORD-01` (Jairo), que no existe todavía; no se fabricó una pantalla que aparente funcionar. Materializa lo que dice ADR-009: "El frontend se agregará cuando Kevin entregue la base de UI".
- Resultado funcional o numérico: `./mvnw clean verify` completo terminó en `BUILD SUCCESS` dentro de un contenedor `eclipse-temurin:25.0.4_7-jdk` con el socket de Docker montado para Testcontainers (no hay JDK instalado en este equipo fuera de Docker). `Tests run: 24, Failures: 0, Errors: 0`, incluyendo `InfrastructureIntegrationTest` contra PostgreSQL 17.11 y RabbitMQ 4.3.5 reales, las cuatro pruebas existentes de `shared.http` y las 14 pruebas nuevas de logistics/notifications/query/incidents. Se detectó y corrigió en el camino un `NoUniqueBeanDefinitionException` real: Spring Modulith Moments exige un único bean `Clock` sin calificar y los cuatro módulos habían declarado el suyo; se centralizó en `shared.SharedClockConfig` (bean `Clock` único, infraestructura técnica, no regla de negocio). Además, `docker compose up --build` levantó los cuatro servicios (`postgres`, `rabbitmq`, `backend`, `frontend`) con salud verde, y se probó manualmente de punta a punta a través del proxy de nginx: `POST /api/v1/shipments` (LOG-01, 201), `POST /api/v1/query/order-summary` + `GET /api/v1/orders/{id}/summary` (QRY-01, 200), `POST /api/v1/incidents` (INC-01, 201) y `POST /api/v1/notifications` (NOT-01, 202); también el caso negativo `GET /api/v1/orders/{id}/summary` sobre una orden inexistente (404 con `ApiError` y `correlationId`). Queda pendiente correr `./tests/smoke/verify-platform.sh` formalmente (el script no contempla todavía el healthcheck del nuevo servicio `frontend`) antes de marcar cualquier tarjeta como `Done`.
- Hoy y trazabilidad a CUN/EaC/restricción/ADR: CUN-07/CDU-07 (despacho), CUN-08/CDU-08 (LogiExpress), CUN-09/CDU-09 (consulta, EaC-01 ≤ 5 s), CUN-10/CDU-10 (devoluciones), CUN-11/CDU-11 (notificaciones, EaC-08 no bloqueante). Puerto de despacho implementado exactamente como lo describe `saga-required-ports.md` (`orderId, origen, destino, correlationId, idempotencyKey` → `shipmentId, guía, estado`).
  - `[QA-03]` Matriz de los 12 EaC con evidencia (`docs/qa/QA-03-matriz-eac.md`): 2 cubiertos con prueba automatizada (EaC-05, EaC-08, ambos de Kevin), 2 parciales (EaC-01, EaC-09), 6 bloqueados por módulos de Jorge/Edson/Andy que no existen todavía, 2 no evaluados (costo, portabilidad — requieren un despliegue real que no existe).
  - `[QA-05]` Pruebas coordinadas de fallas externas (`docs/qa/QA-05-pruebas-fallas-externas.md`): verificado el timeout de LogiExpress (LOG-03) y la indisponibilidad del proveedor de notificaciones (NOT-01); **no se puede cerrar** porque depende también de PAY-02 (Edson) y FIS-02 (Andy), inexistentes.
  - `[DEMO-01]` Guion, datos y plan de contingencia (`docs/qa/DEMO-01-guion-demo.md`), acotado explícitamente a lo que existe hoy (sin checkout real). Se registró un ensayo técnico individual contra el stack real (evidencia en §6 de ese documento); el segundo ensayo con el equipo completo sigue pendiente porque requiere la participación de Jairo, Jorge, Edson y Andy.
  - `[PO-QA]` Snapshot de backlog y prioridades (`docs/qa/PO-QA-backlog-prioridades.md`), con el estado real de `git log` (no el planificado) y las próximas acciones de coordinación.
  - Checklist de entrega oficial llenado para esta entrega: `docs/integration/module-delivery-checklist-kevin-2026-09-22.md`. Decisión propia: **Corregir**, no `Aceptar` — falta revisor distinto del autor y las aprobaciones de Jairo sobre los cambios a `shared`.
- Impedimentos, responsable e impacto: BB-03 (Jorge), BB-04/Outbox (Edson) y BB-06/simuladores oficiales (Andy) no están disponibles todavía; Logistics y Notifications usan adaptadores simulados propios y documentados como temporales, y Query no tiene productores reales de eventos aún, así que su ingesta es manual vía API. QA-05 y el segundo ensayo de DEMO-01 no pueden cerrarse sin las entregas de Edson y Andy.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna todavía depende de estas entregas (Kevin es el último eslabón del camino feliz antes de la Saga); Jairo podría usar un `ShippingPort` temporal contra estos endpoints para avanzar ORD-03 si lo necesita antes de que Jorge/Edson/Andy completen sus módulos.

### Fecha: 2026-09-22 (continuación) — Integrante: Kevin Girón

- Ayer: ver entrada anterior del mismo día; primera versión de UI-01 entregada como formularios sueltos por pestaña.
- Trabajo realizado hoy (continuación): revisión visual — la primera versión de UI-01 se consideró demasiado simple para representar "la funcionalidad ya hecha con su área específica". Se rediseñó como un panel único por orden ("workbench"): se escribe un Order ID una vez y las cuatro áreas (Despacho, Notificaciones, Resumen consolidado, Devoluciones) se muestran juntas en tarjetas, con listas/tablas, insignias de estado por color, líneas de tiempo de tracking expandibles y formularios colapsables para crear cada elemento — en vez de cuatro pestañas desconectadas. Esto además representa mejor CDU-09 ("vista consolidada... sin que el solicitante deba consultar cada área por separado") que el diseño anterior.
  - Se agregó `GET /api/v1/shipments?orderId=` (antes solo existía `GET /api/v1/shipments/{id}`), simétrico a los endpoints de listado que ya tenían `notifications` e `incidents`, para poder mostrar todos los despachos de una orden en el panel. Incluye repositorio, servicio y prueba unitaria nuevos.
- Resultado funcional o numérico: `./mvnw clean verify` en contenedor con Testcontainers reales: `BUILD SUCCESS`, `Tests run: 25, Failures: 0, Errors: 0` (24 anteriores + 1 nueva para el endpoint de listado). `docker compose up --build -d` reconstruyó `backend` y `frontend`; los 4 servicios quedaron `healthy`. Prueba de humo repetida a través de nginx: dos despachos creados para la misma orden y confirmados con `GET /api/v1/shipments?orderId=` devolviendo ambos ordenados por fecha descendente (`HTTP 200`). `app.js` verificado con `node --check` (sin errores de sintaxis) dentro de un contenedor `node:22-alpine` desechable.
- Hoy y trazabilidad a CUN/EaC/restricción/ADR: mismo alcance que la entrada anterior (CDU-07 a CDU-11); el rediseño no agrega comportamiento de negocio nuevo, solo presenta mejor el que ya existía.
- Impedimentos, responsable e impacto: ninguno nuevo.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

### Fecha: 2026-09-22 (segunda continuación) — Integrante: Kevin Girón

- Ayer: ver entradas anteriores del mismo día; la segunda versión de UI-01 era un único panel operativo (útil, pero no representaba "el sistema completo" que pide la ingeniera).
- Trabajo realizado hoy: se rediseñó `frontend/` como sitio de tres páginas, porque CDU-07/09/10/11 tienen tanto a Cliente como a Personal de logística/atención como actores en el DDA — no era correcto mostrar solo la vista interna:
  - `index.html`: landing que elige entre "Soy cliente" y "Soy personal interno".
  - `cliente.html` + `store.css` + `app-cliente.js`: tienda omnicanal con catálogo/carrito de referencia (mock, sin backend real — depende de `INV-01`), checkout visualmente completo que al confirmar explica honestamente que depende de `ORD-01`/`PAY-01`/`FIS-01` (ningún cargo ni orden se genera), y "Mis pedidos"/"Devoluciones" conectados de verdad a QRY-01, LOG-02, NOT-01 e INC-01.
  - `staff.html`: la consola operativa de la iteración anterior, renombrada y enlazada desde la landing.
  - Se agregó `landing.css` (tema de la landing, reutiliza variables de `styles.css`).
- Resultado funcional o numérico: `app.js` y `app-cliente.js` verificados con `node --check` (sin errores de sintaxis) en un contenedor `node:22-alpine` desechable. `docker compose up --build -d` reconstruyó `frontend`; los 4 servicios quedaron `healthy`. Prueba de humo completa del flujo de cliente: se creó un despacho real (`DISTRIBUTION_CENTER`), se marcó `IN_TRANSIT`, se alimentó el resumen y se envió una notificación; `GET /api/v1/orders/{id}/summary`, `GET /api/v1/shipments?orderId=` y `GET /api/v1/notifications?sourceAggregateId=` devolvieron exactamente los campos que `app-cliente.js` espera para pintar el stepper de seguimiento del cliente.
- Hoy y trazabilidad a CUN/EaC/restricción/ADR: mismo alcance funcional que las entradas anteriores; el cambio es de presentación (quién ve qué), no de lógica de negocio nueva.
- Impedimentos, responsable e impacto: ninguno nuevo. El catálogo/carrito del cliente sigue siendo un mock hasta que exista `INV-01` (Jorge) y `ORD-01` (Jairo).
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

### Fecha: 2026-09-22 (tercera continuación) — Integrante: Kevin Girón

- Trabajo realizado hoy: pase de fidelidad visual sobre las tres páginas (sin tocar el backend ni agregar funcionalidad nueva). Se reemplazaron los íconos de texto/emoji por SVG en línea consistentes, se agregó `favicon.svg` (marca SGO-Logic) a las tres páginas, se agregaron sombras/profundidad a tarjetas y contenedores, y la tarjeta de checkout bloqueado en la consola operativa pasó de un borde punteado rojo (parece error) a un tono neutro con insignia "Próximamente" (comunica el mismo límite sin verse roto). El objetivo explícito: que las partes que sí están implementadas (despacho, notificaciones, consulta, devoluciones) se vean como producto terminado, no como panel de pruebas — sin fingir que el checkout, el catálogo o el pago funcionan de verdad, porque eso seguiría sin existir en el backend.
- Resultado funcional o numérico: `docker compose up --build -d frontend` reconstruyó la imagen; los 4 servicios quedaron `healthy`; `favicon.svg` y todas las páginas/hojas de estilo responden `200`.
- Impedimentos: ninguno nuevo — el límite de fondo sigue siendo el mismo (ORD-01, INV-01, PAY-01, FIS-01 no existen); lo que cambió es solo la presentación visual, no el alcance funcional real.

### Fecha: 2026-09-22 (cuarta continuación) — Integrante: Kevin Girón

- Trabajo realizado hoy: se agregó login simulado a `frontend/` (`login.html`, `session.js`, `app-login.js`). La arquitectura sí contempla autenticación (módulo `security`, BB-07/ADR-013, propiedad de Edson Sosa) pero está declarado sin implementación — igual que con LogiExpress y el proveedor de notificaciones, se simula del lado del cliente y se documenta explícitamente como tal, sin llamar a ningún endpoint de autenticación real (no existe). Dos cuentas de demostración visibles en la propia pantalla de login: `cliente@megastore.com` / `Cliente123` (rol `CLIENTE`) y `staff@megastore.com` / `Staff123` (rol `STAFF`). La sesión se guarda en `sessionStorage` (no en el backend); `cliente.html` y `staff.html` verifican el rol al cargar y redirigen a `login.html` si no corresponde, con botón "Cerrar sesión" visible en ambas.
- Resultado funcional o numérico: `session.js`, `app-login.js`, `app.js` y `app-cliente.js` verificados con `node --check` (sin errores). `docker compose up --build -d frontend` reconstruyó la imagen; los 4 servicios quedaron `healthy`; `login.html`, `session.js` y `app-login.js` responden `200`.
- Impedimentos: ninguno nuevo. Sigue sin existir autenticación real en el backend; este login no debe presentarse como tal ante la ingeniera — es explícitamente una simulación de frontend, documentada en el propio `login.html`.

### Fecha: 2026-09-22 (quinta continuación) — Integrante: Kevin Girón

- Trabajo realizado hoy: se rediseñó `login.html` para que se vea como el login final de producto, no como un selector de cuentas de prueba. Layout dividido (hero de marca MegaStore a la izquierda, formulario a la derecha), pestaña real Cliente/Personal interno, "Recordarme" funcional de verdad (`localStorage` vs `sessionStorage`, no decorativo), y las credenciales de demo escondidas detrás de un enlace discreto ("¿Necesitas datos de acceso de prueba?") en vez de tarjetas grandes de colores. Se agregó validación de que la cuenta corresponda a la pestaña seleccionada (mensaje "esa cuenta pertenece al perfil X").
- Resultado funcional o numérico: `session.js`, `app-login.js`, `app.js`, `app-cliente.js` verificados con `node --check`. `docker compose up --build -d frontend`: los 4 servicios quedaron `healthy`; `login.html`, `cliente.html`, `staff.html` responden `200`.
- Impedimentos: ninguno nuevo. Sigue siendo login simulado del lado del cliente, documentado como tal dentro del propio panel de credenciales de demo — el diseño más pulido no cambia esa realidad.

### Fecha: 2026-09-22 — Integrante: Andy Sánchez

- Ayer: no existe una entrada anterior verificable de Andy en esta bitácora; no se reconstruye retroactivamente.
- Trabajo realizado hoy:
  - `[DEC-01]` se redactó ADR-010 para separar la aceptación asíncrona del documento fiscal de la compuerta sincrónica de confirmación de la orden.
  - `[DEC-04]` se redactó ADR-012 con contratos y modos configurables para PayKlik, SAT, ERP, POS, LogiExpress y notificaciones.
  - `[BB-06]` se implementaron simuladores configurables (`SUCCESS`, `REJECTION`, `LATENCY`, `TIMEOUT`, `OUTAGE`, `RANDOM`), configuración externa y restricción de PAN/CVV para PayKlik.
  - `[FIS-01/FIS-02]` se implementaron XML fiscal escapado, CUI/serie, persistencia, `FISCAL_PENDING`, envío SAT asíncrono, respuesta aprobada/rechazada, correlación, idempotencia y reintentos 2/8/32 s.
  - `[ERP-01/ERP-02]` se implementaron registro idempotente de ventas, lotes CSV/XML cada 30 minutos, ejecución vacía sin archivo, acuse, reintentos y `DEAD_LETTER`.
  - `[POS-01]` se implementó distribución a tiendas 1–32, acuse, deduplicación por evento+tienda, reintentos y `DEAD_LETTER`, sin modificar POS.
  - `[FIS-03/DOC-02]` se agregaron 16 pruebas, documentación de endpoints/configuración, checklist de entrega y evidencia QA.
- Resultado funcional o numérico: compilación local `BUILD SUCCESS`. Suite completa sin contenedores: 40 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas; dentro de ella, 16 pruebas nuevas cubren la entrega de Andy. Se usó Java 21 con `-Djava.version=21` porque la máquina no tiene JDK 25; no se modificó `pom.xml`.
- Hoy y trazabilidad: CDU-04/FIS-01/FIS-02, ERP-01/ERP-02, POS-01, BB-06, EaC-04/EaC-11/EaC-12 y ADR-001. Las entidades solo conservan UUID de agregados ajenos; no leen sus tablas.
- Impedimentos, responsable e impacto: Docker no está instalado, por lo que quedan pendientes `clean verify`, Testcontainers, Flyway contra PostgreSQL y smoke Compose. BB-04/Outbox (Edson) y ORD-03/Saga (Jairo) no existen en el árbol local; las entregas conservan estado propio, pero la recuperación durable e integración E2E quedan pendientes. Edson debe realizar la revisión cruzada asignada.
- Horas reales y estimadas restantes: pendientes de registrar por Andy.
- Persona que quedaría bloqueada y acción temporal: Jairo puede revisar el endpoint de compuerta fiscal y los contratos propuestos; no debe acoplarse a entidades/repositorios internos ni aceptar ADR-010/012 sin la revisión del equipo.

### Fecha: YYYY-MM-DD — Integrante: nombre

- Ayer: pendiente.
- Resultado funcional o numérico: pendiente.
- Hoy y trazabilidad a CUN/EaC/restricción/ADR: pendiente.
- Impedimentos, responsable e impacto: pendiente.
- Horas reales y estimadas restantes: pendiente.
- Persona que quedaría bloqueada y acción temporal: pendiente.

## 5. Bloqueos

| Fecha | Tarea | Causa reproducible | Impacto | Puede desbloquear | Próxima revisión | Estado |
|---|---|---|---|---|---|---|
| 2026-09-14 | BB-02 | ADR-009 todavía no cuenta con tres aprobaciones registradas | BB-02 no puede cerrarse formalmente | Jairo coordina; al menos tres integrantes aprueban | Pendiente de acordar | Abierto |
| 2026-09-14 | ORD-01 a ORD-05 | Faltan contratos aprobados y entregas de Inventario, Pagos, Fiscal y Logística | No puede iniciarse una implementación integrada definitiva | Responsables de cada módulo y revisión coordinada por Jairo | Pendiente de acordar | Abierto |

## 6. Decisiones arquitectónicas

| Fecha | ADR/decisión | Estado | Participantes | Evidencia de aprobación | Consecuencia |
|---|---|---|---|---|---|
| 2026-09-14 | ADR-009, borrador de stack y topología | Proposed | Jairo (redacción); aprobación del equipo pendiente | Pendiente | BB-02 no puede cerrarse |
| 2026-09-14 | Diferencia de versión JUnit | Pendiente de ratificación | Jairo (detección); equipo pendiente | Resolución efectiva del BOM de Spring Boot 4.1.1 | La planificación indica JUnit 5 y el BOM resuelve JUnit Jupiter 6.0.3 |
| 2026-09-14 | Línea base documental de integración | Proposed | Jairo (preparación); propietarios pendientes | Documentos en `FASE 2/docs/contracts` y `docs/integration` | Permite revisión anticipada sin declarar contratos aceptados |
| 2026-09-22 | ADR-010, confirmación fiscal asíncrona | Proposed | Andy (redacción); Edson y Jairo pendientes | `FASE 2/docs/adr/ADR-010-confirmacion-fiscal-asincrona.md` | La orden solo puede confirmarse con documento fiscal aprobado |
| 2026-09-22 | ADR-012, contratos de simuladores | Proposed | Andy (redacción); Edson, Kevin y Jairo pendientes | `FASE 2/docs/adr/ADR-012-contratos-de-simuladores.md` | Modos externos reproducibles sin afirmar integración real |

## 7. Resultados de pruebas

| Fecha | Comando/escenario | Configuración y datos | Resultado | Evidencia | Conclusión |
|---|---|---|---|---|---|
| 2026-09-14 | `./mvnw clean verify` | Java 25.0.4.1; PostgreSQL 17.11 y RabbitMQ 4.3.5 mediante Testcontainers | `BUILD SUCCESS`; 6 pruebas, 0 fallos, 0 errores, 0 omitidas | Reportes Surefire locales en `FASE 2/backend/target/surefire-reports` | Arquitectura, HTTP e infraestructura inicial verificados |
| 2026-09-14 | `docker compose config` | Valores locales ficticios predeterminados; tres servicios y red bridge dedicada | Exit code 0; configuración válida | Salida reproducible del comando | Sintaxis, interpolación, dependencias, puertos, volúmenes y health checks válidos |
| 2026-09-14 | Smoke test Compose | PostgreSQL 17.11, RabbitMQ 4.3.5 Management y backend Java 25/Spring Boot 4.1.1 | Tres contenedores `healthy`; Actuator y status HTTP 200; correlation ID retornado | Observación local de `docker compose ps` y respuestas HTTP del 2026-09-14 | Backbone ejecutable sin tablas de negocio |
| 2026-09-14 | `./tests/smoke/verify-platform.sh` | `.env` local ignorado; Maven, Compose, PostgreSQL, RabbitMQ y backend | Correcto; 13 pruebas verdes, tres health checks, Actuator y status/correlation ID verificados | Reportes Surefire y salida local de las 14:18–14:19 | Comando único reproducible; Compose apagado sin eliminar volúmenes |
| 2026-09-14 | Workflow QA-01 | Secretos `SGO_*` todavía no configurados en GitHub | Pendiente de ejecución remota | Espacio de captura reservado | No se marca QA-01 terminado hasta observar una ejecución verde |
| 2026-09-14 | Primera ejecución remota QA-01 | Cuatro secretos disponibles; `SGO_RABBITMQ_PASSWORD` llegó vacío | Falló en la validación previa con exit code 1, antes de compilar | Salida de GitHub Actions reportada por Jairo | Se separaron pruebas y smoke test; se agregaron credenciales ficticias de respaldo porque CI no realiza despliegue |
| 2026-09-22 | Compilación de entrega Andy | Java 21 local mediante `-Djava.version=21`; fuentes de producción | `BUILD SUCCESS` | Salida Maven local | El código compila; falta repetir con Java 25 oficial |
| 2026-09-22 | FIS-03 + integraciones + arquitectura | `FiscalServiceTest`, `ErpIntegrationServiceTest`, `PosIntegrationServiceTest`, `SimulatedPayKlikAdapterTest`, `ModularArchitectureTest` | 19 pruebas incluidas en la suite final, 0 fallos, 0 errores, 0 omitidas | Surefire local y `FASE 2/docs/qa/FIS-03-pruebas-fiscal-erp-pos.md` | Caminos feliz/negativo, timeout, reintento, idempotencia y límites modulares verificados sin infraestructura |
| 2026-09-22 | Suite local sin contenedores | Todas las pruebas salvo `InfrastructureIntegrationTest` | `BUILD SUCCESS`; 40 pruebas, 0 fallos, 0 errores, 0 omitidas | Surefire local | Los cambios no rompen los módulos unitarios/MVC existentes; falta Docker/Testcontainers |

## 8. Retrospectiva individual

### Integrante: nombre

1. Decisiones y patrones consolidados con éxito: pendiente.
2. Supuestos o diagramas de Fase 1 que fallaron o exigieron refactorización: pendiente.
3. Mejoras técnicas concretas para mantenibilidad futura: pendiente.

## 9. Burndown o resumen consolidado

La captura de estado actualizado permite registrar un conteo observable del tablero, sin equipararlo a avance aceptado del producto.

| Categoría | Cantidad | Justificación |
|---|---:|---|
| Completadas (`Done`) | 0 | Columna vacía en la captura |
| En curso | 1 | DOC-03 |
| Bloqueadas | 1 | BB-02 |
| Listas para pruebas | 2 | BB-01 y BB-05 |
| Pendientes (`To Do`) | 50 | Conteo visible en la captura |

## 10. Captura final del Kanban

- Estado: pendiente.
- Ruta o enlace de evidencia: pendiente.
- Fecha y responsable de captura: pendiente.
