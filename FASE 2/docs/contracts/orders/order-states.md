# Estados de orden v1

**Estado del contrato:** `PROPOSED`  
**Decisión relacionada:** ADR-005 de la Fase 1

## Propósito

Esta propuesta establece el vocabulario preliminar del ciclo de vida de una orden:

- `CART`
- `RESERVED`
- `PAYMENT_PENDING`
- `PAID`
- `FISCAL_PENDING`
- `CONFIRMED`
- `PREPARING`
- `DISPATCHED`
- `DELIVERED`
- `CANCELLED`
- `RETURNED`

La lista traduce a identificadores técnicos los estados descritos en ADR-005. Las transiciones, precondiciones, responsables, estados terminales y compensaciones necesitan aprobación del equipo antes de congelar el contrato.

Este documento no implementa ni autoriza todavía una máquina de estados. ORD-02 continúa pendiente y depende, además, de AUD-01 mínimo.
