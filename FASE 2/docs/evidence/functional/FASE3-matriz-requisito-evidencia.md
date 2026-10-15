# Matriz requisito → implementación → evidencia — Fase 3

**Corte:** 14 de octubre de 2026

**Estado:** implementación integrada y validada en CI/nube; promoción final a `v4` pendiente

| Requisito / riesgo | Implementación | Prueba o documento | Estado local |
|---|---|---|---|
| Crédito solo para elegibles | `PaymentOptionsService`, `CreditEligibilityPolicy` | `PaymentOptionsServiceTest`, `PaymentOptionsIntegrationTest` | Verde |
| PayKlik ante falla o timeout | TimeLimiter configurable, fallback seguro | `degradesToPayKlikWhenCreditFails`, `returnsPayKlikWithinBoundWhenCreditEvaluationTimesOut` | Verde |
| Propiedad cliente–carrito | registro persistente BCrypt, claim JWT individual `customer_id`, `CustomerAccessGuard` | `SecurityAuthorizationTest.registeredClientsHaveIndependentSignedIdentitiesAndResources` | Verde para múltiples clientes |
| Crédito aprobado sin PayKlik ficticio | Saga `OWN_CREDIT`, referencia fiscal neutral | E2E `eligibleCustomerBuysWithOwnCreditWithoutCreatingPayKlikAttempt` | Verde |
| Repetición sin duplicar efectos | recuperación por orden/llave | mismo E2E: una operación, factura y despacho | Verde |
| Dos concesiones simultáneas | advisory locks + restricciones únicas | `CreditServiceIntegrationTest.concurrentEquivalentRequestsCreateOneCreditOperation` | Verde |
| Límite acumulado entre compras | descuento transaccional de `available_credit` | `grantedPurchasesConsumeTheSharedLimitAndTheNextPurchaseCannotExceedTheBalance` | Verde |
| Dos órdenes simultáneas del cliente | lock por cliente y reevaluación transaccional | `concurrentDifferentOrdersCannotSpendTheSameCustomerLimitTwice` | Verde |
| Venta identificada como crédito | ruta en orden, fiscal, consulta, reporte y ERP | E2E de crédito, `confirmedCreditSaleKeepsItsFinancialRouteInTheErp`, `ownCreditSaleIsClassifiedAndReconciledWithoutPretendingThereWasAPayKlikPayment` | Verde |
| Rechazo sin deuda | operación `REJECTED`, `due_date` nulo | `customerWithoutProfileIsRejectedWithoutDebtOrDueDate` | Verde |
| Rechazo y continuación PayKlik | transición recuperable desde `CREDIT_REJECTED` | E2E `rejectedCreditCreatesNoDebtAndSameOrderContinuesWithPayKlik` | Verde |
| Llave reutilizada para otra orden | validación de equivalencia | `reusedIdempotencyKeyForAnotherOrderIsRejectedWithoutCreatingAnotherOperation` | Verde |
| Misma orden con otra llave | unicidad por orden y relectura | `sameOrderWithAnotherKeyReturnsTheExistingOperation` | Verde |
| Notificación no bloqueante e idempotente | Outbox/RabbitMQ/Inbox | `CreditResultNotificationConsumerTest`, `CreditNotificationIntegrationTest` (reintento/DLQ), `CreditNotificationOutageE2ETest` | Verde |
| Explicación consultable de la decisión | `CreditExplanationService`, `GET /api/v1/credit/operations/{operationId}` | `CreditExplanationIntegrationTest` (concedida y rechazada), `CreditSecurityTest` (401/403/404) | Verde |
| PayKlik se conserva con crédito caído o lento | degradación con límite de tiempo en `PaymentOptionsService` | `PaymentOptionsServiceTest`, `CreditDegradationE2ETest` (2 casos) | Verde |
| Límite de módulo | `credit :: api` | `ModularArchitectureTest` | Verde focalizado |
| Vistas coherentes | draw.io editable + 4 SVG | catálogo de trazabilidad y validador draw.io | Verde documental |

## Conteo identificable de esta rebanada

- Integración real con PostgreSQL/Testcontainers: **32 escenarios** —
  `CreditServiceIntegrationTest` (7), `CreditHistoryIngestIntegrationTest` (6),
  `CreditLifecycleIntegrationTest` (10), `PaymentOptionsIntegrationTest` (3),
  `CreditNotificationIntegrationTest` (4) y `CreditExplanationIntegrationTest` (2).
- E2E de plataforma con PostgreSQL y RabbitMQ/Testcontainers: **10 recorridos** —
  7 en `OrderJourneysE2ETest` (2 de crédito), 2 de degradación de crédito
  (`CreditDegradationE2ETest`) y 1 de caída de notificaciones
  (`CreditNotificationOutageE2ETest`).
- Opciones/fallback: **4 pruebas unitarias** más 2 recorridos E2E con demora y
  falla medidas.
- Notificación de crédito: **3 pruebas de consumidor**, 4 de integración
  (reintento/DLQ) y 1 E2E de caída con reintento exitoso.

La regresión completa del 14 de octubre con la rebanada de Edson integrada
ejecutó `./mvnw clean verify` sobre `develop` con Java 25 y Testcontainers:
**320 pruebas, 0 fallos, 0 errores y 0 omitidas** (`BUILD SUCCESS`), frente a
las 306 previas. El detalle caso a caso está en
`docs/qa/reporte-pruebas-funcionales-fase3.md`.

El SHA `274ee024781a3c54c9816d798a8b3603b784e207` pasó el
[run de CI 38616631224](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38616631224),
que ejecutó los cinco proyectos Java, validó el frontend y publicó seis
imágenes. El
[run 38618417187](https://github.com/JairoGH/AYD2_A_2S2026_PROYECTO_G5/actions/runs/38618417187)
desplegó ese SHA en `http://136.65.180.39` y completó health checks y smoke
externo. El smoke independiente posterior verificó 17 controles sin fallos.

Los cinco escenarios de carga y cinco de estrés exigidos permanecen pendientes
de ejecución. El arnés y el Manual Técnico están desarrollados en
`origin/feature/201904024-fase3-rendimiento`, pero todavía no forman parte de
`develop` ni contienen métricas ejecutadas. También quedan pendientes la
promoción a `main` y los releases `v2`, `v3` y `v4`.
