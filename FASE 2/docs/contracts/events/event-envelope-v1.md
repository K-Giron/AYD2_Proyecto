# Sobre común de eventos v1

**Estado del contrato:** `PROPOSED`

## Campos propuestos

| Campo | Tipo propuesto | Propósito |
|---|---|---|
| `eventId` | UUID | Identidad única del evento. |
| `eventType` | String | Nombre estable del hecho ocurrido. |
| `eventVersion` | Integer positivo | Versión del contrato del evento. |
| `occurredAt` | Instant ISO 8601 UTC | Momento en que ocurrió el hecho. |
| `correlationId` | UUID | Correlación del flujo completo. |
| `causationId` | UUID o `null` | Evento o comando que causó el hecho. |
| `aggregateType` | String | Tipo de agregado propietario. |
| `aggregateId` | UUID | Identidad del agregado. |
| `payload` | Object JSON | Datos versionados específicos del evento. |

## Ejemplo no contractual

```json
{
  "eventId": "00000000-0000-0000-0000-000000000001",
  "eventType": "ExampleOccurred",
  "eventVersion": 1,
  "occurredAt": "2026-09-14T18:00:00Z",
  "correlationId": "00000000-0000-0000-0000-000000000002",
  "causationId": null,
  "aggregateType": "Example",
  "aggregateId": "00000000-0000-0000-0000-000000000003",
  "payload": {}
}
```

La semántica, nulabilidad, compatibilidad y validación definitiva requieren revisión de productores, consumidores y del arquitecto integrador. No se configura todavía exchange, colas, Outbox, Inbox, retries ni DLQ.
