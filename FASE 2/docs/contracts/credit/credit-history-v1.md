# Contrato de historial crediticio `credit-history-v1`

Cómo el comportamiento real del cliente (que vive en el ERP) llega al módulo
`credit` para decidir elegibilidad. `credit` **no consulta tablas del ERP, de
pagos, de órdenes ni de incidencias**: recibe hechos por este contrato y los
conserva en una proyección local.

- **Propietario:** Jorge Pumay · **Puerto:** `credit :: api · CreditHistoryPort`
- **Entrada HTTP:** `POST /api/v1/credit/history` (rol `FINANCE`).
- **Consumidores del dato:** política de elegibilidad y cálculo del límite.

## Lote

```json
{
  "contractVersion": "credit-history-v1",
  "correlationId": "uuid",
  "entries": [
    {
      "customerId": "uuid",
      "sourceReference": "erp-venta-000123",
      "type": "PURCHASE",
      "occurredAt": "2026-08-14T10:00:00Z",
      "amount": 750.00,
      "onTime": null,
      "critical": false
    }
  ]
}
```

| Campo | Regla |
|---|---|
| `contractVersion` | Debe ser exactamente `credit-history-v1`. |
| `correlationId` | Obligatorio; viaja a la auditoría. |
| `entries` | De 1 a 1000 registros por lote. |
| `sourceReference` | Identifica el hecho en el sistema de origen. Única en todo el sistema, hasta 160 caracteres, no se repite dentro del lote. **Es la llave de idempotencia.** |
| `type` | `PURCHASE`, `PAYMENT`, `RETURN` o `INCIDENT`. |
| `occurredAt` | Instante del hecho; no puede estar en el futuro (tolerancia de 5 minutos). |
| `amount` | No negativo. En `PURCHASE` debe ser mayor que cero. |
| `onTime` | Obligatorio en `PAYMENT` (¿llegó dentro del plazo?). Se ignora en los demás. |
| `critical` | En `INCIDENT`, marca severidad crítica. |

Cualquier violación rechaza **todo** el lote con `400 INVALID_CREDIT_REQUEST` y
no persiste nada.

## Respuesta

```json
{ "received": 30, "accepted": 30, "duplicates": 0, "profilesRebuilt": 5 }
```

## Idempotencia

- Un hecho cuyo `sourceReference` ya existe se cuenta como `duplicates` y **no
  modifica ningún total**. Reenviar el mismo lote es seguro.
- Si un hecho ya recibido llega con datos distintos, prevalece el primero: el
  contrato no admite correcciones; un cambio se registra como un hecho nuevo
  con otra referencia.
- El perfil nunca se acumula: se **reconstruye desde todos los hechos de la
  ventana** para cada cliente con hechos nuevos, así que el orden de llegada no
  altera el resultado.

## Cómo se calcula el perfil

Ventana: los últimos `SGO_CREDIT_HISTORY_WINDOW_MONTHS` meses (12).

| Campo del perfil | Cálculo |
|---|---|
| `completed_purchases` | Cantidad de `PURCHASE` en la ventana. |
| `on_time_payment_rate` | `PAYMENT` con `onTime = true` ÷ `PAYMENT`. Sin pagos: `1.0000`. |
| `return_rate` | `RETURN` ÷ `PURCHASE`, con tope `1.0000`. Sin compras: `0.0000`. |
| `critical_incidents` | `INCIDENT` con `critical = true`. |
| `available_credit` | `min(0.40 × suma de PURCHASE, tope)` menos el crédito vigente (`GRANTED` y `OVERDUE`), sin bajar de cero. |
| `has_overdue_credit` | **No lo toca la ingesta**: lo gobierna el ciclo de vida de las obligaciones propias. |

Un cliente sin ningún hecho no tiene perfil y la política lo rechaza con
`INSUFFICIENT_HISTORY`.

## Datos de demostración

`tests/data/credit-history-demo.csv` y `scripts/load-credit-demo-history.sh`
cargan cinco clientes ficticios con resultados esperados distintos (ver
`tests/data/README.md`).
