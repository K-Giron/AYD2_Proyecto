# Matriz requisito → implementación → evidencia — Fase 3

**Corte:** 14 de octubre de 2026

**Estado:** implementación integrada y validada en CI/nube; promoción final a `v4` pendiente

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
