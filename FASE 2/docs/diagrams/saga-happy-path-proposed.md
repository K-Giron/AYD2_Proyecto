# Saga orquestada — camino feliz propuesto

**Estado:** `PROPOSED`

**Tarjeta relacionada:** ORD-03

**Código ejecutable:** no implementado

```mermaid
sequenceDiagram
    autonumber
    actor Client as Cliente/Canal
    participant Orders as Orders / Orquestador
    participant Inventory as Inventory
    participant Payments as Payments
    participant Fiscal as Fiscal
    participant Logistics as Logistics

    Client->>Orders: Solicitar confirmación de compra
    Orders->>Inventory: Reservar inventario
    Inventory-->>Orders: Reserva confirmada
    Orders->>Payments: Solicitar autorización
    Payments-->>Orders: Pago aprobado
    Orders->>Fiscal: Solicitar documento fiscal
    Fiscal-->>Orders: Confirmación fiscal
    Orders->>Logistics: Solicitar despacho
    Logistics-->>Orders: Despacho aceptado
    Orders-->>Client: Orden confirmada
```

## Condiciones del diagrama

- Representa secuencia lógica, no protocolo síncrono definitivo.
- La confirmación fiscal condiciona el avance desde `FISCAL_PENDING`.
- Cada interacción conserva correlation ID, causation ID e idempotencia acordada.
- Los participantes responden mediante APIs públicas o eventos; nunca mediante acceso a tablas.
- Outbox, Inbox, retries y DLQ pertenecen a BB-04 y no se implementan aquí.
- Las ramas de error y compensación se definirán con ORD-04 cuando existan INV-02 y PAY-02.
