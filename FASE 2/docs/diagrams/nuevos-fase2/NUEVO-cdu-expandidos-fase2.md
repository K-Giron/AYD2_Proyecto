# NUEVO · CDU expandidos de Fase 2 (ampliaciones y casos nuevos)

> **NUEVO** — Kevin Girón, 2026-10-07. Este documento **no sustituye** los CDU
> de `Docs/DDA.md` §3 ni `Docs/diagramas/cdu-negocio-y-expandidos.drawio`:
> agrega lo que el código de Fase 2 hace y aún no aparece en ellos.
>
> - Las ampliaciones de un CDU existente se marcan **(NUEVO Fase 2)** y van
>   después del último flujo que ya tenía.
> - Los casos nuevos usan los números CDU-13 a CDU-16.
>
> Referencia de diseño: ADR-015.

---

## Ampliaciones de CDU existentes

### CDU-03: Procesar pago — ampliación

```
Flujos alternativos (NUEVO Fase 2):
  A2. PayKlik responde después del tiempo límite (5 s):
      1. El intento queda PAYMENT_INDETERMINATE; PayKlik pudo haber cobrado.
      2. La conciliación (PAY-03) consulta a PayKlik con la misma llave de
         idempotencia; PayKlik devuelve la autorización ya registrada, sin
         un segundo cobro.
      3. El checkout repite la misma solicitud de la Saga mientras tanto.
      4. Con el pago APPROVED, la Saga retoma: factura SAT, confirmación,
         despacho.
      5. Con el pago REJECTED, la Saga compensa y libera la reserva.
Integración (NUEVO Fase 2): PayKlik es un sistema propio (payklik-service)
  consumido por REST con X-Api-Key; nunca recibe PAN ni CVV.
Evidencia: OrderSagaReconciledPaymentTest, HttpPayKlikAdapterTest,
  PayKlikIntegrationTest.
```

### CDU-05: Sincronizar venta con ERP — ampliación

```
Flujo principal (NUEVO Fase 2):
  1a. Las ventas web entran solas al confirmarse la orden (legacyfeed).
      Las ventas de la tienda física llegan desde el almacén central cada
      60 s (facturas de tienda) como canal STORE.
  2a. El archivo CSV/XML lleva una línea por venta (canal, punto de venta,
      referencia, neto, IVA, total) y un registro de control (TRAILER /
      <Control>) con la cantidad y las sumas.
  3a. El archivo se escribe en la carpeta de entrada del ERP
      (/var/sgo/erp-outbox) cada 30 min, configurable.
Flujos alternativos (NUEVO Fase 2):
  A2. Finanzas genera el lote antes de la ventana ("Generar lote ERP ahora").
  A3. El backend se reinició con un lote pendiente: la siguiente ventana lo
      retoma.
Flujo de excepción (NUEVO Fase 2):
  E2. El ERP no responde: se reintenta a los 2, 8 y 32 s; si sigue sin
      responder, el lote queda DEAD_LETTER. Finanzas lo reenvía con
      "Reintentar" cuando el ERP vuelve.
Postcondición (NUEVO Fase 2):
  - El informe de ventas (CDU-13) muestra cada venta como entregada al ERP o
    pendiente.
```

### CDU-06: Notificar eventos a POS — ampliación

```
Flujo principal (NUEVO Fase 2):
  1a. Eventos de orden (ORDER_CONFIRMED, PREPARING, IN_TRANSIT, DELIVERED,
      CANCELLED) a las 32 tiendas, con la tienda de retiro si aplica.
      La tienda 1 es un POS real (store-service); las otras 31 usan el
      simulador.
  1b. ORDER_READY_FOR_PICKUP solo a la tienda donde el cliente recoge.
  1c. Eventos de stock: cada entrada, venta o traslado que cambia la
      existencia de un punto de venta se envía al sistema de ESE punto de
      venta, con la existencia resultante. La tienda lo compara con la suya
      y lo anota en su bitácora ("Cuadra" / "No cuadra").
Flujo de excepción (NUEVO Fase 2):
  E2. La tienda no responde: el evento de stock se reintenta a los 2, 8 y
      32 s y los siguientes de esa tienda esperan (orden). Al agotarse queda
      Fallido; el almacén lo reenvía desde "Stock por tienda".
  E3. El punto de venta aún no tiene sistema conectado: sus eventos se
      guardan como "Sin sistema conectado" y se reintentan cada minuto.
```

### CDU-07 / CDU-08: Gestionar despacho e integrar LogiExpress — ampliación

```
Actor (NUEVO Fase 2): LogiExpress (sistema propio logiexpress-service) y su
  mensajero.
Flujo principal (NUEVO Fase 2):
  1. SGO-Logic pide la guía por REST (idempotente por id del despacho),
     indicando la tienda de retiro si el cliente recoge.
  2. El mensajero recolecta en su portal; SGO-Logic lee el rastreo cada 10 s
     y el despacho y la orden pasan a "En tránsito"; se avisa al cliente.
  3. El mensajero entrega a domicilio y el despacho y la orden pasan a
     "Entregada" (fulfillment).
Flujo alternativo (NUEVO Fase 2) — retiro en tienda:
  A2. El mensajero deja el paquete en la tienda de retiro → READY_FOR_PICKUP,
      correo "listo para recoger" y aviso al POS de esa tienda → el cliente
      llega, el vendedor registra quién recoge (nombre y documento) →
      SGO-Logic valida que el pedido llegó, que es de esa tienda y que no se
      entregó antes → "Entregada".
Flujos de excepción (NUEVO Fase 2):
  E2. LogiExpress emitió la guía pero respondió tarde: el rastreo la asocia
      al despacho y un reintento tardío no la pisa.
  E3. LogiExpress estuvo caído y se agotaron los reintentos (LABEL_FAILED):
      Logística usa "Reintentar guía".
Evidencia: HttpLogiExpressAdapterTest, LogiExpressTrackingSyncTest,
  ShipmentServiceTest, LogiExpressIntegrationTest.
```

### CDU-09: Consultar estado de la orden — ampliación

```
Postcondición (NUEVO Fase 2):
  - El resumen consolidado incluye orden, pago (incluso conciliado), despacho,
    entrega e incidencia, alimentado por eventos. Los hechos que llegan a la
    vez no se pierden y un hecho viejo no reemplaza uno nuevo.
Evidencia: OrderSummaryConcurrencyIntegrationTest, PaymentStatusChangedConsumerTest.
```

---

## Casos nuevos

### CDU-13: Generar informe de ventas conciliado (NUEVO)

```
ID: CDU-13
Nombre: Generar informe de ventas conciliado
Derivado de (CUN): CUN-05 / CUN-12 — Auditar y consolidar
Actor(es): Finanzas
Descripción breve: Cruza cada venta de un rango (web y tienda física) con su
  pago, su factura SAT y su entrega al ERP, y marca las diferencias.
Precondiciones:
  - Usuario con rol FINANCE autenticado
Flujo principal:
  1. Finanzas elige el rango de fechas (por defecto, hoy en Guatemala)
  2. El sistema reúne las órdenes web y las ventas de tienda registradas en el ERP
  3. Para cada venta compara el pago, la factura SAT, la venta en el ERP y el monto
  4. Muestra los totales (neto, IVA, total), los totales por canal y la
     conciliación ("Cuadra" o "N diferencias")
  5. Registra la consulta en auditoría (SALES_REPORT_VIEWED)
Flujos alternativos:
  A1. Exportar CSV: se descarga el mismo informe y se audita
      (SALES_REPORT_EXPORTED)
Flujo de excepción:
  E1. Rango inválido (la fecha final es anterior a la inicial):
      400 INVALID_REPORT_RANGE
Diferencias detectadas:
  - Venta confirmada sin pago aprobado
  - Venta confirmada sin factura SAT
  - Pago aprobado sin venta
  - Venta no registrada en el ERP
  - Monto distinto al del ERP
  - Lote ERP en DEAD_LETTER o REJECTED
Postcondiciones:
  - Queda evidencia de quién consultó qué rango y con qué resultado
Implementación: módulo salesreport; GET /api/v1/reports/sales y /export
Evidencia: SalesReportServiceTest
```

### CDU-14: Abastecer un punto de venta por folio (NUEVO)

```
ID: CDU-14
Nombre: Abastecer un punto de venta por folio
Actor(es): Almacén central; vendedor de la tienda o administrador de la tienda online
Precondiciones:
  - El artículo existe en el central con existencia suficiente
Flujo principal:
  1. El almacén registra una salida hacia el punto de venta (folio SAL-…)
  2. El central baja su existencia y sube el stock propio del punto de venta
     (POS-ENT-…)
  3. El central emite el evento de stock a ese punto de venta (CDU-06 1c)
  4. El punto de venta ve el folio "En tránsito" y lo confirma al recibirlo
  5. Su existencia local sube y queda en su bitácora
Flujos alternativos:
  A1. Traslado de la tienda al central o a otra tienda (folio TRA-…)
  A2. Ajuste del central por merma (folio AJU-…)
Flujo de excepción:
  E1. El folio ya fue recibido: 409 FOLIO_ALREADY_RECEIVED
Implementación: inventory-service (stock, posstock, storesync, posevents),
  store-service, onlinestore
```

### CDU-15: Vender en la tienda física (NUEVO)

```
ID: CDU-15
Nombre: Vender en la tienda física
Actor(es): Vendedor; cliente (CF, NIT u opcionalmente su cuenta web)
Precondiciones:
  - La tienda tiene existencia local del artículo
Flujo principal:
  1. El vendedor escanea o busca el artículo
  2. Cobra en efectivo, tarjeta o transferencia; emite la factura (TKT-…)
  3. Baja la existencia local en la misma transacción
  4. Sube la factura al central (FT-…): el central baja su vista del stock y
     emite el evento de stock
  5. SGO-Logic la recoge para el ERP (CDU-05)
Flujos alternativos:
  A1. Cliente con cuenta web: se trae su carrito y se cierra al cobrar
  A2. Reimprimir una factura por fecha
Flujo de excepción:
  E1. Central caído: la venta se hace igual y la factura queda Pendiente;
      sube sola al volver el central
Implementación: store-service; inventory-service (storesync)
```

### CDU-16: Entregar un pedido web en la tienda de retiro (NUEVO)

```
ID: CDU-16
Nombre: Entregar un pedido web en la tienda de retiro
Actor(es): Vendedor de la tienda de retiro; cliente
Precondiciones:
  - LogiExpress dejó el paquete en la tienda (despacho READY_FOR_PICKUP)
Flujo principal:
  1. La tienda ve el pedido "Llegó, lista para entregar" (evento
     ORDER_READY_FOR_PICKUP)
  2. El cliente se presenta; el vendedor registra el nombre y documento de
     quien recoge
  3. La tienda confirma a SGO-Logic (rol STORE)
  4. El despacho y la orden pasan a "Entregada"; el cliente recibe el aviso;
     las 32 tiendas reciben ORDER_DELIVERED
Flujo de excepción:
  E1. El pedido no ha llegado, es de otra tienda o ya se entregó:
      409 con el motivo, y queda en la bitácora de la tienda
Implementación: logistics (StorePickupController), store-service (Pedidos web)
Evidencia: ShipmentServiceTest, StoreServiceIntegrationTest
```
