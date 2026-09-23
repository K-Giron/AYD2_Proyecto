# ADR-010 — Confirmación fiscal asíncrona y compuerta de la orden

- **Fecha:** 2026-09-22
- **Estado:** Proposed
- **Responsable:** Andy Sánchez
- **Revisores:** Edson Sosa y Jairo Gómez

## Contexto

CDU-04 exige generar el documento fiscal después del pago, enviarlo a SAT y no confirmar la orden hasta recibir una autorización. SAT puede rechazar, responder tarde o no estar disponible. Una llamada HTTP bloqueada no debe mantener abierta toda la operación ni convertir un timeout en éxito.

La Fase 1 describe la confirmación como sincrónica respecto de la transición fiscal: la orden solo avanza cuando existe respuesta aprobada. Esto no obliga a mantener una conexión de red sincrónica con SAT. Se necesita separar la aceptación local del trabajo y la decisión de negocio.

## Drivers relacionados

- CDU-04, FIS-01 y FIS-02: XML, CUI, serie, envío y respuesta SAT.
- EaC-04: una caída de SAT no debe corromper ni perder la orden.
- EaC-11: configuración externa.
- ADR-001: módulos propietarios de sus datos y comunicación por puertos.
- BB-04: Outbox/Inbox transversal, todavía pendiente de integración.

## Decisión propuesta

El módulo `fiscal` persiste primero un `FiscalDocument` con estado `FISCAL_PENDING`, XML, CUI, serie, UUID de correlación e `Idempotency-Key`. Después envía el documento a SAT mediante un ejecutor asíncrono y el puerto `SatGatewayPort`.

La respuesta SAT produce uno de estos resultados:

| Resultado SAT | Estado del documento fiscal | ¿Orders puede confirmar? |
|---|---|---|
| Aprobado | `APPROVED` y autorización persistida | Sí |
| Rechazo funcional | `REJECTED` y causa persistida | No; la orden permanece fiscalmente pendiente |
| Timeout o caída | `FISCAL_PENDING` | No |

Los fallos transitorios se reintentan con esperas de 2, 8 y 32 segundos. Después del cuarto intento el documento permanece `FISCAL_PENDING` con `SAT_RETRY_EXHAUSTED`; nunca se infiere aprobación ni se cancela automáticamente la orden. Una recuperación manual o el mecanismo durable de BB-04 podrá reanudarlo.

`GET /api/v1/fiscal-documents/orders/{orderId}/confirmation` expone la compuerta sin permitir que Orders lea tablas o entidades privadas. El contrato definitivo con la Saga debe ratificarse antes de conectar ORD-03.

## Alternativas evaluadas

1. **Llamada SAT completamente sincrónica:** simplifica la secuencia, pero propaga latencia y fallas externas al cliente.
2. **Confirmar la orden antes de SAT:** reduce espera, pero incumple CDU-04 y puede crear órdenes confirmadas sin documento autorizado.
3. **Asíncrono con estado fiscal explícito:** conserva la regla de negocio y aísla la espera externa; requiere reintentos, idempotencia y recuperación durable.

## Consecuencias

- El alta fiscal responde `202 Accepted`; la aprobación se consulta después.
- `Idempotency-Key` evita crear otro documento ante una repetición del comando.
- El XML y la evidencia SAT pertenecen exclusivamente a `fiscal`.
- Orders debe tratar `fiscallyConfirmed=false` como una compuerta cerrada, no como rechazo de pago.
- Mientras BB-04 no exista, los reintentos temporizados sobreviven a fallas del proveedor pero no al reinicio exacto entre persistencia y despacho; el registro `FISCAL_PENDING` permite detectar y recuperar esos casos, pero el barrido durable queda pendiente.

## Riesgos

- Dos comandos simultáneos con la misma llave pueden competir hasta que la restricción única de base de datos resuelva el conflicto.
- La entrega asíncrona en memoria tiene una ventana de pérdida de ejecución ante caída del proceso; BB-04 debe cerrarla.
- El contrato propuesto puede cambiar cuando Jairo integre la Saga.

## Criterios de validación o ratificación

- XML bien formado y datos escapados.
- CUI y serie persistidos con el documento.
- Pruebas de aprobación, rechazo, timeout, reintentos e idempotencia.
- La orden no se considera confirmable sin un documento `APPROVED`.
- Revisión de Edson Sosa por resiliencia y de Jairo Gómez por integración con ORD-03.

## Referencias

- `backend/src/main/java/com/megastore/sgologic/fiscal/`
- `backend/src/test/java/com/megastore/sgologic/fiscal/FiscalServiceTest.java`
- `docs/contracts/orders/saga-required-ports.md`
