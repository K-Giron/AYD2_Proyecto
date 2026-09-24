# QA-05 — Pruebas coordinadas de fallas externas

| Campo | Valor |
|---|---|
| Responsable | Kevin Girón |
| Depende de (según §8.1 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`) | PAY-02 (Edson), FIS-02 (Andy), LOG-03 (Kevin), NOT-01 (Kevin) |
| Corte | 2026-09-24 |

## 1. Propósito

Verificar que una falla de un proveedor externo (PayKlik, SAT, LogiExpress o el proveedor de notificaciones) no bloquee ni corrompa el flujo principal, y que cada módulo reaccione según lo que su EaC exige. QA-05 no puede cerrarse cubriendo solo las piezas de Kevin: la tarjeta original bloquea con PAY-02 y FIS-02.

## 2. Estado por dependencia externa

| Fuente de falla | Módulo dueño | Estado | Evidencia |
|---|---|---|---|
| Timeout de LogiExpress (~2 %) | Logistics (Kevin) | **Verificado** | `ShipmentServiceTest.exhaustsThreeRetriesThenSendsToDeadLetterQueue`: fuerza `LogiExpressTimeoutException` en las 4 llamadas, confirma backoff 2/8/32 s exacto y registro en `shipment_dead_letters` con `attempts = 4`. También `requestsLabelAndMarksIssuedOnSuccess` cubre el camino feliz para contraste. |
| Indisponibilidad del proveedor de notificaciones | Notifications (Kevin) | **Verificado** | `NotificationServiceTest.exhaustsThreeRetriesThenSendsToDeadLetterQueue`: fuerza `NotificationProviderUnavailableException` en cada intento, confirma la misma secuencia 2/8/32 s que LOG-03 (4 llamadas al proveedor), estado final `FAILED` y registro en `notification_dead_letters`, sin bloquear la respuesta original (`202` ya se había devuelto). |
| Indeterminación de pago (PayKlik) | Payments | **Verificado** | `PaymentServiceTest` comprueba persistencia previa al proveedor, estado `INDETERMINATE`, Circuit Breaker y conciliación; `SimulatedPayKlikAdapterTest` cubre éxito, rechazo, timeout y protección PAN/CVV. |
| Confirmación fiscal pendiente/timeout (SAT) | Fiscal | **Verificado** | `FiscalServiceTest.transientSatFailureRetriesThreeTimesAndRemainsFiscalPending`: fuerza timeout en 4 llamadas (inicial + 2/8/32 s), confirma `FISCAL_PENDING`, `SAT_RETRY_EXHAUSTED` y compuerta de orden cerrada. Ejecutado con Java 25 y dentro de la suite completa. |

## 3. Lo que falta para cerrar QA-05

1. Jairo cerró ORD-03 a ORD-05 el 2026-09-23 con `OrderJourneysE2ETest` (5 recorridos: compra web, recuperación omnicanal, cancelación, compra y entrega, compra con incidencia). Ninguno de esos 5 recorridos fuerza explícitamente una falla externa (timeout/caída) sobre la Saga real todavía — son caminos felices y de cancelación. Sigue faltando una prueba E2E que fuerce **las cuatro** fallas (PayKlik, SAT, LogiExpress, notificaciones) sobre una orden real de la Saga y confirme que ninguna se pierde ni bloquea.
2. Guardar resultados reproducibles en `docs/evidence/resilience/`, no solo la descripción de las pruebas.
3. Medir las ventanas temporales exigidas por EaC-04 (cerrado, ver QA-03) y EaC-06 (parcial).

## 4. Conclusión de este corte

QA-05 está **funcionalmente cubierta por pruebas de módulo** para PayKlik, SAT, LogiExpress y notificaciones — las cuatro dependencias externas de la tarjeta original ya tienen implementación y prueba (PAY-02 y FIS-02 se cerraron el 2026-09-23). Permanece **parcial a nivel de aceptación integral** hasta que exista un recorrido E2E que inyecte las cuatro fallas sobre la Saga real (§3.1). Ver el conteo verificado de la suite completa en la entrada de bitácora de Kevin del 2026-09-24.
