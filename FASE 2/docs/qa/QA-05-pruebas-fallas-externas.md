# QA-05 — Pruebas coordinadas de fallas externas

| Campo | Valor |
|---|---|
| Responsable | Kevin Girón |
| Depende de (según §8.1 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`) | PAY-02 (Edson), FIS-02 (Andy), LOG-03 (Kevin), NOT-01 (Kevin) |
| Corte | 2026-09-22 |

## 1. Propósito

Verificar que una falla de un proveedor externo (PayKlik, SAT, LogiExpress o el proveedor de notificaciones) no bloquee ni corrompa el flujo principal, y que cada módulo reaccione según lo que su EaC exige. QA-05 no puede cerrarse cubriendo solo las piezas de Kevin: la tarjeta original bloquea con PAY-02 y FIS-02.

## 2. Estado por dependencia externa

| Fuente de falla | Módulo dueño | Estado | Evidencia |
|---|---|---|---|
| Timeout de LogiExpress (~2 %) | Logistics (Kevin) | **Verificado** | `ShipmentServiceTest.exhaustsThreeRetriesThenSendsToDeadLetterQueue`: fuerza `LogiExpressTimeoutException` en las 4 llamadas, confirma backoff 2/8/32 s exacto y registro en `shipment_dead_letters` con `attempts = 4`. También `requestsLabelAndMarksIssuedOnSuccess` cubre el camino feliz para contraste. |
| Indisponibilidad del proveedor de notificaciones | Notifications (Kevin) | **Verificado** | `NotificationServiceTest.retriesOnceThenFailsWhenProviderStaysUnavailable`: fuerza `NotificationProviderUnavailableException` en cada intento, confirma exactamente 1 reintento y estado final `FAILED` sin bloquear la respuesta original (`202` ya se había devuelto). |
| Indeterminación de pago (PayKlik) | Payments (Edson) | **Bloqueado** | PAY-01/PAY-02 no existen en `develop` a la fecha de este corte (ver `BITACORA_FASE_2.md`, §5). No hay código que probar. |
| Confirmación fiscal pendiente/timeout (SAT) | Fiscal (Andy) | **Verificado localmente** | `FiscalServiceTest.transientSatFailureRetriesThreeTimesAndRemainsFiscalPending`: fuerza timeout en 4 llamadas (inicial + 2/8/32 s), confirma `FISCAL_PENDING`, `SAT_RETRY_EXHAUSTED` y compuerta de orden cerrada. El camino feliz y el rechazo explícito también están cubiertos. |

## 3. Lo que falta para cerrar QA-05

1. Edson entrega PAY-02 (timeout, circuit breaker, pago indeterminado) con su propia prueba de falla forzada, análoga a `ShipmentServiceTest`.
2. Ejecutar FIS-02 con Java 25, PostgreSQL y Docker reales; la verificación local de Andy se realizó con Java 21 y dobles de repositorio por falta de Docker.
3. Kevin (o quien coordine QA) ejecuta una prueba de extremo a extremo que fuerce **las cuatro** fallas simultáneamente sobre una orden real de la Saga (ORD-03, Jairo) y confirma:
   - La orden no se pierde ni queda en un estado inconsistente.
   - Ninguna operación principal esperó de forma bloqueante a un proveedor caído.
   - Cada falla quedó registrada de forma auditable (pendiente también de AUD-01, Edson).
4. Actualizar `QA-03-matriz-eac.md` (EaC-04, EaC-05, EaC-08) con el resultado consolidado.

## 4. Conclusión de este corte

QA-05 está **parcialmente cubierto**: LOG-03, NOT-01 y FIS-02 tienen pruebas automatizadas. La evidencia de FIS-02 forma parte de la suite local de 40 pruebas, pero no todavía de `clean verify` con Docker. La tarjeta no puede moverse a `Done` porque PAY-02 y la prueba E2E con la Saga siguen pendientes.
