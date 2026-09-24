# ORD-02 — Diseño propuesto de la máquina de estados

**Estado:** `PROPOSED`

**Implementación:** no iniciada

**Dependencias:** contrato de estados aprobado y AUD-01 mínimo

## Propósito

Preparar una matriz revisable para ORD-02. La tabla no constituye una máquina ejecutable ni reemplaza la decisión del equipo relacionada con ADR-005.

## Transiciones nominales candidatas

| Desde | Hacia | Condición que deberá demostrarse | Responsable del dato |
|---|---|---|---|
| `CART` | `RESERVED` | Reserva confirmada | Inventory |
| `RESERVED` | `PAYMENT_PENDING` | Solicitud de pago registrada | Orders/Payments, por contrato |
| `PAYMENT_PENDING` | `PAID` | Pago aprobado de forma inequívoca | Payments |
| `PAID` | `FISCAL_PENDING` | Solicitud fiscal persistida | Fiscal |
| `FISCAL_PENDING` | `CONFIRMED` | Confirmación fiscal con evidencia | Fiscal |
| `CONFIRMED` | `PREPARING` | Preparación iniciada | Logistics |
| `PREPARING` | `DISPATCHED` | Guía y despacho confirmados | Logistics |
| `DISPATCHED` | `DELIVERED` | Entrega confirmada | Logistics |
| `DELIVERED` | `RETURNED` | Devolución válida vinculada a la venta | Incidents/Fiscal, por contrato |

## Cancelación pendiente de decisión

La transición a `CANCELLED` no se declara universal. El equipo debe acordar desde qué estados se permite, qué autorización necesita y qué compensaciones corresponden. En particular:

- Antes de pagar puede requerir liberar una reserva.
- Después de pagar puede requerir tratamiento financiero.
- Después de emitir información fiscal puede requerir un documento fiscal relacionado.
- Después de despachar puede corresponder una devolución y no una cancelación.

## Pruebas preparatorias

- Cada transición nominal acepta únicamente evidencia del módulo propietario.
- Saltar pasos devuelve un conflicto con el formato común de error.
- Repetir una transición ya aplicada es idempotente o devuelve un resultado acordado.
- Una transición inválida no modifica estado ni publica un hecho de éxito.
- Cada cambio registra actor, instante, estado anterior, estado nuevo y correlation ID mediante AUD-01.
- Un timeout de SAT conserva `FISCAL_PENDING`.
- Un pago indeterminado no se interpreta como `PAID` ni como rechazo definitivo.

## Decisiones pendientes

- Matriz final de cancelaciones y devoluciones.
- Semántica de pago rechazado o indeterminado sin agregar estados no aprobados.
- Comandos, eventos y códigos de conflicto definitivos.
- Autoridad para ejecutar cada transición.
- Política de concurrencia y versionado persistente.
