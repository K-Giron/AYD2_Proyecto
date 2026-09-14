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

## 7. Resultados de pruebas

| Fecha | Comando/escenario | Configuración y datos | Resultado | Evidencia | Conclusión |
|---|---|---|---|---|---|
| 2026-09-14 | `./mvnw clean verify` | Java 25.0.4.1; PostgreSQL 17.11 y RabbitMQ 4.3.5 mediante Testcontainers | `BUILD SUCCESS`; 6 pruebas, 0 fallos, 0 errores, 0 omitidas | Reportes Surefire locales en `FASE 2/backend/target/surefire-reports` | Arquitectura, HTTP e infraestructura inicial verificados |
| 2026-09-14 | `docker compose config` | Valores locales ficticios predeterminados; tres servicios y red bridge dedicada | Exit code 0; configuración válida | Salida reproducible del comando | Sintaxis, interpolación, dependencias, puertos, volúmenes y health checks válidos |
| 2026-09-14 | Smoke test Compose | PostgreSQL 17.11, RabbitMQ 4.3.5 Management y backend Java 25/Spring Boot 4.1.1 | Tres contenedores `healthy`; Actuator y status HTTP 200; correlation ID retornado | Observación local de `docker compose ps` y respuestas HTTP del 2026-09-14 | Backbone ejecutable sin tablas de negocio |
| 2026-09-14 | `./tests/smoke/verify-platform.sh` | `.env` local ignorado; Maven, Compose, PostgreSQL, RabbitMQ y backend | Correcto; 13 pruebas verdes, tres health checks, Actuator y status/correlation ID verificados | Reportes Surefire y salida local de las 14:18–14:19 | Comando único reproducible; Compose apagado sin eliminar volúmenes |
| 2026-09-14 | Workflow QA-01 | Secretos `SGO_*` todavía no configurados en GitHub | Pendiente de ejecución remota | Espacio de captura reservado | No se marca QA-01 terminado hasta observar una ejecución verde |
| 2026-09-14 | Primera ejecución remota QA-01 | Cuatro secretos disponibles; `SGO_RABBITMQ_PASSWORD` llegó vacío | Falló en la validación previa con exit code 1, antes de compilar | Salida de GitHub Actions reportada por Jairo | Se separaron pruebas y smoke test; se agregaron credenciales ficticias de respaldo porque CI no realiza despliegue |

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
