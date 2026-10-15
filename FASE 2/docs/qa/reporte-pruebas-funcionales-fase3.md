# Reporte de pruebas funcionales — Fase 3 (EDS3-05/06/07)

**Responsable del rol:** Edson Sosa (frente Integración, seguridad y pruebas funcionales)  
**Revisor:** Jorge Pumay  
**Corte:** 14 de octubre de 2026  
**Rama/corte:** `develop` @ `274ee02` + cambios locales de esta rebanada  
**Ambiente:** Java 25.0.4.1; PostgreSQL 17.11 y RabbitMQ 4.3.5 con Testcontainers

## 1. Resultado de la suite

Comando: `./mvnw clean verify` en `FASE 2/backend/`.

| Resultado | Valor |
|---|---|
| Pruebas | **320** |
| Fallos / errores / omitidas | 0 / 0 / 0 |
| Build | `BUILD SUCCESS` |

La rebanada agregó 14 pruebas (306 → 320) y habilitó por primera vez el
recorrido real Outbox → RabbitMQ → Inbox → Notifications para eventos de
crédito.

## 2. Pruebas de integración (EDS3-05)

| ID | Caso | Prueba | Resultado |
|---|---|---|---|
| IT-01 | El evento/adaptador ERP actualiza la proyección una sola vez | `credit/CreditHistoryIngestIntegrationTest.reprocessingTheSameBatchDoesNotDuplicateNorChangeTheProfile` (reutilizada y verificada) | Verde |
| IT-02 | El checkout consulta elegibilidad y expone opciones correctas | `orders/PaymentOptionsIntegrationTest` (3 casos: elegible, sin historial, carrito ajeno 403/401) | Verde |
| IT-03 | Crédito aprobado confirma orden, conserva reserva y registra auditoría | `e2e/OrderJourneysE2ETest.eligibleCustomerBuysWithOwnCreditWithoutCreatingPayKlikAttempt` + `credit/CreditServiceIntegrationTest` + `CREDIT_DECISION`/`CREDIT_OPERATION` en `audit_events` | Verde |
| IT-04 | Crédito rechazado no crea obligación ni afecta PayKlik | `e2e/OrderJourneysE2ETest.rejectedCreditCreatesNoDebtAndSameOrderContinuesWithPayKlik` + `CreditServiceIntegrationTest.customerWithoutProfileIsRejectedWithoutDebtOrDueDate` | Verde |
| IT-05 | Evento de resultado crea notificación y soporta reintento/DLQ | `notifications/CreditNotificationIntegrationTest` (4 casos: GRANTED único, REJECTED, DLQ tras 4 intentos, recuperación en el primer reintento) | Verde |

## 3. Pruebas end to end (EDS3-06)

| ID | Caso | Prueba | Resultado |
|---|---|---|---|
| E2E-01 | Cliente elegible compra a crédito y recibe fecha de pago | `OrderJourneysE2ETest.eligibleCustomerBuysWithOwnCreditWithoutCreatingPayKlikAttempt` | Verde |
| E2E-02 | Cliente no elegible completa la compra con PayKlik | `OrderJourneysE2ETest.rejectedCreditCreatesNoDebtAndSameOrderContinuesWithPayKlik` | Verde |
| E2E-03 | Evaluación indisponible o lenta no congela el checkout ni daña PayKlik | `e2e/CreditDegradationE2ETest` (2 casos: falla y timeout de 2 s) | Verde |
| E2E-04 | Repetición con la misma llave no duplica crédito ni orden | `OrderJourneysE2ETest.eligibleCustomerBuysWithOwnCreditWithoutCreatingPayKlikAttempt` extendida: checkout repetido con la misma llave y conteos 1 orden / 1 crédito / 1 factura / 1 despacho | Verde |
| E2E-05 | Notificación caída deja la orden confirmada y el mensaje reintentable | `e2e/CreditNotificationOutageE2ETest`: el proveedor falla una vez, el trabajador reintenta y la orden permanece `PREPARING` con la operación `GRANTED` | Verde |

Cierre E2E: 10 recorridos (7 de `OrderJourneysE2ETest`, 2 de degradación y 1 de
caída de notificaciones), todos verdes desde base limpia con PostgreSQL y
RabbitMQ reales.

## 4. Explicación consultable y seguridad (EDS3-04 / EDS3-07)

- **Explicación consultable:** `GET /api/v1/credit/operations/{operationId}`
  (rol `FINANCE`, `SecurityConfig`) devuelve la operación y la decisión: quién
  (`customerId`), cuándo (`evaluatedAt`, `createdAt`), regla (`ruleVersion`),
  resultado (`status`/`eligible`), razón (`reasonCode`) y las entradas
  (`inputsSnapshot`) con su `correlationId`.
  - `credit/CreditExplanationIntegrationTest` (2 casos: concedida y rechazada).
  - `security/CreditSecurityTest`: 401 sin token, 403 para CLIENT y staff,
    404 para operación desconocida.
- **Matriz 401/403:** `CreditSecurityTest` (9 casos) cubre historial,
  explicación y pago de la obligación: solo `FINANCE`.
- **Sanitización:** `SensitiveDataLeakScanTest` ahora genera actividad de
  crédito real (historial ERP, opciones, concesión) y confirma 0 PAN/CVV en
  base, mensajes y logs; incluye aserciones de que la decisión y la operación
  existieron antes del escaneo.
- **Secretos:** revisión de `.env.example` y `application.yml`: el módulo de
  crédito no tiene credenciales propias; solo parámetros `SGO_CREDIT_OPTIONS_*`
  (timeout y tamaño de pool) y el secreto JWT local de desarrollo.

## 5. Defectos y ajustes encontrados

| Hallazgo | Tipo | Acción |
|---|---|---|
| El perfil `test` deshabilita consumidores y outbox (`application-test.yml`), por lo que el camino Rabbit de crédito nunca se probó | Configuración de pruebas | La prueba IT-05 habilita `sgo.messaging.consumers.enabled` y `sgo.messaging.outbox.enabled` con `@TestPropertySource` |
| Los campos nulos se omiten al serializar (`non_null`), incluido `dueDate` de una operación rechazada | Comportamiento correcto, aserción ajustada | La prueba de explicación acepta ausencia o nulo |
| Ningún defecto de producto en la ruta de crédito durante esta rebanada | — | — |

## 6. Evidencia

- Suites: `FASE 2/backend/target/surefire-reports/`.
- Código de la rebanada: `CreditExplanationService`, `CreditOperationExplanation`,
  `CreditOperationsController#explain` y las pruebas citadas arriba.
- Pendiente personal de los responsables del rol (no se redacta por terceros):
  entradas diarias y retrospectiva en `BITACORA_FASE_3.md`, y registro de
  aprobación de **ADR-016** (revisor: Edson Sosa).
