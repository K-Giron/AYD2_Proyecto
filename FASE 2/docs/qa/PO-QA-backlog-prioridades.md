# PO-QA — Priorización, aceptación y coordinación QA

| Campo | Valor |
|---|---|
| Responsable | Kevin Girón (Product Owner operativo / QA Coordinator) |
| Corte | 2026-09-23 |
| Fuente de tareas | `RESPONSABILIDADES_INTEGRANTES_FASE_2.md` |

## 1. Qué es este documento

Herramienta de arranque para las tareas `DEC-05/07` (prioridades P0/P1 sin inventar requisitos, registro del feedback real) y `PO-QA` (priorización, aceptación, daily, coordinación QA). No sustituye la coordinación diaria en vivo con el equipo — eso solo lo puede hacer Kevin, en el daily real, junto con la tabla de control de la sección 11 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`.

## 2. Estado observable a la fecha de corte

El corte se basa en el workspace integrado y no afirma que los cambios ya estén publicados en `origin/develop`. Existen módulos ejecutables de órdenes, inventario, pagos, fiscal, logística, notificaciones, consulta, incidencias, ERP, POS, messaging y audit. La suite completa registra 87 pruebas verdes. Outbox, Inbox y los consumidores Query/Notifications cubren transiciones de orden; siguen pendientes ERP/POS, productores de otros agregados, seguridad backend, prueba de carga 8x y evidencia integral de resiliencia.

## 3. Prioridades P0/P1 (sin inventar alcance nuevo)

Se listan tal como aparecen en `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`; este documento no agrega tareas nuevas.

| Prioridad | Elemento crítico | Dueño | Estado |
|---|---|---|---|
| P0 | BB-03 (modelo relacional) | Jorge/equipo | **Modelo ER editable producido** en `docs/diagrams/SGO-Logic-Modelo-ER.drawio`; revisión cruzada pendiente |
| P0 | BB-04 (Outbox) | Edson/Jairo | **Parcial integrado** — Outbox, publicador RabbitMQ, retry/DLQ, Inbox y consumidores Query/Notifications listos; faltan ERP/POS y productores de otros agregados |
| P0 | BB-06 (simuladores oficiales) | Andy/equipo | Integrados para PayKlik, SAT, ERP, POS, LogiExpress y notificaciones; evidencia de fallas coordinadas pendiente |
| P0 | LOG-01/02/03, NOT-01, QRY-01, INC-01 | Kevin/equipo | **Integrado**, verificado dentro de la suite completa de 87 pruebas; NOT-01 ya recibe confirmaciones de orden automáticamente |
| P0 | UI-01 | Kevin | **Entregado** en esta rama, probado con `docker compose up` real |
| P1 | ORD-03 (Saga) | Jairo/equipo | Camino feliz carrito → inventario → pago → fiscal → confirmación integrado; despacho se coordina desde STAFF |
| P1 | QA-03 | Kevin | **Actualizado**; 2 EaC cubiertos, 7 parciales, 1 bloqueado y 2 no evaluados |
| P1 | QA-05 (fallas externas) | Kevin/equipo | Pruebas de módulo cubiertas; evidencia E2E versionada y mediciones temporales pendientes |
| P1 | QA-06 (gate de penalizaciones) | Equipo | **Creado**; obligatorio para todo trabajo nuevo |
| P1 | DEMO-01 | Kevin/equipo | Guion actualizado al flujo integrado; capturas y ensayo cruzado pendientes |

## 4. Registro de feedback real

Ningún stakeholder externo (Roberto Quezada, Finanzas, Personal de atención, etc.) ha dado feedback formal registrado a la fecha de este corte. Este documento no inventa citas ni aprobaciones. Cuando ocurra una sesión real de feedback, se registra aquí:

| Fecha | Quién dio el feedback | Contenido | Acción tomada |
|---|---|---|---|
| Pendiente | Pendiente | Pendiente | Pendiente |

## 5. Reglas de aceptación que Kevin aplica como QA Coordinator

Tomadas literalmente de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md` §8.3 y §12, no reinterpretadas:

- Ninguna tarjeta que afirme cumplir un EaC pasa a `Done` si `QA-03-matriz-eac.md` no lo respalda con evidencia.
- P0 y P1 tienen prioridad sobre cualquier refinamiento visual P2. La UI solo puede mostrar como terminadas capacidades respaldadas por el gate QA-06.
- Un integrante cierra su responsabilidad solo si sus pruebas automatizadas y negativas están ejecutadas, otra persona revisó su código, y su trabajo corre dentro de Docker Compose — no se acepta "funciona en mi máquina".
- Kevin no acepta sus propias tareas sin revisor (§8.3): **el código de esta rama todavía no tiene revisor distinto del autor** y no debe marcarse `Done` en Kanban hasta que alguien del equipo lo revise.

## 6. Próxima acción de coordinación

1. Pedir revisor distinto del autor para el contenido de esta rama (`feature/kevin-logistica-consulta-notificaciones-incidencias`).
2. Llevar a Jairo los dos cambios que tocan `shared` (`ModularArchitectureTest`, `shared/package-info.java`) para su aprobación como arquitecto integrador antes de cualquier integración a `develop`.
3. En el próximo daily, actualizar la tabla de control de la sección 11 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md` con horas reales de Kevin (no registradas todavía).
4. Reconfirmar con Jorge, Edson y Andy sus fechas objetivo (11, 12 y 12 de septiembre respectivamente ya vencidas a la fecha de este corte) para poder reprogramar ORD-03, QA-05 y DEMO-01 con una fecha realista.
