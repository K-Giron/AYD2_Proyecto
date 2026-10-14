# Matriz requisito → implementación → evidencia — Fase 3

**Corte:** 13 de octubre de 2026  
**Estado:** evidencia local; todavía no sustituye CI, nube, SHA ni release

| Requisito / riesgo | Implementación | Prueba o documento | Estado local |
|---|---|---|---|
| Crédito solo para elegibles | `PaymentOptionsService`, `CreditEligibilityPolicy` | `PaymentOptionsServiceTest` | Verde |
| PayKlik ante falla o timeout | TimeLimiter configurable, fallback seguro | `degradesToPayKlikWhenCreditFails`, `returnsPayKlikWithinBoundWhenCreditEvaluationTimesOut` | Verde |
| Propiedad cliente–carrito | claim JWT `customer_id`, `CustomerAccessGuard` | `SecurityAuthorizationTest` | Verde para cuenta canónica |
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
| Notificación no bloqueante e idempotente | Outbox/RabbitMQ/Inbox | `CreditResultNotificationConsumerTest` | Verde focalizado |
| Límite de módulo | `credit :: api` | `ModularArchitectureTest` | Verde focalizado |
| Vistas coherentes | draw.io editable + 4 SVG | catálogo de trazabilidad y validador draw.io | Verde documental |

## Conteo identificable de esta rebanada

- Integración real con PostgreSQL/Testcontainers: **7 escenarios** en
  `CreditServiceIntegrationTest`.
- E2E de plataforma con PostgreSQL y RabbitMQ/Testcontainers: **7 recorridos**
  en `OrderJourneysE2ETest`, incluidos 2 de crédito.
- Opciones/fallback: **4 pruebas**, incluida demora medible.
- Notificación de crédito: **3 pruebas**.

La regresión completa del 13 de octubre, después de incorporar línea acumulada
y clasificación financiera, ejecutó **260 pruebas, 0 fallos, 0 errores y 0
omitidas** (`BUILD SUCCESS`). También pasaron 27 pruebas focalizadas y los 7
recorridos E2E. Antes del cierre se detectó que
`OnlineStoreIntegrationTest` usaba un cliente aleatorio con el token canónico;
el dato se corrigió y la segunda corrida completa quedó verde.

Los cinco escenarios de carga y cinco de estrés exigidos son otra categoría y
siguen pendientes del frente de Andy. Los runs de CI, URL de nube, smoke y
releases dependen de Kevin. Los resultados exactos de la suite completa deben
repetirse en CI para obtener evidencia enlazable.
