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
| BB-04 | Outbox transaccional, dispatcher, reintento y DLQ | Edson | Implementado en rama `docs/rev-edson-cruzadas`; pendiente push/PR | BB-03 y ratificación de rangos | `V3001`, `integration/outbox`, `OutboxIntegrationTest` y `docs/qa/trazabilidad-cun-mvp.md` |
| BB-07 | Autenticación, autorización y sanitización | Edson | Implementado en rama `docs/rev-edson-cruzadas`; pendiente push/PR | ADR-013 y decisión JWT | `security/`, `ADR-013`, `SecurityAuthorizationTest` y `SensitiveDataSanitizerTest` |
| AUD-01 | Registro auditable transversal | Edson | Implementado en rama `docs/rev-edson-cruzadas`; pendiente push/PR | BB-03 y correlación | `V3002`, `audit/api`, `AuditIntegrationTest` y `docs/diagrams/modelo-datos.md` §3.7 |
| AUD-02 | Consulta indexada de evidencia | Edson | Implementado en rama `docs/rev-edson-cruzadas`; pendiente push/PR | AUD-01 | `AuditController`, `AuditQueryIntegrationTest` y evidencia EaC-02 < 1 s |
| PAY-01 | Puerto PayKlik e intentos de pago | Edson | Implementado en rama `docs/rev-edson-cruzadas`; pendiente push/PR | BB-06 y BB-07 | `V3003`, `payments.api.PaymentPort`, `PaymentIntegrationTest` y `docs/diagrams/modelo-datos.md` §3.8 |
| PAY-02 | Timeout, circuit breaker y pago indeterminado | Edson | Implementado en rama `docs/rev-edson-cruzadas`; pendiente push/PR | BB-04 y PAY-01 | `ResilientPayKlikClient`, `PaymentIndeterminateIntegrationTest` y evento `PaymentIndeterminate` |
| PAY-03 | Conciliación/reintento idempotente ≤ 60 s | Edson | Implementado en rama `docs/rev-edson-cruzadas`; pendiente push/PR | PAY-02 | `V3004`, `PaymentReconciliationService`, `PaymentReconciliationIntegrationTest` y alerta auditable |
| SEC-01 | Pruebas de autorización y fuga de datos | Edson | Implementado en rama `docs/rev-edson-cruzadas`; pendiente push/PR | BB-07 y PAY-01 | `SensitiveDataLeakScanTest`, `SecurityAuthorizationTest` y `docs/evidence/security/SEC-01-scan-pan-cvv.md` |
| DEC-02 | Protocolo de línea base `T` y carga `8T` | Edson | Cerrado con protocolo y medición | Gate 0 | `docs/qa/DEC-02-protocolo-carga-T-8T.md` |
| QA-02 | Línea base `T` y carga `8T` | Edson | Implementado en rama `docs/rev-edson-cruzadas`; pendiente push/PR | ORD-05/QRY-01 para EaC-03 completo | `tests/k6/`, `tests/data/seed-load.sql` y `docs/evidence/performance/QA-02-linea-base-y-8T.md` |
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

### Fecha: 2026-09-23 — Integrante: Jairo Gómez

- Ayer: se contaba con el flujo integrado de carrito, reserva, pago y confirmación fiscal, además de los módulos de logística, consulta, notificaciones e incidencias; permanecían abiertas las brechas de trazabilidad entre el DDA, el modelo de datos objetivo y las migraciones existentes.
- Trabajo realizado hoy:
  - Se revisó y corrigió el modelo ER para que represente el alcance completo descrito por el DDA, en lugar de limitarse a las migraciones ya implementadas.
  - Se estableció la página `01 - Modelo ER objetivo DDA` como fuente de diseño para las migraciones, con cobertura de CUN-01 a CUN-12 y ADR-001 a ADR-008.
  - Se conservaron en páginas separadas el modelo físico actualmente implementado y los controles de trazabilidad pendientes, evitando presentar estructuras objetivo como si ya existieran.
  - Se incorporaron al modelo las estructuras faltantes para cuenta de cliente, Saga y compensaciones, conciliación de pagos, intentos SAT, movimientos de inventario, cambios de entrega, devolución/reembolso, Outbox/Inbox/DLQ, notificaciones y auditoría de accesos/exportaciones.
  - Se simplificó la presentación del `.drawio` a notación ER/UML monocromática, con referencias externas y elementos pendientes diferenciados por línea discontinua, sin depender de colores.
  - Se añadió la subsección `6.3 Modelo ER` en `Docs/DDA.md` y se enlazó la exportación del diagrama como evidencia arquitectónica.
- Resultado funcional o numérico: el artefacto editable `FASE 2/docs/diagrams/SGO-Logic-Modelo-ER.drawio` contiene tres páginas; su XML es válido, no contiene identificadores duplicados ni relaciones colgantes. La exportación `FASE 2/docs/diagrams/SGO-Logic-Modelo-ER.jpg` queda referenciada desde el DDA.
- Hoy y trazabilidad a CUN/EaC/restricción/ADR: el modelo cubre CUN-01 a CUN-12, hace visibles las decisiones ADR-001 a ADR-008 y fortalece el gate de EaC-07/CUN-12 al separar claramente diseño objetivo, implementación física y brechas.
- Impedimentos, responsable e impacto: el ER completo revela que las migraciones actuales todavía no materializan Outbox, auditoría transversal, conciliaciones históricas, intentos SAT, movimientos de inventario y postventa completa. Jairo continuará la integración priorizando las brechas transversales para el entregable.
- Horas reales y estimadas restantes: pendientes de registrar por Jairo; no se infieren métricas.
- Personas o tareas desbloqueadas: el equipo puede derivar migraciones y pruebas desde una fuente de datos única y verificar cada entrega contra el gate DDA → ER → código → prueba.

### Fecha: 2026-09-23 (continuación) — Integrante: Jairo Gómez

- Trabajo de integración realizado con asistencia de Codex y bajo coordinación de Jairo:
  - `[BB-04/ADR-002]` se creó el módulo `messaging`, la tabla `outbox_messages` y un publicador hacia el exchange durable `sgo.business.events.v1`.
  - El Outbox deduplica por agregado, tipo de evento y clave idempotente; conserva intentos, error, próxima disponibilidad, publicación y estado `DEAD_LETTER` después de agotar cinco intentos.
  - `[AUD-01/CUN-12]` se implementaron `audit_events` y `audit_evidence_access` como evidencia append-only con actor, acción, estado previo/nuevo, resultado, orden/transacción, correlación y hash SHA-256.
  - Se añadieron consultas autorizadas por orden o transacción y exportación JSON mediante `/api/v1/audit/events` y `/api/v1/audit/exports`; toda búsqueda, rechazo o exportación deja registro de acceso.
  - Las transiciones del módulo Orders ahora guardan historial, evento Outbox y evidencia de auditoría dentro de la misma transacción local. Esto cubre la base confiable sin afirmar todavía que ERP, POS y Notifications ya consuman esos eventos.
  - Se agregó la migración `V4001__messaging_and_audit_create_tables.sql`, se actualizó la arquitectura modular para declarar `messaging` y se documentaron los límites pendientes.
  - Se conectó el panel interno con los endpoints reales de CUN-12: una cuenta demostrativa de Finanzas puede consultar y exportar la evidencia de una orden; STAFF recibe una restricción visible y no suplanta ese rol.
  - Se actualizaron el guion de demostración, QA-05 y las limitaciones del README para distinguir entre la base Outbox/auditoría ya implementada y los consumidores o productores que aún faltan.
  - `[BB-04/QRY-01]` se agregó `inbox_messages`, el sobre público de eventos v1, una cola RabbitMQ durable y el consumidor idempotente que proyecta automáticamente `ORDER_STATUS_CHANGED` en la consulta consolidada.
  - El endpoint manual de proyección se conserva únicamente para pagos, despacho, entrega e incidencias mientras esos módulos incorporan sus productores; Query ya no depende de carga manual para estados de orden.
- Resultado funcional o numérico: la línea base avanzó de 73 a 79 pruebas con Outbox/auditoría y finalmente a **84 pruebas, 0 fallos, 0 errores y 0 omitidas** con Inbox/Query y la regresión de eventos de creación. Flyway validó y aplicó **13 migraciones** desde una base PostgreSQL vacía; RabbitMQ real respondió dentro de la prueba de infraestructura.
  - El smoke Compose creó una orden aislada, detectó y corrigió que `Map.copyOf` no admite `previousStatus=null`, y luego comprobó el recorrido real: Outbox `PUBLISHED` → Inbox `PROCESSED` → resumen de orden `CREATED`. Backend, frontend, PostgreSQL y RabbitMQ terminaron saludables.
- Hoy y trazabilidad: ADR-002, ADR-004, CUN-09, CUN-12, EaC-01, EaC-02 y EaC-07. La implementación reduce la brecha crítica del modelo ER, pero BB-04 permanece parcial hasta disponer de consumidores de ERP/POS/Notifications y productores de los demás agregados; AUD-01 permanece parcial hasta incorporar eventos directos de pagos, fiscal, logística e incidencias y medir recuperación menor a un segundo.
- Impedimentos y siguiente acción: seguridad backend todavía no existe; la cuenta de Finanzas y los encabezados `X-Actor-Id`/`X-Actor-Role` son mecanismos demostrativos y no deben presentarse como autenticación productiva. La siguiente prioridad es reutilizar el Inbox idempotente para conectar Notifications, ERP y POS.
- Horas reales y estimadas restantes: pendientes de registrar por Jairo.

### Fecha: 2026-09-23 (segunda continuación) — Integrante: Jairo Gómez

- Trabajo de integración realizado con asistencia de Codex y bajo coordinación de Jairo:
  - Se transformó la portada pública en una tienda departamental MegaStore, manteniendo la identidad azul/cian del inicio de sesión y dejando la mención de MVP únicamente en el pie de página.
  - Se publicó un catálogo navegable sin autenticación, con búsqueda, filtros por departamento, existencias consultadas desde Inventory y acceso a una ficha independiente por producto.
  - Se agregaron productos semilla con stock verificable para permitir recorridos E2E reproducibles; la interfaz muestra la suma real de `quantityAvailable` y el detalle por tienda o centro de distribución.
  - La ficha de producto permite consultar descripción, precio, disponibilidad por ubicación, guardar favoritos e iniciar una compra. Si no existe sesión, conserva la intención de compra y solicita iniciar sesión o crear una cuenta.
  - Se rediseñó la experiencia autenticada del cliente con accesos permanentes a Favoritos, Mis pedidos, Cerrar sesión y Mi carrito. El carrito muestra siempre su cantidad, y las tarjetas permiten ver detalle, marcar favoritos o agregar productos.
  - Se incorporó el recurso visual propio `frontend/assets/megastore-campaign-hero.png` para la campaña principal, evitando depender de una captura o marca comercial ajena.
  - `[NOT-01/CUN-11/ADR-008]` se agregó la cola durable `sgo.notifications.order-status.v1` y un consumidor que procesa `ORDER_STATUS_CHANGED` mediante Inbox, sin leer tablas de Orders. Una confirmación resuelve el contacto habilitado y verificado de mayor preferencia y crea el envío no bloqueante.
  - La migración `V4003` materializa `customer_contacts` y deja un contacto verificable para la cuenta CLIENTE E2E. Los eventos de orden ahora incluyen `customerId` como dato del contrato, sin compartir entidades entre módulos.
- Resultado funcional o numérico: `node --check` validó los tres scripts nuevos o modificados sin errores. Compose reconstruyó frontend y backend; PostgreSQL, RabbitMQ, backend y frontend quedaron `healthy`. Portada, ficha de producto, catálogo, recurso visual y consulta de inventario respondieron HTTP 200. Una comprobación automatizada en Chromium inició una sesión CLIENTE, guardó un favorito, confirmó una tarjeta en `Mis favoritos`, agregó un producto con stock y observó `1 producto` en el carrito con la compra habilitada. La suite completa terminó en `BUILD SUCCESS` con **87 pruebas, 0 fallos, 0 errores y 0 omitidas**, y Flyway aplicó **14 migraciones** desde una base PostgreSQL vacía.
- Hoy y trazabilidad: UI-01, ORD-01/ORD-02, INV-01, NOT-01, CUN-01, CUN-02, CUN-11, ADR-002 y ADR-008. El catálogo es público; la modificación del carrito y el checkout requieren sesión. Notifications consume por contrato público y conserva idempotencia por consumidor.
- Evidencia E2E: Chromium completó carrito → reserva → PayKlik → SAT → orden `CONFIRMED` para `f4ab0563-81b4-408a-b62b-8e2698e1bdb5`. RabbitMQ entregó el evento tanto a Query como a Notifications; ambos Inbox quedaron `PROCESSED` y se observó una solicitud `ORDER_CONFIRMED`, canal `EMAIL`, estado `SENT`, un intento.
- Impedimentos y siguiente acción: la autenticación continúa siendo una simulación de frontend y no debe presentarse como seguridad productiva. CUN-11 continúa parcial hasta recibir eventos de despacho/incidencias, almacenar cada intento y disponer de DLQ del trabajador. La siguiente prioridad es conectar ERP y POS al mismo patrón Outbox/Inbox.
- Horas reales y estimadas restantes: pendientes de registrar por Jairo; no se infieren métricas.
### Fecha: 2026-09-23 — Integrante: Edson Sosa

- Ayer: revisión cruzada de las entregas de Andy (fiscal/ERP/POS) y Jorge (inventario), aprobación de Edson sobre ADR-009/010/012, modelo de datos y matriz de trazabilidad CUN (commits `56de66d`, `e08b939`, `340eb1e`). En la prueba manual se detectó y corrigió el bloqueo optimista A8 de los flujos asíncronos (commits `4525bae`, `2b0aab1`).
- Trabajo realizado hoy:
  - `[BB-04]` Outbox/Inbox transversal en `integration.outbox` con migración `V3001`: `PublishEvent` con transacción obligatoria, dispatcher con reintentos 2/8/32 s y estado `FAILED`, reclamo con `FOR UPDATE SKIP LOCKED`, topología `sgo.events` / `sgo.events.dlx` / `sgo.events.dlq` e `IdempotentConsumer` con `INSERT ... ON CONFLICT DO NOTHING`.
  - `[DEC-06]` decisión de autenticación JWT registrada para ADR-013 (BB-07, siguiente tarjeta).
  - Rangos de migración ratificados (`V3001`-`V3999` para integration/payments/audit) y documentados en `db/migration/README.md`.
  - Se actualizó `ModularArchitectureTest` para permitir la dependencia a la API pública `integration.outbox`, y la documentación del sobre de eventos, modelo de datos, trazabilidad y README.
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers: `BUILD SUCCESS`, 74 pruebas, 0 fallos, 0 errores, 0 omitidas. Smoke manual en Compose: un evento insertado en `outbox_event` pasó a `PUBLISHED`; se capturó el sobre con `messageId`, `X-Correlation-Id` y `X-Event-Type`; y el exchange, exchange de fallidos y cola de fallidos quedaron declarados en RabbitMQ.
- Hoy y trazabilidad: BB-04; precondición de CDU-06, E1 de CDU-03 y entrega asíncrona de CDU-04/CDU-05/CDU-11; ADR-002 y ADR-004; EaC-04, EaC-05, EaC-06 y EaC-07. Los consumidores de negocio se conectarán con ORD-03.
- Impedimentos: ninguno propio. Los flujos de negocio todavía no consumen el Outbox porque dependen de ORD-03 (Jairo); la rama permanece local por indicación del equipo.
- Horas reales y estimadas restantes: pendiente de registrar por Edson.
- Persona que quedaría bloqueada y acción temporal: Jairo puede consumir `PublishEvent` desde la Saga; Kevin y Andy pueden reemplazar sus colas locales por la DLQ del broker cuando integren.

### Fecha: 2026-09-23 (continuación) — Integrante: Edson Sosa

- Ayer: ver entrada anterior del mismo día; BB-04 cerrado con Outbox/Inbox, dispatcher, reintentos y DLQ.
- Trabajo realizado hoy:
  - `[BB-07/DEC-06]` autenticación JWT HMAC-SHA256 sin estado en el módulo `security`: `POST /api/v1/auth/login`, cuentas de demostración configurables por `SGO_SECURITY_*`, `JwtTokenService` con vigencia de 8 h y roles `CLIENT`, `SUPPORT`, `LOGISTICS` y `FINANCE`.
  - Autorización por endpoint en la cadena de filtros y respuestas `401 UNAUTHENTICATED` / `403 FORBIDDEN` con el formato común `ApiError` y correlation ID.
  - `SensitiveDataSanitizer` para PAN/CVV y registro de eventos de seguridad con ruta enmascarada.
  - `ADR-013` redactado en estado `Proposed` con la matriz de autorización, alternativas, consecuencias y limitaciones.
  - UI-01 (Kevin): el login simulado se reemplazó por el real; `session.js` guarda el JWT y `apiCall` lo envía como `Authorization: Bearer`. Cambio sujeto a revisión de Kevin.
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers: `BUILD SUCCESS`, 85 pruebas, 0 fallos, 0 errores, 0 omitidas. Smoke en Compose: login válido devuelve token; sin token 401; `CLIENT` recibe 403 en fiscal y despacho; `FINANCE` y `LOGISTICS` acceden a lo suyo; health, estado de plataforma y frontend siguen públicos.
- Hoy y trazabilidad: BB-07, DEC-06, precondición de CDU-12, paso 3 de CDU-03, EaC-12, R4 y ADR-013.
- Impedimentos: ninguno propio. El ADR-013 queda `Proposed` hasta la revisión de Jairo, Andy y Kevin; SEC-01 cerrará el escaneo de PAN/CVV sobre base, mensajes y logs.
- Horas reales y estimadas restantes: pendiente de registrar por Edson.
- Persona que quedaría bloqueada y acción temporal: PAY-01 (Edson) ya puede construirse sobre la API protegida; SEC-01 usará los roles y el sanitizador existentes.

### Fecha: 2026-09-23 (segunda continuación) — Integrante: Edson Sosa

- Ayer: ver entradas anteriores del mismo día; BB-04 y BB-07 cerrados.
- Trabajo realizado hoy:
  - `[AUD-01]` registro auditable transversal en el módulo `audit` con migración `V3002`: `AuditRecorder` como API pública (`@NamedInterface`), `AuditService` que exige transacción activa, actor tomado del contexto autenticado (o `system`) y evidencia con acción, resultado, antes/después, correlación y causalidad.
  - Tabla `audit_events` de solo anexado: el repositorio extiende `Repository` y no expone actualización ni borrado; índices por orden, agregado y correlación para AUD-02.
  - Primer consumidor: `incidents` registra `INCIDENT_REGISTERED` e `INCIDENT_RESOLVED` en la misma transacción del cambio, y `ModularArchitectureTest` permite la dependencia a `audit.api`.
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers: `BUILD SUCCESS`, 91 pruebas, 0 fallos, 0 errores, 0 omitidas; `AuditIntegrationTest` verifica atomicidad, actor autenticado, rechazo sin transacción y evidencia desde incidencias.
- Hoy y trazabilidad: AUD-01, CDU-12 pasos 2 y 3, EaC-02/EaC-07, ADR-002/ADR-006/ADR-007 y modelo de datos §3.7.
- Impedimentos: ninguno propio. AUD-02 (consulta indexada < 1 s, registro de acceso, búsqueda fallida y exportación) es la siguiente tarjeta.
- Horas reales y estimadas restantes: pendiente de registrar por Edson.
- Persona que quedaría bloqueada y acción temporal: PAY-01 podrá dejar evidencia de cada intento; fiscal, logística y órdenes podrán sumar sus acciones al registro cuando integren `AuditRecorder`.

### Fecha: 2026-09-23 (tercera continuación) — Integrante: Edson Sosa

- Ayer: ver entradas anteriores del mismo día; AUD-01 cerrado.
- Trabajo realizado hoy:
  - `[AUD-02]` consulta indexada de evidencia en `audit`: `GET /api/v1/audit/events` por `orderId` o por `aggregateType`+`aggregateId`, y `GET /api/v1/audit/orders/{orderId}/export` en CSV, ambos con rol `FINANCE`.
  - Cada acceso y exportación se registran como evidencia (`AUDIT_EVIDENCE_ACCESSED`, `AUDIT_EVIDENCE_EXPORTED`); la búsqueda sin resultados responde `404 AUDIT_EVIDENCE_NOT_FOUND`, registra `AUDIT_SEARCH_FAILED` y deja alerta para Finanzas (CDU-12 E1) en una transacción independiente para que sobreviva al fallo.
  - `SecurityConfig` restringe `/api/v1/audit/**` a `FINANCE`; ADR-013, README y matriz de trazabilidad actualizados.
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers: `BUILD SUCCESS`, 97 pruebas, 0 fallos, 0 errores, 0 omitidas. `AuditQueryIntegrationTest` verifica consulta por orden y agregado, acceso registrado, búsqueda fallida, exportación CSV y **EaC-02 < 1 s** sobre 10 000 eventos (1 000 de una orden). Smoke real: consulta en ~63 ms, export CSV correcto, 404 con evidencia de fallo y 403 para `CLIENT`.
- Hoy y trazabilidad: AUD-02, CDU-12 pasos 5 y 6, A1, E1; EaC-02 cerrado y EaC-07 parcial; ADR-006 y modelo de datos §3.7.
- Impedimentos: ninguno propio. EaC-07 queda parcial hasta que pagos, fiscal y logística registren sus acciones.
- Horas reales y estimadas restantes: pendiente de registrar por Edson.
- Persona que quedaría bloqueada y acción temporal: PAY-01 puede construir su evidencia sobre `AuditRecorder`; SEC-01 puede escanear `audit_events` como parte del barrido de PAN/CVV.

### Fecha: 2026-09-23 (cuarta continuación) — Integrante: Edson Sosa

- Ayer: ver entradas anteriores del mismo día; AUD-02 cerrado.
- Trabajo realizado hoy:
  - `[PAY-01]` intentos de pago con migración `V3003` (`payment_attempts`) y `payments.api.PaymentPort` para la Saga: orden, canal, método, importe, moneda, referencia segura, correlación e idempotencia; estados `PENDING`, `APPROVED`, `REJECTED` e `INDETERMINATE`.
  - `PaymentAttemptService` usa el `PayKlikGatewayPort` de BB-06 y persiste cada paso en transacción propia con evidencia auditable (`PAYMENT_ATTEMPT_CREATED`, `PAYMENT_APPROVED`, `PAYMENT_REJECTED`, `PAYMENT_PENDING_CONFIRMATION`). Idempotencia por llave única, incluida la concurrencia; un timeout deja `PENDING` y nunca se reporta como aprobación (frontera con PAY-02).
  - `PaymentController`: `POST /api/v1/payments/attempts` con `Idempotency-Key` (201/202/200), `GET` por id y por orden; PAN/CVV se rechazan con 400 antes de llamar a PayKlik; `/api/v1/payments/**` queda para `CLIENT`/`SUPPORT`.
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers: `BUILD SUCCESS`, 107 pruebas, 0 fallos, 0 errores, 0 omitidas. Smoke real: intento `APPROVED` con referencia del proveedor, repetición con la misma llave devuelve el mismo intento (200), PAN rechazado (400), `FINANCE` recibe 403 y la BD no contiene PAN/CVV; dos eventos de auditoría con el actor autenticado.
- Hoy y trazabilidad: PAY-01, CDU-03 pasos 2 y 3, postcondición auditable; EaC-04 parcial y EaC-12 parcial; ADR-003, ADR-012 y `saga-required-ports.md`.
- Impedimentos: ninguno propio. PAY-02 (timeout, circuit breaker, indeterminado) y PAY-03 (conciliación ≤ 60 s) son las siguientes tarjetas.
- Horas reales y estimadas restantes: pendiente de registrar por Edson.
- Persona que quedaría bloqueada y acción temporal: Jairo puede consumir `payments.api.PaymentPort` en ORD-03; SEC-01 puede escanear `payment_attempts` como parte del barrido de PAN/CVV.

### Fecha: 2026-09-23 (quinta continuación) — Integrante: Edson Sosa

- Ayer: ver entradas anteriores del mismo día; PAY-01 cerrado.
- Trabajo realizado hoy:
  - `[PAY-02]` timeout y circuit breaker con Resilience4j (módulos core, sin starter de plataforma) en `ResilientPayKlikClient`: límite de tiempo configurable, umbral de fallas, ventana deslizante, espera en abierto y llamadas permitidas en semiabierto.
  - Un timeout, una caída o el circuito abierto dejan el intento `INDETERMINATE` (antes `PENDING` en PAY-01), conservan la orden y publican `PaymentIndeterminate` por Outbox en la misma transacción para la conciliación de PAY-03.
  - Configuración externalizada `SGO_PAYKLIK_*` en `application.yml`, `.env.example`, Compose y README.
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers: `BUILD SUCCESS`, 112 pruebas, 0 fallos, 0 errores, 0 omitidas. Smoke real con PayKlik en modo `TIMEOUT`: dos intentos `INDETERMINATE` con `PAYKLIK_TIMEOUT` y el tercero con `PAYKLIK_CIRCUIT_OPEN` (sin llamar al proveedor), 0 órdenes perdidas, evidencia auditable y evento `PaymentIndeterminate` con payload sin PAN/CVV.
- Hoy y trazabilidad: PAY-02, CDU-03 E1, EaC-04 y ADR-003; modelo de datos §3.8 y `saga-required-ports.md`.
- Impedimentos: ninguno propio. PAY-03 (conciliación/reintento idempotente ≤ 60 s) es la siguiente tarjeta.
- Horas reales y estimadas restantes: pendiente de registrar por Edson.
- Persona que quedaría bloqueada y acción temporal: ORD-04 (compensaciones) depende de PAY-02 y ya cuenta con el estado indeterminado; QA-05 podrá inyectar timeout y caída con los modos del simulador.

### Fecha: 2026-09-23 (sexta continuación) — Integrante: Edson Sosa

- Ayer: ver entradas anteriores del mismo día; PAY-02 cerrado.
- Trabajo realizado hoy:
  - `[PAY-03]` conciliación en `payment_reconciliations` (`V3004`) con programación dentro de los 60 s (`first-delay` 30 s por defecto), índice único parcial de un trabajo abierto por intento y reclamo `FOR UPDATE SKIP LOCKED`.
  - `PaymentReconciliationService` reutiliza la misma `Idempotency-Key` y la referencia segura del intento (columna nueva `payment_attempts.payment_reference`), por lo que nunca genera un segundo cobro; resuelve como `APPROVED`/`REJECTED`, reintenta con backoff 60/120/240 s y deja `DEAD_LETTER` con alerta auditable `PAYMENT_RECONCILIATION_ALERT` si supera 5 min o agota intentos.
  - Los eventos terminales `PaymentApproved` y `PaymentRejected` se publican por Outbox en la misma transacción, con payload común sin PAN/CVV.
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers: `BUILD SUCCESS`, 116 pruebas, 0 fallos, 0 errores, 0 omitidas. Smoke real: un intento `INDETERMINATE` sembrado se concilió como `APPROVED` con `PaymentApproved` y auditoría, y en modo `TIMEOUT` con reintentos cortos la conciliación terminó en `DEAD_LETTER` sin perder la orden ni duplicar el cobro.
- Hoy y trazabilidad: PAY-03, CDU-03 E1, EaC-04 cerrado, ADR-003 y modelo de datos §3.8.
- Impedimentos: ninguno propio. Con PAY-01 a PAY-03 cerrados, el siguiente bloque del plan para Edson es SEC-01 (pruebas de autorización y fuga de PAN/CVV) y QA-02.
- Horas reales y estimadas restantes: pendiente de registrar por Edson.
- Persona que quedaría bloqueada y acción temporal: Jairo puede consumir `payments.api.PaymentPort` y los eventos de pago en ORD-03; QA-05 ya tiene timeout, caída y conciliación reproducibles.

### Fecha: 2026-09-23 (séptima continuación) — Integrante: Edson Sosa

- Ayer: ver entradas anteriores del mismo día; PAY-03 cerrado.
- Trabajo realizado hoy:
  - `[SEC-01]` `SensitiveDataLeakScanTest` genera actividad real (pago aprobado, intentos con PAN/CVV rechazados, incidencia resuelta, despacho con guía y tracking, notificación, fiscal, consulta de auditoría y un 401) y escanea todas las columnas de texto/JSON del esquema público, los payloads del Outbox, los estados de auditoría y los logs capturados con patrones de PAN (13–19 dígitos) y CVV/CVC: 0 apariciones, con verificación de no vacuidad.
  - `SecurityAuthorizationTest.roleMatrixRestrictsEndpoints` consolida la matriz 401/403: cliente prohibido en fiscal/query/notificaciones, logística en pagos/auditoría, finanzas en despachos/pagos y soporte en fiscal.
  - Evidencia en `docs/evidence/security/SEC-01-scan-pan-cvv.md`.
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers: `BUILD SUCCESS`, 118 pruebas, 0 fallos, 0 errores, 0 omitidas. El escaneo cubrió más de 20 columnas, con eventos en Outbox y auditoría, y los intentos con PAN/CVV no se persistieron.
- Hoy y trazabilidad: SEC-01, CDU-03 paso 3, EaC-12 cerrado, R4 y ADR-013.
- Impedimentos: ninguno propio. Queda QA-02/DEC-02 (línea base `T` y carga `8T`) como siguiente bloque del plan para Edson.
- Horas reales y estimadas restantes: pendiente de registrar por Edson.
- Persona que quedaría bloqueada y acción temporal: el release candidate puede citar EaC-12 como cubierto; QA-02 depende de la estabilidad del ambiente y de ORD-05/QRY-01 para el escenario completo.

### Fecha: 2026-09-23 (octava continuación) — Integrante: Edson Sosa

- Ayer: ver entradas anteriores del mismo día; SEC-01 cerrado.
- Trabajo realizado hoy:
  - `[DEC-02]` protocolo de línea base `T` y carga `8T` con ambiente, conjunto declarado (500 proyecciones y 10 000 eventos), escenarios, umbrales, procedimiento y limitaciones.
  - `[QA-02]` scripts k6 `consult-order-summary.js` y `audit-evidence.js`, datos semilla idempotentes y guía de ejecución; corridas `T` (10 VUs) y `8T` (80 VUs) de 1 minuto con p50/p95/p99, errores y recursos.
  - Hallazgo 1: la consulta de auditoría agotaba el pool de conexiones con 10 VUs (p95 30 s, 95 % de errores) porque `AuditQueryService` era transaccional y escribía la evidencia de acceso con `REQUIRES_NEW`. Se corrigió quitando la transacción externa y separando los eventos de acceso de la evidencia de negocio; se agregó una prueba de concurrencia de 16 hilos.
  - Hallazgo 2: RabbitMQ consumía ~36 % de un núcleo en reposo por el healthcheck cada 5 s (una VM Erlang por invocación); se subió a 30 s y el consumo quedó en ~0 %.
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers: `BUILD SUCCESS`, 119 pruebas, 0 fallos, 0 errores, 0 omitidas. `T` ≈ 19.6–19.8 req/s y `8T` ≈ 157–159 req/s (≈ 8.04×) con 0 % de errores: consulta consolidada p95 4.22 ms y evidencia p95 7.23 ms en `8T`. Recursos en `8T`: backend 8–21 % de CPU, PostgreSQL 2–6 %, RabbitMQ ~0.1 %.
- Hoy y trazabilidad: DEC-02 cerrado, QA-02 con resultados; EaC-01 cubierto, EaC-02 con evidencia de carga y EaC-03 parcial hasta contar con Orders/Saga.
- Impedimentos: EaC-03 no puede cerrarse sin los módulos P0 de Jairo (ORD-01 a ORD-05) ni un ambiente con balanceo; se documenta como limitación honesta.
- Horas reales y estimadas restantes: pendiente de registrar por Edson.
- Persona que quedaría bloqueada y acción temporal: el equipo puede citar EaC-01/02/03 con evidencia; Jairo debe integrar Orders/Saga para completar EaC-03.

### Fecha: 2026-09-23 (novena continuación) — Integrante: Edson Sosa

- Ayer: ver entradas anteriores del mismo día; QA-02 cerrado.
- Trabajo realizado hoy: inspección de la implementación y corrección de los bugs propios detectados.
  - `[A1]` `PaymentRequest` ahora normaliza separadores (espacio, guion, punto) y rechaza PAN de 13 o más dígitos y variantes `cvv|cvv2|cvc|cvc2|securityCode`; `SensitiveDataSanitizer` y el escaneo de SEC-01 se alinearon para detectar PAN con separadores, y se agregaron pruebas con PAN con guiones/espacios/puntos y `cvc2`.
  - `[A4]` `ResilientPayKlikClient` usa un pool acotado (`SGO_PAYKLIK_CALL_POOL_SIZE`) y `FutureTask`, de modo que el timeout interrumpe de verdad la llamada al proveedor.
  - `[A5]` el barrido de conciliación recupera intentos `PENDING` más antiguos que `SGO_PAYMENT_RECONCILIATION_STALE_PENDING_AFTER` y los pasa a `INDETERMINATE` con código `STALE_PENDING`.
  - `[A6]` el seed de carga es idempotente (event_id determinista); verificado con dos corridas consecutivas.
  - `[A7]` el arranque del Outbox ya no falla si RabbitMQ no está disponible; la topología se declara al establecer conexión.
  - `[A8]` `InvalidPaymentRequestException` pasó a la API pública y el handler de pagos ya no captura `IllegalArgumentException` genérico.
  - `[A9/A10]` documentadas la idempotencia concurrente (segunda solicitud puede ver `PENDING`) y la ausencia de retención de eventos de acceso de auditoría.
  - Hallazgos de otros dueños (pendientes de coordinar): parámetros/headers faltantes devuelven 500 (handler compartido de Jairo) y scheduler de un solo hilo (config de Andy).
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers: `BUILD SUCCESS`, 127 pruebas, 0 fallos, 0 errores, 0 omitidas.
- Hoy y trazabilidad: correcciones A1, A4 a A8 sobre BB-04, PAY-01 a PAY-03, SEC-01 y QA-02; EaC-12 sigue cubierto con evidencia más estricta.
- Impedimentos: los dos hallazgos restantes pertenecen a `shared` (Jairo) y `integration` (Andy); se documentan para su revisión.
- Horas reales y estimadas restantes: pendiente de registrar por Edson.
- Persona que quedaría bloqueada y acción temporal: Jairo debe corregir el handler de parámetros/headers para que el contrato 400 sea consistente; Andy puede subir el pool del scheduler o permitir un scheduler propio en pagos.

### Fecha: 2026-09-23 — Integrante: Jairo Gómez

- Ayer: la rama `feature/carrito-ordenes` contenía el flujo comercial y la experiencia de catálogo, pero estaba 37 commits detrás de `develop` y no podía integrarse directamente por conflictos en pagos, auditoría, mensajería, seguridad, migraciones y frontend.
- Trabajo realizado hoy:
  - Se integró `develop` preservando el catálogo, carrito, órdenes, ficha de producto, dashboard, Query y Notifications de la rama funcional.
  - Se adoptaron como contratos canónicos las APIs públicas de Payments, Audit y Outbox aportadas en `develop`, eliminando implementaciones duplicadas y declarando las fronteras con interfaces nombradas de Spring Modulith.
  - Se conectaron las transiciones de orden al Outbox `sgo.events`; Query y Notifications consumen `ORDER_STATUS_CHANGED` de forma idempotente.
  - Se unificaron JWT y roles con la navegación comercial; catálogo y stock permanecen públicos, mientras carrito, compra y paneles protegidos envían el token correspondiente.
  - Se resolvieron colisiones Flyway: infraestructura transversal en `V3001`–`V3004` y flujo comercial en `V4001`–`V4003`.
  - Se mantuvo `Docs/DDA.md` sin modificaciones durante la integración.
- Resultado funcional o numérico: `./mvnw test` terminó con `BUILD SUCCESS`; 134 pruebas, 0 fallos, 0 errores y 0 omitidas; Flyway aplicó 16 migraciones desde cero. Todos los archivos JavaScript pasaron `node --check`, `docker compose config --quiet` terminó correctamente y no quedaron marcadores ni rutas sin resolver.
- Hoy y trazabilidad a CUN/EaC/restricción/ADR: se consolidaron ORD-01/ORD-02 con PAY-01/02/03, AUD-01/02, BB-04, BB-07, QRY-01, NOT-01 y SEC-01 sin romper el flujo comercial del MVP descrito en el DDA.
- Impedimentos, responsable e impacto: no hay impedimento técnico local para continuar; queda publicar la rama y realizar la revisión/aceptación del equipo antes de integrar a `develop` remoto.
- Horas reales y estimadas restantes: pendientes de registrar por Jairo.
- Persona que quedaría bloqueada y acción temporal: nadie queda bloqueado localmente; el equipo debe basarse en el commit de integración para evitar reintroducir las implementaciones duplicadas.

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
| 2026-09-23 | ADR-009, ADR-010 y ADR-012, aprobación de Edson | Aprobados por Edson; siguen `Proposed` | Edson (revisión); Jairo y Kevin pendientes | `contract-status.md` y REV-EDSON | Habilita construir pagos y auditoría sobre contratos ya revisados |
| 2026-09-23 | DEC-06, autenticación para ADR-013 | Aceptada: JWT | Equipo (registrada por Edson) | Pendiente de redactar ADR-013 en BB-07 | BB-07 y SEC-01 usarán tokens JWT y roles por endpoint |
| 2026-09-23 | BB-04, implementación del sobre de eventos v1 | Implementado contra contrato `Proposed` | Edson | `V3001`, `integration/outbox` y `OutboxIntegrationTest` | El contrato sigue `Proposed` hasta ratificación del equipo |
| 2026-09-23 | ADR-013, autenticación JWT, roles y sanitización | Proposed | Edson (redacción y decisión JWT del equipo); revisión de Jairo, Andy y Kevin pendiente | `docs/adr/ADR-013-autenticacion-jwt-y-roles.md` | BB-07 implementado; SEC-01 y PAY-01 se apoyan en la API protegida |

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
| 2026-09-23 | Línea base integrada antes de BB-04/AUD-01 | Java 25; PostgreSQL 17.11 y RabbitMQ 4.3.5 mediante Testcontainers | `BUILD SUCCESS`; 73 pruebas, 0 fallos, 0 errores, 0 omitidas | Surefire local | Punto de comparación verificado antes de crear nuevas tablas y módulos |
| 2026-09-23 | Outbox + auditoría transversal inicial | Migración V4001, módulos `messaging`/`audit`, integración con Orders y Testcontainers reales | `BUILD SUCCESS`; 79 pruebas, 0 fallos, 0 errores, 0 omitidas; 12 migraciones aplicadas desde cero | Surefire local y salida Maven | Publicación confiable y evidencia append-only operativas para transiciones de orden; consumidores y cobertura de otros agregados continúan pendientes |
| 2026-09-23 | Inbox idempotente + consumidor Query | Migración V4002, sobre JSON v1, cola durable y proyección `ORDER_STATUS_CHANGED` | `BUILD SUCCESS`; 84 pruebas, 0 fallos, 0 errores, 0 omitidas; 13 migraciones aplicadas desde cero; smoke Outbox → RabbitMQ → Inbox → Query exitoso | Surefire local, Testcontainers PostgreSQL/RabbitMQ, pruebas de arquitectura y Compose | Query procesa cada evento una sola vez por consumidor; ERP, POS, Notifications y otros productores continúan pendientes |
| 2026-09-23 | Catálogo comercial + consumidor Notifications | Catálogo público, ficha de producto, favoritos/carrito autenticado, V4003, contacto confirmado y cola Notifications | `BUILD SUCCESS`; 87 pruebas, 0 fallos, 0 errores, 0 omitidas; 14 migraciones; compra E2E y notificación `SENT` | Chromium, Surefire, Testcontainers, Compose y PostgreSQL/RabbitMQ reales | El flujo principal llega a orden confirmada y notifica sin llamada manual; ERP/POS y eventos de logística/incidencias continúan pendientes |
| 2026-09-23 | `./mvnw clean verify` | Java 25.0.4_7; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 74 pruebas, 0 fallos, 0 errores, 0 omitidas | Surefire y `docs/qa/trazabilidad-cun-mvp.md` | Incluye la regresión A8 y las pruebas de Outbox/Inbox |
| 2026-09-23 | Smoke Outbox en Compose | Cuatro servicios `healthy`; evento `PENDING` insertado y capturado con cola temporal | Evento `PUBLISHED`; sobre con `messageId`, correlación y tipo; topología declarada | Salida local del 2026-09-23 | Publicación confiable verificada contra RabbitMQ real |
| 2026-09-23 | `./mvnw clean verify` (BB-07) | Java 25.0.4_7; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 85 pruebas, 0 fallos, 0 errores, 0 omitidas | Surefire y `docs/adr/ADR-013-autenticacion-jwt-y-roles.md` | Login JWT, 401/403 por rol y sanitización verificados |
| 2026-09-23 | Smoke de seguridad en Compose | Backend y frontend `healthy`; login de los cuatro roles | Token válido; 401 sin token; 403 de `CLIENT` en fiscal/despacho; health y frontend públicos | Salida local del 2026-09-23 | Autorización por rol operando en el stack real |
| 2026-09-23 | `./mvnw clean verify` (AUD-01) | Java 25.0.4_7; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 91 pruebas, 0 fallos, 0 errores, 0 omitidas | Surefire y `docs/diagrams/modelo-datos.md` §3.7 | Evidencia auditable atómica con actor, cambio, resultado y correlación |
| 2026-09-23 | `./mvnw clean verify` (AUD-02) | Java 25.0.4_7; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 97 pruebas, 0 fallos, 0 errores, 0 omitidas | `AuditQueryIntegrationTest` y `docs/qa/QA-03-matriz-eac.md` | EaC-02 < 1 s sobre 10 000 eventos; acceso, fallo y exportación registrados |
| 2026-09-23 | Smoke AUD-02 en Compose | Backend `healthy`; consulta, exportación, búsqueda fallida y 403 de `CLIENT` | Consulta ~63 ms; CSV con `Content-Disposition`; 404 con evidencia `AUDIT_SEARCH_FAILED` | Salida local del 2026-09-23 | Consulta indexada y evidencia de acceso operando en el stack real |
| 2026-09-23 | `./mvnw clean verify` (PAY-01) | Java 25.0.4_7; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 107 pruebas, 0 fallos, 0 errores, 0 omitidas | `PaymentAttemptServiceTest`, `PaymentIntegrationTest` y `docs/qa/QA-03-matriz-eac.md` | Intentos idempotentes, concurrencia y ausencia de PAN/CVV verificados |
| 2026-09-23 | Smoke PAY-01 en Compose | Backend `healthy`; intento, repetición, consulta, PAN y rol | 201 `APPROVED`; 200 mismo intento; 400 PAN; 403 `FINANCE`; auditoría y BD sin PAN/CVV | Salida local del 2026-09-23 | Puerto PayKlik e intentos operando en el stack real |
| 2026-09-23 | `./mvnw clean verify` (PAY-02) | Java 25.0.4_7; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 112 pruebas, 0 fallos, 0 errores, 0 omitidas | `ResilientPayKlikClientTest`, `PaymentIndeterminateIntegrationTest` y QA-03 | Timeout, circuito abierto e indeterminado verificados |
| 2026-09-23 | Smoke PAY-02 en Compose | PayKlik en modo `TIMEOUT`, umbral de 2 fallas; tres intentos | Dos `INDETERMINATE`/`PAYKLIK_TIMEOUT` y uno `PAYKLIK_CIRCUIT_OPEN`; evento `PaymentIndeterminate`; 0 órdenes perdidas | Salida local del 2026-09-23 | Aislamiento de fallas de PayKlik operando en el stack real |
| 2026-09-23 | `./mvnw clean verify` (PAY-03) | Java 25.0.4_7; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 116 pruebas, 0 fallos, 0 errores, 0 omitidas | `PaymentReconciliationServiceTest`, `PaymentReconciliationIntegrationTest` y QA-03 | Conciliación sin doble cobro, ≤ 60 s y alerta verificadas |
| 2026-09-23 | Smoke PAY-03 en Compose | Intento indeterminado sembrado y, aparte, PayKlik `TIMEOUT` con reintentos cortos | Conciliación `RESOLVED/APPROVED` con evento `PaymentApproved`; en fallo, `DEAD_LETTER` sin perder la orden | Salida local del 2026-09-23 | Resolución y bandeja de fallidos operando en el stack real |
| 2026-09-23 | `./mvnw clean verify` (SEC-01) | Java 25.0.4_7; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 118 pruebas, 0 fallos, 0 errores, 0 omitidas | `SensitiveDataLeakScanTest`, `SecurityAuthorizationTest` y `docs/evidence/security/SEC-01-scan-pan-cvv.md` | 0 PAN/CVV en base, mensajes y logs; matriz 401/403 completa |
| 2026-09-23 | k6 `T` y `8T` — consulta consolidada | 10 y 80 VUs, 1 min, 500 proyecciones | `T` 1 190 iter / p95 5.88 ms; `8T` 9 600 iter / p95 4.22 ms; 0 % errores | `docs/evidence/performance/QA-02-linea-base-y-8T.md` | EaC-01 cubierto; `8T` ≈ 8.04× `T` |
| 2026-09-23 | k6 `T` y `8T` — evidencia | 10 y 80 VUs, 1 min, 10 000 eventos | `T` 1 180 iter / p95 17.2 ms; `8T` 9 540 iter / p95 7.23 ms; 0 % errores | `docs/evidence/performance/QA-02-linea-base-y-8T.md` | EaC-02 con evidencia de carga; `8T` ≈ 8.04× `T` |
| 2026-09-23 | Corrección por QA-02 | 10 VUs sobre auditoría antes/después | Antes: p95 30 s y 95 % errores; después: p95 17.2 ms y 0 % | `AuditQueryIntegrationTest.concurrentQueriesDoNotExhaustTheConnectionPool` | Pool de conexiones liberado; acceso separado de la evidencia |
| 2026-09-23 | `./mvnw clean verify` (QA-02) | Java 25.0.4_7; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 119 pruebas, 0 fallos, 0 errores, 0 omitidas | Surefire y QA-03 | Suite completa tras las correcciones de DEC-02/QA-02 |
| 2026-09-23 | `./mvnw clean verify` (inspección A1/A4–A8) | Java 25.0.4_7; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 127 pruebas, 0 fallos, 0 errores, 0 omitidas | `PaymentRequestTest`, `ResilientPayKlikClientTest` y `PaymentReconciliation*Test` | Validación endurecida, timeout interrumpible y recuperación de PENDING |
| 2026-09-23 | Smoke A1 en Compose | Referencias `4111-1111-1111-1111`, `4111 1111 1111 1111`, `4111.1111.1111.1111`, `cvc2=123`, `securityCode=123` y `tok_test_123` | Las cinco inseguras → `400 INVALID_PAYMENT_REQUEST`; la segura → `201 APPROVED`; 0 referencias inseguras en BD | Salida local del 2026-09-23 | El borde rechaza PAN con separadores y variantes de CVV |
| 2026-09-23 | Smoke A5 en Compose | Intento `PENDING` con 5 minutos de antigüedad | Pasa a `INDETERMINATE`/`STALE_PENDING`; se programa conciliación `PENDING` y evidencia auditable | Salida local del 2026-09-23 | El barrido recupera intentos huérfanos |
| 2026-09-23 | Seed A6 (idempotencia) | Dos corridas consecutivas de `tests/data/seed-load.sql` | 10 000 filas nuevas en la primera; la segunda no agrega ninguna | Conteo `actor='load-seed'` en PostgreSQL | El seed ya es idempotente |
| 2026-09-23 | Integración `feature/carrito-ordenes` + `develop` | Java 25; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers; 16 migraciones Flyway | `BUILD SUCCESS`; 134 pruebas, 0 fallos, 0 errores, 0 omitidas | Surefire local, `node --check`, `docker compose config --quiet` y verificación de conflictos | Flujo comercial, seguridad, pagos, auditoría y Outbox quedaron consolidados sin dependencias modulares inválidas |
| 2026-09-23 | Merge de `develop` en `docs/rev-edson-cruzadas` | Conflictos resueltos en pagos (validación tipada + `PaymentMethod` del api), README y bitácora; Java 25 + Testcontainers | `BUILD SUCCESS`; 143 pruebas, 0 fallos, 0 errores, 0 omitidas; `docker compose config --quiet` correcto | Surefire y este registro | La rama queda con el carrito/órdenes de Jairo y los bugfixes A1/A4–A8 sin perder ninguna parte |

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
