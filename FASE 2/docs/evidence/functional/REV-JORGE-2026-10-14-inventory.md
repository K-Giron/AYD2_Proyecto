# REV-JORGE — Revisión de los cambios en `inventory` (2026-10-14)

**Revisor:** Jorge Pumay (dueño del módulo `inventory`, ADR-011)
**Alcance:** lo que Kevin Girón agregó al módulo `inventory` en el cierre de
Fase 2 para la tienda online (ADR-015, puntos 17 y 18).
**Evidencia:** lectura estática del código, plataforma completa levantada con
`docker compose` (12 contenedores) y las pruebas automatizadas del módulo.

## 1. Qué se revisó

| Elemento | Dónde | Veredicto |
|---|---|---|
| Ubicación `CD-WEB` (almacén de la tienda online) | `V1010__onlinestore_create_tables.sql` | Correcta |
| Kárdex de una ubicación | `InventoryLocationActivityService` | Correcto, solo lectura |
| Entrada/salida por código de ubicación | `InventoryMovementService.receiveAtLocation` / `issueFromLocation` | Correcto, con una observación (1) |
| Temporizador configurable de reservas | `application.yml` + `docker-compose.yml` | Correcto; verificado en vivo (INV-03) |

## 2. Lo que está bien

- **Atomicidad.** Entradas y salidas usan `UPDATE` condicionales
  (`tryIncreaseOnHand`, `tryDecreaseOnHand`). La salida exige
  `quantityOnHand - quantityReserved >= cantidad`, de modo que **nunca saca
  unidades reservadas** por una orden en curso.
- **Unicidad.** `uq_inventory_items_product_location (product_id,
  location_id)` impide duplicados aunque dos recepciones coincidan.
- **El algoritmo de reserva no cambió.** `ORIGIN_PRIORITY` (centro de
  distribución antes que tienda, por código) y el `UPDATE` atómico por intento
  siguen intactos (ADR-014).
- **Expiración sin pérdidas.** `InventoryExpirationService` usa `saveAndFlush`
  por reserva: sin eso, el `clearAutomatically` del siguiente `tryRelease`
  descartaría cambios sin volcar (hay prueba que lo cubre).
- **Acceso.** En la plataforma levantada: `CD-WEB` aparece entre las 35
  ubicaciones (34 sembradas + `CD-WEB`); el kárdex de la tienda online responde
  `401` sin token y `403` a un cliente.
- **Temporizador.** Con `PT2M`/`PT10S` la reserva venció a los 2 minutos y se
  marcó `EXPIRED` 7 s después; lo reservado volvió a 0
  (`INV-03-temporizador-reserva-evidencia.md`).

## 3. Observaciones (ninguna bloquea la entrega)

1. **Carrera en la primera recepción (baja).** `receiveAtLocation` verifica que
   el ítem no exista y luego lo inserta. Si dos recepciones del mismo producto
   llegan a la vez a una ubicación nueva, la segunda choca con la restricción
   única y falla; no corrompe datos y el folio se puede reintentar (la
   confirmación está protegida contra doble clic). Una mejora futura sería un
   `INSERT … ON CONFLICT DO NOTHING`.
2. **`CD-WEB` es de tipo `DISTRIBUTION_CENTER`.** Para el algoritmo genérico
   es un candidato más, ordenado por código tras `CD-01` y `CD-02`. Hoy la web
   solo vende desde `CD-WEB` porque los productos publicados únicamente tienen
   existencia ahí; si un mismo producto tuviera stock también en `CD-01`, una
   compra web podría reservarse allí. Es una limitación de datos, no un
   defecto: conviene que el catálogo web se mantenga con productos exclusivos
   de `CD-WEB`, o que la Saga pase `locationId` explícito en una fase futura.
3. **Reserva expirada sin fila afectada.** Si `tryRelease` no afecta filas, la
   reserva se marca `EXPIRED` igual y solo se registra una advertencia en el
   log. Es el comportamiento deseado (no dejar la reserva activa), pero
   indicaría una inconsistencia del ítem que alguien debería revisar.
4. **Kárdex acotado a 200 movimientos y 200 reservas** por ubicación
   (`findTop200…`). Suficiente para la consola; no sirve como histórico
   completo.

## 4. Conclusión

Los cambios respetan las invariantes de `inventory` (reserva atómica, sin
sobreventa, liberación por temporizador) y no rompen el contrato que consume la
Saga. Se **aprueba** la integración. Queda pendiente que el ADR-011 pase de
`Proposed` a `Accepted` con la aprobación de tres integrantes.
