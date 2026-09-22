# PO-QA — Priorización, aceptación y coordinación QA

| Campo | Valor |
|---|---|
| Responsable | Kevin Girón (Product Owner operativo / QA Coordinator) |
| Corte | 2026-09-22 |
| Fuente de tareas | `RESPONSABILIDADES_INTEGRANTES_FASE_2.md` |

## 1. Qué es este documento

Herramienta de arranque para las tareas `DEC-05/07` (prioridades P0/P1 sin inventar requisitos, registro del feedback real) y `PO-QA` (priorización, aceptación, daily, coordinación QA). No sustituye la coordinación diaria en vivo con el equipo — eso solo lo puede hacer Kevin, en el daily real, junto con la tabla de control de la sección 11 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`.

## 2. Estado observable a la fecha de corte

Basado en el historial real de `git log` sobre `origin/develop` (no en lo que la planificación esperaba que hubiera pasado):

| Integrante | Última entrega visible en `develop` | Fecha |
|---|---|---|
| Jairo Gómez | BB-01, BB-02 (borrador), BB-05, QA-01 (local) | 2026-09-14 |
| Jorge Pumay | Ninguna todavía (BB-03 pendiente) | — |
| Edson Sosa | Ninguna todavía (BB-04, BB-07, PAY pendientes) | — |
| Andy Sánchez | Ninguna todavía (BB-06, FIS pendientes) | — |
| Kevin Girón | `logistics`, `notifications`, `query`, `incidents`, `frontend` (esta rama, sin commitear todavía) | 2026-09-22 |

El plan original suponía que todos empezarían en paralelo el 10 de septiembre contra contratos congelados. A la fecha de corte, solo Jairo y Kevin tienen entregas visibles en el repositorio.

## 3. Prioridades P0/P1 (sin inventar alcance nuevo)

Se listan tal como aparecen en `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`; este documento no agrega tareas nuevas.

| Prioridad | Elemento crítico | Dueño | Estado |
|---|---|---|---|
| P0 | BB-03 (modelo relacional) | Jorge | Pendiente — bloquea INV-01, ORD-01/02, AUD-01 |
| P0 | BB-04 (Outbox) | Edson | Pendiente — bloquea PAY, FIS, LOG-03/NOT-01 definitivos, ERP, POS |
| P0 | BB-06 (simuladores oficiales) | Andy | Pendiente — bloquea reemplazar los simuladores temporales de Kevin |
| P0 | LOG-01/02/03, NOT-01, QRY-01, INC-01 | Kevin | **Entregado** en esta rama, verificado con `./mvnw clean verify` (24/24) |
| P0 | UI-01 | Kevin | **Entregado** en esta rama, probado con `docker compose up` real |
| P1 | ORD-03 (Saga) | Jairo | Pendiente — depende de INV-01, PAY-01, FIS-01, LOG-01 (Kevin ya lo expone) |
| P1 | QA-03 (esta matriz) | Kevin | **Entregado** (`QA-03-matriz-eac.md`), 2 de 12 EaC cubiertos con evidencia |
| P1 | QA-05 (fallas externas) | Kevin | Parcial (`QA-05-pruebas-fallas-externas.md`); bloqueado por PAY-02 y FIS-02 |
| P1 | DEMO-01 | Kevin | Guion y datos entregados (`DEMO-01-guion-demo.md`); falta el ensayo con el equipo |

## 4. Registro de feedback real

Ningún stakeholder externo (Roberto Quezada, Finanzas, Personal de atención, etc.) ha dado feedback formal registrado a la fecha de este corte. Este documento no inventa citas ni aprobaciones. Cuando ocurra una sesión real de feedback, se registra aquí:

| Fecha | Quién dio el feedback | Contenido | Acción tomada |
|---|---|---|---|
| Pendiente | Pendiente | Pendiente | Pendiente |

## 5. Reglas de aceptación que Kevin aplica como QA Coordinator

Tomadas literalmente de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md` §8.3 y §12, no reinterpretadas:

- Ninguna tarjeta que afirme cumplir un EaC pasa a `Done` si `QA-03-matriz-eac.md` no lo respalda con evidencia.
- P0 y P1 tienen prioridad sobre cualquier refinamiento visual P2 (aplica también al propio UI-01: por eso el checkout quedó bloqueado en vez de simulado).
- Un integrante cierra su responsabilidad solo si sus pruebas automatizadas y negativas están ejecutadas, otra persona revisó su código, y su trabajo corre dentro de Docker Compose — no se acepta "funciona en mi máquina".
- Kevin no acepta sus propias tareas sin revisor (§8.3): **el código de esta rama todavía no tiene revisor distinto del autor** y no debe marcarse `Done` en Kanban hasta que alguien del equipo lo revise.

## 6. Próxima acción de coordinación

1. Pedir revisor distinto del autor para el contenido de esta rama (`feature/kevin-logistica-consulta-notificaciones-incidencias`).
2. Llevar a Jairo los dos cambios que tocan `shared` (`ModularArchitectureTest`, `shared/package-info.java`) para su aprobación como arquitecto integrador antes de cualquier integración a `develop`.
3. En el próximo daily, actualizar la tabla de control de la sección 11 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md` con horas reales de Kevin (no registradas todavía).
4. Reconfirmar con Jorge, Edson y Andy sus fechas objetivo (11, 12 y 12 de septiembre respectivamente ya vencidas a la fecha de este corte) para poder reprogramar ORD-03, QA-05 y DEMO-01 con una fecha realista.
