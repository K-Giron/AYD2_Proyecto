# QA-05 — Pruebas coordinadas de fallas externas

| Campo | Valor |
|---|---|
| Responsable | Kevin Girón |
| Depende de (según §8.1 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`) | PAY-02 (Edson), FIS-02 (Andy), LOG-03 (Kevin), NOT-01 (Kevin) |
| Corte | 2026-09-23 |

## 1. Propósito

Verificar que una falla de un proveedor externo (PayKlik, SAT, LogiExpress o el proveedor de notificaciones) no bloquee ni corrompa el flujo principal, y que cada módulo reaccione según lo que su EaC exige. QA-05 no puede cerrarse cubriendo solo las piezas de Kevin: la tarjeta original bloquea con PAY-02 y FIS-02.

## 2. Estado por dependencia externa

| Fuente de falla | Módulo dueño | Estado | Evidencia |
|---|---|---|---|
| Timeout de LogiExpress (~2 %) | Logistics (Kevin) | **Verificado** | `ShipmentServiceTest.exhaustsThreeRetriesThenSendsToDeadLetterQueue`: fuerza `LogiExpressTimeoutException` en las 4 llamadas, confirma backoff 2/8/32 s exacto y registro en `shipment_dead_letters` con `attempts = 4`. También `requestsLabelAndMarksIssuedOnSuccess` cubre el camino feliz para contraste. |
| Indisponibilidad del proveedor de notificaciones | Notifications (Kevin) | **Verificado** | `NotificationServiceTest.retriesOnceThenFailsWhenProviderStaysUnavailable`: fuerza `NotificationProviderUnavailableException` en cada intento, confirma exactamente 1 reintento y estado final `FAILED` sin bloquear la respuesta original (`202` ya se había devuelto). |
| Indeterminación de pago (PayKlik) | Payments | **Verificado** | `PaymentServiceTest` comprueba persistencia previa al proveedor, estado `INDETERMINATE`, Circuit Breaker y conciliación; `SimulatedPayKlikAdapterTest` cubre éxito, rechazo, timeout y protección PAN/CVV. |
| Confirmación fiscal pendiente/timeout (SAT) | Fiscal | **Verificado** | `FiscalServiceTest.transientSatFailureRetriesThreeTimesAndRemainsFiscalPending`: fuerza timeout en 4 llamadas (inicial + 2/8/32 s), confirma `FISCAL_PENDING`, `SAT_RETRY_EXHAUSTED` y compuerta de orden cerrada. Ejecutado con Java 25 y dentro de la suite completa. |

## 3. Lo que falta para cerrar QA-05

1. Ejecutar una prueba de extremo a extremo que fuerce **las cuatro** fallas sobre órdenes reales de la Saga y confirme:
   - La orden no se pierde ni queda en un estado inconsistente.
   - Ninguna operación principal esperó de forma bloqueante a un proveedor caído.
   - Cada falla quedó registrada de forma auditable; la base transversal ya cubre transiciones de orden, pero falta instrumentar directamente pagos, fiscal, logística y notificaciones.
2. Guardar resultados reproducibles en `docs/evidence/resilience/`, no solo la descripción de las pruebas.
3. Medir las ventanas temporales exigidas por EaC-04 y EaC-06.

## 4. Conclusión de este corte

QA-05 está **funcionalmente cubierta por pruebas de módulo** para PayKlik, SAT, LogiExpress y notificaciones. Permanece **parcial a nivel de aceptación integral** hasta almacenar evidencia E2E de fallas sobre la Saga, extender la auditoría a esos agregados y medir las ventanas temporales del DDA. La suite completa cerró con 87 pruebas, 0 fallos, 0 errores y 0 omitidas el 2026-09-23.
