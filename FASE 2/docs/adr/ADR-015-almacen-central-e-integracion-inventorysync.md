# ADR-015 — Almacén central (inventory-service) e integración en vivo inventorysync

- **Fecha:** 2026-10-02 (ampliado tres veces el 2026-10-05)
- **Estado:** Proposed
- **Responsable del borrador:** Kevin Girón
- **Aprobación pendiente:** Jairo Gómez, Jorge Pumay, Edson Sosa, Andy Sánchez
- **Resuelve:** petición directa de Kevin (fuera del backlog original) — manejo de inventario a nivel de almacén central/bodega, con existencia propia por punto de venta y trazabilidad de folios.

## Contexto

`inventory-service` es un sistema nuevo, separado del monolito `backend` (`sgo-logic`), con su propia base de datos — un "almacén central" tipo mini-SAP: artículos, existencia, folios de entrada/salida, puntos de venta (cada uno con su propio almacén dedicado) y una bitácora transversal. Nace fuera del backlog original de Fase 2 (no es CUN/CDU del DDA) y hasta ahora se mantuvo **deliberadamente desacoplado** del monolito: dos bases de datos, sin ninguna llamada de red entre los dos sistemas, conviviendo solo en el mismo `docker-compose.yml` por conveniencia de infraestructura.

El 2026-10-02, Kevin pidió registrar la web del monolito (`frontend` + `backend`) como un punto de venta más de `inventory-service` ("Tienda Online"), con una diferencia explícita respecto a las tiendas físicas: su stock debía reflejar **ventas reales** confirmadas en la web, no solo folios manuales. Esto exige, por primera vez, una integración en vivo entre los dos sistemas — una reversa intencional del desacoplamiento original, que este ADR documenta en vez de dejar como un hecho silencioso en el código.

## Drivers relacionados

- Petición directa de Kevin (ver `BITACORA_FASE_2.md`, entradas del 2026-10-01 y 2026-10-02): almacén central con existencia propia por punto de venta, alimentada por folios.
- `STACK_TECNOLOGICO_FASE_2.md` §20: "microservicios" y "bases de datos independientes por integrante" exigen ADR nuevo + aprobación del equipo — este documento es esa aprobación pendiente.
- CUN-01/CDU-01 (DDA): carrito omnicanal — **no** es lo que resuelve este ADR; la sesión del cliente entre web/tienda ya existe en `orders` (Cart + SalesChannel, responsabilidad de Jairo). Este ADR es sobre inventario, no sobre identidad del cliente.

## Decisión

1. **`inventory-service` sigue siendo un sistema aparte**, con su propia base de datos (`inventory-db`), su propio login JWT (roles ALMACEN, FINANZAS, LOGISTICA) y su propia API REST. El monolito no le cede ninguna tabla ni lee su base de datos directamente.
2. **La integración es unidireccional: monolito → inventory-service, nunca al revés.** `inventory-service` no sabe que el monolito existe; no lo llama, no depende de él, y sigue funcionando con tiendas físicas aunque el monolito esté caído.
3. **Disparador:** cuando una orden del monolito transiciona `FISCAL_APPROVED → CONFIRMED` (venta real, ya pagada y fiscalmente aprobada — mismo filtro que ya usa `notifications.OrderStatusNotificationConsumer`), un nuevo módulo (`inventorysync`) consume el evento `ORDER_STATUS_CHANGED` ya publicado al Outbox, vuelve a leer la orden completa (el evento no trae las líneas) y registra en `inventory-service` una salida por cada línea, contra el punto de venta `POS-ONLINE` (`"Tienda Online"`, con su propio almacén `ALM-POS-ONLINE`). *Reemplazado por el punto 14: la venta web ya no sale del almacén central, descuenta la existencia propia de `POS-ONLINE`.*
4. **Resolución de catálogo por nombre, no por ID.** El monolito no tiene ningún campo SKU en sus productos de orden (solo `productId` interno + `productName`); `inventory-service` identifica artículos por `sku`. El único identificador compartido entre los dos catálogos independientes es el **nombre exacto del producto** (coincide hoy porque Kevin sembró ambos catálogos a mano con los mismos nombres). Si un nombre no resuelve a ningún artículo, esa línea va directo a una bandeja de fallidos local — no se pierde silenciosamente, pero tampoco bloquea nada.
5. **Nunca bloquea la venta.** La orden ya quedó `CONFIRMED`/`PREPARING` en el monolito antes de que `inventorysync` actúe; una caída de `inventory-service` jamás revierte ni detiene la Saga. El primer intento se programa después del commit (mismo patrón `afterCommit` que `logistics.ShipmentService`), con reintentos en 2/8/32 s; al agotarlos, la venta queda en la bandeja de fallidos (`inventory_bridge_dead_letters`) para reconciliación manual.
6. **Idempotencia de extremo a extremo:** cada llamada incluye `externalReference = "WEB:" + orderId + ":" + productId`. `inventory-service` (`stock_movements.external_reference`, `UNIQUE`) devuelve el movimiento ya existente en vez de duplicarlo si la misma referencia llega dos veces — necesario porque los reintentos de red, por diseño, pueden repetir la misma llamada.
7. **Cuenta de servicio propia** (`integracion-web@almacencentral.local`, rol LOGISTICA) — no una cuenta humana — para que la bitácora de `inventory-service` distinga una venta real de la web de una acción manual de un logístico.

## Ampliación del 2026-10-05: tienda física y sesión de carrito entre canales

8. *(Retirado por el punto 12: la terminal `pos-tienda` se reemplazó por el sistema `store-service`.)* **Terminal de tienda física** (`FASE 2/pos-tienda/`, contenedor propio): canal `STORE` de CDU-01 ("personal de tienda actuando en representación del cliente"). El equipo inicia sesión con su **propia credencial** (cuenta de servicio `terminal-pos01@megastore.com`, rol SUPPORT); el vendedor solo escribe su usuario, que viaja como `X-Actor` (`seller:<usuario>@POS-01`) y queda en la auditoría de la orden. No se agregó ningún login sin contraseña al backend.
9. **Directorio de clientes en el servidor** (módulo `customers`, nuevo): un `customerId` fijo por usuario. Antes la cuenta y su `customerId` vivían solo en el `localStorage` del navegador, así que la tienda no podía encontrar al cliente y otro dispositivo le daba otro carrito. Ahora la web resuelve el `customerId` en el servidor al iniciar sesión, y si el cliente ya fue registrado en una tienda, se **enlaza** a ese mismo registro. El carrito no cambió: sigue siendo el de `orders` (único ACTIVE por cliente, garantizado por `uq_carts_active_customer`).
10. **Clave de carrito vencida**: si la clave de idempotencia guardada apunta a un carrito ya cerrado (p. ej. se cobró en la tienda), la web y la terminal piden el carrito con una clave nueva y obtienen el vigente — antes la web quedaba mostrando el carrito cerrado.
11. **Venta en tienda descuenta la existencia de esa tienda**, no la del almacén central: la terminal registra el origen de la orden (`inventorysync_order_origins`) antes de cobrar; al confirmarse, `inventorysync` llama `POST /api/v1/pos-stock/sales` de inventory-service (folio `POS-VTA-NNNNNN`, idempotente por `STORE:orderId:productId`). Sin existencia en la tienda = dato, no red → bandeja de fallidos sin reintentar. *(Con el punto 12 la tienda física ya no cobra por el monolito; este camino queda solo para la venta web, punto 14.)*

Limitación conocida (para BB-07, Edson): las cuentas web siguen compartiendo la identidad JWT demo del cliente; por eso no se puede restringir "solo consultar tu propio registro de cliente", y un cliente registrado en la web no puede iniciar sesión con contraseña desde otro dispositivo (la contraseña sigue siendo local). La tienda sí lo encuentra, porque busca en el servidor.

## Segunda ampliación del 2026-10-05: la tienda física como sistema propio y almacén por punto de venta

Kevin revisó la terminal del punto 8 y la encontró insuficiente: un cliente no siempre quiere identificarse (venta normal con NIT o **CF**), la tienda debe manejar su inventario (escanear, ver existencia), ver la bitácora de lo que envía y recibe del servidor, y **cada punto de venta —incluida la tienda online— tiene su propio almacén, que solo se mueve por folios**.

12. **`store-service`** (`FASE 2/store-service/`, puerto 8084, base propia `store-db`): sistema de la tienda física, configurado por entorno para **una** tienda (`STORE_CODE=POS-01`; otra tienda = otro par de contenedores). Registra la venta y el cobro localmente (ticket `TKT-NNNNNN`, NIT validado o CF, efectivo/tarjeta/transferencia, recibido y cambio) y **luego sube la factura al central**. Su catálogo es exactamente lo que el central le envió por folio (`POS-ENT`); vende escaneando el SKU; la existencia baja de forma atómica (nunca negativa). **Vende aunque el central esté caído**: la factura queda `PENDIENTE` y se reintenta cada 30 s; las recepciones se bajan cada 15 s. Todo intento queda en su `sync_log` (SUBIDA/BAJADA). El vendedor solo escribe su usuario. Identificar al cliente es **opcional**: "Traer carrito web" busca su carrito en el monolito (cuenta de equipo del punto 8, ahora usada por `store-service`), lo cruza por nombre con los artículos de la tienda, cobra a precio de tienda y al final cierra ese carrito web.
13. **`inventory-service` recibe a las tiendas** (`/api/v1/store-sync/{posCode}/…`, rol nuevo `TIENDA`; cada cuenta TIENDA está ligada a **su** punto de venta y no puede actuar por otro — `StoreAccountGuard`):
    - `GET receipts?since=`: entradas `POS-ENT` de esa tienda con SKU, nombre y precio.
    - `POST invoices`: factura de tienda, idempotente por ticket; folio central `FT-NNNNNN` (visible en Finanzas); baja la vista central del stock de la tienda (`POS-VTA`). Como la venta **ya ocurrió**, nunca se rechaza por existencia: si la vista central no alcanza, se registra igual y queda `DISCREPANCIA_STOCK_TIENDA` en la bitácora.
    - `POST transfers`: traslado por folio `TRA-NNNNNN`, idempotente por el folio local `TRS-…`. Saca la existencia de la tienda (movimiento `TRASLADO`) y la suma al **central** (entrada `ENT` "Devolución de tienda") o a **otra tienda** (`POS-ENT`, que esa tienda baja en su próxima sincronización). Si la tienda no tiene esa existencia según el central → 409, la tienda lo marca `RECHAZADO` y devuelve la mercadería a su existencia.
14. **Tienda online por etapas** (monolito, `inventorysync`): la venta web descuenta la existencia propia de `POS-ONLINE` (`POST /api/v1/pos-stock/sales`, referencia `WEB:orderId:productId`), ya no crea una salida del almacén central. `POS-ONLINE` solo se surte con folios de salida del central. Nuevo `GET /api/v1/inventory-sync/online-stock` (público, solo lectura, caché de 10 s) que usan el catálogo, el detalle de producto y la portada de la web para mostrar disponibilidad y deshabilitar "Agregar". La reserva de la Saga sigue usando el inventario de Jorge (no se tocó).
16. **Recepción por folio en la tienda** (revisión de Kevin, mismo día): lo que envía el central ya no entra solo a la existencia. La tienda lo baja como **en tránsito**; alguien de la tienda escribe el folio de envío (`SAL-…` del central o `TRA-…` de otra tienda), ve qué artículos y cantidades trae, y **confirma** — solo entonces suma (migración `V2` de `store-service`; confirmación protegida contra doble clic con actualización condicional). El central no necesita confirmar las entradas que le llegan por traslado. Las entradas al central por traslado ahora guardan la tienda de origen y el motivo, y la UI del central se actualiza sola (cada 15 s y al cambiar de pestaña) y permite **agregar existencia** a un artículo ya registrado desde Artículos o Existencias (entrada con folio).
15. **Limpieza reproducible**: `FASE 2/scripts/reset-demo-data.sh` vacía los datos de operación de los tres sistemas y reinicia las secuencias de folios, conservando usuarios, almacenes, puntos de venta, catálogo web e inventario web.

Límites de esta ampliación, a propósito:
- *(Reemplazado por el punto 17: la web ya no usa `catalog.json`.)*
- Solo hay una cuenta TIENDA (POS-01). Una tienda nueva necesita su cuenta en `inventory.security.users` con `point-of-sale-code`.
- `inventory-service` sigue sin llamar a nadie: las tiendas lo llaman a él (misma dirección de dependencia que el punto 2).

## Tercera ampliación del 2026-10-05: la tienda online como punto de venta conectado

Revisión de Kevin: "el POS online no está conectado" — la web mostraba un catálogo fijo (`catalog.json`) y reservaba contra el inventario sembrado de CD-01/CD-02, mientras la venta descontaba POS-ONLINE en el central: **la reserva no protegía el mismo stock que se vendía**. La hoja de calificación exige reserva atómica sin doble compromiso, liberación por temporizador y actualización de inventario por entradas/salidas; por eso la tienda online se conecta al inventario donde vive esa reserva.

17. **Módulo `onlinestore`** (monolito, Kevin; `allowedDependencies = {shared, inventory}`, migración `V1010`):
    - **Recepción por folio**, igual que una tienda física: baja del central lo enviado a POS-ONLINE (cuenta TIENDA `tienda-online@almacencentral.local`, ligada a POS-ONLINE), lo deja **en tránsito** y solo al confirmar el folio (`SAL-…`) lo ingresa al inventario del monolito en una ubicación nueva **`CD-WEB` "Almacén Tienda Online"** (`InventoryMovementService.receiveAtLocation`, entrada INBOUND con su evento). Confirmación protegida contra doble clic.
    - **Catálogo administrado**: lo recibido aparece **sin publicar**; quien administra (cuenta `tienda-online@megastore.com`, rol nuevo `ONLINE_STORE`, página `tienda-online.html`) edita nombre, descripción, categoría y precio, carga la **imagen** (JPG/PNG/WebP ≤ 2 MB, guardada en la base) y **publica**. Publicar exige imagen y precio.
    - **La web muestra solo lo publicado** (`GET /api/v1/online-catalog`, público) con disponibilidad = en mano − reservado en `CD-WEB`. El `productId` del carrito es el id del artículo del central: la Saga reserva y confirma en `CD-WEB` (reserva atómica y temporizador existentes de Jorge, sin cambios) e `inventorysync` reporta la venta al central por ese id (ya no por nombre).
18. **Cambios aditivos en el módulo `inventory` (Jorge)**: `InventoryItem.empty`, `InventoryMovementService.receiveAtLocation` (crea el ítem en la primera entrada a una ubicación) e `InventoryQueryService.getStockAtLocation`. La ubicación `CD-WEB` se siembra desde `V1010` (no cambia el esquema de inventory). La reserva, su temporizador y la confirmación no se tocaron.
19. **Ajuste de existencia en el central** (`AJU-NNNNNN`, migración `V18`): baja por merma, daño o corrección de conteo, motivo obligatorio, solo rol Almacén. Los folios no se borran: se corrigen con otro folio.
20. Se retiró `GET /api/v1/inventory-sync/online-stock` y `catalog.json` (la disponibilidad y el catálogo salen ahora de `onlinestore`).

21. *(Revisado por el punto 23: la vista general de almacenes vive en el almacén central, no en la consola de la web.)* El despacho guarda el almacén concreto de origen (`shipments.origin_code`, migración `V1011`); la Saga lo llena con la ubicación de la reserva (`CD-WEB` para la tienda online) y la consola permite despachar desde el almacén central o una tienda.
22. **Factura para el cliente** (`GET /api/v1/invoices/orders/{orderId}`, CLIENT/FINANCE, módulo fiscal): CUI, serie, autorización SAT, NIT, nombre, total y XML emitido, sin los datos internos de `/api/v1/fiscal-documents` (que sigue solo para Finanzas, ADR-013). La web la muestra al finalizar la compra y desde "Mis pedidos", imprimible y con descarga del XML.

23. **Vista general de existencias en el almacén central** (`GET /api/v1/stock-overview`, pestaña "Vista general" de inventory-service): por artículo, existencia en el central y en el almacén propio de cada punto de venta (incluida POS-ONLINE) y total de la red. Se retiró la vista unificada de la consola de la web (`/api/v1/inventory-sync/warehouses`).
24. **Tienda online como punto de venta completo** (`onlinestore`, migración `V1012`): pestañas Inventario (en mano, reservado, disponible, en tránsito), Ventas y facturas (órdenes web por fecha y su factura), **Traslados por folio** `TOL-NNNNNN` hacia el central u otra tienda (salen de `CD-WEB` solo unidades disponibles —`InventoryMovementService.issueFromLocation`, aditivo—; si el central rechaza, vuelven; sin conexión quedan pendientes y se reintentan) y **Bitácora** de sincronización (recepciones, ventas reportadas por `inventorysync` —que ahora depende de `onlinestore` solo para escribir en esa bitácora— y traslados).

25. **Checkout de la web**: "Recoger en tienda" ofrece las ubicaciones físicas activas (configurables en `sgo.integrations.online-store.pickup-points`; hoy el almacén central y la tienda física POS-01, `GET /api/v1/online-catalog/pickup-points`), y la ubicación elegida es el destino del despacho desde CD-WEB. Ventana de pago PayKlik simulada: tarjeta tokenizada con botón "Cobrado" o transferencia con una cuenta y referencia de demostración (`paymentMethod` CARD/TRANSFER; la referencia nunca parece un número de tarjeta).
26. **La cuenta de la tienda online administra la web completa** (roles ONLINE_STORE, SUPPORT, LOGISTICS, FINANCE): entra a su página y al centro de operaciones. En la consola, el panel de inventario muestra existencias y **kárdex de CD-WEB** (entradas por folio, salidas por traslado y reservas activas/vendidas/liberadas por el temporizador; `InventoryLocationActivityService`, aditivo en `inventory`), y "Buscar cliente" pasa a **atención al cliente (call center)**: búsqueda por correo, carrito compartido en canal CALL_CENTER e historial de compras y atenciones.
27. **Notificaciones para todo cliente registrado**: si un cliente no tiene contacto verificado, su correo del directorio de clientes pasa a ser su contacto de correo (`notifications` depende de `customers`, solo lectura).

28. **Correo real de notificaciones** (módulo `notifications`, NOT-01): `ChannelRoutingNotificationProvider` envía el correo por SMTP (Gmail con contraseña de aplicación, configurable por `SGO_MAIL_*`) cuando hay credenciales, y si no lo deja en el simulador BB-06; SMS y push siguen simulados. Una falla del servidor de correo es `NotificationProviderUnavailableException`: reintento y bandeja de fallidos existentes, nunca afecta la orden. El salud del backend no depende del correo (`management.health.mail.enabled=false`).

Nota de alcance: el DDA (§2.2, citado en `V2__inventory_seed…`) dejó la **gestión de catálogo de productos fuera de alcance**. Este catálogo es solo la *publicación* de la tienda online sobre artículos que ya existen en el almacén central (no da de alta productos). Para la trazabilidad que pide la hoja de calificación, el módulo `onlinestore`, la ubicación `CD-WEB` y la página de administración deben agregarse al diagrama de componentes, al de despliegue y al CDU expandido correspondiente (CDU de inventario/venta), o quedarían como "código huérfano".

## Cuarta ampliación del 2026-10-06: ERP AS/400, POS de las 32 tiendas e informe de ventas conciliado

Revisión contra el enunciado (ERP-01/02, POS-01) y la hoja de calificación ("Sincronizar ventas con ERP: CSV/XML automático cada 30 minutos y con reintentos", 3 pts; "Informe de ventas: conciliado y auditable", 4 pts). El módulo `integration` (Andy) ya tenía el lote con reintentos 2/8/32 s y la entrega a POS con dead-letter, pero **nada lo alimentaba**: la venta se registraba en el ERP y los eventos se publicaban a mano desde la consola, las ventas de la tienda física nunca llegaban al ERP, el "archivo" solo existía en la base de datos y no había informe de ventas. Cambios (aditivos sobre `integration`; módulos nuevos de Kevin):

29. **Alimentación automática (`legacyfeed`, nuevo)**: consume `ORDER_STATUS_CHANGED` (cola propia `sgo.legacyfeed.order-status.v1`, idempotente por inbox). En `CONFIRMED` registra la venta web en el ERP (neto/IVA desde el total con IVA 12 % incluido, canal de la orden, `POS-ONLINE`). En `CONFIRMED`, `PREPARING`, `IN_TRANSIT`, `DELIVERED` y `CANCELLED` publica `ORDER_<estado>` a las **32 tiendas** con orden, artículos, destino y tienda de recogida. Las ventas de la tienda física llegan por `StoreSalesErpFeed`: cada 60 s lee del central `GET /api/v1/store-sync/invoices?since=` y registra cada factura de tienda como venta `STORE` (referencia `FT-xxxxxx/TKT-xxxxxx`), idempotente por id de factura.
30. **Lote ERP como archivo real**: `FileDropErpGatewayAdapter` escribe cada lote de forma atómica en la carpeta de entrada del AS/400 (`SGO_ERP_OUTBOX_DIR`, volumen `erp-outbox`) como `ventas_AAAAMMDD_HHMMSS_<lote>.csv|xml`. El archivo trae una línea por venta (canal, punto de venta, referencia, neto, IVA, total) y un registro de control (`TRAILER`/`<Control>`) con la cantidad y las sumas. La ventana es automática cada 30 min (`SGO_ERP_INTERVAL_MS`). El simulador BB-06 sigue decidiendo las fallas (`SGO_SIMULATORS_ERP_MODE`).
31. **Reintentos ERP completos**: además de 2/8/32 s → `DEAD_LETTER`, cada ventana retoma los lotes pendientes que un reinicio dejó huérfanos, y Finanzas puede reenviar un lote `DEAD_LETTER` (`POST /api/v1/integrations/erp/batches/{id}/retry`) cuando el ERP vuelve.
32. **POS-01 real**: `RoutingPosGatewayAdapter` entrega por HTTP a las tiendas con endpoint configurado (`SGO_POS_STORE_01_ENDPOINT` → `store-service` `POST /api/store/pos-events`, idempotente por `eventId`). Un 2xx es ACK, un 4xx es rechazo y una caída o un 5xx es "no disponible" (reintento). Las otras 31 siguen en el simulador. La tienda muestra la pestaña **"Pedidos web"** con los pedidos y marca "Recoger aquí" los que eligieron POS-01.
33. **Informe de ventas conciliado (`salesreport`, nuevo; `GET /api/v1/reports/sales`, `/export` en CSV, rol FINANCE)**: por rango de fechas, cada venta web cruzada con su pago (PayKlik), su factura SAT (serie + CUI) y su venta en el ERP, más cada venta de tienda física. Marca diferencias: confirmada sin pago o sin factura, pago aprobado sin venta, venta fuera del ERP, monto distinto, lote ERP `DEAD_LETTER`/`REJECTED`. Totales por canal y "Cuadra / N diferencias". **Auditable**: cada consulta y exportación queda en `audit_events` (`SALES_REPORT_VIEWED`/`_EXPORTED`, actor, rango y resultado).
34. **Consola interna**: panel "ERP AS/400 y POS de tiendas" (lotes con estado, intentos, descarga del archivo y "Reintentar"; eventos por orden con el resumen de las 32 tiendas y el ACK de la tienda 01) y panel "Informe de ventas conciliado". En la tarjeta de la orden, su venta en el ERP y sus avisos a las tiendas. Se retiraron los botones manuales.
35. Soporte: `erp_sales` gana `channel`, `point_of_sale` y `source_reference` (V2004); `store_invoices` del central expone su `id`; `pos_events` en `store-service` (V4). `ModularArchitectureTest` declara `legacyfeed` y `salesreport` con sus dependencias (`salesreport` solo usa `payments :: payments-api` y `audit :: audit-api`).

## Quinta ampliación del 2026-10-06: PayKlik y LogiExpress como sistemas reales, eventos de stock a los POS y entrega en tienda

Kevin pidió que nada quedara simulado en estas integraciones. La hoja de calificación exige "integración REST con PayKlik" (Recibir pago) y "integración REST para generar guía" (Solicitar envío a LogiExpress). También pide "actualización de tiendas/POS" (Actualizar inventario). Hasta aquí PayKlik y LogiExpress eran simuladores dentro del mismo proceso y los POS solo recibían el estado de las órdenes. Cambios:

36. **PayKlik como sistema propio (`payklik-service`, :8086, base `payklik-db`)**. Es la pasarela del proveedor:
    - `POST /api/v1/authorizations` con `X-Api-Key`, idempotente por llave del comercio. Rechaza PAN y CVV.
    - `GET /api/v1/authorizations/{llave}` para conciliar.
    - Portal de comercios con las transacciones y el "Estado del servicio": Normal, Emisor declina, Lento (procesa, pero responde después de los 5 s del comercio) y Fuera de servicio (503).

    En SGO-Logic, `HttpPayKlikAdapter` (aditivo en `payments`) reemplaza al simulador cuando hay `SGO_PAYKLIK_BASE_URL`. El timeout de 5 s y el circuit breaker siguen en `ResilientPayKlikClient`. El simulador BB-06 queda solo para las pruebas automáticas.
37. **"Reintentar pago" completo (`OrderSagaService`)**. Antes, una orden con pago indeterminado se quedaba así aunque la conciliación (PAY-03) ya hubiera aprobado o rechazado el intento. Ahora la Saga retoma desde ese resultado:
    - Aprobado: sigue con la factura, la confirmación y el despacho.
    - Rechazado: compensa y libera la reserva.

    El checkout repite la misma solicitud (misma llave, sin segundo cobro) mientras PayKlik se concilia.
38. **LogiExpress como sistema propio (`logiexpress-service`, :8085, base `logiexpress-db`)**:
    - `POST /api/v1/guides` con `X-Api-Key`, idempotente por el id del despacho. Lleva la tienda de retiro si el cliente recoge.
    - `GET /api/v1/guides?updatedSince=` para el rastreo incremental.
    - Portal de operación: los mensajeros recolectan ("En tránsito") y entregan a domicilio ("Entregada") o en la tienda de retiro ("En tienda de retiro"). Tiene el mismo "Estado del servicio".

    En SGO-Logic:
    - `HttpLogiExpressAdapter` genera la guía con reintentos 2/8/32 s y bandeja de fallidos (LOG-03).
    - `LogiExpressTrackingSync` consulta cada 10 s lo que cambió y lo aplica al despacho. Solo avanza, nunca retrocede.
    - Si la guía se emitió pero la respuesta se perdió por timeout, el rastreo la asocia al despacho. Un reintento tardío no la pisa.
    - Un despacho en fallidos se puede volver a pedir ("Reintentar guía", `POST /api/v1/shipments/{id}/label-retry`).

    La consola ya no marca el tránsito ni la entrega a mano: lo reporta LogiExpress.
39. **La orden sigue al despacho (`fulfillment`, módulo nuevo)**. Consume `SHIPMENT_STATUS_CHANGED`:
    - Tránsito o llegada a la tienda → la orden pasa a `IN_TRANSIT`.
    - Entrega → la orden pasa a `DELIVERED`.

    Recorre los pasos intermedios en orden y no toca órdenes canceladas. El evento lleva ahora `source` (LOGIEXPRESS, `TIENDA POS-xx`, CONSOLA) y `destination`.
40. **Retiro en tienda de punta a punta**:
    - LogiExpress deja el paquete en la tienda, el despacho pasa a `READY_FOR_PICKUP` y el cliente recibe el correo "listo para recoger" (`SHIPMENT_READY_FOR_PICKUP`).
    - Solo el POS de esa tienda recibe `ORDER_READY_FOR_PICKUP` (`legacyfeed`). En "Pedidos web" aparece "Entregar al cliente".
    - La tienda pide nombre y documento de quien recoge y confirma a SGO-Logic con `POST /api/v1/store-pickups/{orden}/handoff` (cuenta de la terminal, rol nuevo `STORE`). SGO-Logic valida que el paquete ya llegó, que es de esa tienda y que no se entregó antes.
41. **Eventos de stock a cada punto de venta (central, `posevents`)**:
    - Cada entrada por folio, venta facturada o traslado que cambia la existencia de una tienda genera un evento para el sistema de **esa** tienda, con la existencia resultante. Queda en `pos_stock_events` en la misma transacción del movimiento.
    - Se entrega en orden por tienda, con reintentos a los 2, 8 y 32 s. Al agotarse queda Fallido y se reenvía desde "Stock por tienda". Una tienda sin sistema conectado acumula sus eventos como "Sin sistema conectado".
    - POS-01 los recibe en `store-service` (`/api/store/stock-events`). POS-ONLINE los recibe en SGO-Logic (`/api/v1/online-store/stock-events`), donde el central entra con la cuenta de servicio `central-eventos@megastore.com` (rol `POS_EVENTS`).
    - Cada punto de venta anota el evento en su bitácora y lo compara con su existencia (más lo que tiene en tránsito): "Cuadra" o "No cuadra".
42. Soporte:
    - Migraciones: central V19; tienda V5; monolito V1013. `payklik-service` y `logiexpress-service` tienen su V1.
    - Compose: 12 contenedores; volúmenes `payklik-postgres-data` y `logiexpress-postgres-data`.
    - `reset-demo-data.sh` también limpia PayKlik, LogiExpress y los eventos.
    - `ModularArchitectureTest` declara `fulfillment`.
    - `SecurityConfig` (Edson) gana dos reglas: `store-pickups` para STORE y `online-store/stock-events` para POS_EVENTS.

43. **Resumen consolidado de la orden completo (`query`, QRY-01, 2026-10-07)**:
    - `PaymentStatusChangedConsumer` proyecta `PaymentApproved/Rejected/Indeterminate` (antes el pago siempre salía "Sin dato").
    - La fila se crea con `INSERT … ON CONFLICT DO NOTHING` y se actualiza con bloqueo (`PESSIMISTIC_WRITE`). Así la orden, el pago y el despacho pueden llegar a la vez sin perder datos por clave duplicada.
    - Un evento más viejo que el registrado no lo reemplaza.
    - El temporizador de reservas (INV-03) queda configurable por entorno: `SGO_INVENTORY_RESERVATION_TTL` y `SGO_INVENTORY_EXPIRATION_SWEEP_INTERVAL`.

## Alternativas evaluadas

1. **Integración en vivo monolito → inventory-service (propuesta):** refleja ventas reales sin que `inventory-service` necesite saber nada del monolito ni de su base de datos. Costo: dos catálogos independientes que deben coincidir por nombre (frágil si alguno cambia sin avisar al otro).
2. **No integrar — "Tienda Online" como un punto de venta más, alimentado solo por folios manuales:** más simple y mantiene el desacoplamiento total, pero no cumple lo que Kevin pidió explícitamente (que el stock refleje ventas reales). Descartada por no resolver el requisito.
3. **inventory-service llama al monolito para enterarse de ventas (polling o webhook inverso):** invierte la dirección de dependencia — `inventory-service` dejaría de ser un sistema genuinamente independiente, que es justo la garantía que este mismo documento (y la corrección explícita de Kevin en sesiones previas) protege. Descartada.
4. **Agregar SKU real a los productos del monolito para un matching robusto:** es la solución correcta a largo plazo, pero toca el modelo de catálogo que no es propiedad de Kevin (Jorge/Jairo) y excede el alcance de esta integración puntual. Se deja como trabajo futuro, documentado como limitación conocida (punto 4 de la Decisión).

## Consecuencias

- Primera y única dependencia de red desde el monolito hacia `inventory-service`: `docker-compose.yml` ahora declara `backend` con `depends_on: inventory-service: condition: service_healthy`.
- Nuevo módulo Spring Modulith `inventorysync` (`allowedDependencies = {shared, messaging, orders}`) — primera vez que un módulo del monolito depende de `orders` solo para leer (nunca escribir) información de una orden ya confirmada.
- `ModularArchitectureTest` (contrato de límites de módulos ratificado por el equipo) se actualizó para declarar `inventorysync` y sus dependencias aprobadas.
- La bitácora de `inventory-service` (`audit_log_entries`) y la de reconciliación (`inventory_bridge_dead_letters`, en el monolito) son las dos superficies para auditar esta integración; no existe ningún otro lugar donde se registre.

- (Segunda ampliación) Un tercer sistema con base propia, `store-service`, y un rol nuevo `TIENDA` en `inventory-service`. El `docker-compose.yml` levanta `store-db` + `store-service` y retira `pos-tienda`. `store-service` no depende de que el central o el monolito estén arriba para arrancar ni para vender.

## Riesgos

- **Matching por nombre es frágil**: si alguien renombra un producto en un catálogo sin avisar al otro, la venta cae en la bandeja de fallidos (visible, no silenciosa) hasta que el nombre vuelva a coincidir o se arregle manualmente.
- **Doble sistema de verdad para "Tienda Online"**: su stock en `inventory-service` es un reflejo *eventual* (asíncrono, con reintentos) de las ventas web, no una fuente transaccional única — aceptable para este alcance (visibilidad de inventario), no para control de stock en tiempo real que bloquee una venta.
- **No hay reconciliación automática** de la bandeja de fallidos — hoy es responsabilidad manual de Kevin revisar `inventory_bridge_dead_letters` periódicamente.
- **Dos vistas del stock de una tienda** (la de la tienda y la del central) que convergen cuando sube la factura. Si divergen (p. ej. una venta hecha sin conexión sobre existencia que el central ya había trasladado), el central registra la factura y deja `DISCREPANCIA_STOCK_TIENDA` en su bitácora; la corrección es manual.
- **El vendedor no tiene contraseña** en `store-service` (decisión de Kevin: equipo de confianza dentro de la tienda). La API local de la tienda no debe exponerse fuera de su red.

## Criterios de ratificación

- Prueba de extremo a extremo: completar una compra real en la web hasta `CONFIRMED` y confirmar que el stock de `POS-ONLINE` en `inventory-service` bajó la cantidad correcta, con folio y `externalReference` propios — **verificado en vivo el 2026-10-02** (folio `SAL-000012`).
- Prueba de falla: con `inventory-service` apagado, la orden se confirma igual en el monolito, los reintentos quedan en el log (2/8/32 s) y, si `inventory-service` vuelve a estar sano dentro de la ventana de reintentos, la venta se recupera sola sin intervención — **verificado en vivo el 2026-10-02** (folio `SAL-000013`, recuperado en el cuarto intento).
- `./mvnw clean verify` en ambos sistemas, incluido `ModularArchitectureTest`, en verde.
- (Segunda ampliación, **verificado en vivo el 2026-10-05** después de limpiar los datos): artículo creado en el central → `SAL-000001/2` a POS-01 → la tienda lo recibe sola (`POS-ENT-000001/2`) → venta CF en efectivo con cambio (`TKT-000001` → `FT-000001`) y venta con NIT (`TKT-000002` → `FT-000002`) → traslado a central (`TRA-000001` → `ENT-000003`) y a POS-02 (`TRA-000002` → `POS-ENT-000004`) → con el central apagado, `TKT-000003` queda Pendiente y sube solo al volver (`FT-000003`) → `SAL-000003` surte POS-ONLINE con 6, la web muestra "6 disponibles" y "Agotado" en lo demás, compra web de 2 → `POS-VTA-000006` en POS-ONLINE (queda 4) sin ninguna salida nueva del central. Existencias iguales en la tienda y en el central. Pruebas: inventory-service 35, store-service 9, backend 180, todas en verde.
- (Cuarta ampliación, **verificado en vivo el 2026-10-06** con datos limpios): venta en POS-01 (`TKT-000001` → `FT-000001`) y compra web con recogida en POS-01 → ambas en el ERP sin intervención. El lote manual escribe `ventas_20261006_122511_f4faf5c3.csv` en `/var/sgo/erp-outbox` con 2 ventas y `TRAILER,2,…,2247.00`. Los eventos `ORDER_CONFIRMED/PREPARING/IN_TRANSIT` llegan a 32 tiendas; la tienda 01 responde con su ACK real y su pestaña "Pedidos web" marca "Recoger aquí". Con `SGO_SIMULATORS_ERP_MODE=OUTAGE` el lote pasa a `DEAD_LETTER` tras 4 intentos y el informe marca la diferencia; con las tiendas 7 y 15 caídas, 30 ACK y 2 `DEAD_LETTER`. Con el ERP de vuelta, "Reintentar" entrega el lote y el informe vuelve a "Cuadra". Pruebas: backend 200, inventory-service 37, store-service 13.
- (Quinta ampliación, **verificado en vivo el 2026-10-06** con datos limpios):
  - Compra web con recogida en POS-01: PayKlik autoriza (`PK-00000001`, portal) y LogiExpress emite `LGX-00000001` con retiro en POS-01.
  - En el portal de LogiExpress, "Recolectar": el despacho y la orden pasan a `IN_TRANSIT` solos y sale el correo de tránsito.
  - "Dejar en tienda POS-01": `READY_FOR_PICKUP`, correo "listo para recoger", `ORDER_READY_FOR_PICKUP` solo a la tienda 1.
  - "Entregar al cliente" en la tienda: `DELIVERED` (fuente `TIENDA POS-01`) y `ORDER_DELIVERED` a 32 tiendas. Un segundo intento de entrega se rechaza.
  - PayKlik en "Lento": el pago queda indeterminado sin segundo cobro, la conciliación lo aprueba (`PK-00000004`) y la compra termina sola con factura y despacho.
  - LogiExpress en "Lento": la guía se asocia por rastreo. En "Fuera de servicio": `LABEL_FAILED` tras 4 intentos y, al volver, "Reintentar guía" emite `LGX-00000003`.
  - Eventos de stock: `POS-ENT`/`POS-VTA` entregados a POS-01 y POS-ONLINE ("Cuadra"). Con la tienda apagada, el evento reintenta, queda Fallido y "Reenviar" lo entrega.
  - Pruebas: backend 223, inventory-service 40, store-service 15, payklik-service 4, logiexpress-service 4.
- Al menos tres integrantes registran su aprobación antes de pasar a `Accepted` (igual que el resto de ADRs de esta fase).

## Referencias

- `BITACORA_FASE_2.md`, entradas del 2026-10-01 y 2026-10-02 (Kevin Girón).
- `FASE 2/inventory-service/` (sistema completo).
- `FASE 2/backend/src/main/java/com/megastore/sgologic/inventorysync/` (módulo de integración).
- `FASE 2/store-service/` (sistema de tienda física) y `FASE 2/inventory-service/.../storesync/` (lado central).
- `FASE 2/scripts/reset-demo-data.sh` (limpieza de datos de operación).
- `FASE 2/backend/src/main/java/com/megastore/sgologic/onlinestore/` y `frontend/tienda-online.html` (tienda online); `OnlineStoreIntegrationTest` (recepción → inventario CD-WEB → publicación → compra con reserva confirmada en CD-WEB, y dos compradores concurrentes por la última unidad: exactamente una reserva).
- `FASE 2/backend/.../legacyfeed/`, `.../salesreport/` e `integration/FileDropErpGatewayAdapter`, `RoutingPosGatewayAdapter`; `store-service/.../posevents/`.
- `FASE 2/payklik-service/`, `FASE 2/logiexpress-service/`, `backend/.../payments/HttpPayKlikAdapter`, `logistics/HttpLogiExpressAdapter`, `LogiExpressTrackingSync`, `StorePickupController`, `fulfillment/`; `inventory-service/.../posevents/`; `store-service/.../stockevents/`.
- `logistics.ShipmentService` (molde de reintento/backoff/dead-letter reutilizado).
- `STACK_TECNOLOGICO_FASE_2.md` §20 (gate de arquitectura para microservicios/bases de datos independientes).
