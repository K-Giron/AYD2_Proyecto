# DEMO-01 — Guion, datos y plan de contingencia

| Campo | Valor |
|---|---|
| Responsable | Kevin Girón |
| Depende de | QA-04 (Jairo), QA-05 (Kevin, parcial), UI-01 (Kevin) |
| Corte | 2026-09-22 |
| Alcance real de esta versión | Solo lo que existe hoy en `develop` + esta rama: `logistics`, `notifications`, `query`, `incidents`, `frontend`. **No incluye** carrito, pago, factura ni Saga completa porque esos módulos no existen todavía. |

## 1. Qué se puede demostrar hoy (y qué no)

Este guion demuestra el camino feliz de **despacho, notificación, consulta e incidencia** de forma aislada. No demuestra un checkout real de punta a punta: FIS-01 ya está preparado localmente, pero todavía faltan ORD-01 (Jairo), INV-01 (Jorge), PAY-01 (Edson) y la conexión de la Saga. Presentarlo como checkout funcional incumpliría el DDA, por eso el límite sigue explícito.

## 2. Preparación (antes de la demo)

```bash
cd "FASE 2"
cp .env.example .env
docker compose up --build -d
docker compose ps   # los 4 servicios deben quedar "healthy"
```

Frontend en `http://localhost:8081`, API en `http://localhost:8080/api/v1`.

## 3. Datos de prueba (reproducibles)

No se usan datos reales de clientes. Los identificadores se generan al vuelo con `crypto.randomUUID()` en la UI o `uuidgen`/`python3 -c "import uuid; print(uuid.uuid4())"` en terminal.

La UI es un panel único por orden (no pestañas sueltas): se escribe un Order ID en el selector superior ("Orden de trabajo") y las cuatro tarjetas de abajo (Despacho, Notificaciones, Resumen consolidado, Devoluciones) se filtran automáticamente por ese `orderId` — así se demuestra CDU-09 en la propia navegación, no solo en la tarjeta de resumen.

| Escenario | Dónde en el panel | Valores de ejemplo |
|---|---|---|
| Despacho recogida en tienda | Tarjeta **Despacho** → "+ Nuevo despacho" | origen `CUSTOMER_PICKUP`, destino "Tienda Zona 10" |
| Despacho con guía LogiExpress | Tarjeta **Despacho** → "+ Nuevo despacho" | origen `DISTRIBUTION_CENTER`, destino "San Salvador, El Salvador" |
| Notificación de confirmación | Tarjeta **Notificaciones** → "+ Nueva notificación" | canal `EMAIL`, evento `ORDER_CONFIRMED` |
| Consulta consolidada | Tarjeta **Resumen consolidado** (arriba, ancho completo) | se llena con lo que ya exista de las otras tarjetas + ingesta manual |
| Devolución parcial | Tarjeta **Devoluciones e incidencias** → "+ Registrar" | tipo `RETURN`, `partial` marcado, SKU de ejemplo |

## 4. Guion paso a paso

### 4.0 Arranque

1. Abrir `http://localhost:8081`. Señalar el punto de estado del backend en la barra superior (verde = activo).
2. En el selector "Orden de trabajo", pulsar **"Orden nueva (demo)"**: genera un UUID y carga el panel vacío para esa orden — así queda claro que no hay datos de clientes reales precargados.

### 4.1 Despacho (LOG-01 / LOG-02 / LOG-03) — CDU-07 y CDU-08

1. En la tarjeta **Despacho**, abrir "+ Nuevo despacho" y crear uno con origen `CUSTOMER_PICKUP`: explicar que, por CDU-08 A1, no se solicita guía externa (queda en `READY_FOR_PICKUP`).
2. Crear un segundo despacho con origen `DISTRIBUTION_CENTER`: explicar que dispara la solicitud a LogiExpress (simulado) en segundo plano; la fila aparece con estado `ASSIGNED` y pasa a `LABEL_ISSUED` al refrescar.
3. Pulsar "Ver tracking" sobre esa fila para mostrar la línea de tiempo (vacía todavía).
4. Pulsar "Marcar en tránsito" y luego "Marcar entregado" para llenar la línea de tiempo en vivo.
5. Mencionar explícitamente: LogiExpress ya usa los modos configurables de BB-06, pero la cola de fallidos local es temporal hasta que Edson entregue el Outbox (BB-04); no se presenta como el mecanismo final.

### 4.2 Notificaciones (NOT-01) — CUN-11

1. En la tarjeta **Notificaciones**, enviar una notificación `ORDER_CONFIRMED` por `EMAIL`.
2. Señalar que la fila aparece de inmediato en estado `PENDING` (respuesta `202 Accepted` sin esperar al proveedor) — es la evidencia visual de EaC-08 (no bloqueante). Al refrescar, normalmente ya está en `SENT`.

### 4.3 Consulta consolidada (QRY-01) — CDU-09

1. Abrir "Alimentar resumen manualmente" dentro de la tarjeta **Resumen consolidado**, y aplicar origen `ORDER`, estado `CONFIRMED` — explicando que este paso lo hará automáticamente un consumidor de eventos cuando el Outbox exista.
2. Mostrar cómo la tarjeta de resumen ya refleja también el despacho y la notificación de los pasos anteriores, sin haber consultado cada área por separado — esa es la demostración central de CDU-09.
3. Cambiar el Order ID del selector superior a un UUID inventado y pulsar "Cargar": la tarjeta de resumen muestra el mensaje explícito de "sin resumen todavía" en vez de romperse (CDU-09, E1).

### 4.4 Devolución (INC-01) — CDU-10

1. Registrar una devolución parcial referenciando el mismo `orderId`.
2. Mostrar que la incidencia queda en `REGISTERED` y que el sistema **no** procesa un reembolso automático (CDU-10, E1: resolución manual mientras AUD-01/PAY y la integración de nota de crédito no existen).

### 4.5 Cierre

Resumir qué quedó fuera (checkout real, Saga, pagos, factura, POS, ERP) y por qué: dependen de entregas de otros integrantes que, a la fecha, no están en el repositorio.

## 5. Plan de contingencia

- **Si la UI falla o no carga:** usar los mismos endpoints con `curl` o una colección Postman (ver ejemplos de §3 de `FASE 2/README.md`, sección "Endpoints de negocio"). El guion no depende de la UI para ser creíble.
- **Si Docker Compose no levanta en la máquina de la demo:** tener capturas de pantalla y la salida de la prueba de humo de este corte (2026-09-22, ver §6) como respaldo.
- **Si LogiExpress simulado cae en el ~2 % de timeout durante la demo:** es el comportamiento esperado, no un error — usarlo para mostrar el reintento en vivo en vez de repetir la llamada.
- **Nunca improvisar un checkout falso** si alguien lo pide en vivo: repetir la explicación de la sección 1.

## 6. Registro de ensayos

| # | Tipo | Fecha | Participantes | Resultado | Evidencia |
|---|---|---|---|---|---|
| 1 | Ensayo técnico individual (sin equipo) | 2026-09-22 | Kevin (asistido) | Los 4 servicios quedaron `healthy`; se ejecutaron en vivo, contra el stack real vía `http://localhost:8081`: `POST /api/v1/shipments` → `201`, `POST /api/v1/query/order-summary` + `GET /api/v1/orders/{id}/summary` → `200`, `POST /api/v1/incidents` → `201`, `POST /api/v1/notifications` → `202`, y el caso negativo `GET` sobre orden inexistente → `404` con `ApiError` completo. | Salida de `curl` capturada el 2026-09-22 ~08:20–08:21 UTC (ver bitácora, entrada de Kevin) |
| 2 | Ensayo con el equipo (guion completo, tiempo cronometrado) | Pendiente | Pendiente | Pendiente | Pendiente |

El ensayo #1 confirma que el guion es técnicamente ejecutable, no sustituye el ensayo #2 que exige la tarjeta DEMO-01 (dos ensayos con el equipo). Ese segundo ensayo requiere coordinar agenda con Jairo, Jorge, Edson y Andy y no se puede completar sin su participación.
