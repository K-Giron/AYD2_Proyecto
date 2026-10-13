# FIS-03 — Evidencia de pruebas fiscal e integraciones legacy

| Campo | Valor |
|---|---|
| Responsable | Andy Sánchez |
| Corte | 2026-10-13 (corte original: 2026-09-22) |
| Estado | Implementado localmente; revisión cruzada pendiente |

## Cobertura automatizada agregada

| Área | Escenarios |
|---|---|
| Fiscal/SAT | XML escapado, CUI/serie, aprobación, rechazo, compuerta de confirmación, idempotencia y timeout con 3 reintentos |
| ERP | lote CSV, ventana vacía sin archivo, consistencia de totales, idempotencia por orden y timeout hasta `DEAD_LETTER` |
| POS | acuse para tienda 32, deduplicación evento+tienda y timeout hasta `DEAD_LETTER` |
| PayKlik | aprobación, rechazo y prohibición de PAN/CVV |
| Arquitectura | límites y ciclos mediante Spring Modulith |

## Resultado reproducible

Se ejecutó localmente la selección:

```bash
./mvnw -Djava.version=21 \
  -Dtest=FiscalServiceTest,ErpIntegrationServiceTest,PosIntegrationServiceTest,SimulatedPayKlikAdapterTest,ModularArchitectureTest \
  test
```

La selección contiene 19 pruebas después de agregar el caso positivo de nota de crédito. Todas están incluidas en la ejecución final descrita a continuación.

Después se ejecutó toda la suite que no usa contenedores:

```bash
./mvnw -Djava.version=21 -Dtest="!InfrastructureIntegrationTest" test
```

Resultado: `BUILD SUCCESS`; 40 pruebas ejecutadas, 0 fallos, 0 errores y 0 omitidas. La aplicación también compiló con `./mvnw -Djava.version=21 -DskipTests compile`: `BUILD SUCCESS`.

## Límites de la evidencia

- La versión oficial del proyecto es Java 25; la máquina disponible tiene Java 21. Se usó la propiedad de Maven solo como verificación local de compatibilidad de fuentes, sin cambiar `pom.xml` ni el Dockerfile.
- Docker no está instalado en la máquina disponible. No se ejecutaron Testcontainers, migraciones contra PostgreSQL real, `clean verify`, Compose ni el smoke test.
- La validación con Java 25 y Docker debe hacerse antes de integrar a `develop`.
- BB-04/Outbox todavía no está disponible; fiscal, ERP y POS conservan estado e idempotencia propios, pero la entrega durable ante reinicio debe integrarse con el mecanismo transversal de Edson.

## Actualización Fase 2 (2026-10-13)

Los límites de arriba quedaron resueltos: la suite completa corre con Java 25,
Docker, Testcontainers (PostgreSQL 17.11, RabbitMQ 4.3.5, Mailpit) y Compose.
La entrega durable usa el Outbox/Inbox de BB-04.

| Área | Escenarios nuevos | Prueba |
|---|---|---|
| ERP automático | La venta web confirmada entra sola al ERP (neto e IVA del 12 % separados); las facturas de tienda llegan desde el central como canal `STORE` | `OrderStatusLegacyFeedConsumerTest`, `ErpIntegrationServiceTest` |
| ERP archivo real | El lote se escribe de forma atómica como `ventas_AAAAMMDD_HHMMSS_<lote>.csv\|xml`, con línea por venta y registro de control (`TRAILER` / `<Control>`); una carpeta inaccesible equivale a ERP no disponible | `FileDropErpGatewayAdapterTest`, `ErpIntegrationServiceTest.exportsConfirmedSalesAsCsvAndMarksThemDelivered` |
| ERP reintentos | 2/8/32 s hasta `DEAD_LETTER`; "Reintentar" reabre el lote y lo entrega; la ventana retoma los lotes que un reinicio dejó huérfanos | `deadLetterBatchCanBeRetriedManuallyOnceTheErpIsBack`, `scheduledWindowResumesBatchesLeftPendingByARestart` |
| Informe de ventas | Concilia venta, pago, factura SAT y ERP; marca diferencias (`DEAD_LETTER`, montos, sin factura); queda auditado | `SalesReportServiceTest` |
| POS real | La tienda 1 recibe por HTTP (`store-service`); 2xx es acuse, 4xx es rechazo y red o 5xx se reintentan; las otras 31 tiendas usan el simulador | `RoutingPosGatewayAdapterTest`, `StoreServiceIntegrationTest` |
| POS retiro en tienda | `ORDER_READY_FOR_PICKUP` solo a la tienda de retiro | `ShipmentPickupLegacyFeedConsumerTest` |
| POS eventos de stock | El central entrega a cada tienda en orden, con reintentos 2/8/32 s, Fallido y reenvío | `PosStockEventServiceTest` |
| PayKlik real | REST con `X-Api-Key`: aprobación, 503, timeout, 400 por PAN/CVV, conciliación 404 | `HttpPayKlikAdapterTest`, `PayKlikIntegrationTest` |

**Evidencia en vivo** (2026-10-06, 12 contenedores, datos limpios):
- Lote `ventas_20261006_122511_f4faf5c3.csv` con 2 ventas (web y tienda) y `TRAILER,2,…,2247.00`.
- ERP en `OUTAGE`: el lote quedó en `DEAD_LETTER` y el informe marcó la diferencia; "Reintentar" lo entregó y el informe volvió a "Cuadra".
- Tiendas 7 y 15 caídas: 30 acuses y 2 `DEAD_LETTER`; la tienda 1 respondió con su acuse real.

Resultado al corte: backend `BUILD SUCCESS`, 227+ pruebas, 0 fallos (ver QA-03 §5).
