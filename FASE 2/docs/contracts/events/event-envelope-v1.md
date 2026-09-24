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

La semántica, nulabilidad, compatibilidad y validación definitiva requieren revisión de productores, consumidores y del arquitecto integrador.

## Implementación de referencia (BB-04, 2026-09-23)

El Outbox implementado por Edson Sosa (`integration.outbox`, migración
`V3001`) construye este sobre al despachar cada evento y añade las cabeceras
AMQP `X-Correlation-Id` y `X-Event-Type` con `messageId = eventId`. El
contrato sigue en estado `PROPOSED`: la implementación se hizo contra esta
versión y no la convierte en aceptada. Cualquier cambio incompatible exige
una nueva versión del sobre y la actualización de los consumidores.

La topología publicada es el exchange topic `sgo.events`, con exchange de
mensajes fallidos `sgo.events.dlx` y cola `sgo.events.dlq`. Cada consumidor
declara su propia cola durable enlazada a `sgo.events` y usa
`integration.outbox.IdempotentConsumer` para deduplicar reenvíos.

Nota de serialización: la configuración global
(`spring.jackson.default-property-inclusion: non_null`) omite del JSON los
campos nulos, por ejemplo `causationId` cuando el evento no tiene causa. El
campo se define como nullable en este contrato, así que la ausencia es
equivalente a `null`; si un consumidor exige la clave explícita, debe
solicitar una nueva versión del sobre.
