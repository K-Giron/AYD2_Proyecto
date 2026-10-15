# Bitácora de trabajo arquitectónico - Fase 2

## 1. Información general del sprint

| Campo | Valor |
|---|---|
| Sistema | SGO-Logic |
| Equipo | Grupo 5 |
| Integrantes | Jairo Gómez, Jorge Pumay, Edson Sosa, Andy Sánchez y Kevin Girón |
| Periodo planificado | 10 al 23 de septiembre de 2026 |
| Objetivo del sprint | Construir e integrar una base ejecutable de SGO-Logic para que cada responsable incorpore progresivamente su módulo. El tablero cerró con 54 tarjetas en `Done`; la evidencia técnica y las limitaciones se detallan en esta bitácora. |
| Enlace al Kanban | [MegaStore - Fase 2](https://trello.com/b/uyoJQ4RH/megastore-fase-2) |


## 2. Sprint Backlog

| ID | Descripción | Dueño | Estado | Dependencias | Evidencia |
|---|---|---|---|---|---|
| BB-01 | Crear estructura FASE 2, README, bitácora y plantillas ADR | Jairo | Cerrado; implementación y validación cruzada registradas | Ninguna | `FASE 2/README.md`, `FASE 2/docs/evidence/functional/VALIDACION_CRUZADA_BB01_BB05.md` y tarjeta [DzYqM3ew](https://trello.com/c/DzYqM3ew) |
| BB-02 | Registrar ADR-009 de stack y arquitectura ejecutable | Jairo | Redacción implementada; cierre formal bloqueado por aprobaciones | Gate 0 y aprobación de al menos tres integrantes | `FASE 2/docs/adr/ADR-009-stack-y-topologia-ejecutable.md` y tarjeta [FRHUOgHv](https://trello.com/c/FRHUOgHv) |
| BB-05 | Docker Compose y configuración por ambiente | Jairo | Cerrado; validado localmente, por CI y mediante revisión cruzada | ADR-009 y componentes compartidos | `FASE 2/docker-compose.yml`, `.env.example`, `FASE 2/docs/evidence/functional/VALIDACION_CRUZADA_BB01_BB05.md` y tarjeta [eu881SUz](https://trello.com/c/eu881SUz) |
| ORD-01 a ORD-05 | Carrito, orden, Saga, compensación y datos/E2E | Jairo | Implementados en `develop`; 5 recorridos E2E verdes | Puertos de Inventory, Payments, Fiscal y Logistics | `OrderSagaService`, `OrderJourneysE2ETest` y evidencia ORD-05/QA-04 |
| QA-01 | Contrato e integración continua | Jairo | Implementado y validado en GitHub Actions | Repositorio, secretos y stack | `.github/workflows/qa-integracion-base.yml`; run `35944837459` verde para `1294231` |
| QA-04 | Automatizar el camino feliz completo | Jairo | Parcial avanzado: Saga/E2E hasta despacho, entrega y devolución; falta demostrar ERP/POS dentro del recorrido de diez pasos | ORD-05 y productores/consumidores restantes | `OrderJourneysE2ETest` y `docs/evidence/functional/ORD-05-QA-04-cinco-flujos-e2e.md` |
| DOC-01 | Consolidar DDA, diagramas y ADR | Jairo | Cerrado en el alcance J3: DDA, CDU, vistas arquitectónicas y modelos físicos actualizados y enlazados | Aprobaciones de ADR administradas por sus respectivos dueños | `Docs/DDA.md`, `FASE 2/docs/diagrams/nuevos-fase2/NUEVO-modelos-datos-fase2.drawio` y `docs/contracts/contract-status.md` |
| DOC-03 | Bitácora, capturas Kanban y burndown | Jairo | Cerrado en el alcance de Jairo; captura inicial, captura final, retrospectiva propia y resumen 54 → 0 incorporados | Aportes personales de retrospectiva de los demás integrantes | Esta bitácora, §§3 y 8–10, y tarjeta [MnIkXzvZ](https://trello.com/c/MnIkXzvZ) |
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

Los estados anteriores reflejan el corte documental de 2026-10-12. Las entradas
diarias posteriores conservan el estado que era verdadero en su fecha y no se
reescriben retroactivamente como si el trabajo ya hubiera estado cerrado.

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
- Horas reales: no fueron registradas en esa fecha; se conserva la ausencia de
  la métrica sin inventar una estimación retroactiva.
- Personas o tareas desbloqueadas: la base técnica permite que Jorge, Edson, Andy y Kevin comiencen sus módulos sin crear otro backend; la disponibilidad o adopción efectiva por ellos aún no está verificada.

### Evidencia visual del estado actualizado el 2026-09-14

![Captura1](FASE%202/docs/evidence/bitacora/2026-09-14_13-57.png)

La captura muestra `[BB-02]` en `Blocked`, `[DOC-03]` en `In Progress` y `[BB-01]`/`[BB-05]` en `Ready for Testing`. Los nombres de archivo se conservan como fueron entregados y no se usan para inferir el orden cronológico entre ambas capturas.

### Evidencia de QA-01

La ejecución de GitHub Actions `35944837459`, asociada al commit de integración
`1294231`, terminó en verde con compilación, pruebas y smoke test de Docker
Compose. Este resultado también está registrado en §7. La evidencia consultable
es el [run 35944837459](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/35944837459); no se exige una captura duplicada para sustituir ese enlace.

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
- Horas reales: no fueron registradas en esa fecha; se conserva la ausencia de
  la métrica sin inventar una estimación retroactiva.
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
- Horas reales: no fueron registradas en esa fecha; no se reconstruyen
  retroactivamente.

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
- Horas reales: no fueron registradas en esa fecha; se conserva la ausencia de
  la métrica sin inventar una estimación retroactiva.
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
- Horas reales: no fueron registradas en esa fecha; no se reconstruyen
  retroactivamente.
- Persona que quedaría bloqueada y acción temporal: nadie queda bloqueado localmente; el equipo debe basarse en el commit de integración para evitar reintroducir las implementaciones duplicadas.

### Fecha: 2026-09-23 (continuación final) — Integrante: Jairo Gómez

- Ayer: el flujo comercial integrado permitía crear órdenes y ejecutar manualmente reserva, pago y fiscal, pero todavía no existía una Saga única que coordinara el recorrido ni pruebas E2E automatizadas de extremo a extremo.
- Trabajo realizado hoy:
  - `[ORD-03]` se implementó `OrderSagaService` como coordinador reentrante e idempotente de carrito/orden → reserva → PayKlik → SAT → confirmación → despacho, consumiendo únicamente los puertos públicos de Inventory, Payments, Fiscal y Logistics.
  - `[ORD-04]` se incorporó la cancelación desde Orders mediante puertos: una orden cancelable libera sus reservas activas; la repetición de la solicitud no duplica la compensación. Los estados posteriores a confirmación no se disfrazan como cancelación y deben seguir el flujo de devolución/reembolso de CUN-10.
  - Se conectó una orden confirmada con `logistics.api.DispatchPort`; la Saga confirma el consumo de inventario, crea un único envío y lleva la orden a preparación sin acceder a repositorios internos de Logistics.
  - El checkout del cliente usa `POST /api/v1/orders/{orderId}/saga`, conserva la misma llave idempotente y reanuda la operación cuando SAT permanece pendiente.
  - `[ORD-05/QA-04]` se crearon datos deterministas y cinco recorridos E2E: compra web completa; recuperación omnicanal WEB→MOBILE; cancelación y liberación de inventario; compra y entrega; compra, entrega e incidencia resuelta administrativamente como `REFUNDED`.
  - Se documentó la evidencia en `FASE 2/docs/evidence/functional/ORD-05-QA-04-cinco-flujos-e2e.md` y se mantuvo `Docs/DDA.md` sin modificaciones.
- Resultado funcional o numérico: `OrderJourneysE2ETest` ejecutó **5 pruebas, 0 fallos y 0 errores** contra PostgreSQL y RabbitMQ reales mediante Testcontainers. La regresión completa `./mvnw test` terminó con **BUILD SUCCESS: 139 pruebas, 0 fallos, 0 errores y 0 omitidas**. Las pruebas verifican autenticación JWT, HTTP, persistencia, idempotencia, stock, fiscal, despacho, tracking e incidencia, sin sustituir los puertos por mocks.
- Hoy y trazabilidad a CUN/EaC/restricción/ADR: ORD-03, ORD-04, ORD-05, QA-04; CUN-01 a CUN-04, CUN-07 a CUN-10; EaC-01, EaC-03, EaC-04, EaC-05, EaC-07, EaC-09, EaC-11 y EaC-12; ADR-001 a ADR-007, ADR-010 y ADR-012.
- Impedimentos, responsable e impacto: el quinto flujo acredita la resolución administrativa `REFUNDED` disponible en INC-01, pero **no debe presentarse como CUN-10 completo**: siguen pendientes la nota de crédito fiscal, el reintegro de inventario y la orquestación de reembolso real. Es trabajo del módulo de postventa, no una cancelación de Orders.
- Horas reales: no fueron registradas en esa fecha; se conserva la ausencia de
  la métrica sin inventar una estimación retroactiva.
- Persona que quedaría bloqueada y acción temporal: nadie queda bloqueado para demostrar ORD-05; la integración final a `main` queda deliberadamente fuera de este trabajo hasta que finalice el equipo.

### Fecha: 2026-09-23 (cierre técnico en `develop`) — Integrante: Jairo Gómez

- Trabajo realizado: se resolvió el merge de `feature/pruebas-e2e` hacia `develop`, conservando los bugfixes de Payments/SEC-01 y la implementación de Saga. El merge `1294231` está publicado en `origin/develop` y el árbol local está limpio y sincronizado.
- Resultado verificable: `./mvnw test` finalizó con **148 pruebas, 0 fallos, 0 errores y 0 omitidas**; `docker compose config --quiet`, `node --check` y la arquitectura modular finalizaron correctamente. La ejecución de GitHub Actions del mismo commit (`35944837459`) terminó verde: compilación/pruebas y smoke test de Docker Compose exitosos.
- Estado de las tareas de desarrollo de Jairo: BB-01, BB-05 y ORD-01 a ORD-05 están implementadas; la parte técnica principal de Orders/Saga queda cerrada. QA-04 permanece parcial frente a su criterio formal de diez pasos porque los cinco E2E no incorporan todavía ERP y POS en el mismo recorrido. El hallazgo de parámetros/headers faltantes que responden 500 en `GlobalExceptionHandler` continúa pendiente.
- Estado documental/administrativo: la bitácora ya registra el merge y la evidencia técnica, pero DOC-03 no puede cerrarse hasta agregar captura final real del Kanban y burndown final. BB-02 y DOC-01 requieren aprobaciones/ratificación de ADR y reconciliar documentos que aún describen la Saga como pendiente. No se modifica el DDA para ocultar o reducir alcance.
- Revisión contra la rúbrica: se contrastaron los 80 puntos base de la Hoja de Calificación con los doce CDU, EaC, código y pruebas; el diagnóstico, brechas y orden de cierre quedaron en `FASE 2/docs/qa/AUDITORIA.md`.
- Impedimentos y responsables: las aprobaciones corresponden al equipo; la captura final requiere el estado real del tablero; ERP/POS depende de la integración transversal con los módulos propietarios. REL-01 y el merge/tag final permanecen fuera de este cierre hasta decisión del equipo.
- Horas reales: no fueron registradas en esa fecha; se conserva la ausencia de
  la métrica sin inventar una estimación retroactiva.

### Fecha: 2026-09-24 — Integrante: Kevin Girón

- Ayer: PR #5 (`feature/kevin-logistica-consulta-notificaciones-incidencias`) fue mergeado a `develop`. Después llegaron 74 commits: Edson cerró BB-04 (Outbox/Inbox), BB-07 (JWT), AUD-01/02 y PAY-01 a PAY-03; Andy cerró BB-06 (simuladores configurables), FIS-01 a FIS-03, ERP-01/02 y POS-01; Jairo integró todo, cerró ORD-03 a ORD-05 (Saga, cancelación, 5 recorridos E2E) y construyó el catálogo/carrito/login real del frontend.
- Trabajo realizado hoy:
  - Se hizo `git fetch` y se revisó a fondo qué cambió en y alrededor de mis 4 módulos antes de tocar nada: confirmé que mi PR ya estaba integrado, que mis archivos de `logistics`/`notifications`/`query`/`incidents` fueron extendidos (no reemplazados) por el equipo — `logistics.api.DispatchPort` para la Saga, `IncidentService` con `AuditRecorder`, `CustomerContact`/`OrderStatusNotificationConsumer` en notifications, `OrderStatusChangedConsumer` en query — y que `login.html`/`session.js` ya usan JWT real en vez de mi simulación.
  - `[ADR]` Revisé y aprobé ADR-009, ADR-012, ADR-013 y ADR-014 (algoritmo de origen de despacho, Jorge) — los cuatro que me listaban como revisor pendiente. Registrado en `docs/contracts/contract-status.md`.
  - `[NOT-01]` Cerré la brecha que Jairo señaló explícitamente el 23 de septiembre ("CUN-11 continúa parcial hasta recibir eventos de despacho/incidencias... y disponer de DLQ del trabajador"):
    - `ShipmentService` (logistics) e `IncidentService` (incidents) ahora publican `SHIPMENT_STATUS_CHANGED`/`INCIDENT_REGISTERED` por el Outbox real (`integration.outbox.PublishEvent`), en la misma transacción del cambio de negocio.
    - `NotificationService` pasó del reintento único a la secuencia 2/8/32 s de LOG-03, con `notification_dead_letters` (DLQ del trabajador) al agotar intentos.
    - Dos consumidores nuevos (`ShipmentStatusNotificationConsumer`, `IncidentNotificationConsumer`) resuelven el cliente mediante `order_customer_links`, una tabla local que recuerda `orderId -> customerId` (alimentada por el propio consumidor de `ORDER_STATUS_CHANGED`), porque Logistics/Incidents no conocen ni deben conocer al cliente.
  - `[QRY-01]` Cerré la brecha equivalente en Query, señalada en `QA-06-gate-trazabilidad-penalizaciones.md` ("faltan productores de... despacho, entrega e incidencia"): dos consumidores nuevos (`ShipmentStatusChangedConsumer`, `IncidentRegisteredConsumer`) proyectan `dispatchStatus`/`deliveryStatus`/`incidentStatus` automáticamente.
  - Migración nueva: `V1005__notifications_create_dead_letters_and_order_links.sql` (rango V1001-V1099 ratificado el 23 de septiembre).
  - **Bug real encontrado y corregido**: envolver `ShipmentService.createDispatch`/`changeDeliveryMethod` en `@Transactional` (necesario para que `PublishEvent` tenga transacción activa) abrió una condición de carrera — el reintento de guía podía programarse y ejecutarse en otro hilo antes del commit, sin encontrar el despacho (`AsyncIntegrationPersistenceTest` lo detectó). Se corrigió con el mismo patrón *after-commit* que ya usaba `NotificationService.dispatchAfterCommit`.
  - Bug de prueba propio corregido en `NotificationServiceTest` (comparaba IDs de dos objetos distintos; mismo tipo de error que ya había corregido en `ShipmentServiceTest` el 22 de septiembre).
  - Actualicé `QA-03`, `QA-05`, `QA-06`, `DEMO-01` y `docs/contracts/contract-status.md` para reflejar la integración real en vez de el estado bloqueado del 22 de septiembre; agregué `module-delivery-checklist-kevin-2026-09-24.md`.
- Resultado funcional o numérico: `./mvnw clean verify` con Java 25 y Testcontainers reales (Postgres 17.11, RabbitMQ 4.3.5): **BUILD SUCCESS, 156 pruebas, 0 fallos, 0 errores, 0 omitidas** (partió de 148 antes de mi trabajo de hoy; 8 pruebas nuevas: 2 de `ShipmentServiceTest`, 1 de `IncidentServiceTest`, 2 de `OrderStatusNotificationConsumerTest`, 3 de los consumidores nuevos de `query`).
- Hoy y trazabilidad a CUN/EaC/restricción/ADR: CUN-09 (QRY-01), CUN-11 (NOT-01), ADR-002 (Outbox/Inbox), ADR-009/012/013/014 (revisión). EaC-05 y EaC-08 (mías) permanecen cubiertas; ver `QA-03-matriz-eac.md` actualizada.
- Impedimentos, responsable e impacto: ninguno propio. Sigue pendiente el historial por intento de notificación (hoy solo el último error) y la prueba E2E que fuerce las cuatro fallas externas a la vez sobre la Saga real (QA-05 §3.1) — ninguno bloquea a otro integrante hoy.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna. Falta revisor distinto del autor para ambas entregas (22 y 24 de septiembre) antes de poder pedir merge.

**Actualización — mismo día, revisión funcional adicional de `incidents`:**

- Revisando `IncidentController` detecté que la consulta de atenciones del cliente solo funcionaba por `orderId` — no había forma de ver el historial de un cliente a través de todas sus órdenes, algo que sí se espera del módulo de atención al cliente.
- `[INC-02]` Cerrado: `customerId` opcional agregado a `Incident` (migración `V1006__incidents_add_customer_id.sql`), `IncidentRepository.findByCustomerIdOrderByCreatedAtDesc`, `IncidentService.getIncidentsForCustomer`, y `IncidentController` ahora expone `GET /api/v1/incidents?orderId=` o `?customerId=` (nuevo `MissingIncidentSearchParameterException` si no se manda ninguno). Conectado en `cliente.html`/`app-cliente.js` con un botón "Ver todo mi historial" y enviando `customerId` al registrar una devolución/incidencia.
- **Bug de integración ajeno, encontrado y corregido**: el cambio de firma de `IncidentService.register()` rompió la compilación de `AuditIntegrationTest` (de Edson, en el módulo `audit`), que llamaba al método con la firma antigua. Se corrigió el call site pasando un `customerId` nuevo — no se tocó nada más de ese archivo.
- Resultado funcional: `./mvnw clean verify` con Java 25 y Testcontainers reales — **BUILD SUCCESS**, sin errores de compilación ni fallos, incluida la prueba nueva `getIncidentsForCustomerDelegatesToRepository`.
- Hallazgos de equipo (no son mi módulo, quedan documentados para priorizar, no implementados por mí): los diagramas de bloques/componentes/despliegue en `Docs/diagramas/` son del 26 de agosto (Fase 1) y no mencionan ningún módulo real de Fase 2. No existe un "Informe de ventas" en ningún lado del código ni de los diagramas. Detalle completo en `ESTADO-MI-PARTE-KEVIN.md` §2 (fuera del repo).

**Actualización — mismo día, entradas/salidas de inventario a solicitud directa (fuera de mi módulo, sobre `inventory` de Jorge Pumay):**

- `inventory` solo tenía dos formas de mover existencia: reservar (INV-02) y confirmar una venta ya reservada (`tryConfirm`). No había manera de registrar una recepción de mercadería ni una salida directa (merma, ajuste de tienda/POS) fuera del flujo de venta en línea.
- Agregado: `StockMovementType` (INBOUND/OUTBOUND), `InventoryStockMovement` (evidencia persistida, migración `V3__inventory_create_stock_movements.sql`, siguiente número libre del rango V1-V99 de Jorge), `InventoryItemRepository.tryIncreaseOnHand`/`tryDecreaseOnHand` (mismo patrón atómico UPDATE...WHERE de `tryReserve`), `InventoryMovementService` (publica `INVENTORY_STOCK_MOVEMENT_REGISTERED` al Outbox en la misma transacción) y `POST /api/v1/inventory/stock-movements` en `InventoryController`. `inventory` ahora depende también de `integration` (antes solo de `shared`) — actualizado en `ModularArchitectureTest`.
- Frontend: nuevo panel "Inventario — entradas y salidas" en `staff.html`/`app.js`, independiente de una orden (a diferencia del resto del dashboard operativo), con selección de producto (desde `catalog.json`) y ubicación (desde `GET /api/v1/inventory/locations`).
- Probado en vivo contra el stack real: entrada de 15 unidades (4→19), salida de 5 unidades (19→14) y salida de 9000 unidades correctamente rechazada con `409 INSUFFICIENT_ON_HAND_STOCK`.
- Resultado funcional: `./mvnw clean verify` — **BUILD SUCCESS**, incluida la prueba nueva `InventoryMovementServiceTest` (4 casos: entrada, salida, salida insuficiente, ubicación inexistente).
- Nota de coherencia con `AUDITORIA.md` (Jairo, 23 de septiembre): "Actualizar inventario" y "Consultar historial" ya estaban señaladas ahí como parciales (1/3 y 1/2 respectivamente, por falta de entradas/POS y de historial consolidado por cliente); ambas quedan cerradas con el trabajo de hoy.

### Fecha: 2026-10-01 — Integrante: Kevin Girón

- Trabajo realizado: construí `FASE 2/inventory-service/`, un almacén central nuevo con login y tres roles, a solicitud directa mía (no es trabajo de Jorge ni de Jairo, y no toca su código — cero archivos modificados en `FASE 2/backend/`).
  - **Login**: JWT por rol (`ALMACEN`, `FINANZAS`, `LOGISTICA`), tres cuentas de demostración, autorización por endpoint.
  - **Almacén**: `Article` (catálogo propio, CRUD), `Stock` (existencia por artículo, solo cambia por movimiento), `StockMovement` (entrada/salida con **folio** secuencial `ENT-000001`/`SAL-000001`).
  - **Finanzas**: cuentas por pagar (proveedores) y por cobrar (tiendas/clientes), con fecha límite, folio relacionado opcional y liquidación.
  - **Logística**: cada folio de salida genera automáticamente un envío en estado Pendiente; avanza Pendiente → En tránsito → Entregado, un paso a la vez.
  - Web con pestañas por módulo, visibilidad de acciones según el rol de la sesión.
  - Un contenedor (API + web) y una base de datos Postgres propia en `docker-compose.yml`, sin ninguna llamada de red hacia/desde `backend` del punto de venta.
- Hallazgo de ecosistema no documentado en ningún lado del proyecto: Spring Boot 4.1 migró a Jackson 3, que cambió su Maven groupId/paquete de `com.fasterxml.jackson.*` a `tools.jackson.*` (ej. `tools.jackson.databind.ObjectMapper`, artefacto `tools.jackson.core:jackson-databind:3.1.5`). Me costó tres iteraciones de build descubrirlo porque el síntoma ("package does not exist") no lo deja obvio. Lo dejo anotado aquí por si alguien más del equipo se topa con el mismo error al escribir una prueba con `ObjectMapper` en Spring Boot 4.1.
- Resultado funcional: `./mvnw clean verify` del `inventory-service/` — **BUILD SUCCESS**, incluida la prueba de integración completa (login por rol, folio de entrada/salida, envío automático desde el folio de salida, cuenta por cobrar y liquidación, rechazo sin token).
- Impedimentos, responsable e impacto: ninguno para el equipo — es un sistema nuevo, aislado, en rama local, sin commit ni push. No bloquea ni depende de nadie.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

### Fecha: 2026-10-02 — Integrante: Kevin Girón

- Ayer: el almacén central (`inventory-service`) quedó con login, artículos/stock/movimientos con folio, finanzas y logística, pero con un diseño visual plano y con "destinatario" como texto libre en vez de un punto de venta real.
- Trabajo realizado:
  - **Rediseño visual completo**: layout con barra lateral y navegación por íconos, pestaña "Resumen" nueva con 4 KPI calculados en vivo (artículos, valor de existencias, cuentas pendientes, envíos en curso), tablas reales en vez de filas sueltas, barra de progreso visual en cada envío, login con panel de marca dividido y accesos directos a las 3 cuentas de demostración.
  - **Punto de venta como entidad real**: nuevo módulo `pointofsale` (identificador propio, código único, almacén asignado) y módulo `warehouse` (almacén central sembrado por migración). Reemplacé el campo de texto libre "destinatario" en movimientos y envíos por una referencia real a `PointOfSale`, resuelta en las respuestas (código, nombre, almacén). Una salida sin punto de venta se rechaza con `400 POINT_OF_SALE_REQUIRED` antes de tocar la existencia.
  - **Datos de demostración**: 3 artículos con stock inicial (camisa, pantalón, zapatos) y 3 puntos de venta sembrados por migración, para no empezar con el catálogo vacío.
  - **Corrección de caché**: el navegador podía quedarse con una versión vieja de `app.js`/`index.html` después de cada rediseño (síntoma reportado: "mis movimientos no funcionan", aunque la API respondía bien por `curl`). Agregué `Cache-Control: no-store` a los recursos estáticos del almacén central.
  - Migraciones nuevas V5-V9 (almacenes, puntos de venta, `point_of_sale_id` en movimientos/envíos reemplazando `destinatario`, artículos de demostración).
- Resultado funcional: `./mvnw clean verify` del `inventory-service/` — ver resultado en este mismo corte; probado en vivo con `curl` de punta a punta (almacén, punto de venta, folio de salida con destino resuelto, rechazo sin destino, envío con tienda y almacén).
- Impedimentos, responsable e impacto: ninguno para el equipo — sigue siendo un sistema nuevo y aislado, sin tocar `FASE 2/backend/`.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día, dos bugs reportados y dos pautas nuevas sobre el almacén central:**

- **Bug 1 corregido**: "si no agrego un artículo manual no se inicia con lo que tiene ya en la BD" y "al hacer un movimiento sí cargan los artículos pero no veo el punto de venta destino habilitado". Causa real: `apiCall()` en `app.js` no pasaba `cache: "no-store"` al `fetch()` — el `Cache-Control: no-store` agregado el 2026-10-01 solo cubre los recursos estáticos de Spring (`index.html`/`app.js`), no las respuestas JSON de los `@RestController`, así que el navegador podía servir una respuesta `GET` vieja (vacía) de `/api/v1/points-of-sale` o `/api/v1/articles` desde antes de sembrar los datos de demostración. Corregido agregando `cache: "no-store"` directo en el `fetch()`. Reforzado además un segundo punto: el campo "Punto de venta destino" solo se sincronizaba en el evento `change` del selector de tipo, nunca al cargar la página — se agregó una llamada a `toggleMovementPosField(...)` al iniciar `bindMainPage()`.
- **Pauta 1 implementada — stock propio por punto de venta**: nuevo módulo `posstock` (`PosStock`, `PosStockMovement`, migración `V11`). Cada punto de venta ahora lleva su propio saldo, separado del almacén central, alimentado **solo** cuando `StockService.registerMovement` registra una salida con `pointOfSaleId`: genera automáticamente un folio propio de tienda (`POS-ENT-NNNNNN`, secuencia `seq_pos_folio_entrada`) correlacionado con el folio de salida del almacén que lo originó. No es posible cargar el stock de una tienda manualmente ni desde ningún otro flujo.
- **Pauta 2 implementada — bitácora/trazo de folios**: nuevo módulo `auditlog` (`AuditLogEntry`, migración `V10`), con `AuditLogService.record(eventType, description, relatedFolio, pointOfSaleId)` inyectado en `ArticleService`, `StockService`, `ShipmentService`, `InvoiceService` y `PointOfSaleService`. Registra automáticamente: creación de artículo, entrada/salida de almacén, recepción en tienda, avance de envío, registro/pago de cuenta — cada evento con actor (resuelto de `SecurityContextHolder`), folio relacionado y fecha. Expuesto en `GET /api/v1/audit-log?relatedFolio=&pointOfSaleId=`.
- **UI nueva**: dos pestañas agregadas a `index.html`/`app.js` del almacén central — "Stock por tienda" (existencia y folios de entrada por punto de venta, con filtro) y "Bitácora" (trazo de eventos con filtro por folio).
- Verificado en vivo con `curl` contra el stack real (`docker compose up --build -d inventory-service`): entrada de 50 unidades (folio `ENT-000001`), salida de 7 hacia POS-01 (folio `SAL-000002`) generó automáticamente `POS-ENT-000001` en el stock de POS-01 (saldo 7) y dos entradas de bitácora (`SALIDA_REGISTRADA`, `POS_ENTRADA_REGISTRADA`) con el folio y el punto de venta correctos.
- Resultado funcional: `./mvnw clean verify` del `inventory-service/` — **BUILD SUCCESS, 25 pruebas, 0 fallos, 0 errores**, incluida la integración completa (migra de v9 a v11 sin perder datos existentes) y la prueba nueva `PosStockServiceTest` (folio de tienda, reutilización de la fila de stock existente).
- **Pauta 3 (centralizar sesión del cliente entre canales) — no es una funcionalidad nueva, ya existe en `develop`**: revisé `Docs/DDA.md` (CDU-01 "Gestionar carrito omnicanal") y el código de Jairo (`orders.Cart`, `orders.SalesChannel{WEB,MOBILE,STORE,CALL_CENTER}`, `POST /api/v1/carts {customerId, channel}` en `OrdersController`) — el mecanismo que pedí ya está implementado como ORD-01 (carrito recuperable entre canales, responsabilidad de Jairo según `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`). Lo que faltaba era un punto de entrada desde el canal tienda: agregué un panel "Buscar cliente (omnicanal)" en `staff.html`/`app.js` (mi módulo UI-01, cero archivos Java tocados) que llama `POST /api/v1/carts` con `channel: "STORE"` y muestra el carrito real del cliente tal cual quedó en la web. Verificado en vivo con `curl` usando un `Idempotency-Key` estable por cliente (header requerido por el endpoint, no documentado en ningún lado que yo haya visto).
- **Hallazgo ajeno, no corregido por mí**: `GlobalExceptionHandler.handleUnexpected()` (módulo `shared`, de Edson) responde `500 INTERNAL_ERROR` sin registrar la excepción real en ningún log — me costó varias iteraciones diagnosticar el `500` de arriba por esto. Documentado aquí para que Edson lo revise, no lo toqué.
- **Pendiente de decisión con Kevin, no implementado todavía**: la cuarta pauta ("levantar otro punto de venta, interfaz de punto local, login solo con nombre de usuario sin contraseña") no se construyó todavía como sistema aparte — quedó pendiente de confirmar si el panel nuevo de `staff.html` ya cubre el caso de uso (vendedor identifica al cliente en tienda física) o si Kevin sigue queriendo un terminal/contenedor nuevo y separado con su propio login simplificado.
- Impedimentos, responsable e impacto: ninguno para el equipo en lo de `inventory-service` (sistema propio, aislado). El hallazgo de `GlobalExceptionHandler` no bloquea a nadie hoy pero sí dificulta diagnóstico futuro de errores 500 — queda como hallazgo para Edson.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día, bug real de caché en `frontend` (mío, UI-01) y validación de la pauta de sesión omnicanal entre puntos de venta:**

- **Bug real encontrado y corregido** (no era un malentendido): el contenedor `frontend` (nginx, `FASE 2/frontend`) nunca tuvo cabeceras `Cache-Control: no-store` — a diferencia de `inventory-service`, donde sí se corrigió este mismo problema el 2026-10-01. Esto hacía que un navegador que ya había visitado `staff.html`/`app.js` antes siguiera sirviendo la versión vieja después de reconstruir el contenedor, sin que el usuario lo notara (sin hard-refresh no vuelve a pedir el archivo). Reproducido de forma controlada con un navegador real (Chrome headless vía DevTools Protocol): en un perfil con una visita previa a `staff.html`, `document.getElementById(...)` no encontraba elementos agregados en el HTML más reciente aunque `curl` sí mostraba el archivo correcto; en un perfil limpio, todo cargaba bien. Corregido agregando `add_header Cache-Control "no-store" always;` en `nginx.conf.template`. Esta es probablemente la causa real de varios reportes de "no carga la lista de artículos" a lo largo de la sesión, no un defecto del código de la pantalla en sí.
- **Validación de la pauta "sesión del cliente no depende de dónde esté"**: el mecanismo de Jairo (`Cart` + `SalesChannel`, CDU-01) ya es agnóstico de punto de venta — el carrito se identifica por `customerId`, no por terminal. Para completar el ciclo (antes solo se podía *ver* el carrito desde tienda, no modificarlo), agregué un formulario "Agregar al carrito desde tienda" en el panel omnicanal de `staff.html`, usando `PUT /api/v1/carts/{cartId}/items/{productId}` (ya existente, de Jairo). Validado en vivo: se abrió un carrito en canal `STORE`, se agregó un artículo desde ese "punto de venta", y una segunda consulta independiente (`GET /api/v1/carts/active?customerId=`, simulando otro canal/terminal) mostró el mismo carrito con el mismo artículo — sin necesidad de ningún sistema nuevo ni de un identificador de punto de venta físico en el modelo de carrito.
- **Corrección a pedido directo de Kevin, en módulo ajeno (`shared`, de Edson)**: `GlobalExceptionHandler.handleUnexpected()` ahora registra la excepción real con `log.error(...)` (método, URI y correlationId) antes de responder `500 INTERNAL_ERROR` — antes no dejaba ningún rastro en logs. Cambio mínimo, no toca el contrato de la API.
- Resultado funcional: `./mvnw clean verify` del `backend/` del monolito — **BUILD SUCCESS, 161 pruebas, 0 fallos, 0 errores**, incluido el cambio de `GlobalExceptionHandler`.
- Impedimentos, responsable e impacto: ninguno. El fix de caché es puramente de infraestructura de `frontend` (mi módulo); el de logging es una mejora de observabilidad de una línea en módulo ajeno, de bajo riesgo, hecha porque Kevin la pidió explícitamente.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día, tercera ronda: TDZ real en `app.js` del almacén central y existencia inicial al crear artículo:**

- **Bug real corregido**: `let currentArticles`, `let currentStock`, `let currentPointsOfSale` y `let currentMovements` estaban declarados junto a cada sección del archivo, pero `bindMainPage()` (llamado de forma síncrona al cargar la página) dispara `loadArticles()` y las demás cargas antes de que la ejecución del script alcance esas líneas — produce `ReferenceError: can't access lexical declaration before initialization` en navegadores donde el `fetch` se resuelve antes de lo esperado. Corregido moviendo las cuatro declaraciones al inicio del archivo, antes de cualquier código que pueda ejecutarse. Verificado con un navegador real (Chrome headless vía DevTools Protocol): cero excepciones al cargar `index.html`.
- **Pauta implementada — existencia inicial al crear un artículo**: nuevo campo "Existencia inicial en almacén" en el formulario de artículos; si es mayor a 0, después de crear el artículo se registra automáticamente una entrada (`POST /api/v1/stock-movements`, mismo folio secuencial `ENT-NNNNNN`) — no se inventó un mecanismo paralelo para tocar el stock, se reutilizó el único camino que ya existía. Validado en vivo: artículo creado con 15 unidades generó el folio `ENT-000002`, visible en Movimientos y reflejado en Existencias.
- Impedimentos, responsable e impacto: ninguno — ambos cambios son solo frontend de `inventory-service` (mi sistema).
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día, cuarta ronda: cada punto de venta con su propio almacén:**

- Kevin notó que los tres puntos de venta sembrados mostraban el mismo "Almacén asignado" (`ALM-CENTRAL`) — correcto según el diseño anterior (el campo indicaba de qué almacén central recibe mercadería cada tienda), pero no lo que él quería: cada punto de venta debe tener su **propio** almacén, no uno compartido.
- Cambiado `PointOfSaleService.create(code, name)`: ya no recibe ni busca un `warehouseId` existente — crea automáticamente un `Warehouse` nuevo y dedicado (código `ALM-<código del POS>`, nombre `Almacén <nombre de la tienda>`) y lo asigna. El formulario "Nuevo punto de venta" ya no pide elegir almacén.
- Migración `V12__points_of_sale_own_warehouses.sql`: crea un almacén dedicado para cada uno de los 3 puntos de venta sembrados y los reasigna; `ALM-CENTRAL` se conserva como el único origen de artículos/existencias/movimientos (`ENT-`/`SAL-`), pero deja de estar asignado a ningún punto de venta.
- Columna renombrada en la tabla de Puntos de Venta: "Almacén asignado" → "Su almacén".
- Verificado en vivo: los 3 puntos de venta existentes migraron a `ALM-POS-01/02/03`; un punto de venta nuevo creado después (`POS-04`) generó automáticamente `ALM-POS-04`.
- Resultado funcional: `./mvnw clean verify` — **BUILD SUCCESS, 24 pruebas, 0 fallos**, incluida la reescritura de `PointOfSaleServiceTest` y la corrección de dos aserciones en `InventoryServiceIntegrationTest` que asumían el almacén compartido.
- Impedimentos, responsable e impacto: ninguno — cambio contenido en `inventory-service` (mi sistema).
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día, quinta ronda: `warehouse_id` real en `stock` y `stock_movements`:**

- Kevin notó que ni la tabla de Existencias ni los movimientos mostraban en qué almacén estaban cargadas las unidades. Causa de fondo: `Stock`/`StockMovement` nunca tuvieron una columna que los ligara a un almacén — "el almacén central" era una convención (el primer almacén sembrado), no una relación real en la base de datos.
- Migración `V13__stock_warehouse_id.sql`: agrega `warehouse_id` (FK real a `warehouses`, `NOT NULL`) a `stock` y `stock_movements`, poblando las filas existentes con el id de `ALM-CENTRAL`.
- `Stock`/`StockMovement` ahora cargan su `warehouseId`; `StockService` resuelve el almacén central por código (`Warehouse.CENTRAL_CODE`) una sola vez y lo usa al inicializar existencia y al registrar cada movimiento — ya no es una suposición implícita.
- `StockResponse`/`StockMovementResponse` exponen `warehouseId/warehouseCode/warehouseName` (existencias) y `sourceWarehouseCode/sourceWarehouseName` (movimientos). Nueva columna "Almacén" en las pestañas Existencias y Movimientos.
- Verificado en vivo (API y navegador real): ambas respuestas y ambas tablas muestran `ALM-CENTRAL` correctamente, sin excepciones.
- Resultado funcional: `./mvnw clean verify` — **BUILD SUCCESS, 24 pruebas, 0 fallos**, incluida la actualización de `StockServiceTest` (constructor con `WarehouseRepository`, verificación de `warehouseId` en el movimiento creado).
- Impedimentos, responsable e impacto: ninguno — cambio contenido en `inventory-service` (mi sistema).
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día, sexta ronda: permiso de Logística sobre folios, "Tienda Online" y la primera integración en vivo entre los dos sistemas (ver ADR-015):**

- **Permiso de rol**: el rol LOGISTICA de `inventory-service` ahora también puede registrar folios de entrada/salida (`POST /api/v1/stock-movements`), antes exclusivo de ALMACEN — despachar hacia un punto de venta es una acción de logística. `SecurityConfig`, UI y pruebas actualizadas.
- **Bug real encontrado y corregido, grave**: cada folio de `SALIDA` (y su envío asociado) se estaba **perdiendo silenciosamente** — la API respondía `201` con un folio "fantasma" que nunca quedaba en la base de datos (0 filas), descubierto al agregar `external_reference` y ver que un reintento con la misma referencia creaba un folio distinto en vez de devolver el existente. Causa: `StockMovementRepository.save(...)`/`ShipmentRepository.save(...)` usan `merge()` (porque el `@Id` se asigna a mano, sin `@Version`), y justo después de la actualización masiva `@Modifying(clearAutomatically = true)` sobre `stock`, ese `merge()` no quedaba programado para el flush de la transacción. Corregido cambiando esos dos `save()` por `saveAndFlush()`. Verificado en vivo con logging SQL completo (antes: `select` sin `insert` subsecuente; después: `insert` real, fila persistida). Esto probablemente explica folios "fantasma" que no se habían notado antes.
- **Idempotencia por referencia externa**: `stock_movements.external_reference` (migración `V14`, `UNIQUE`) — un reintento de red con la misma referencia devuelve el mismo movimiento en vez de descontar la existencia dos veces.
- **Nuevo punto de venta "Tienda Online"** (`POS-ONLINE`, migración `V15`), con su propio almacén (`ALM-POS-ONLINE`) — representa la web del monolito dentro de `inventory-service`.
- **Primera integración en vivo entre los dos sistemas** (ver `ADR-015`, Proposed): nuevo módulo `inventorysync` en el monolito — cuando una orden llega a `CONFIRMED`, registra automáticamente en `inventory-service` la salida de cada línea vendida contra `POS-ONLINE`. Reintentos 2/8/32 s (mismo molde que LOG-03), bandeja de fallidos (`inventory_bridge_dead_letters`) si se agotan o si el nombre del producto no tiene artículo correspondiente, cuenta de servicio propia (no humana). Nunca bloquea la venta.
- **Verificado en vivo, extremo a extremo, dos veces**: (1) compra real en la web → folio `SAL-000012` en `inventory-service`, stock de `POS-ONLINE` correcto; (2) con `inventory-service` apagado a propósito, la orden se confirmó igual en el monolito, los reintentos (2s, 8s, 32s) quedaron en el log, y al volver a levantar `inventory-service` el cuarto intento se recuperó solo (folio `SAL-000013`) sin ninguna intervención manual.
- Resultado funcional: `./mvnw clean verify` en `inventory-service/` (**BUILD SUCCESS, 25 pruebas**) y en `backend/` (**BUILD SUCCESS, 169 pruebas**, incluye `ModularArchitectureTest` actualizado para aprobar el nuevo módulo `inventorysync` y 8 pruebas nuevas del módulo).
- Impedimentos, responsable e impacto: esta integración cruza a propósito el límite de "sin acoplamiento" que el equipo tenía documentado — queda señalado explícitamente en `ADR-015` (Proposed, pendiente de aprobación de al menos tres integrantes) para que el equipo lo revise, aunque la decisión y la implementación completa sean de Kevin.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

### Fecha: 2026-10-05 — Integrante: Kevin Girón

- Trabajo realizado: segundo punto de venta (**terminal de tienda física**) y continuidad del carrito entre canales, a partir de CDU-01 del DDA (ver `ADR-015`, sección de ampliación).
  - Hallazgo de diseño: el carrito ya era único y del servidor (ORD-01, Jairo); lo que hacía que "se perdiera" era la **identidad del cliente**, que vivía solo en el `localStorage` del navegador. Nuevo módulo `customers` (directorio en el servidor, migración `V1008`); la web resuelve ahí su `customerId` y se enlaza al cliente si ya fue registrado en tienda.
  - Bug corregido en la web (mi UI-01): con la clave de carrito guardada apuntando a un carrito cerrado, la web quedaba mostrando ese carrito; ahora pide uno vigente con clave nueva.
  - Nueva terminal `pos-tienda` (puerto 8084): vendedor solo con usuario (credencial del equipo, no de la persona), buscar/registrar cliente, carrito compartido con la web, cobro con Saga. El vendedor queda como `X-Actor` en la auditoría de la orden.
  - Venta en tienda descuenta la existencia de esa tienda en inventory-service (`POST /api/v1/pos-stock/sales`, folio `POS-VTA-NNNNNN`, migración `V16`); `inventorysync` enruta por el origen registrado (`V1009`). Las ventas web siguen yendo a `POS-ONLINE`.
- Resultado funcional: `./mvnw clean verify` — backend **177 pruebas** y inventory-service **28 pruebas**, BUILD SUCCESS. E2E con navegador real: registro web → carrito con camisa → en la terminal, la vendedora "maria" busca al cliente (escribiendo el correo en mayúsculas) y ve **el mismo carrito** ("abierto en WEB · ahora en STORE") → agrega pantalón → cobra (orden PREPARING, factura emitida) → POS-01 baja 1 camisa y 1 pantalón (`POS-VTA-000001/2`), POS-ONLINE sin cambios → la web muestra carrito vacío, no el cerrado. Cliente nuevo registrado directamente en la terminal. Venta web de regresión → `SAL-000015` a POS-ONLINE.
- Impedimentos, responsable e impacto: toca configuración de seguridad del backend (un usuario de servicio nuevo en `application.yml`, módulo de Edson) y agrega un módulo de identidad que Edson (BB-07) debe revisar; no se modificó `SecurityConfig` del monolito ni el módulo `orders` (Jairo). `ModularArchitectureTest` actualizado para aprobar `customers`.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día (2026-10-05), segunda ronda: la tienda física como sistema propio y almacén por punto de venta (ver `ADR-015`, segunda ampliación):**

- Trabajo realizado, a partir de la revisión de Kevin ("el punto de venta 01 está simple"):
  - **Nuevo sistema `store-service`** (`FASE 2/store-service/`, puerto 8084, base propia `store-db`), reemplaza a la terminal `pos-tienda`. Venta y cobro locales: escáner por SKU, ticket `TKT-NNNNNN`, **NIT o CF** (NIT validado; CF = "Consumidor Final" por defecto), efectivo con cambio, tarjeta o transferencia. Inventario propio (solo lo que el central envió por folio), recepciones, traslados y **bitácora de sincronización** (subidas y bajadas). Vende sin conexión con el central; las facturas pendientes suben solas.
  - Identificar al cliente es **opcional**: "Traer carrito web" por usuario, que cierra el carrito web al cobrar.
  - **`inventory-service`**: endpoints `store-sync` con rol nuevo `TIENDA` (cuenta ligada a su propio punto de venta), facturas de tienda `FT-NNNNNN` en Finanzas, traslados `TRA-NNNNNN` tienda → central / tienda → tienda (migración `V17`), y movimiento de tienda tipo `TRASLADO`.
  - **Tienda online por etapas**: la venta web descuenta la existencia propia de `POS-ONLINE` (ya no crea salida del central); la web muestra disponibilidad con ese stock (`GET /api/v1/inventory-sync/online-stock`, público, solo lectura).
  - **Limpieza**: `FASE 2/scripts/reset-demo-data.sh` (los tres sistemas; conserva usuarios, almacenes, puntos de venta, catálogo e inventario web). Ejecutado: folios desde `000001`.
- Resultado funcional: `./mvnw clean verify` — inventory-service **35 pruebas**, store-service **9 pruebas** (Testcontainers; incluye arranque con el central inalcanzable), backend **180 pruebas**, todos BUILD SUCCESS. Compose con 8 contenedores sanos. E2E en vivo después de la limpieza (API y navegador): surtido por folio → recepción automática en la tienda → venta CF con cambio y venta con NIT → `FT-000001/2` en Finanzas → traslados `TRA-000001` (→ `ENT-000003`) y `TRA-000002` (→ `POS-ENT-000004` en POS-02) → venta con el central apagado queda Pendiente y sube sola (`FT-000003`) → POS-ONLINE surtido con 6, web muestra "6 disponibles", compra web de 2 → `POS-VTA-000006`, queda 4, sin salida del central. Sin errores de JavaScript en las tres UIs.
- Impedimentos, responsable e impacto: dos cambios pequeños fuera de mis módulos, para revisión: (1) Edson — una línea en `SecurityConfig` del monolito para que `GET /api/v1/inventory-sync/online-stock` sea público como `GET /api/v1/inventory/stock`; (2) Jorge — la web ya no muestra la disponibilidad de su inventario sino la de `POS-ONLINE` (su inventario sigue intacto y la Saga sigue reservando contra él). No se tocó `orders` (Jairo) ni fiscal/ERP (Andy).
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día (2026-10-05), tercera ronda: recepción por folio y ajustes del central tras la revisión de Kevin:**

- Trabajo realizado:
  - **Tienda — recepción por folio**: pestaña Recepción; se escribe el folio de envío (`SAL-…`/`TRA-…`), con Tab se cargan artículos y cantidades, y la existencia solo sube al confirmar. Lo enviado y no confirmado se ve como "en tránsito" (también en Inventario y con contador en el menú).
  - **Central — "no vi llegar el traslado"**: los traslados sí llegaban (`ENT-000002/3`, existencia y bitácora), pero la UI del central solo cargaba datos al abrir la página. Ahora se actualiza cada 15 s y al cambiar de pestaña, y la entrada por traslado se distingue ("Entrada · traslado de tienda", folio `TRA-…`, tienda de origen y motivo).
  - **Bug real en la tienda**: el formulario de traslado mostraba un error ("Cannot read properties of null") aunque el traslado sí se registraba (uso de `event.currentTarget` después de un `await`). Corregido.
  - **Central — agregar existencia** a un artículo ya registrado: botón "+ Existencia" en Artículos y Existencias (entrada con su folio); Artículos muestra la existencia.
- Resultado funcional: store-service **10 pruebas**, inventory-service **35 pruebas**, BUILD SUCCESS; migración `V2` aplicada sobre los datos existentes (lo ya recibido quedó como recibido). E2E en navegador: "+ Existencia" 25 → `ENT-000044`; `SAL-000003` a POS-01 → en la tienda, folio + Tab muestra 3 unidades en tránsito, existencia 0 hasta confirmar, luego 3; traslado desde la UI de la tienda → `TRA-000003`/`ENT-000045` aparece en Movimientos y Bitácora del central sin recargar.
- Impedimentos, responsable e impacto: mismo patrón de bug detectado en la consola staff de la web (`frontend/app.js`, formulario de movimiento de inventario, ~línea 622): registra el movimiento y luego muestra un error. No es de mi módulo; se reporta para el responsable de esa pantalla.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día (2026-10-05), cuarta ronda: búsqueda manual en caja y facturas reimprimibles:**

- Trabajo realizado:
  - **Caja — búsqueda manual**: el mismo campo del escáner busca por código, nombre o descripción mientras se escribe (lista con flechas, Enter o clic; muestra precio y existencia, deshabilita lo que no tiene existencia). El código exacto + Enter sigue agregando directo (escáner). La descripción del artículo ahora viaja del central a la tienda con cada envío (`StoreReceiptResponse.description`, migración `V3` de `store-service`).
  - **Factura al cobrar**: al finalizar la venta se genera la factura (emisor configurable `STORE_LEGAL_NAME`/`STORE_TAX_ID`/`STORE_ADDRESS`, serie = código de tienda, número = ticket, NIT/nombre del cliente, líneas, subtotal sin IVA, IVA 12 % incluido, forma de pago, efectivo y cambio, estado de envío al central), lista para imprimir. Indica que la certificación FEL ante SAT no está integrada (es del módulo fiscal de Andy).
  - **Ventas y facturas**: filtro por rango de fechas (hora de Guatemala, por defecto hoy) y texto (folio, NIT, cliente, vendedor), resumen de cantidad y totales por forma de pago, y **reimpresión**: cada reimpresión se cuenta y la copia sale marcada "REIMPRESIÓN n — COPIA".
- Resultado funcional: store-service **12 pruebas**, inventory-service **35**, BUILD SUCCESS. En navegador: "talla" (descripción) + Enter agrega; "mc001" + Enter (escáner) agrega; clic en resultado agrega; "zzzz" avisa que no existe; venta con NIT 4567890-1 en efectivo → factura `TKT-000002` (subtotal Q2,035.71, IVA Q244.29, total Q2,280.00, cambio Q220.00, enviada `FT-000002`); filtro por fechas y por "pérez"; reimpresión marcada y contada.
- Impedimentos, responsable e impacto: ninguno nuevo. Artículos que la tienda ya tenía antes de este cambio reciben su descripción con el próximo envío confirmado.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día (2026-10-05), quinta ronda: tienda online conectada (ver `ADR-015`, tercera ampliación):**

- Trabajo realizado:
  - **Módulo nuevo `onlinestore`** (monolito, `V1010`): recepción por folio de lo que el central envía a POS-ONLINE (en tránsito hasta confirmar), ingreso al inventario del monolito en la ubicación nueva `CD-WEB`, catálogo administrable (nombre, descripción, categoría, precio, imagen) y publicación. Página `tienda-online.html` con cuenta propia `tienda-online@megastore.com` (rol `ONLINE_STORE`).
  - **La web** carga el catálogo publicado desde `GET /api/v1/online-catalog` (se retiró `catalog.json`); disponibilidad = en mano − reservado en `CD-WEB`. La Saga reserva y confirma ahí (reserva atómica y temporizador existentes), e `inventorysync` reporta la venta al central por id de artículo.
  - **Central**: ajuste de existencia `AJU-NNNNNN` (motivo obligatorio, solo Almacén, migración `V18`) con botón "− Ajuste" en Existencias; cuenta TIENDA para POS-ONLINE.
  - **Limpieza** ampliada a la tienda online (recepciones, catálogo, inventario de `CD-WEB`); ejecutada al final.
- Resultado funcional: backend **180 pruebas** (incluye `OnlineStoreIntegrationTest`: flujo completo y **dos compradores concurrentes por la última unidad → exactamente una reserva**), inventory-service **37**, BUILD SUCCESS. E2E en navegador: `SAL-000001` (4) a POS-ONLINE → folio + Tab en la tienda online → confirmar → catálogo "sin publicar · falta imagen" → publicar sin imagen rechazado → imagen + precio Q749 → publicado → portada y catálogo del cliente muestran imagen y "4 disponibles" → compra completa (Saga hasta despacho) → 3 en `CD-WEB` y 3 en POS-ONLINE del central. "− Ajuste" sin motivo no se envía; con motivo → `AJU-000002`.
- Impedimentos, responsable e impacto: (1) Jorge — tres métodos aditivos en `inventory` y la ubicación `CD-WEB`; la reserva no cambió. (2) Edson — reglas en `SecurityConfig` (`/api/v1/online-catalog/**` público de lectura, `/api/v1/online-store/**` solo `ONLINE_STORE`) y un usuario nuevo. (3) Jairo — `catalog.json` se retiró; la web usa el catálogo publicado. (4) **Prueba inestable preexistente** `SensitiveDataLeakScanTest` (Edson): su patrón de PAN (13+ dígitos con puntos o guiones intercalados) a veces encuentra valores aleatorios (vista en columnas distintas: `shipments.idempotency_key`, `outbox_event.payload`); 1 de 3 corridas aisladas falló sin cambios de por medio. (5) Bug de la consola staff de inventario (`frontend/app.js`, `event.currentTarget` tras `await`) sigue reportado. (6) DDA §2.2 dejó el catálogo fuera de alcance: agregar `onlinestore`/`CD-WEB` a los diagramas y CDU para que no cuente como código huérfano.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día (2026-10-05), sexta ronda: almacenes en la consola interna y factura al finalizar la compra web:**

- Trabajo realizado:
  - **Consola staff**: el panel de inventario pasa a "vista unificada de almacenes" (almacén central, almacén de cada tienda y almacén de la tienda online con reservas), con filtro por almacén y búsqueda. El ajuste manual de Jorge (INV-04) queda plegado y ya no ofrece `CD-WEB` (la tienda online solo se surte por folio). "Nuevo despacho" elige el almacén de origen (central, tienda online, tiendas o recogida). Corregido el bug de `event.currentTarget` tras `await` de ese formulario.
  - **Logística (LOG-01)**: el despacho guarda el almacén concreto de origen (`origin_code`, `V1011`); la Saga lo llena con la ubicación de la reserva.
  - **Factura web**: endpoint de cliente `GET /api/v1/invoices/orders/{orderId}` (módulo fiscal, aditivo) y factura imprimible con descarga del XML al terminar la compra y en "Mis pedidos". "Mis pedidos" antes intentaba leer `/api/v1/fiscal-documents` (solo Finanzas) y fallaba en silencio.
- Resultado funcional: backend **183 pruebas**, BUILD SUCCESS (incluye las de autorización de Edson sin cambios: el cliente sigue sin acceso a `fiscal-documents`). E2E en navegador: compra con NIT 4567890-1 → factura abierta sola (serie SGO-2026, CUI, autorización SAT, subtotal Q668.75 + IVA Q80.25 = Q749.00) → XML descargado → "Ver factura" desde Mis pedidos; consola staff con 6 almacenes (central, online, 4 tiendas), despacho de la orden "Almacén Tienda Online (CD-WEB) → Zona 10" y uno nuevo "Almacén Central (ALM-CENTRAL) → Mixco".
- Impedimentos, responsable e impacto: Andy — controlador nuevo `CustomerInvoiceController` en `fiscal` (solo lectura, no cambia emisión). Edson — dos reglas nuevas en `SecurityConfig` (`/api/v1/invoices/orders/*` CLIENT/FINANCE; `/api/v1/inventory-sync/warehouses` interno). Jorge — su formulario de ajuste manual sigue igual, plegado y sin `CD-WEB`.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día (2026-10-05), séptima ronda: vista general en el central y tienda online completa:**

- Trabajo realizado:
  - **Almacén central**: pestaña "Vista general" (`GET /api/v1/stock-overview`): existencia por artículo en el central y en cada punto de venta, con total de la red. Se retiró la vista unificada que se había puesto en la consola de la web (pedido de Kevin: la vista general va en el servidor).
  - **Tienda online** (`tienda-online.html`): Inventario (en mano / reservado / disponible / en tránsito), Ventas y facturas (por fecha, con la factura certificada imprimible y su XML), Traslados por folio `TOL-NNNNNN` (central u otra tienda; rechazo devuelve a `CD-WEB`; pendientes se reintentan) y Bitácora (recepciones, ventas reportadas al central, traslados). Migración `V1012`.
- Resultado funcional: backend **182 pruebas**, inventory-service **37**, BUILD SUCCESS. E2E desde cero en navegador: `SAL-000001` (6) recibido + `SAL-000002` (2) en tránsito → publicado con imagen → compra de 2 con factura → Ventas muestra la orden y su factura → traslado `TOL-000001` → `TRA-000001`/`ENT-000002` en el central → inventario online 3 en mano + 2 en tránsito → bitácora con recepción, venta (`POS-VTA-000001`) y traslado → vista general del central: central 13, POS-ONLINE 5, total red 18.
- Impedimentos, responsable e impacto: Jorge — un método aditivo más (`issueFromLocation`, salida desde una ubicación por código; solo disponible, no reservado).
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día (2026-10-05), octava ronda: correcciones generales de la tienda online:**

- Trabajo realizado:
  - **Checkout**: "Recoger en tienda" lista las ubicaciones físicas activas (almacén central y tienda física 1) con dirección y horario; "Envío a domicilio" pide la dirección. **Ventana PayKlik simulada**: tarjeta tokenizada con botón "Cobrado", o transferencia con cuenta y referencia inventadas; cancelar deja el carrito intacto.
  - **Roles**: `tienda-online@megastore.com` administra la web completa (ONLINE_STORE + SUPPORT + LOGISTICS + FINANCE) y navega entre su página y el centro de operaciones; la auditoría de la consola usa los roles reales del token.
  - **Consola — "Inventario — entradas y salidas"** (estaba roto: ofrecía productos de la tienda online en ubicaciones donde no existen) → existencias y kárdex de la tienda online, incluidas las reservas y su liberación por temporizador. **"Buscar cliente (omnicanal)"** (pedía un UUID) → call center por correo con carrito compartido en canal CALL_CENTER, historial de compras y atenciones. La web relee el carrito al abrirlo (cambios desde otro canal).
  - **Notificaciones**: un cliente recién registrado no recibía nada (solo el cliente demo tenía contacto); ahora su correo de registro es su contacto.
  - **Devoluciones** (cliente): se elige la compra y el producto de listas, sin teclear UUID ni SKU.
  - Corregidos: filas de auditoría de la consola (`undefined`, estados vacíos) y la miniatura del carrito (`undefined`).
- Resultado funcional: backend **184 pruebas**, BUILD SUCCESS. E2E en navegador con cliente registrado desde cero: compra con recogida en Tienda Centro (POS-01) pagada por transferencia y compra a domicilio con tarjeta (cancelada una vez) → facturas → notificaciones `ORDER_CONFIRMED` y `SHIPMENT_PREPARING` enviadas a su correo → devolución registrada; consola como tienda online: kárdex con entradas y ventas, call center agrega al carrito del cliente (último canal CALL_CENTER) y la web lo muestra sin recargar; auditoría con actor y estados.
- Impedimentos, responsable e impacto: Jorge — `InventoryLocationActivityService` y dos consultas de repositorio (solo lectura). Edson — regla de lectura de inventario/kárdex para roles internos y roles ampliados de la cuenta de tienda online. Jairo — sin cambios en orders; el call center usa los endpoints de carrito existentes.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día (2026-10-05): registro web bloqueado tras limpiar datos:**

- Causa: el registro validaba duplicados contra las cuentas guardadas en el `localStorage` del navegador (modelo demo de autenticación). Al limpiar el servidor, el navegador seguía "recordando" esos correos y respondía "Ya existe una cuenta con ese correo" aunque el cliente ya no existiera.
- Corrección (`frontend/session.js`): el registro consulta primero el directorio de clientes del servidor; si el servidor no tiene el correo, se reemplaza la copia vieja del navegador y se registra; si lo tiene, se pide iniciar sesión. Verificado en navegador: correo viejo del navegador → registra; correo existente en el servidor → aviso; correo nuevo → registra; login con la contraseña nueva → entra.
- Limitación que sigue (BB-07, Edson): la contraseña del cliente vive en el navegador; en otro dispositivo no puede iniciar sesión con contraseña.

### Fecha: 2026-10-06 — Integrante: Kevin Girón

- Trabajo realizado: **correo real de notificaciones**. El proveedor era solo un simulador (BB-06): marcaba los avisos como enviados pero no salía ningún correo. Nuevo envío SMTP (`SmtpEmailSender`, Gmail con contraseña de aplicación por `SGO_MAIL_USERNAME`/`SGO_MAIL_PASSWORD` en `.env`, que no se versiona) con correo HTML por evento (confirmación, preparación, en tránsito, entregado, incidencia) y enlace a "Mis pedidos". Selección automática por canal: correo real si hay credenciales; si no, simulador; SMS/push simulados. Además: enlaces "Crear cuenta" en la portada y en el login (antes solo aparecía al intentar comprar sin sesión).
- Resultado funcional: backend **189 pruebas**, BUILD SUCCESS; incluye envío SMTP real contra un servidor de correo de prueba en contenedor (Mailpit): llega con asunto, destinatario y enlace; servidor caído → proveedor no disponible (reintento).
- Impedimentos, responsable e impacto: falta que Kevin genere la contraseña de aplicación de Gmail y la ponga en `.env`; mientras tanto el correo sigue simulado.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día (2026-10-06): ERP AS/400, POS de las 32 tiendas e informe de ventas (ver `ADR-015`, cuarta ampliación):**

- Trabajo realizado: revisión del módulo contra el enunciado y la hoja de calificación. Brechas encontradas: ERP y POS solo funcionaban con botones manuales en la consola, las ventas de tienda física no llegaban al ERP, el lote no se escribía como archivo y no había informe de ventas. Nuevo módulo `legacyfeed`: la venta confirmada entra sola al ERP y cada cambio de la orden se avisa a las 32 tiendas; las facturas de tienda llegan desde el central cada 60 s. El lote se escribe como archivo CSV/XML con registro de control en la carpeta del AS/400 (volumen `erp-outbox`, cada 30 min), los lotes huérfanos se retoman y un `DEAD_LETTER` se puede reenviar. La tienda 01 recibe los eventos de verdad (`store-service`, pestaña "Pedidos web"). Nuevo módulo `salesreport`: informe conciliado por rango (pago, factura SAT, ERP) con diferencias, exportación CSV y auditoría de cada consulta. En la consola, paneles de lotes/eventos e informe.
- Resultado funcional: backend **200**, inventory-service **37** y store-service **13** pruebas, en verde. E2E en vivo con datos limpios, incluidas la caída del ERP (`DEAD_LETTER` → reintento manual → entregado) y la caída de las tiendas 7 y 15 (30 ACK / 2 `DEAD_LETTER`); el informe marca la diferencia mientras el lote no se entrega.
- Impedimentos, responsable e impacto: cambios aditivos en `integration` (Andy) y una regla nueva `/api/v1/reports/**` → FINANCE en `SecurityConfig` (Edson); a revisar con ellos. Los diagramas (despliegue: volumen `erp-outbox`, POS-01 real; modelo de datos: columnas nuevas de `erp_sales`, `pos_events`) deben actualizarse para no quedar como código huérfano.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

**Actualización — mismo día (2026-10-06): PayKlik y LogiExpress reales, eventos de stock a los POS y entrega en tienda (ver `ADR-015`, quinta ampliación):**

- Trabajo realizado:
  - PayKlik y LogiExpress pasan de simuladores dentro del backend a sistemas propios, cada uno con su base y su portal (`payklik-service` :8086, `logiexpress-service` :8085). SGO-Logic los consume por REST con llave de comercio.
  - El rastreo de LogiExpress mueve el despacho y la orden (módulo nuevo `fulfillment`) y avisa al cliente y a las tiendas. El retiro en tienda termina con "Entregar al cliente" en el POS.
  - La Saga retoma la compra cuando la conciliación de PayKlik resuelve un pago indeterminado.
  - El central envía a cada punto de venta sus eventos de stock con reintentos, y cada uno los anota en su bitácora comparados con su existencia.
- Resultado funcional:
  - Pruebas en verde: backend 223, central 40, tienda 15, PayKlik 4, LogiExpress 4.
  - E2E en vivo con datos limpios, incluidos PayKlik lento (sin doble cobro), LogiExpress lento o caído (rastreo y "Reintentar guía") y tienda caída para eventos de stock ("Reenviar").
- Impedimentos, responsable e impacto:
  - Cambios aditivos en `payments` (Edson: adaptador REST; ver ADR-015 §36) y en `OrderSagaService` (Jairo: retomar tras conciliar).
  - Dos reglas nuevas en `SecurityConfig`.
  - Diagramas de despliegue, componentes y modelo de datos pendientes de reflejar los dos sistemas nuevos.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

### Fecha: 2026-10-07 — Integrante: Kevin Girón

- Trabajo realizado:
  - Revisión contra la hoja de calificación.
  - Arreglo del resumen consolidado de la orden (`query`): consumidor de eventos de pago y creación de la proyección sin carrera (`ON CONFLICT` + bloqueo de fila).
  - Temporizador de reservas configurable por entorno.
  - Lista de lo que le toca a cada integrante y de los cambios de diagramas (documento personal y ADR-015 §43).
- Resultado funcional: backend **227 pruebas** en verde, incluida la concurrencia real del resumen (orden, pago y despacho a la vez, 10 rondas).
- Impedimentos, responsable e impacto: los diagramas (DOC-01, Jairo) no reflejan lo construido desde el 23-09. Riesgo de "código huérfano" en la calificación.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

### Fecha: 2026-10-13 — Integrante: Kevin Girón

- Trabajo realizado:
  - Revisión del cierre contra `planificacion.md` después del merge de Jairo (diagramas, DDA, Kanban, planning).
  - Para no dejar huecos antes de Fase 3, completé las tareas documentales y técnicas que estaban sin avance:
    - Jorge (G2, G3): sección "Actualización Fase 2" en ADR-011 y ADR-014.
    - Edson (E3, E4, E5): ADR-013 con los roles STORE, POS_EVENTS y ONLINE_STORE; matriz de trazabilidad actualizada; `SensitiveDataLeakScanTest` ya no falla al azar (descarta UUID y valida con Luhn).
    - Andy (A2, A3): FIS-03 con ERP automático, archivo real y POS real; README de `FASE 2` con los 12 contenedores, puertos, cuentas, variables y la limpieza de datos.
  - Mi parte: retrospectiva actualizada, figuras de PayKlik y LogiExpress en el DDA (ya no "simulador") y registro de aprobaciones de ADR-015.
- Resultado funcional: ver §7 (ejecución del 2026-10-13).
- Impedimentos, responsable e impacto:
  - Las retrospectivas y las entradas diarias de Jorge, Edson y Andy son personales y no se redactan en su nombre: siguen pendientes.
  - ADR-015 sigue en `Proposed` hasta tener tres aprobaciones.
  - El tag `v2.0.0` apunta a un `main` anterior al PR #6 (release pendiente de Jairo).
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna.

### Fecha: 2026-09-22 — Integrante: Jorge Pumay

> Entrada reconstruida a partir del historial de Git (5 commits `201213421:` de ese día) y de los ADR. En su fecha no se registraron horas ni conteos de pruebas propios; no se inventan ahora.

- Trabajo realizado: módulo `inventory`. BB-03 (esquema relacional inicial: `inventory_locations` e `inventory_items` con `UNIQUE (product_id, location_id)`, migración `V1`), INV-01 (ubicaciones sembradas, 2 centros de distribución y 32 tiendas, y consulta de stock), INV-02 (reserva atómica e idempotente mediante un `UPDATE` condicional) e INV-04 (algoritmo de selección de origen). Se redactó ADR-011 (fuente lógica autoritativa de inventario).
- Resultado funcional: la reserva compromete unidades en una sola sentencia atómica y una repetición con la misma llave idempotente no vuelve a reservar; sin `locationId` el origen se elige por prioridad centro de distribución y luego tienda, por código.
- Hoy y trazabilidad: CUN-02/CDU-02, EaC-03, ADR-011 (`Proposed`) y ADR-014.
- Impedimentos, responsable e impacto: ninguno registrado.
- Horas reales: no registradas en esa fecha.
- Persona que quedaría bloqueada y acción temporal: ninguna.

### Fecha: 2026-09-23 — Integrante: Jorge Pumay

> Entrada reconstruida a partir del historial de Git (3 commits `201213421:` de ese día).

- Trabajo realizado: INV-03 (liberación automática de reservas vencidas con temporizador configurable), `locationType` y `locationCode` en la respuesta de la reserva, e INV-05 (prueba de concurrencia con Testcontainers: dos solicitudes simultáneas por la última unidad).
- Resultado funcional: las reservas `ACTIVE` vencidas pasan a `EXPIRED` y devuelven lo reservado; solo una de dos solicitudes concurrentes por la última unidad obtiene la reserva.
- Hoy y trazabilidad: CDU-02, EaC-03 (sin doble compromiso) y ADR-011/ADR-014.
- Impedimentos, responsable e impacto: ninguno registrado.
- Horas reales: no registradas en esa fecha.
- Persona que quedaría bloqueada y acción temporal: ninguna.

### Fecha: 2026-10-13 — Integrante: Jorge Pumay

- Trabajo realizado (Fase 3, crédito propio; JOR3-02 y JOR3-03):
  - Contrato `credit-history-v1`: el ERP entrega compras, pagos (puntuales o no), devoluciones e incidentes. Ingesta idempotente por `sourceReference` único (`V4007`, `POST /api/v1/credit/history`, rol FINANCE). El perfil crediticio se reconstruye desde los hechos de la ventana de 12 meses en lugar de acumularse, y el límite pasó a ser el 40 % de las compras de la ventana con tope de 5000, menos el crédito vigente.
  - Dataset de demostración con cinco clientes ficticios (uno aprobado y cuatro rechazados por poca historia, devoluciones, incidentes y pagos tardíos) en un solo CSV que consumen el script de carga y una prueba.
  - Cada decisión guarda las entradas y umbrales con los que se tomó (`V4008`, `inputs_snapshot`); pruebas de límite exacto (UT-03) y de vencimiento a 30 días con reloj fijo (UT-05).
- Resultado funcional o numérico: 41 pruebas de crédito y de arquitectura en verde; 7 recorridos E2E y 3 pruebas de la Saga sin regresión. Dos errores propios corregidos al probar: una columna de 16 caracteres para un nombre de contrato de 17, y un caso de prueba cuya historia superaba el 30 % de devoluciones y la regla lo rechazaba.
- Hoy y trazabilidad: ADR-016, `credit-v1.md`, `credit-history-v1.md`; reglas versionadas `credit-rules-v1`.
- Impedimentos, responsable e impacto: el JDK local era 21 y el proyecto exige 25; se resolvió con un JDK 25 portable sin tocar la instalación del sistema. La alimentación automática desde las ventas del ERP no se hizo porque cruza el módulo `integration` (Andy) y el flujo de órdenes (Jairo); queda como mejora acordada con ellos.
- Horas reales: no registradas todavía; las registrará Jorge.
- Persona que quedaría bloqueada y acción temporal: ninguna; el historial entra por el contrato mientras no exista la alimentación automática.

### Fecha: 2026-10-14 — Integrante: Jorge Pumay

- Trabajo realizado:
  - JOR3-04 y JOR3-06: ciclo de vida de la obligación (`GRANTED` → `OVERDUE` / `SETTLED` / `CANCELLED`, `V4009`), pago idempotente y seguro ante concurrencia (`POST /api/v1/credit/operations/{id}/settle`), vencimiento automático que bloquea nuevas concesiones y anulación por evento `ORDER_STATUS_CHANGED` sin modificar `orders` ni la Saga. Se actualizó `credit-v1.md`, se creó `credit-history-v1.md` y se agregó la §8 de crédito al modelo de datos.
  - Matriz 401/403 de los endpoints de crédito (`CreditSecurityTest`, 7 casos).
  - Fase 2: G1 (revisión de los cambios en `inventory`, ver `REV-JORGE-2026-10-14-inventory.md`), G2 (ADR-011 y ADR-014 ya traían su actualización de Fase 2), G3 (CDU-07 enlaza la regla de origen y el componente) y G4 (temporizador de reservas probado en vivo).
  - El CI de `develop` salió en rojo tras mi merge. Causa: una carrera en Logística entre el evento de tracking y la actualización de la guía de LogiExpress (bloqueo optimista, respuesta 500). Se agregó un reintento acotado de 3 intentos en `ShipmentController` con prueba.
  - `.gitattributes` para forzar LF en `mvnw`, scripts y Dockerfile: con `core.autocrlf=true` en Windows, el build de Docker fallaba con `./mvnw: not found`.
- Resultado funcional o numérico: suite completa antes del arreglo, 296 pruebas con 1 fallo (`SensitiveDataLeakScanTest`, 3 de 3 corridas en local); con el arreglo, 299 pruebas y 0 fallos, y la prueba pasó 4 de 4. Sobre `develop` integrado, 306 pruebas, 0 fallos. CI de GitHub (#30, `23b9b79`): verde en 10m9s. En el sistema real de 12 contenedores: carga del historial de demostración con 29 hechos aceptados y 5 perfiles reconstruidos, y 29 duplicados sin cambios en la segunda carga; reserva `ACTIVE` a las 06:25:48Z que pasó a `EXPIRED` a las 06:27:55Z con lo reservado de 2 a 0.
- Hoy y trazabilidad: ADR-011, ADR-014, ADR-016, CDU-02, CDU-07, EaC-03; `INV-03-temporizador-reserva-evidencia.md`.
- Impedimentos, responsable e impacto: (1) los ADR-011 y ADR-014 siguen en `Proposed` hasta tener tres aprobaciones del equipo. (2) El arreglo de Logística está en un módulo de Kevin; se le avisa. (3) Un primer diagnóstico mío («la prueba ya fallaba antes de mi merge») era incompleto: en local fallaba en `develop` limpio pero el CI de ese commit había salido verde, así que era una carrera intermitente y no una falla fija; se corrigió ante el equipo.
- Horas reales: no registradas todavía; las registrará Jorge.
- Persona que quedaría bloqueada y acción temporal: ninguna.

## 5. Bloqueos

| Fecha | Tarea | Causa reproducible | Impacto | Puede desbloquear | Próxima revisión | Estado |
|---|---|---|---|---|---|---|
| 2026-09-14 | BB-02 | ADR-009 todavía no cuenta con tres aprobaciones registradas | BB-02 no puede cerrarse formalmente | Jairo coordina; al menos tres integrantes aprueban | Pendiente de acordar | Abierto |
| 2026-09-14 | ORD-01 a ORD-05 | Faltaban contratos aprobados y entregas de Inventario, Pagos, Fiscal y Logística | Impedía iniciar una implementación integrada definitiva | Responsables de cada módulo y revisión coordinada por Jairo | Resuelto el 2026-09-23 con merge `1294231` | Resuelto |

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
| 2026-09-23 | ORD-05 / QA-04 — cinco recorridos E2E | JWT + MockMvc; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers; fixtures deterministas | E2E: 5 pruebas; suite completa: `BUILD SUCCESS`, 139 pruebas, 0 fallos, 0 errores, 0 omitidas | `OrderJourneysE2ETest` y `docs/evidence/functional/ORD-05-QA-04-cinco-flujos-e2e.md` | Saga completa, compensación, despacho, entrega y resolución administrativa de reembolso verificados por HTTP |
| 2026-09-23 | Merge `feature/pruebas-e2e` → `develop` | Se conservaron Saga/E2E y correcciones A1/A4–A8; conflicto de bitácora resuelto; mensaje de validación sensible neutralizado y UUID excluido del detector de PAN | `BUILD SUCCESS`; 148 pruebas, 0 fallos, 0 errores, 0 omitidas | Surefire, `PaymentRequestTest`, `SensitiveDataLeakScanTest` y `OrderJourneysE2ETest` | `develop` reúne ambos trabajos sin fuga en logs ni falsos positivos del escaneo SEC-01 |
| 2026-10-10 | J1 — reanudación de Saga después de conciliar pago | `./mvnw -Dtest=OrderSagaReconciledPaymentTest test`; Java 25 | `BUILD SUCCESS`; 3 pruebas, 0 fallos, 0 errores, 0 omitidas | Surefire local y `OrderSagaReconciledPaymentTest` | Aprobado, rechazado e indeterminado continúan o compensan sin repetir el cobro |
| 2026-10-13 | Cierre de E5 y documentación | `./mvnw clean verify` del backend con Java 25 y Testcontainers (PostgreSQL, RabbitMQ, Mailpit) | `BUILD SUCCESS`: 228 pruebas, 0 fallos, 0 errores, 0 omitidas; `SensitiveDataLeakScanTest` 2 de 2 con el detector nuevo (descarta UUID, valida Luhn) | Surefire local | E5 cerrado; la prueba de fuga ya no depende del azar |
| 2026-10-14 | `./mvnw test` completo sobre `develop` con las ramas de crédito y Logística integradas | Java 25.0.4.1; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers | `BUILD SUCCESS`; 306 pruebas, 0 fallos, 0 errores, 0 omitidas | `FASE 2/backend/target/surefire-reports` | Verificación previa al push; el CI de GitHub (#30, `23b9b79`) salió verde en 10m9s |
| 2026-10-14 | INV-03 en vivo: temporizador de reservas | 12 contenedores con `docker compose`; `SGO_INVENTORY_RESERVATION_TTL=PT2M` y barrido `PT10S` | Reserva `ACTIVE` a las 06:25:48Z → `EXPIRED` a las 06:27:55Z; reservado en `CD-01` de 2 a 0 | `FASE 2/docs/evidence/functional/INV-03-temporizador-reserva-evidencia.md` | Criterio «liberar inventario» verificado con el sistema real |
| 2026-10-14 | Carga del historial de crédito de demostración contra el backend real | `scripts/load-credit-demo-history.sh`; 5 clientes ficticios; JWT de Finanzas | 1.ª carga: 29 aceptados y 5 perfiles reconstruidos; 2.ª: 29 duplicados y 0 cambios | Salida del script y consulta a `credit_customer_profiles` | Idempotencia y límites verificados en el sistema real |

## 8. Retrospectiva individual

### Integrante: Jairo Gómez

1. **Decisiones y patrones que funcionaron:** el monolito modular con puertos y
   adaptadores permitió que `orders` coordinara reserva, pago, fiscal y despacho
   sin escribir datos de módulos ajenos. La reanudación de la Saga después de
   conciliar un pago conserva la llave idempotente y evita el doble cobro;
   `OrderSagaReconciledPaymentTest` verificó aprobado, rechazado e indeterminado
   con 3 pruebas aprobadas el 2026-10-10.
2. **Supuesto que tuvo que corregirse:** el CDU-01 de Fase 1 vinculaba agregar o
   quitar productos con una reserva ya creada. El código reserva al iniciar
   checkout. Se documentó esa política porque un carrito puede permanecer
   abierto o abandonarse y no debe bloquear inventario durante ese tiempo; la
   reserva del checkout sí expira automáticamente si la compra no concluye.
3. **Mejora de mantenibilidad:** conservar dos niveles de arquitectura —objetivo
   de Fase 1 y realización de Fase 2— y enlazarlos desde el DDA. Las vistas de
   `nuevos-fase2` ahora identifican módulos, adaptadores, doce contenedores,
   cinco bases físicas y CDU implementados, con fuentes editables y
   exportaciones regenerables.

### Integrante: Kevin Girón (carnet 202010844)

1. Decisiones y patrones consolidados con éxito: reutilizar el patrón Outbox/Inbox (ADR-002/BB-04) en `logistics` e `incidents` en vez de inventar un mecanismo propio de notificación de eventos evitó acoplar esos módulos a Notifications/Query — ambos consumidores nuevos (`ShipmentStatusNotificationConsumer`, `IncidentNotificationConsumer`, `ShipmentStatusChangedConsumer`, `IncidentRegisteredConsumer`) se integraron sin que `logistics` o `incidents` conocieran quién los consume. Igual de útil fue el patrón *after-commit scheduling* (`TransactionSynchronizationManager.registerSynchronization().afterCommit()`), que ya existía en `NotificationService` y que tuve que reaplicar en `ShipmentService` al envolverlo en `@Transactional`: confirma que es un patrón de equipo, no una solución puntual.
2. Supuestos o diagramas de Fase 1 que fallaron o exigieron refactorización: el diseño original de `incidents` no contemplaba un `customerId` en la incidencia — el CDU de Fase 1 resolvía "historial del cliente" únicamente a través de `orderId`, lo cual no permite ver el historial de un cliente a través de todas sus órdenes. Se agregó `customer_id` opcional (V1006) sin que `incidents` necesite leer tablas de `orders`. Además, envolver `createDispatch` en `@Transactional` para poder publicar al Outbox expuso que el diagrama de secuencia de despacho de Fase 1 no consideraba el reintento asíncrono de guía como una operación que debe esperar al commit — un supuesto implícito que solo se hizo visible al fallar `AsyncIntegrationPersistenceTest`.
3. Mejoras técnicas concretas para mantenibilidad futura (actualizado 2026-10-13):
   - **Lo que señalé el 24-09 ya se resolvió.** Los diagramas de Fase 2 existen en `FASE 2/docs/diagrams/nuevos-fase2/` y están enlazados desde el DDA. El informe de ventas existe (`salesreport`, CDU-13).
   - **Lección principal de la segunda parte.** Separar cada responsabilidad en su propio sistema con su base fue lo que hizo verificables las integraciones:
     - El almacén central consolida el inventario.
     - Cada punto de venta tiene su almacén.
     - PayKlik y LogiExpress son proveedores con su propia API.

     Con eso se pudieron provocar fallas reales del proveedor (lento, caído) y comprobar reintentos y conciliación, en vez de suponerlos.
   - **Errores que no estaban en el diseño de Fase 1 y que conviene prevenir desde el diseño de Fase 3:**
     - Un pago conciliado después del timeout no retomaba la Saga.
     - Un reintento tardío de guía pisaba la guía ya asociada por rastreo.
     - Dos consumidores creando a la vez la misma proyección perdían datos.

     Los tres tienen ahora su prueba de regresión (`OrderSagaReconciledPaymentTest`, `lateTimeoutNeverOverridesAGuideFoundByTracking`, `OrderSummaryConcurrencyIntegrationTest`). En Fase 3, cada flujo asíncrono nuevo debería diseñarse con su caso de "respuesta tardía" y de "llegada concurrente" desde el CDU.
   - **Mantenibilidad.** Los modelos físicos se generaron desde las migraciones Flyway para que el diagrama no se desactualice respecto al código; vale la pena conservar esa práctica.

### Integrante: Jorge Pumay (carnet 201213421)

1. **Decisiones y patrones que funcionaron.**
   - *Reserva atómica por sentencia condicional* (Fase 2): reservar y descontar en un solo `UPDATE ... WHERE disponible >= cantidad` evitó la ventana de carrera entre «elegir» y «comprometer», y la prueba INV-05 lo demostró con dos solicitudes concurrentes por la última unidad. Una restricción `UNIQUE (product_id, location_id)` respalda el esquema.
   - *Perfil crediticio reconstruido desde los hechos* (Fase 3): la ingesta del ERP es idempotente por `sourceReference` y el perfil se recalcula desde todos los hechos de la ventana. Por eso el orden de llegada no altera el resultado y reprocesar un lote no cambia totales. Lo comprobé en el sistema real: la segunda carga dio 29 duplicados y 0 cambios.
   - *Compensación por evento, sin acoplar módulos*: al cancelarse una orden, `credit` anula la obligación y devuelve el saldo escuchando `ORDER_STATUS_CHANGED`. No toqué `orders` ni la Saga; `credit` solo tuvo que declarar su dependencia de `messaging`, igual que `query` y `notifications`.
   - *Decisión auditable*: guardar con cada decisión las entradas y los umbrales evaluados permite reconstruir por qué se aprobó o rechazó aunque el perfil cambie después.
   - *Bloqueo por cliente*: serializar concesiones, pagos y reconstrucción del perfil con el mismo bloqueo de base de datos evitó doble consumo y doble liberación del límite, con prueba de concurrencia.
2. **Supuestos o diagramas que fallaron o exigieron corrección.**
   - *Fase 1 → Fase 2*: ADR-011 declaró al módulo `inventory` como única fuente de inventario. Al aparecer el almacén central y la tienda online (ADR-015, de Kevin), hubo dos fuentes con responsabilidades distintas; el ADR tuvo que actualizarse y sigue `Proposed`. Además `CD-WEB` quedó tipificado como centro de distribución, de modo que el algoritmo genérico lo considera un origen más; hoy no causa problemas solo porque los productos de la web únicamente tienen existencia ahí.
   - *Errores propios durante Fase 3*, corregidos antes de integrar: una columna demasiado corta para el nombre del contrato y un código de error documentado distinto del real (los atraparon las pruebas); y una lectura en caché dentro de la transacción que habría permitido liberar el saldo dos veces ante dos pagos simultáneos, que detecté al revisar mi propio diseño y corregí releyendo desde la base bajo el bloqueo.
   - *Diagnóstico incompleto*: tras mi merge, el CI salió en rojo y afirmé que la prueba ya fallaba antes de mi cambio. En local era cierto, pero el CI del mismo commit había salido verde. Era una carrera intermitente de Logística, no una falla fija. Lo corregí ante el equipo y se arregló la causa.
3. **Mejoras técnicas para la mantenibilidad futura.**
   - Alimentar el historial crediticio automáticamente desde las ventas del ERP mediante eventos del módulo `integration`, en lugar de depender del endpoint.
   - Hacer atómica la primera recepción en una ubicación (`INSERT ... ON CONFLICT DO NOTHING`) y que la Saga pase un `locationId` explícito en las compras web.
   - Correr la suite completa y el build de las imágenes Docker antes de cada merge a `develop`, y mantener el `.gitattributes` que ya fuerza LF en `mvnw`, scripts y Dockerfile.
   - Aprobar los ADR dentro del propio sprint: ADR-011 y ADR-014 siguen `Proposed` por falta de tres aprobaciones.
   - Abonos parciales, intereses y recargos por mora del crédito quedan fuera del alcance actual y requieren un contrato de cartera adicional.

## 9. Burndown o resumen consolidado

El corte final se obtuvo del tablero público de Trello el 2026-10-10, no de
inferencias sobre ramas o commits. La captura inicial mostraba 54 tarjetas en
`To Do`; la captura final muestra las mismas 54 en `Done` y cero en las otras
columnas.

| Estado | Inicial | Final | Evidencia |
|---|---:|---:|---|
| `To Do` | 54 | 0 | Capturas inicial y final |
| `Blocked` | 0 | 0 | Captura final |
| `In Progress` | 0 | 0 | Captura final |
| `Ready for Testing` | 0 | 0 | Captura final |
| `Test/QA` | 0 | 0 | Captura final |
| `Done` | 0 | 54 | Captura final |

Este conteo demuestra movimiento del tablero, no certifica por sí solo que
cada criterio técnico esté satisfecho. La evidencia de código, pruebas, DDA y
diagramas se mantiene en los archivos enlazados por cada tarjeta.

## 10. Captura final del Kanban

- Estado: captura final incorporada.
- Tablero: [MegaStore - Fase 2](https://trello.com/b/uyoJQ4RH/megastore-fase-2).
- Ruta: `FASE 2/docs/evidence/bitacora/2026-10-10-kanban-final.png`.
- Fecha y responsable: 2026-10-10, Jairo Gómez.
- Conteo visible: `To Do` 0, `Blocked` 0, `In Progress` 0,
  `Ready for Testing` 0, `Test/QA` 0 y `Done` 54.

![Captura final del Kanban](<FASE 2/docs/evidence/bitacora/2026-10-10-kanban-final.png>)

## 11. Estado de cierre de Jairo Gómez

**Corte verificado:** 2026-10-12.

Esta tabla reconcilia el estado histórico de las entradas diarias con los
criterios J1–J6 de `planificacion.md`. Un estado histórico como `Blocked` o
`In Progress` describe el tablero de aquella fecha y no contradice el cierre
posterior.

| Tarea | Estado de cierre | Evidencia o condición restante |
|---|---|---|
| J1 — Saga | Cerrada | `OrderSagaReconciledPaymentTest`: 3 pruebas, 0 fallos y 0 errores. |
| J2 — Política de reserva | Cerrada | DDA §§3.5–3.6: consulta en carrito y reserva al iniciar checkout. |
| J3 — Diagramas | Cerrada | CDU, bloques, componentes, despliegue y modelos físicos están integrados; la vista principal del almacén central incluye sus 14 tablas Flyway. |
| J4 — Release | Excluida de esta ejecución | No se hicieron ramas, commits, merges ni tags por instrucción expresa. Formalmente REL-01 solo puede cerrarse si se autoriza ese flujo Git. |
| J5 — Scrum | Cerrada en el alcance de Jairo | Retrospectiva propia, burndown 54 → 0 y capturas inicial y final incorporadas. Los aportes personales de otros integrantes no se fabrican ni se atribuyen a Jairo. |
| J6 — Sprint Planning | Cerrada | Backlog y captura inicial enlazados; ausencia de feedback docente documentada sin inventar observaciones. |

Por tanto, la parte de Jairo puede declararse cerrada **con la exclusión
documentada de J4**. El cumplimiento literal de REL-01 continúa fuera del
alcance mientras se mantenga la prohibición de crear ramas, commits, merges o
tags.
