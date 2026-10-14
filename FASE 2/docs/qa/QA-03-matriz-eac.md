# QA-03 — Matriz de Escenarios de Atributos de Calidad (EaC-01 a EaC-12)

| Campo | Valor |
|---|---|
| Responsable | Kevin Girón (PO-QA) |
| Fuente | [`Docs/DDA.md`](../../../Docs/DDA.md), §4.2 |
| Corte | 2026-10-07 (Kevin: PayKlik y LogiExpress como sistemas reales, eventos de stock a los POS, ERP automático, resumen de la orden completo; antes 2026-09-24) |
| Estado | Instantánea viva — se actualiza conforme el equipo entrega evidencia, no una medición final |

## 1. Criterio de uso

- `Cubierto`: existe implementación y prueba automatizada o manual reproducible que ejercita el escenario descrito en el DDA.
- `Parcial`: existe implementación funcional, pero la medida de respuesta completa (carga, p95/p99, ventana de tiempo real) no está verificada.
- `Bloqueado`: depende de un módulo que otro integrante todavía no entrega; no se puede probar honestamente hasta entonces.
- `No evaluado`: requiere un ejercicio que no se ha realizado (ej. despliegue en nube, taller de runbook) y no depende de código faltante.
- Ninguna fila se marca `Cubierto` sin una evidencia reproducible enlazada. Si la evidencia es de esta sesión de trabajo, se cita el comando o la prueba exacta.

## 2. Matriz

| ID | Atributo (DDA) | Medida de respuesta (DDA) | Estado | Evidencia | Responsable del pendiente |
|---|---|---|---|---|---|
| EaC-01 | Rendimiento — consulta de orden | 100 % de consultas ≤ 5 s, con p50/p95/p99 | **Cubierto** | QA-02 con k6 sobre 500 proyecciones: `T` (10 VUs) 1 190 iteraciones, p50 3.35 ms / p95 5.88 ms / p99 7.10 ms; `8T` (80 VUs) 9 600 iteraciones, p50 2.07 ms / p95 4.22 ms / p99 8.30 ms; 0 % de errores y 100 % ≤ 5 s. Evidencia en `docs/evidence/performance/QA-02-linea-base-y-8T.md`. | Edson Sosa — cerrado en este incremento |
| EaC-02 | Rendimiento/Auditabilidad — evidencia de transacción | < 1 s | **Cubierto** | AUD-01/AUD-02 implementados: `GET /api/v1/audit/events` y exportación sobre `audit_events` (`V3002`) con índices por orden, agregado y correlación. `AuditQueryIntegrationTest.queryStaysUnderOneSecondWithDeclaredDataset` verifica < 1 s sobre 10 000 eventos (1 000 de una misma orden) y el smoke real respondió en ~63 ms. | Edson Sosa — cerrado en este incremento |
| EaC-03 | Escalabilidad — Black Friday 8x | Soportar 8x sin degradar EaC-01/EaC-02 | **Parcial** | QA-02 midió `8T` (80 VUs, ≈ 8.04× la tasa de `T`) sobre los endpoints consolidados disponibles sin degradar EaC-01 ni EaC-02 y con 0 % de errores. Jairo cerró ORD-01 a ORD-05 el 2026-09-23 (Saga, cancelación/compensación y 5 recorridos E2E, 139 pruebas en verde), así que ya existe el conjunto completo de módulos P0. Falta re-ejecutar `8T` contra el sistema integrado completo (carrito → Saga) y un ambiente con balanceo para escalabilidad horizontal real. | Edson Sosa (re-medición QA-02 sobre el sistema integrado) |
| EaC-04 | Disponibilidad/Resiliencia — falla de PayKlik | 0 órdenes perdidas; reintento/conciliación ≤ 60 s | **Cubierto** | PAY-01 a PAY-03 (Edson) más, desde 2026-10-06, **PayKlik real** (`payklik-service`, REST con `X-Api-Key`, idempotente por llave): `HttpPayKlikAdapterTest` (aprobado, 503, timeout, 400 y conciliación 404). Con la **Saga retomando** tras conciliar (`OrderSagaReconciledPaymentTest`), un pago tardío ya no deja la orden atorada. **En vivo** con PayKlik en modo "Lento" (7 s > 5 s): el intento quedó `INDETERMINATE`, se concilió `APPROVED` con la misma autorización `PK-00000004` en < 60 s, PayKlik registró **un solo cobro** y la compra terminó sola con factura y despacho. | Kevin/Edson — cerrado (2026-10-06) |
| EaC-05 | Confiabilidad — timeout LogiExpress (2 %) | 0 pedidos bloqueados; hasta 3 reintentos (2/8/32 s) antes de mensajes fallidos | **Cubierto** | 1 intento + 3 reintentos (2/8/32 s) y `shipment_dead_letters` (`ShipmentServiceTest`). Desde 2026-10-06, **LogiExpress real** (`logiexpress-service`) por REST (`HttpLogiExpressAdapterTest`) con rastreo cada 10 s (`LogiExpressTrackingSyncTest`). Una guía emitida con respuesta tardía se asocia por rastreo y un reintento tardío no la pisa (`lateTimeoutNeverOverridesAGuideFoundByTracking`). "Reintentar guía" recupera un `LABEL_FAILED` (`deadLetteredLabelCanBeRequestedAgainOnceLogiExpressIsBack`). **En vivo**: modo "Lento", guía `LGX-00000002` asociada por rastreo; "Fuera de servicio", `LABEL_FAILED` tras 4 intentos y, al volver, "Reintentar guía" emitió `LGX-00000003`. Ningún pedido quedó bloqueado. | Kevin — cerrado (2026-10-06) |
| EaC-06 | Confiabilidad — entrega eventual a 32 POS | Acuse ≤ 5 min; reintento persistente, alerta antes de 24 h | **Cubierto** (alerta en consola y bitácora) | Eventos de **orden** a 32 tiendas automáticos (`legacyfeed`; `OrderStatusLegacyFeedConsumerTest`); la tienda 1 es un POS real (`RoutingPosGatewayAdapterTest`, `store-service`) y acusa en segundos (en vivo: 32 `ACKNOWLEDGED` por evento). Con las tiendas 7 y 15 caídas: 30 acuses y 2 `DEAD_LETTER` visibles en la consola. Eventos de **stock** del central a cada punto de venta, en bandeja persistente con orden por tienda y reintentos 2/8/32 s (`PosStockEventServiceTest`). En vivo, con la tienda apagada: Fallido, registro `POS_EVENTO_FALLIDO` en la bitácora del central, y "Reenviar" lo entregó al volver. Limitación honesta: la "alerta" es visual (consola, bitácora), no una notificación activa al operador. | Kevin — cerrado (2026-10-06/07) |
| EaC-07 | Auditabilidad — reconstrucción SAT | Reconstruir quién/cuándo/qué; evidencia < 1 s (EaC-02) | **Parcial** | AUD-01/AUD-02 (Edson) e `incidents`. Desde 2026-10-06, el **informe de ventas conciliado** audita cada consulta y exportación (`SALES_REPORT_VIEWED/EXPORTED`, `SalesReportServiceTest`) y cruza venta, pago, factura SAT y ERP. El resumen de la orden ya incluye el pago (`PaymentStatusChangedConsumer`) y no pierde hechos concurrentes (`OrderSummaryConcurrencyIntegrationTest`). Falta que pagos, fiscal y logística escriban sus acciones en `audit_events`. | Edson Sosa y dueños de cada módulo |
| EaC-08 | Disponibilidad parcial — falla del proveedor de notificaciones | 0 operaciones principales bloqueadas | **Cubierto** | `NotificationService.requestNotification` responde `202 Accepted` de inmediato y despacha en un `Executor` propio; el reintento pasó de uno solo a la misma secuencia 2/8/32 s que LOG-03, con `notification_dead_letters` (DLQ del trabajador) al agotar intentos — `NotificationServiceTest.exhaustsThreeRetriesThenSendsToDeadLetterQueue`. NOT-01 ya no depende solo de la confirmación de orden: `ShipmentStatusNotificationConsumer` e `IncidentNotificationConsumer` avisan también preparación/tránsito/entrega e incidencia registrada, resolviendo el cliente mediante `order_customer_links` sin leer tablas de Orders. | Kevin — cerrado en este incremento (2026-09-24) |
| EaC-09 | Mantenibilidad — equipo interno | Despliegue con runbook ≤ 2 h; diagnóstico de falla inyectada ≤ 30 min | **Parcial** | `docker compose up --build` reproducible con health checks verdes (Jairo, BB-01/BB-05) y ahora incluye `frontend` (Kevin). No se ha cronometrado un despliegue por una persona no autora ni un diagnóstico de falla inyectada. | Jorge Pumay (EAC-09, tarea explícita en su tabla) |
| EaC-10 | Costo — infraestructura ≤ $1,200/mes | Costo operativo ≤ límite o excepción documentada | **No evaluado** | No existe despliegue en un ambiente medible con costo real; solo Docker Compose local. No se inventa una cifra. | Equipo, al planificar el despliegue real |
| EaC-11 | Portabilidad — on-premises a nube | Mismo artefacto contenedorizado en nube equivalente, sin reescribir el núcleo | **No evaluado** | El backend ya es un artefacto Docker único (BB-05), condición necesaria pero no ejercitada contra un entorno de nube. | Jairo Gómez (release/infra) |
| EaC-12 | Seguridad — datos de pago | 0 apariciones de PAN/CVV en BD, mensajes o logs | **Cubierto** | SEC-01: `SensitiveDataLeakScanTest` genera actividad real (pago, incidencia, despacho, notificación, fiscal, auditoría y 401), rechaza PAN con separadores y variantes CVV/CVC con `400` y escanea todas las columnas de texto/JSON del esquema público, los payloads del Outbox, los estados de auditoría y los logs capturados con patrones que aceptan separadores: 0 apariciones. `PaymentRequestTest` cubre la validación del borde, `SecurityAuthorizationTest` la matriz 401/403 y `SensitiveDataSanitizerTest` el enmascarado. Evidencia en `docs/evidence/security/SEC-01-scan-pan-cvv.md`. | Edson Sosa — cerrado en este incremento |

## 3. Resumen

| Estado | Cantidad |
|---|---:|
| Cubierto | 7 (EaC-01, EaC-02, EaC-04, EaC-05, EaC-06, EaC-08, EaC-12) |
| Parcial | 3 (EaC-03, EaC-07, EaC-09) |
| Bloqueado | 0 |
| No evaluado | 2 (EaC-10, EaC-11) |

EaC-05 y EaC-08 permanecen cubiertos. La entrega local de Andy aporta evidencia parcial a EaC-06 y EaC-12, pero no se marcan cubiertos sin infraestructura real, alertas y los módulos de Edson. Esta matriz debe releerse después de revisión e integración.

## 4. Próxima actualización

Actualizar esta matriz en cada daily (sección 11 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`) cuando cualquier integrante reporte una entrega que cambie el estado de una fila. Ninguna tarjeta que afirme cumplir un EaC pasa a `Done` sin que esta tabla lo respalde (regla explícita de la sección 8.3 de responsabilidades).

## 5. Evidencia en vivo de Fase 2 (2026-10-06/07, Kevin)

Todas las pruebas se hicieron con `docker compose` (12 contenedores) y datos limpios, salvo que se indique otra cosa.

| Escenario | Resultado | EaC |
|---|---|---|
| Compra web con retiro en POS-01 | PayKlik `PK-00000001` aprobado. LogiExpress `LGX-00000001`: recolectar, dejar en tienda, entregar al cliente. Orden `DELIVERED` sin marcar nada a mano. 5 correos reales. `ORDER_READY_FOR_PICKUP` solo a la tienda 1 | 01, 05, 06, 08 |
| PayKlik "Lento" (7 s) | Pago `INDETERMINATE`, conciliado `APPROVED` `PK-00000004`, un solo cobro, compra terminada sola | 04 |
| LogiExpress "Lento" / "Fuera de servicio" | Guía asociada por rastreo / `LABEL_FAILED` y luego "Reintentar guía" (`LGX-00000003`) | 05 |
| Tiendas 7 y 15 caídas (POS) | 30 acuses y 2 `DEAD_LETTER` | 06 |
| Tienda POS-01 apagada (eventos de stock) | Reintentos, Fallido, reenvío exitoso | 06 |
| ERP caído (`OUTAGE`) | Lote `DEAD_LETTER`; el informe marca "1 diferencias"; "Reintentar" lo entrega y vuelve a "Cuadra" | 07 |
| Orden, pago y despacho concurrentes | 10 rondas sin perder datos (prueba con PostgreSQL real) | 01 |

Pruebas automáticas al corte: backend 227, inventory-service 40, store-service 15, payklik-service 4, logiexpress-service 4.
