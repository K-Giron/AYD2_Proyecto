# DEMO-01 — Guion integrado del MVP

| Campo | Valor |
|---|---|
| Responsable | Kevin Girón / equipo de integración |
| Fuentes | `Docs/DDA.md`, `QA-03-matriz-eac.md`, `QA-05-pruebas-fallas-externas.md`, `QA-06-gate-trazabilidad-penalizaciones.md` |
| Corte | 2026-09-24 |
| Alcance honesto | Camino feliz completo con simuladores controlados; Outbox, Inbox, proyección de estados de orden/despacho/entrega/incidencia, auditoría y autenticación JWT real están integrados. Sigue pendiente: nota de crédito/reposición de CUN-10 completo, historial por intento de notificación, y la prueba E2E que fuerce las cuatro fallas externas a la vez sobre la Saga (ver QA-05 §3.1) |

## 1. Preparación

```bash
cd "FASE 2"
docker compose up --build -d
docker compose ps
```

Los cuatro servicios deben quedar `healthy`. Abrir `http://localhost:8081`.

Credenciales de demostración:

- Cliente: `cliente@megastore.com` / `Cliente123`
- Personal interno: `staff@megastore.com` / `Staff123`
- Finanzas (consulta/exportación CUN-12): `finanzas@megastore.com` / `Finanzas123`

## 2. Recorrido principal verificable

### 2.1 Catálogo público y continuidad de intención

1. Abrir la landing sin sesión.
2. Mostrar catálogo y disponibilidad consultada al backend.
3. Seleccionar **Comprar**.
4. Explicar que el producto seleccionado queda como intención temporal y se solicita autenticación.
5. Iniciar sesión o crear una cuenta; comprobar que el producto entra al carrito recuperable del cliente.

Trazabilidad: CUN-01, CDU-01, `carts`, `cart_items`, módulo Orders.

### 2.2 Saga de compra

1. Confirmar datos fiscales y compra.
2. Mostrar las etapas: orden creada, inventario reservado, pago aprobado, documento fiscal y orden confirmada.
3. Conservar el UUID de la orden y el CUI SAT.
4. Consultar el seguimiento del pedido desde la vista del cliente.

Trazabilidad: CUN-01 a CUN-04; `customer_orders`, `inventory_reservations`, `payment_attempts`, `fiscal_documents`.

### 2.3 Centro de operaciones STAFF

1. Ingresar como personal interno.
2. Mostrar métricas y la orden real en **Órdenes recientes**.
3. Seleccionar **Gestionar** y revisar el resumen consolidado.
4. Crear un despacho únicamente para una orden `CONFIRMED`.
5. Mostrar guía LogiExpress, tracking y transición de orden a `PREPARING`, `IN_TRANSIT` y `DELIVERED`.
6. Enviar una notificación y comprobar que el proveedor no bloquea la operación.

Trazabilidad: CUN-07 a CUN-09 y CUN-11.

### 2.4 ERP, POS y auditoría

1. Registrar la venta confirmada para la siguiente ventana ERP.
2. Publicar el estado hacia una tienda POS entre 1 y 32.
3. Consultar la entrega hasta obtener `ACKNOWLEDGED` o un estado de reintento controlado.
4. Cerrar la sesión STAFF e ingresar con la cuenta de Finanzas.
5. Cargar el mismo UUID y revisar la evidencia de auditoría con actor, fecha, cambio, correlation ID y hash SHA-256.
6. Exportar la evidencia JSON y explicar que el acceso también queda registrado.

Trazabilidad: CUN-05, CUN-06 y cobertura parcial de CUN-12.

### 2.5 Devolución

1. Registrar una incidencia o devolución contra la orden existente.
2. Resolverla desde STAFF.
3. Explicar con precisión que el registro y la resolución existen, pero la compensación automática de pago, nota de crédito, inventario y notificación todavía no está orquestada.

Trazabilidad: cobertura parcial de CUN-10.

## 3. Fallas que deben demostrarse

| Proveedor | Escenario | Resultado esperado |
|---|---|---|
| PayKlik | timeout o unavailable | Intento persistido, orden recuperable, `PAYMENT_INDETERMINATE`, conciliación programada |
| SAT | timeout | Documento e intento conservados; orden permanece `FISCAL_PENDING` |
| LogiExpress | timeout | Reintentos 2/8/32; despacho no bloqueado; DLQ local al agotar intentos |
| POS | unavailable | Entrega persistida, reintentos, deduplicación y eventual `ACKNOWLEDGED` o `DEAD_LETTER` |
| Notificaciones | unavailable | Respuesta no bloqueante; reintento y estado final observable |

No ejecutar todas las fallas aleatorias durante el único camino feliz. Preparar una orden separada por escenario para conservar evidencia legible.

## 4. Capacidades que no deben presentarse como terminadas

- Consumidores RabbitMQ de ERP y POS, además de productores para despacho e incidencias; Query y Notifications ya consumen confirmaciones con Inbox idempotente.
- Auditoría directa de pagos, fiscal, logística e incidencias; orden y accesos/exportaciones ya cuentan con base transversal.
- Autenticación/autorización backend con roles reales.
- Prueba Black Friday `T` contra `8T`.
- Despliegue en nube y validación real de costos.
- Compensación completa de devoluciones.

Estas capacidades aparecen como objetivo arquitectónico en el DDA y en la página 01 del modelo ER; las entidades aún no implementadas por completo tienen borde discontinuo. La página 02 conserva el corte físico real y la página 03 amplía los controles de trazabilidad pendientes.

## 5. Evidencia previa a presentar

- [x] Suite local: 87 pruebas, 0 fallos, 0 errores, 0 omitidas.
- [x] Camino feliz real hasta orden `CONFIRMED`, CUI y serie SAT.
- [x] POS verificado hasta `ACKNOWLEDGED` con acuse.
- [x] Backend, frontend, PostgreSQL y RabbitMQ saludables.
- [ ] Capturas actuales de landing, cliente y STAFF.
- [ ] Evidencia versionada de cada escenario de falla en `docs/evidence/resilience/`.
- [ ] Ensayo completo por una persona distinta de quien implementó el flujo.
- [ ] Revisión final de `QA-06` contra cualquier cambio posterior.

## 6. Plan de contingencia

- Si la UI falla, usar los endpoints documentados en `FASE 2/README.md`.
- Si un simulador entra en timeout, mostrar el estado persistido y el mecanismo de reintento; no ocultar la falla.
- Si Docker no inicia, presentar capturas y evidencia versionada, sin afirmar que el ambiente está operativo.
- No inventar métricas de rendimiento, costo, disponibilidad o seguridad que todavía no hayan sido medidas.
