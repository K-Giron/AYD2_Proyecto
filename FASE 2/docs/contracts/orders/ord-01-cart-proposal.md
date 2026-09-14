# ORD-01 — Diseño propuesto del carrito omnicanal

**Estado:** `PROPOSED`

**Implementación:** no iniciada

**Dependencias:** BB-03 y contratos aprobados

## Objetivo

Definir los casos que deberá soportar un carrito recuperable entre canales sin fijar todavía entidades, tablas, endpoints ni detalles internos de Inventario.

## Responsabilidad de Orders

- Mantener la identidad del carrito y su asociación con el cliente.
- Incorporar, modificar y retirar líneas.
- Recuperar el mismo carrito desde un canal distinto.
- Calcular una vista preliminar de cantidades e importes con datos confirmados por contratos futuros.
- Conservar correlation ID e idempotency key cuando la operación lo requiera.

Orders no puede reservar stock, consultar tablas de Inventario ni decidir el origen de despacho.

## Información conceptual mínima

| Dato | Regla propuesta |
|---|---|
| `cartId` | UUID estable |
| `customerId` | UUID requerido para recuperación omnicanal |
| `channel` | Identificador de canal; catálogo pendiente de aprobación |
| `items` | Colección de producto, cantidad y datos comerciales necesarios |
| `updatedAt` | Instante UTC ISO 8601 |
| `version` | Control de concurrencia; mecanismo definitivo depende de BB-03 |

## Casos de aceptación que deben convertirse en pruebas

1. Un cliente crea un carrito en un canal y lo recupera desde otro usando la misma identidad.
2. Agregar la misma línea respeta la política de acumulación acordada.
3. Una cantidad inválida devuelve el error común y no modifica el carrito.
4. Dos actualizaciones concurrentes no pierden cambios silenciosamente.
5. Repetir una operación con la misma `Idempotency-Key` no duplica el efecto.
6. La respuesta no contiene credenciales, información interna ni datos privados de otros módulos.
7. Toda respuesta incluye `X-Correlation-Id`.

## Decisiones pendientes

- Catálogo de canales y reglas de carrito invitado.
- Fuente y vigencia de precios.
- Política de combinación de líneas.
- Expiración del carrito.
- Control de concurrencia definido junto con BB-03.
- Contrato público para disponibilidad informativa de Inventario.

No se implementará ORD-01 hasta resolver estas decisiones y disponer del modelo relacional de Jorge.
