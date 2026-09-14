# Bitácora de trabajo arquitectónico — Fase 2

## 1. Información general del sprint

| Campo | Valor |
|---|---|
| Sistema | SGO-Logic |
| Equipo | Grupo 5 |
| Integrantes | Jairo Gómez, Jorge Pumay, Edson Sosa, Andy Sánchez y Kevin Girón |
| Periodo planificado | 10 al 23 de septiembre de 2026 |
| Objetivo del sprint | Pendiente de ratificación por el equipo |
| Enlace al Kanban | Pendiente de registrar/verificar |

## 2. Sprint Backlog

| ID | Descripción | Dueño | Estado | Dependencias | Evidencia |
|---|---|---|---|---|---|
| BB-01 | Estructura, README, bitácora y plantilla ADR | Jairo | En validación | Ninguna | Pendiente de revisión cruzada |
| BB-02 | ADR-009 de stack y topología | Jairo | Proposed / parcial | Aprobación de al menos tres integrantes | `FASE 2/docs/adr/ADR-009-stack-y-topologia-ejecutable.md` |
| BB-05 | Compose y configuración por ambiente | Jairo | En validación / parcial | ADR-009 y componentes compartidos | Pendiente de revisión cruzada |
| Resto del backlog | Consultar planificación oficial | Según planificación | Pendiente | Contratos e implementaciones del equipo | Pendiente |

Los estados anteriores describen únicamente los artefactos presentes; no equivalen a tarjetas `Done` ni inventan aceptación del equipo.

## 3. Observaciones reales de Fase 1

Los documentos disponibles indican que no existe un registro verificable del feedback docente de Fase 1 (DEC-07). No se transcribe ninguna observación sin evidencia.

| ID | Observación textual | Fuente verificable | Acción o justificación | Estado |
|---|---|---|---|---|
| OBS-F1-XX | Pendiente | Pendiente | Pendiente | Pendiente |

## 4. Captura inicial del Kanban

- Estado: pendiente.
- Ruta o enlace de evidencia: pendiente.
- Fecha y responsable de captura: pendiente.

## 5. Daily Standup

Copiar un bloque por fecha e integrante sin completar información retroactiva.

### Fecha: YYYY-MM-DD — Integrante: nombre

- Ayer: pendiente.
- Resultado funcional o numérico: pendiente.
- Hoy y trazabilidad a CUN/EaC/restricción/ADR: pendiente.
- Impedimentos, responsable e impacto: pendiente.
- Horas reales y estimadas restantes: pendiente.
- Persona que quedaría bloqueada y acción temporal: pendiente.

## 6. Bloqueos

| Fecha | Tarea | Causa reproducible | Impacto | Puede desbloquear | Próxima revisión | Estado |
|---|---|---|---|---|---|---|
| Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |

## 7. Decisiones arquitectónicas

| Fecha | ADR/decisión | Estado | Participantes | Evidencia de aprobación | Consecuencia |
|---|---|---|---|---|---|
| 2026-09-14 | ADR-009, borrador de stack y topología | Proposed | Jairo (redacción); aprobación del equipo pendiente | Pendiente | BB-02 no puede cerrarse |
| 2026-09-14 | Diferencia de versión JUnit | Pendiente de ratificación | Jairo (detección); equipo pendiente | Resolución efectiva del BOM de Spring Boot 4.1.1 | La planificación indica JUnit 5 y el BOM resuelve JUnit Jupiter 6.0.3 |

## 8. Resultados de pruebas

| Fecha | Comando/escenario | Configuración y datos | Resultado | Evidencia | Conclusión |
|---|---|---|---|---|---|
| 2026-09-14 | `./mvnw clean verify` | Java 25.0.4.1; PostgreSQL 17.11 y RabbitMQ 4.3.5 mediante Testcontainers | `BUILD SUCCESS`; 6 pruebas, 0 fallos, 0 errores, 0 omitidas | Reportes Surefire locales en `FASE 2/backend/target/surefire-reports` | Arquitectura, HTTP e infraestructura inicial verificados |
| 2026-09-14 | `docker compose config` | Valores locales ficticios predeterminados; tres servicios y red bridge dedicada | Exit code 0; configuración válida | Salida reproducible del comando | Sintaxis, interpolación, dependencias, puertos, volúmenes y health checks válidos |
| 2026-09-14 | Smoke test Compose | PostgreSQL 17.11, RabbitMQ 4.3.5 Management y backend Java 25/Spring Boot 4.1.1 | Tres contenedores `healthy`; Actuator y status HTTP 200; correlation ID retornado | Observación local de `docker compose ps` y respuestas HTTP del 2026-09-14 | Backbone ejecutable sin tablas de negocio |

## 9. Retrospectiva individual

### Integrante: nombre

1. Decisiones y patrones consolidados con éxito: pendiente.
2. Supuestos o diagramas de Fase 1 que fallaron o exigieron refactorización: pendiente.
3. Mejoras técnicas concretas para mantenibilidad futura: pendiente.

## 10. Burndown o resumen consolidado

No existen métricas verificadas para completar un burndown en esta plantilla.

| Categoría | Cantidad | Justificación |
|---|---:|---|
| Completadas | Pendiente | Pendiente |
| En curso | Pendiente | Pendiente |
| Bloqueadas | Pendiente | Pendiente |
| Pendientes | Pendiente | Pendiente |

## 11. Captura final del Kanban

- Estado: pendiente.
- Ruta o enlace de evidencia: pendiente.
- Fecha y responsable de captura: pendiente.
