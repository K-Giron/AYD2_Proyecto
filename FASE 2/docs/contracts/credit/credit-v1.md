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
- Si falta el perfil, se crea idempotentemente con una línea inicial individual
  de Q2,500 antes de evaluar.
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
| `CREDIT_OVERDUE` | `CREDIT_OPERATION` | La obligación venció sin pago |
| `CREDIT_SETTLED` | `CREDIT_OPERATION` | El cliente pagó y se liberó la línea |
| `CREDIT_CANCELLED` | `CREDIT_OPERATION` | La orden se canceló y se liberó la línea |

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

## Ciclo de vida de la obligación

```text
GRANTED --(plazo vencido)--> OVERDUE --(pago)--> SETTLED
GRANTED --(pago)-----------> SETTLED
GRANTED --(orden cancelada)-> CANCELLED
REJECTED  (terminal desde el inicio, sin vencimiento)
```

- **Vencimiento:** un proceso programado (cada hora por defecto) marca como
  `OVERDUE` toda operación `GRANTED` cuyo `dueDate` es anterior al día actual.
  Activa `has_overdue_credit` del cliente, de modo que la política rechaza
  nuevas concesiones con `OVERDUE_CREDIT`.
- **Pago:** `POST /api/v1/credit/operations/{id}/settle` (rol `FINANCE`).
  Devuelve el monto a la línea y recalcula la marca de mora. Es idempotente:
  repetirlo o ejecutarlo en paralelo libera la línea una sola vez. Sobre una
  operación `REJECTED` o `CANCELLED` responde `409`.
- **Anulación:** `credit` escucha `ORDER_STATUS_CHANGED` con `newStatus =
  CANCELLED` (Inbox, cola `sgo.credit.order-status.v1`). Si la orden tenía una
  operación `GRANTED`, la pasa a `CANCELLED` y devuelve la línea. Si no hay
  operación, o ya está pagada, vencida o rechazada, no hace nada.
- **Concurrencia:** todo cambio que toca el saldo se serializa con el mismo
  bloqueo por cliente que usan las concesiones y la reconstrucción del perfil.

## Trazabilidad de la decisión

Cada decisión guarda en `credit_decisions.inputs_snapshot` (JSON) el perfil que
se evaluó, el monto solicitado, la versión de la regla y los umbrales vigentes.
La auditoría de `CREDIT_DECISION_RECORDED` incluye esa misma instantánea. Solo
contiene cifras de negocio; no hay datos personales ni de pago. Las decisiones
anteriores a la migración `V4008` quedan sin instantánea.

## Reglas de elegibilidad (`credit-rules-v1`)

Se evalúan en este orden y la primera que falla fija la razón:

1. Todo cliente obtiene de forma idempotente un perfil con línea inicial propia.
2. Crédito vencido activo: `OVERDUE_CREDIT`.
3. Puntualidad menor al mínimo: `LATE_PAYMENT_HISTORY`.
4. Tasa de devoluciones mayor al máximo: `HIGH_RETURN_RATE`.
5. Incidentes críticos mayores al máximo: `CRITICAL_INCIDENTS`.
6. Monto mayor al límite disponible: `INSUFFICIENT_LIMIT`.

Los umbrales son inclusivos: igualar el mínimo o el máximo aprueba.

| Parámetro | Variable | Valor |
|---|---|---|
| Línea inicial por cliente | `SGO_CREDIT_INITIAL_LIMIT` | 2500.00 |
| Puntualidad mínima | `SGO_CREDIT_MINIMUM_ON_TIME_RATE` | 0.80 |
| Devoluciones máximas | `SGO_CREDIT_MAXIMUM_RETURN_RATE` | 0.30 |
| Incidentes críticos máximos | `SGO_CREDIT_MAXIMUM_CRITICAL_INCIDENTS` | 1 |
| Tope del límite | `SGO_CREDIT_MAXIMUM_LIMIT` | 5000.00 |
| Plazo en días | `SGO_CREDIT_TERM_DAYS` | 30 |
| Ventana de historial (meses) | `SGO_CREDIT_HISTORY_WINDOW_MONTHS` | 12 |
| Fracción de compras que fija el límite | `SGO_CREDIT_LIMIT_RATIO` | 0.40 |

El límite del cliente es `min(0.40 × compras de la ventana, tope)` menos el
crédito vigente (`GRANTED` y `OVERDUE`). Cambiar cualquier valor exige cambiar
`SGO_CREDIT_RULE_VERSION` para que cada decisión indique con qué reglas se tomó.

## Alcance de cartera

El contrato cubre concesión, vencimiento, pago íntegro y anulación. Los abonos
parciales, intereses y recargos por mora quedan fuera de esta versión: el pago
liquida la operación completa y no se simulan aumentando el saldo desde la
interfaz de compra. El historial proveniente del ERP se recibe por el contrato
[credit-history-v1](credit-history-v1.md).
