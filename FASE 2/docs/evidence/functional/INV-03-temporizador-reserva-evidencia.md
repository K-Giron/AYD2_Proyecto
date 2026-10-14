# INV-03 — Evidencia en vivo del temporizador de reservas (2026-10-14)

**Responsable:** Jorge Pumay · **Criterio de la hoja:** «Liberar inventario»,
verificable en código: temporizador configurable que libera la reserva.
**Código:** `InventoryExpirationService` (barrido) y `InventoryReservationService`
(`expiresAt = createdAt + reservation-ttl`). **ADR:** ADR-011 y ADR-014.

## Configuración usada

La plataforma completa (12 contenedores) se levantó con `docker compose` y el
backend recibió estas variables, que `docker-compose.yml` mapea a
`sgo-logic.inventory.*`:

| Variable | Valor de la prueba | Valor por defecto |
|---|---|---|
| `SGO_INVENTORY_RESERVATION_TTL` | `PT2M` (2 minutos) | `PT15M` |
| `SGO_INVENTORY_EXPIRATION_SWEEP_INTERVAL` | `PT10S` | `PT1M` |

Comprobado dentro del contenedor: `docker exec <backend> printenv
SGO_INVENTORY_RESERVATION_TTL SGO_INVENTORY_EXPIRATION_SWEEP_INTERVAL` →
`PT2M` y `PT10S`.

## Procedimiento y resultado

Producto `11111111-1111-1111-1111-111111111101` (Camisa manga larga azul).
Sesión de cliente (`cliente@megastore.com`). Horas en UTC.

| Hora | Acción | Resultado |
|---|---|---|
| 06:25:41 | `GET /api/v1/inventory/stock?productId=…` | `CD-01`: en mano 5, reservado 0; `ST-01`: en mano 1, reservado 0 |
| 06:25:48 | `POST /api/v1/inventory/reservations` (2 unidades, sin `locationId`) | `201`, `status: ACTIVE`, origen `CD-01`, `expiresAt: 06:27:48` |
| 06:25:48 | `GET /api/v1/inventory/stock` | `CD-01`: en mano 5, **reservado 2** |
| 06:27:55 | El barrido marca la reserva (`updatedAt: 06:27:55`) | `status: EXPIRED` |
| 06:27:58 | `GET /api/v1/inventory/stock` | `CD-01`: en mano 5, **reservado 0** |

- `expiresAt` coincide exactamente con 2 minutos después de `createdAt`, así
  que el TTL configurable se aplica.
- La reserva quedó `EXPIRED` 7 segundos después de vencer, dentro del
  intervalo de barrido de 10 s.
- La existencia en mano no cambió (5): solo se liberó lo reservado.
- Sin `locationId` se aplicó el algoritmo de origen de ADR-014: se eligió el
  centro de distribución `CD-01` y no la tienda `ST-01` (que solo tenía 1
  unidad, menos de las 2 pedidas).

## Pruebas automatizadas que respaldan el comportamiento

- `InventoryExpirationServiceTest`: liberación de reservas vencidas.
- `InventoryReservationServiceTest`: cálculo de `expiresAt` y algoritmo de origen.
- `InventoryConcurrencyIntegrationTest`: dos solicitudes por la última unidad
  producen una sola reserva.

## Cómo repetirlo

```bash
# En FASE 2, con SGO_INVENTORY_RESERVATION_TTL=PT2M y
# SGO_INVENTORY_EXPIRATION_SWEEP_INTERVAL=PT10S en el entorno del backend:
docker compose up -d backend
# 1) iniciar sesión como cliente y copiar accessToken
# 2) crear la reserva (orderId nuevo, Idempotency-Key único)
# 3) esperar ~2 min 10 s y consultar GET /api/v1/inventory/reservations/{id}
```
