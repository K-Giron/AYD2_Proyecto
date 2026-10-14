# Contrato interno de crédito propio v1

Este contrato es la frontera pública `credit :: api` consumida por `orders`.
Las entidades y repositorios de `credit` no forman parte del contrato.

## Evaluar opción

```text
evaluate(customerId, amount, currency, correlationId)
 -> decisionId, eligible, approvedLimit, ruleVersion,
    publicReasonCode, evaluatedAt
```

- `amount > 0`; `currency` usa tres letras y se normaliza a mayúsculas.
- Persiste una decisión incluso cuando el resultado es no elegible.
- Sin perfil o historial suficiente devuelve no elegible.
- El consumidor público no expone las métricas históricas.

## Solicitar concesión

```text
requestCredit(orderId, customerId, amount, currency,
              correlationId, idempotencyKey)
 -> creditOperationId, decisionId, GRANTED|REJECTED,
    approvedLimit, dueDate, ruleVersion, publicReasonCode, createdAt
```

- Idempotencia por `orderId` y `idempotencyKey`.
- Reusar una llave con orden, cliente, monto o moneda diferentes es inválido.
- `GRANTED` exige `dueDate`; `REJECTED` la omite y no representa deuda.
- El límite es una línea disponible acumulada, no un límite nuevo por carrito.
  Cada `GRANTED` descuenta atómicamente `amount` de `availableCredit`; las
  compras posteriores se evalúan contra el remanente.
- Las concesiones del mismo cliente se serializan. Dos órdenes concurrentes no
  pueden consumir juntas más línea que la disponible.
- `latestForOrder(orderId)` permite reanudar la Saga.

## Eventos Outbox v1

| Routing key | Aggregate type | Uso |
|---|---|---|
| `CREDIT_DECISION_RECORDED` | `CREDIT_DECISION` | Auditoría de evaluación |
| `CREDIT_GRANTED` | `CREDIT_OPERATION` | Concesión y notificación |
| `CREDIT_REJECTED` | `CREDIT_OPERATION` | Rechazo y notificación |

Payload permitido para operación: `orderId`, `customerId`,
`creditOperationId`, `decisionId`, `status`, `amount`, `currency`,
`approvedLimit`, `remainingAvailableCredit`, `dueDate`, `ruleVersion` y
`reasonCode`. El envelope común aporta `eventId`, `correlationId`, versión y
fecha. No se publican historial completo, credenciales ni datos de tarjeta.

## Razones públicas v1

- `ELIGIBLE`
- `INSUFFICIENT_HISTORY`
- `OVERDUE_CREDIT`
- `LATE_PAYMENT_HISTORY`
- `HIGH_RETURN_RATE`
- `CRITICAL_INCIDENTS`
- `INSUFFICIENT_LIMIT`

Estas razones son códigos estables; el texto presentado al usuario debe ser
genérico y no permitir inferir el perfil completo.

## Compatibilidad fiscal

La referencia canónica es:

```text
financialOperationId + financialOperationType(PAYKLIK|OWN_CREDIT)
```

Durante la transición, `paymentAttemptId` se acepta únicamente como alias de
una operación `PAYKLIK`. Crédito nunca crea un intento PayKlik ficticio.

## Alcance de cartera

Este contrato registra la concesión y consume la línea disponible. El ciclo de
abonos, liquidación, mora, devolución y restitución de línea requiere un
contrato de cartera adicional; no debe simularse aumentando el saldo desde la
interfaz de compra.
