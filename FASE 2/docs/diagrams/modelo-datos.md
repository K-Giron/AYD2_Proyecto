# Modelo de datos de Fase 2 (MVP)

**Sistema:** SGO-Logic
**Fuente arquitectónica:** `Docs/DDA.md` (§3 CDU, §4 EaC, §5 ADR, §6 Vistas)
**Organización técnica:** `Docs/planificacion/STACK_TECNOLOGICO_FASE_2.md` (§6 y §8)
**Estado:** vivo; se actualiza en la misma rama que la migración que lo modifica.
**Responsable de consolidación:** Jairo Gómez (DOC-01); cada módulo actualiza sus tablas.

---

## 1. Propósito

Este documento cierra el artefacto de **modelo de datos** que exige la tabla de
trazabilidad arquitectónica del curso. Cada capacidad del MVP debe poder
rastrearse en el CDU expandido (CUN), en el modelo de datos, en el diagrama de
despliegue y en el código. Sin este artefacto, una funcionalidad implementada
pierde puntaje aunque su código sea correcto.

Reglas de lectura:

- Un esquema lógico pertenece a un único módulo y solo ese módulo lo escribe.
- Las referencias entre módulos usan `UUID` de correlación, nunca claves
  foráneas hacia tablas privadas de otro módulo.
- Las tablas marcadas **PLANIFICADO** todavía no tienen migración Flyway; su
  forma final se ratifica al implementar la tarjeta indicada.
- Los datos semilla son simulados y no representan inventario real.

---

## 2. Propiedad lógica por esquema

| Esquema lógico | Módulo propietario | Responsable | CUN/CDU principal | EaC | Migraciones |
|---|---|---|---|---|---|
| `inventory` | `inventory` | Jorge Pumay | CUN-02 / CDU-02 | EaC-03, EaC-07 | `V1`, `V2` |
| `fiscal` | `fiscal` | Andy Sánchez | CUN-04 / CDU-04 | EaC-02, EaC-07 | `V2001` |
| `integration` (ERP/POS) | `integration` | Andy Sánchez | CUN-05, CUN-06 | EaC-06, EaC-07 | `V2002`, `V2003` |
| `integration` (Outbox/Inbox) | `integration` | Edson Sosa | Transversal a CDU-03 a CDU-06 y CDU-11 | EaC-04, EaC-07 | `V3001` (BB-04) |
| `logistics` | `logistics` | Kevin Girón | CUN-07, CUN-08 | EaC-05, EaC-07 | `V1001` |
| `notifications` | `notifications` | Kevin Girón | CUN-11 / CDU-11 | EaC-08, EaC-07 | `V1002` |
| `query` | `query` | Kevin Girón | CUN-09 / CDU-09 | EaC-01, EaC-07 | `V1003` |
| `incidents` | `incidents` | Kevin Girón | CUN-10 / CDU-10 | EaC-02, EaC-07 | `V1004` |
| `orders` | `orders` | Jairo Gómez | CUN-01 / CDU-01 | EaC-01, EaC-09 | **PLANIFICADO** (ORD-01/ORD-02) |
| `payments` | `payments` | Edson Sosa | CUN-03 / CDU-03 | EaC-04, EaC-12 | `V3003` (PAY-01) |
| `audit` | `audit` | Edson Sosa | CUN-12 / CDU-12 | EaC-02, EaC-07 | `V3002` (AUD-01) |

La instancia PostgreSQL es una sola; la separación es lógica y verificable por
Spring Modulith, según ADR-001 y la sección 8 del stack.

---

## 3. Esquemas implementados

### 3.1 `inventory` — CUN-02 (INV-01 a INV-05)

```mermaid
erDiagram
    inventory_locations ||--o{ inventory_items : "tiene stock"
    inventory_locations ||--o{ inventory_reservations : "origen"
    inventory_locations {
        uuid id PK
        varchar code UK
        varchar name
        varchar type "DISTRIBUTION_CENTER | STORE"
    }
    inventory_items {
        uuid id PK
        uuid product_id
        uuid location_id FK
        int quantity_on_hand
        int quantity_reserved "CHECK reserved <= on_hand"
        bigint version
    }
    inventory_reservations {
        uuid id PK
        uuid order_id "referencia a orders, sin FK"
        uuid product_id
        uuid location_id FK
        int quantity "CHECK > 0"
        varchar status "ACTIVE | CONFIRMED | RELEASED | EXPIRED"
        varchar idempotency_key UK
        timestamptz expires_at
        bigint version
    }
```

- 34 ubicaciones semilla (2 CD + 32 tiendas) y stock simulado en `V2`.
- `tryReserve`/`tryRelease` son sentencias `UPDATE ... WHERE` atómicas; los
  `CHECK` de la tabla son la red de seguridad de negocio.

### 3.2 `fiscal` — CUN-04 (FIS-01/FIS-02)

```mermaid
erDiagram
    fiscal_documents ||--o{ fiscal_documents : "nota de credito referencia original"
    fiscal_documents {
        uuid id PK
        uuid order_id "sin FK a orders"
        uuid payment_attempt_id "sin FK a payments"
        varchar document_type "INVOICE | CREDIT_NOTE"
        uuid original_document_id FK
        varchar tax_identifier
        varchar customer_name
        numeric total "CHECK > 0"
        varchar currency
        varchar cui UK
        varchar series
        text xml_payload
        varchar status "FISCAL_PENDING | APPROVED | REJECTED"
        varchar sat_authorization
        varchar idempotency_key UK
        int submission_attempts
        timestamptz confirmed_at
        bigint version
    }
```

- La compuerta de confirmación es `status = APPROVED`; un timeout mantiene
  `FISCAL_PENDING` (ADR-007, ADR-010).

### 3.3 `integration` ERP y POS — CUN-05 y CUN-06 (ERP-01/02, POS-01)

```mermaid
erDiagram
    erp_batches ||--o{ erp_sales : "incluye"
    erp_batches {
        uuid id PK
        varchar format "CSV | XML"
        text payload
        varchar status
        int sale_count
        int attempts
        varchar acknowledgement
        timestamptz delivered_at
        bigint version
    }
    erp_sales {
        uuid id PK
        uuid order_id UK "sin FK a orders"
        numeric net_total
        numeric tax_total
        numeric grand_total "CHECK no negativos; neto+impuesto se valida en el servicio"
        varchar currency
        varchar status
        uuid batch_id FK
    }
    pos_deliveries {
        uuid id PK
        uuid event_id "UK con store_number"
        varchar event_type
        uuid order_id "sin FK a orders"
        int store_number "CHECK 1..32"
        text payload
        varchar status
        int attempts
        varchar acknowledgement
        uuid correlation_id
        bigint version
    }
```

- La deduplicación ERP es por `order_id`; la de POS por `(event_id, store_number)`.
- La entrega durable a POS/ERP después de un reinicio queda pendiente de BB-04.

### 3.4 `logistics` — CUN-07 y CUN-08 (LOG-01/02/03)

```mermaid
erDiagram
    shipments ||--o{ shipment_tracking_events : "registra"
    shipments ||--o{ shipment_dead_letters : "agota reintentos"
    shipments {
        uuid id PK
        uuid order_id "sin FK a orders"
        varchar origin "DISTRIBUTION_CENTER | STORE | PICKUP"
        varchar destination
        varchar status
        varchar tracking_number
        varchar idempotency_key UK
        int label_attempts
        bigint version
    }
    shipment_tracking_events {
        uuid id PK
        uuid shipment_id FK
        varchar status
        varchar source
        timestamptz occurred_at
    }
    shipment_dead_letters {
        uuid id PK
        uuid shipment_id FK
        varchar reason
        int attempts
        timestamptz registered_at
    }
```

- `shipment_dead_letters` es una cola local explícitamente temporal mientras
  BB-04 no existe; debe migrar a la DLQ del broker.

### 3.5 `notifications`, `query` e `incidents` — CUN-09, CUN-10 y CUN-11

```mermaid
erDiagram
    notification_requests {
        uuid id PK
        varchar event_type
        varchar channel
        varchar recipient
        uuid source_aggregate_id "orderId o shipmentId"
        varchar status
        int attempts
        timestamptz sent_at
    }
    order_summary_projections {
        uuid order_id PK
        varchar order_status
        timestamptz order_status_as_of
        varchar payment_status
        timestamptz payment_status_as_of
        varchar dispatch_status
        timestamptz dispatch_status_as_of
        varchar delivery_status
        timestamptz delivery_status_as_of
        varchar incident_status
        timestamptz incident_status_as_of
    }
    incidents ||--o{ incident_items : "detalla"
    incidents {
        uuid id PK
        uuid order_id "sin FK a orders"
        varchar type "RETURN | INCIDENT"
        varchar reason
        boolean partial
        varchar status
        numeric refund_amount
        timestamptz resolved_at
    }
    incident_items {
        uuid incident_id FK
        varchar sku
        int quantity
    }
```

- `order_summary_projections` materializa CDU-09 E1: cada campo expone `asOf`
  para mostrar la última información conocida sin fallar la consulta completa.
- Su alimentación actual por `POST /api/v1/query/order-summary` es temporal;
  el consumidor de Outbox la reemplaza cuando BB-04 exista.

### 3.6 `integration` Outbox/Inbox — transversal (BB-04)

```mermaid
erDiagram
    outbox_event {
        uuid event_id PK
        varchar aggregate_type
        uuid aggregate_id
        varchar event_type
        int event_version "CHECK > 0"
        text payload
        uuid correlation_id
        uuid causation_id
        varchar status "PENDING | PUBLISHED | FAILED"
        int attempts
        timestamptz next_attempt_at
        timestamptz published_at
        bigint version
    }
    inbox_event {
        uuid id PK
        varchar consumer
        uuid event_id "UK con consumer"
        timestamptz processed_at
    }
```

- `outbox_event` se escribe en la **misma transacción** que el cambio de
  negocio (ADR-002, `PublishEvent` exige transacción activa); el dispatcher
  publica después al exchange `sgo.events`.
- Reintentos 2/8/32 s y estado `FAILED` tras agotar los intentos; el lote se
  reclama con `FOR UPDATE SKIP LOCKED` para que varios dispatchers no
  dupliquen publicaciones.
- `inbox_event` con `UNIQUE (consumer, event_id)` da idempotencia al
  consumidor mediante `INSERT ... ON CONFLICT DO NOTHING`.
- Es la precondición explícita de CDU-06 y el cierre de la ventana de pérdida
  descrita en ADR-010.

### 3.7 `audit` — CUN-12 (AUD-01/AUD-02)

```mermaid
erDiagram
    audit_events {
        uuid id PK
        uuid event_id UK "idempotencia del registro"
        varchar aggregate_type
        uuid aggregate_id
        uuid order_id "correlacion de la orden"
        varchar actor
        varchar action
        varchar result
        text before_state
        text after_state
        uuid correlation_id
        uuid causation_id
        timestamptz occurred_at
    }
```

- Tabla de solo anexado: el repositorio extiende `Repository` y no expone
  actualización ni borrado.
- El registro se escribe en la misma transacción del cambio de negocio
  (`AuditRecorder` exige transacción activa); el actor sale del contexto
  autenticado o es `system`.
- Índices por `order_id`, por `(aggregate_type, aggregate_id)` y por
  `correlation_id` para responder en menos de 1 s (EaC-02).
- CDU-12 exige además registrar el acceso a la evidencia, la búsqueda fallida
  con alerta y la exportación autorizada: AUD-02 lo cubre con
  `AUDIT_EVIDENCE_ACCESSED`, `AUDIT_SEARCH_FAILED` y `AUDIT_EVIDENCE_EXPORTED`,
  escritos en transacción independiente para sobrevivir a la búsqueda fallida.

### 3.8 `payments` — CUN-03 (PAY-01)

```mermaid
erDiagram
    payment_attempts {
        uuid id PK
        uuid order_id "sin FK a orders"
        varchar channel "WEB | MOBILE | STORE | CONTACT_CENTER"
        varchar method "CARD | TRANSFER | DIGITAL_WALLET"
        numeric amount "CHECK > 0"
        varchar currency
        varchar status "PENDING | APPROVED | REJECTED | INDETERMINATE"
        varchar provider_reference
        uuid provider_attempt_id
        varchar failure_code
        varchar failure_message
        varchar idempotency_key UK
        uuid correlation_id
        timestamptz created_at
        bigint version
    }
```

- Cubre CDU-03 paso 2 (intento con orden, canal y fecha), paso 3 (idempotencia
  sin PAN/CVV), el resultado aprobado/rechazado y la postcondición de registro
  auditable; un timeout deja `PENDING` y nunca se reporta como aprobación.
- `provider_attempt_id` conserva el identificador del intento que devuelve
  PayKlik (BB-06) para trazabilidad, además de `provider_reference`.
- No almacena PAN completo ni CVV (EaC-12).
- La conciliación (`payment_reconciliations`) pertenece a PAY-03 y no existe
  todavía.

---

## 4. Esquemas planificados

> Estas tablas **no existen todavía**. Se documentan para que la
> implementación de Jairo (ORD-01/ORD-02) y de Edson (PAY-02/PAY-03) no se
> convierta en código huérfano. Su forma final se ratifica en el ADR o
> contrato correspondiente antes de la migración.

### 4.1 `orders` — CUN-01 (ORD-01/ORD-02, Jairo Gómez)

```mermaid
erDiagram
    carts ||--o{ cart_items : "contiene"
    carts ||--o| orders : "se convierte"
    orders ||--o{ order_state_transitions : "audita"
    carts {
        uuid id PK
        uuid customer_id
        varchar channel
        varchar status "CART | CONVERTED | ABANDONED"
        timestamptz updated_at
    }
    cart_items {
        uuid cart_id FK
        uuid product_id
        int quantity
    }
    orders {
        uuid id PK
        uuid cart_id FK
        uuid customer_id
        varchar status "CART..RETURNED"
        numeric total
        varchar currency
        bigint version
    }
    order_state_transitions {
        uuid id PK
        uuid order_id FK
        varchar from_status
        varchar to_status
        varchar actor
        timestamptz occurred_at
    }
```

Estados según `docs/contracts/orders/order-states.md` (propuesta `PROPOSED`).

### 4.2 `payments` conciliación — CUN-03 (PAY-03, Edson Sosa)

```mermaid
erDiagram
    payment_attempts ||--o{ payment_reconciliations : "se concilia"
    payment_reconciliations {
        uuid id PK
        uuid payment_attempt_id FK
        timestamptz scheduled_at
        varchar status
        varchar result
        int attempts
    }
```

Cubre CDU-03 E1 (conciliación antes de reintentar) y el criterio de PAY-03
(primer trabajo ≤ 60 s sin doble cobro). `payment_attempts` ya existe en
`V3003` (§3.8); esta tabla se creará en PAY-03.

---

## 5. Reglas de integridad y trazabilidad

1. Ningún módulo usa repositorios, entidades ni tablas de otro módulo; la
   correlación es por `UUID` (`order_id`, `product_id`, `event_id`).
2. Toda operación reintentable tiene clave de idempotencia única en su tabla.
3. Todo cambio de estado relevante debe producir un registro auditable
   (CUN-12) y, cuando corresponda, un evento Outbox.
4. Los `CHECK` de base de datos refuerzan las invariantes de negocio:
   `quantity_reserved <= quantity_on_hand`, `store_number BETWEEN 1 AND 32`,
   `total > 0` y totales ERP no negativos. La igualdad
   `net_total + tax_total = grand_total` se valida en el servicio antes de
   insertar.
5. No se almacenan PAN completo, CVV, secretos ni credenciales.

## 6. Referencias

- Migraciones: `backend/src/main/resources/db/migration/`
- Contratos y estados: `docs/contracts/`
- Matriz EaC: `docs/qa/QA-03-matriz-eac.md`
- Trazabilidad CUN → código → datos → despliegue: `docs/qa/trazabilidad-cun-mvp.md`
- Vistas arquitectónicas de Fase 1: `Docs/DDA.md` §6 y `Docs/diagramas/`
