# QA-06 — Gate de trazabilidad contra penalizaciones

| Campo | Valor |
|---|---|
| Fuente normativa | `../Penalizaciones.peg` |
| Fuente arquitectónica | `../../../Docs/DDA.md` |
| Modelo de datos | `../diagrams/SGO-Logic-Modelo-ER.drawio` |
| Corte | 2026-09-23 |
| Regla | Ninguna funcionalidad pasa a `Done` si falta CDU/CUN, modelo ER, despliegue, código o prueba |

## 1. Interpretación operativa de la tabla de penalizaciones

| Nivel | Gate de trazabilidad |
|---|---|
| 100 % | El comportamiento se identifica en CDU/CUN, modelo ER, despliegue, código y evidencia de prueba. |
| 75 % | Falta representación en modelo ER o despliegue. |
| 50 % | Falta representación tanto en despliegue como en CDU/CUN. |
| 25 % | Existe código huérfano, sin driver ni representación arquitectónica. |
| 0 % | No está implementado o presenta una falla crítica del negocio. |

La existencia de una clase o de una pantalla no demuestra cobertura completa. Las cinco columnas del gate deben apuntar a artefactos verificables.

## 2. Matriz CUN → datos → despliegue → código → prueba

| CUN | CDU expandido | Modelo ER objetivo DDA (página 01) | Despliegue | Código principal | Evidencia automatizada | Estado del gate |
|---|---|---|---|---|---|---|
| CUN-01 Carrito omnicanal | `Docs/DDA.md` §3.5 y página 01 del draw.io de CUN | `CUSTOMER_ACCOUNT`, `CART`, `CART_ITEM`, `ORDER`, `ORDER_ITEM`, `ORDER_STATUS_HISTORY` | Artefacto SGO-Logic / módulo Carrito y Órdenes | `orders/OrdersService`, `OrderSagaService`, `OrdersController`; JWT en `security` | `OrdersServiceTest`, `OrderJourneysE2ETest` | **IMPLEMENTADO PARA EL MVP**: carrito recuperable entre canales, identidad autenticada, creación idempotente y Saga completa verificadas; `CUSTOMER_ACCOUNT` continúa representado por las cuentas demo configurables del MVP |
| CUN-02 Verificar y reservar inventario | §3.6 / página 02 | `LOCATION`, `INVENTORY_ITEM`, `INVENTORY_RESERVATION`, `INVENTORY_MOVEMENT` | Módulo Inventario y Reservas | `inventory/InventoryReservationService`, `InventoryReservationPort` | Pruebas de reserva, consulta, expiración, concurrencia y `OrderJourneysE2ETest` | **PARCIAL AVANZADO**: reservar, confirmar consumo y liberar por compensación están conectados a la Saga e idempotencia; falta materializar el libro histórico `INVENTORY_MOVEMENT` del modelo objetivo |
| CUN-03 Procesar pago | §3.7 / página 03 | `PAYMENT_ATTEMPT`, `PAYMENT_RECONCILIATION`, `ORDER_PROCESS_STEP` | Adaptador PayKlik, timeout y Circuit Breaker | `payments/PaymentAttemptService`, `PaymentReconciliationService`, `ResilientPayKlikClient`, `payments.api.PaymentPort` | Pruebas unitarias/integración de Payments y `OrderJourneysE2ETest` | **IMPLEMENTADO PARA EL MVP**: intento, timeout, circuito, estado indeterminado, conciliación idempotente y consumo desde la Saga están automatizados; `ORDER_PROCESS_STEP` permanece como estructura objetivo, pues el estado durable se conserva en orden, pago y conciliación |
| CUN-04 Emitir documentación fiscal | §3.8 / página 04 | `FISCAL_DOCUMENT`, `FISCAL_SUBMISSION_ATTEMPT` | Adaptador SAT y almacenamiento operacional | `fiscal/FiscalService` | `FiscalServiceTest` | **PARCIAL**: documento disponible; falta separar cada intento/intercambio SAT como evidencia histórica |
| CUN-05 Sincronizar con ERP | §3.9 / página 05 | `ERP_SALE`, `ERP_EXPORT_BATCH`, `OUTBOX_MESSAGE`, `INTEGRATION_DELIVERY` | Trabajador de integración e intercambio CSV/XML | `integration/ErpIntegrationService` | `ErpIntegrationServiceTest` | **PARCIAL**: registro disponible; falta disparo automático confiable por Outbox |
| CUN-06 Notificar eventos a POS | §3.10 / página 06 | `OUTBOX_MESSAGE`, `INTEGRATION_DELIVERY`, `INTEGRATION_DELIVERY_ATTEMPT`, `CONSUMER_INBOX`, `DEAD_LETTER_MESSAGE` | POS x32, trabajador, reintento y acuse | `integration/PosIntegrationService` | `PosIntegrationServiceTest`; acuse reproducido contra simulador | **PARCIAL**: adaptador funcional; falta publicación automática por Outbox y deduplicación general del consumidor |
| CUN-07 Gestionar despacho | §3.11 / página 07 | `SHIPMENT`, `DELIVERY_METHOD_HISTORY`, `SHIPMENT_TRACKING_EVENT` | Módulo Despacho y trabajador de integración | `logistics/ShipmentService`, `logistics.api.DispatchPort`, `orders/OrderSagaService` | `ShipmentServiceTest`, `OrderJourneysE2ETest` | **PARCIAL AVANZADO**: la confirmación crea el despacho automáticamente y el tracking llega a entrega; falta historial explícito de cambio de método/origen |
| CUN-08 Integrar con LogiExpress | §3.12 / página 08 | `SHIPMENT`, `SHIPMENT_TRACKING_EVENT`, `INTEGRATION_DELIVERY`, `INTEGRATION_DELIVERY_ATTEMPT`, `DEAD_LETTER_MESSAGE` | Adaptador REST, retry 2/8/32 y mensajes fallidos | `SimulatedLogiExpressAdapter`, `ShipmentService` | `ShipmentServiceTest` | **PARCIAL**: simulación y DLQ local disponibles; falta libro transversal de cada intercambio/acuse |
| CUN-09 Consultar estado de orden | §3.13 / página 09 | `ORDER_SUMMARY_PROJECTION`, `SERVICE_INCIDENT` y correlaciones con pago, fiscal, despacho y devolución | Auditoría/Consulta y almacenamiento de lectura | `OrderStatusChangedConsumer`, `ShipmentStatusChangedConsumer`, `IncidentRegisteredConsumer`, `InboxService`, `OrderSummaryQueryService`; agregación temporal en frontend | `OrderStatusChangedConsumerTest`, `ShipmentStatusChangedConsumerTest`, `IncidentRegisteredConsumerTest`, `InboxServiceTest`, `OrderSummaryQueryServiceTest`; smoke Compose real | **PARCIAL AVANZADO** (Kevin, 2026-09-24): Orders, Logistics (despacho/entrega) e Incidents ya alimentan la proyección automáticamente vía Outbox/Inbox y conservan antigüedad por campo; falta el productor de pago (Payments no publica hacia Query todavía) |
| CUN-10 Gestionar devoluciones | §3.14 / página 10 | `RETURN_REQUEST`, `RETURN_ITEM`, `REFUND`, `SERVICE_INCIDENT`, `INVENTORY_MOVEMENT`; relación con `FISCAL_DOCUMENT` | Módulo Postventa | `incidents/IncidentService` | `IncidentServiceTest`; recorrido E2E de entrega → incidencia → resolución `REFUNDED` | **PARCIAL CRÍTICO**: el E2E acredita la resolución administrativa disponible, pero no sustituye devolución, reembolso monetario, reposición y nota de crédito orquestados |
| CUN-11 Notificar al cliente | §3.15 / página 11 | `CUSTOMER_CONTACT`, `NOTIFICATION_REQUEST`, `NOTIFICATION_ATTEMPT`, Outbox y DLQ | Cola/consumidor de notificaciones | `notifications/OrderStatusNotificationConsumer`, `ShipmentStatusNotificationConsumer`, `IncidentNotificationConsumer`, `NotificationService`, `CustomerLookupService` | `OrderStatusNotificationConsumerTest`, `NotificationServiceTest`; smoke Outbox → Inbox → Notifications | **PARCIAL AVANZADO** (Kevin, 2026-09-24): confirmación de orden, preparación/tránsito/entrega de despacho e incidencia registrada ya crean el envío automático, resolviendo el cliente mediante `order_customer_links` (Logistics/Incidents no conocen `customerId`); el trabajador reintenta 2/8/32 s y cae en `notification_dead_letters` (DLQ) al agotar intentos. Sigue faltando el historial separado por intento (hoy solo se conserva el último error y un contador) |
| CUN-12 Auditar operaciones | §3.16 / página 12 | `AUDIT_EVENT`, `AUDIT_EVIDENCE_ACCESS` y correlación transversal | Nodo Auditoría | `audit/AuditService`, `AuditController`; productor integrado en `OrdersService` | `AuditServiceTest`; suite completa de 87 pruebas | **PARCIAL**: append-only, búsqueda, rechazo y exportación están implementados para transiciones de orden; faltan productores directos de pagos, fiscal, logística e incidencias y medición < 1 s |

## 3. Elementos objetivo que no deben presentarse como implementados

| Elemento | Aparece en DDA/despliegue | Estado real | Cierre requerido |
|---|---|---|---|
| Outbox transaccional | Sí, ADR-002 | `outbox_messages`, productor de órdenes, publicador RabbitMQ, backoff, DLQ e Inbox implementados | Añadir consumidores restantes; probar caída exacta entre commit/publicación |
| RabbitMQ como flujo de negocio | Sí | Exchange, publicador Outbox y consumidores idempotentes Query/Notifications verificados en Compose | Conectar consumidores con ERP y POS; publicar eventos de despacho e incidencias |
| Auditoría transversal | Sí | `audit_events` y `audit_evidence_access` implementados para transiciones de orden | Incorporar pagos, fiscal, logística e incidencias como productores directos; medir consulta < 1 s |
| Historial de conciliación de pago | Sí, CDU-03 / ADR-003 | El intento conserva un instante agregado | Crear `payment_reconciliations`, agenda ≤ 60 s y evidencia por intento |
| Historial de intercambios SAT | Sí, CDU-04 / ADR-007 | El documento acumula contador/último resultado | Crear `fiscal_submission_attempts` y conservar respuesta/evidencia por intento |
| Libro de movimientos de inventario | Sí, CDU-02 / CDU-10 | Solo saldo y reservas | Crear movimientos de reserva, liberación, venta, devolución y ajuste |
| Cambio de método de entrega | Sí, CDU-07 | Estado actual del envío | Persistir historia de método/origen, actor, motivo e instante |
| Devolución y reembolso completos | Sí, CDU-10 | Incidencia e ítems básicos | Separar solicitud, ítems, reembolso, reposición y documento fiscal relacionado |
| Intentos de notificación | Sí, CDU-11 / ADR-008 | Contador, último error y **DLQ del trabajador** (`notification_dead_letters`) implementados (Kevin, 2026-09-24) | Conservar cada intento individual con canal y respuesta (hoy solo el último) |
| Consulta consolidada event-driven | Sí | Orders, Logistics (despacho/entrega) e Incidents alimentan la proyección con RabbitMQ e Inbox (Kevin, 2026-09-24); smoke verificó `PUBLISHED` → `PROCESSED` → resumen | Incorporar el productor de pago y medir consistencia eventual |
| Seguridad backend | Módulo declarado | JWT, roles y sanitización implementados; matriz 401/403 y escaneo sin PAN/CVV verificados | Mantener secretos reales fuera del repositorio y ratificar ADR-013 antes de producción |
| Carga Black Friday 8x | Sí, EaC-03 | No ejecutada | Línea base `T`, escenario `8T`, p50/p95/p99 y resultados versionados |

## 4. Gate obligatorio para trabajo nuevo

Antes de mover una tarjeta a `Done`, responder y enlazar evidencia para cada punto:

- [ ] ¿Qué CUN/CDU y driver justifican el cambio?
- [ ] ¿Qué entidad, atributo o relación del ER se crea o modifica?
- [ ] ¿Qué nodo, artefacto o integración del despliegue lo ejecuta?
- [ ] ¿Qué clase, endpoint, consumidor o pantalla lo implementa?
- [ ] ¿Qué prueba reproduce el camino feliz?
- [ ] ¿Qué prueba reproduce al menos una excepción relevante?
- [ ] ¿La migración parte de una base vacía y preserva una base existente?
- [ ] ¿DDA, ER, despliegue, contrato y código usan los mismos nombres y estados?
- [ ] ¿La evidencia evita afirmar métricas o integraciones no medidas?

Si cualquier respuesta es “no aplica”, debe existir una justificación explícita revisada por QA. Si falta CDU/CUN, ER o despliegue, la tarjeta no puede declararse con trazabilidad completa.

## 5. Evidencia mínima para la entrega

- `./mvnw clean verify` verde y conteo de pruebas.
- `docker compose ps` con todos los servicios saludables.
- Camino feliz completo automatizado: login → catálogo/carrito → reserva → pago → fiscal → confirmación → despacho.
- Falla PayKlik con orden conservada e intento indeterminado.
- Falla SAT con orden en `FISCAL_PENDING`.
- Timeout LogiExpress con reintentos 2/8/32 y mensaje fallido.
- POS con deduplicación, reintento y acuse.
- Notificación fallida sin bloquear la operación principal.
- Consulta y exportación de evidencia con actor, instante, cambio y correlación.
- Captura inicial/final del tablero y bitácora actualizada.

La evidencia automatizada vigente de ORD-05/QA-04 se encuentra en
`../evidence/functional/ORD-05-QA-04-cinco-flujos-e2e.md`; utiliza los datos
deterministas de `../../tests/data/ord-05-deterministic-fixtures.json`.
