# ADR-014 — Algoritmo explicable de selección de origen de despacho

- **Fecha:** 2026-09-23
- **Estado:** Proposed
- **Responsable del borrador:** Jorge Pumay
- **Aprobación pendiente:** Jairo Gómez, Edson Sosa, Andy Sánchez y Kevin Girón
- **Resuelve:** INV-04

## Contexto

`saga-required-ports.md` (ORD-03) describe el puerto "Reserva de inventario" con entrada `orderId`, líneas, correlation ID e idempotency key — **sin `locationId`**: el llamador (Orders/Saga) no decide desde qué ubicación se despacha, eso lo decide Inventory. El diseño de INV-02, sin embargo, exigía `locationId` explícito en la solicitud, lo cual no coincide con ese contrato. INV-04 cierra esa brecha: cuando el llamador no indica ubicación, Inventory debe elegir una automáticamente entre las que tienen stock suficiente.

El DDA (CDU-02, A1) ya contempla esto: "No hay existencia en la ubicación preferida: el sistema evalúa otra ubicación (tienda o centro de distribución) antes de declarar indisponibilidad." El DDA no define un criterio de optimización (no hay coordenadas, costos de envío ni tarifas por ubicación — §2.6 deja abierta la fuente y el mecanismo de actualización de inventario), así que cualquier criterio de prioridad geográfica sería inventado. Este ADR fija un criterio simple, determinista y documentado en su lugar.

## Drivers relacionados

- CDU-02 (A1): evaluar otra ubicación si la preferida no tiene stock.
- CUN-07 / CDU-07: el sistema determina las opciones de despacho disponibles.
- EaC-03: la selección debe seguir siendo rápida y no degradar EaC-01/EaC-02 bajo carga.
- ADR-011: Inventory como fuente lógica autoritativa.

## Decisión propuesta

1. **Criterio de prioridad:** entre las ubicaciones con disponibilidad suficiente (`quantityOnHand - quantityReserved >= quantity`) para el producto solicitado, se prueban en este orden:
   - Primero los centros de distribución (`DISTRIBUTION_CENTER`), después las tiendas (`STORE`). Razón: despachar desde un centro de distribución no reduce el stock de piso disponible para clientes que compran en tienda física.
   - Dentro del mismo tipo, por `code` ascendente (`CD-01` antes que `CD-02`, `ST-01` antes que `ST-02`...). Es un criterio arbitrario pero determinista y reproducible en pruebas; no representa cercanía real al cliente.
2. **Selección sin condición de carrera:** el algoritmo no primero "elige" y luego "reserva" en pasos separados (eso reabriría la ventana de concurrencia que INV-02 cierra). En su lugar, recorre las ubicaciones candidatas en el orden anterior e intenta `InventoryItemRepository#tryReserve` (la misma sentencia `UPDATE ... WHERE` atómica de INV-02) en cada una hasta que una tenga éxito. Si la primera candidata pierde la carrera contra otra solicitud concurrente entre el listado y el intento, el algoritmo simplemente continúa con la siguiente candidata: sigue siendo correcto bajo concurrencia porque cada intento es atómico por sí mismo.
3. **Compatibilidad:** `POST /api/v1/inventory/reservations` acepta `locationId` opcional. Si se envía, se reserva exactamente ahí (comportamiento de INV-02, útil para el canal tienda que ya conoce su propia ubicación). Si se omite, se aplica este algoritmo.

## Alternativas evaluadas

1. **Prioridad CD → tienda, por código (propuesta):** simple, explicable, no depende de datos que el DDA no define. No optimiza distancia ni costo real.
2. **Prioridad por mayor stock disponible:** reduce fragmentación del inventario, pero no está pedida por el DDA y puede vaciar primero las tiendas con más stock, afectando ventas de piso; se descarta por no aportar valor claro sobre la opción 1 dentro del alcance del MVP.
3. **Selección aleatoria entre candidatas:** simple de implementar, pero no es "explicable" (viola directamente el criterio de aceptación de INV-04) y dificulta reproducir pruebas. Descartada.
4. **Elegir primero y reservar después en dos pasos:** más fácil de leer, pero reintroduce una ventana de condición de carrera entre "decidir" y "comprometer" que INV-02 específicamente evita. Descartada.

## Consecuencias

- El puerto de reserva ahora coincide con el contrato de `saga-required-ports.md`: la Saga puede pedir "producto X, cantidad N" sin conocer ubicaciones.
- Sigue siendo posible reservar en una ubicación específica (canal tienda, pruebas, demo dirigida) pasando `locationId`.
- El criterio de prioridad es una decisión de diseño explícita, no una regla de negocio del DDA; si Logística define después datos reales de distancia o costo, este ADR debe actualizarse o superseder-se.

## Riesgos

- Bajo alta contención con muchas ubicaciones candidatas, el algoritmo puede intentar varias sentencias `UPDATE` antes de tener éxito (o de agotar candidatas); no se ha medido su costo bajo `8T` — queda para QA-02.
- Si en el futuro se agregan ubicaciones con el mismo `code`, el desempate por código deja de ser determinista; el `UNIQUE (code)` de la migración V1 lo impide a nivel de base de datos.

## Criterios de ratificación

- Existe una prueba que demuestra que, con solo una ubicación con stock, el algoritmo la elige.
- Existe una prueba que demuestra que, si la primera candidata por prioridad no tiene stock, se intenta la siguiente.
- El endpoint documenta que `locationId` es opcional y describe el criterio en el README.
- Al menos tres integrantes registran su aprobación antes de pasar a `Accepted`.

## Actualización Fase 2 (2026-10-13)

- **El algoritmo no cambió.** `InventoryReservationService` sigue probando las
  ubicaciones con stock en orden CD → tienda y por código (`ORIGIN_PRIORITY`),
  con un `UPDATE` atómico por intento. La Saga toma el origen del despacho de
  la ubicación donde quedó la reserva (`OrderSagaService`:
  `DISTRIBUTION_CENTER` o `STORE` más `originCode`).
- **Decisión de negocio de Fase 2 (ADR-015):** la tienda online tiene su propio
  almacén `CD-WEB`, que se surte por folio desde el almacén central. Como los
  productos publicados en la web solo tienen existencia vendible en `CD-WEB`,
  el algoritmo elige ese origen para las compras web. El resultado es
  explicable y se ve en el despacho ("Centro de distribución · CD-WEB").
- **Destino:** lo elige el cliente en el checkout: su domicilio o un punto de
  retiro activo (almacén central o Tienda Centro POS-01,
  `sgo.integrations.online-store.pickup-points`). Con retiro en tienda,
  LogiExpress lleva el paquete de `CD-WEB` a esa tienda (guía con
  `pickupPoint`) y la tienda lo entrega (CDU-16).
- **Cómo defenderlo:** el criterio "selección óptima según ubicación del stock"
  se cumple porque solo se elige un origen con existencia suficiente, siguiendo
  una prioridad explicable y determinista. Si el equipo decide vender desde
  más ubicaciones, basta con cargar existencia vendible en ellas: el mismo
  algoritmo las considera sin cambiar código.

Evidencia: `InventoryReservationServiceTest`
(`reserveAtBestOriginPrefersDistributionCenterOverStoreRegardlessOfOrder`,
`reserveAtBestOriginFallsBackToNextCandidateWhenPreferredHasNoStock`) y
`OnlineStoreIntegrationTest` (compra web reservada en `CD-WEB`).

## Referencias

- DDA CDU-02 (A1), CUN-07/CDU-07, EaC-03.
- ADR-011 (fuente lógica autoritativa de inventario).
- `docs/contracts/orders/saga-required-ports.md`.
