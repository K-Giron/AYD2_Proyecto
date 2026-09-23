# REV-EDSON — Revisión cruzada de entregas (2026-09-23)

**Revisor:** Edson Sosa (Security & Quality Champion)
**Alcance:** entrega fiscal/ERP/POS de Andy Sánchez; esquema e invariantes de
inventario de Jorge Pumay; coherencia documentación ↔ código del MVP.
**Rama de revisión:** `docs/rev-edson-cruzadas`
**Evidencia base:** `./mvnw clean verify` con Java 25 y Testcontainers.

## 1. Método

1. Lectura estática de servicios, controladores, entidades, migraciones Flyway
   y pruebas de cada entrega contra los CDU, EaC y ADR citados.
2. Verificación de límites modulares (sin acceso a tablas ajenas, solo
   referencias por `UUID`).
3. Ejecución de la suite completa contra PostgreSQL 17.11 y RabbitMQ 4.3.5
   reales mediante Testcontainers.

## 2. Evidencia de verificación

Comando, dentro de `FASE 2/backend` con Docker activo:

```bash
./mvnw clean verify
```

| Resultado | Valor |
|---|---|
| Build | `BUILD SUCCESS` |
| JDK | Eclipse Temurin 25.0.4_7 |
| Clases de prueba | 18 |
| Pruebas | 65 |
| Fallos / errores / omitidas | 0 / 0 / 0 |
| Infraestructura real | PostgreSQL 17.11 y RabbitMQ 4.3.5 vía Testcontainers |
| Migraciones ejecutadas | `V1`, `V2`, `V1001`–`V1004`, `V2001`–`V2003` |

Este resultado cierra el pendiente que Andy no pudo ejecutar por no tener
Docker: sus migraciones `V2001`–`V2003` sí aplican sobre PostgreSQL real.

## 3. Revisión de la entrega fiscal/ERP/POS (Andy Sánchez)

### 3.1 Fortalezas verificadas

- `fiscal` referencia Orders y Payments solo por `UUID`; no lee tablas ajenas.
- XML fiscal generado con StAX (bien formado y escapado) y probado.
- `FISCAL_PENDING` se conserva ante timeout/rechazo; `canConfirmOrder` exige
  `APPROVED` (ADR-007 y ADR-010 respetados).
- Idempotencia por `idempotency_key`/`order_id`/`(event_id, store_number)` con
  restricciones únicas y `CHECK` de totales y tiendas 1–32.
- Reintentos 1 + 3 (2/8/32 s) con `DEAD_LETTER`, lotes vacíos no enviados y
  acuse persistido.
- PayKlik simulado rechaza referencias con PAN/CVV; prueba automatizada.

### 3.2 Hallazgos

| # | Severidad | Hallazgo | Recomendación |
|---|---|---|---|
| A1 | Media | POS-01 no hace fan-out automático a las 32 tiendas: el llamador debe publicar una vez por tienda. El criterio de la tarjeta dice "fan-out lógico a 32 POS" y CDU-06 describe un trabajador que identifica la tienda destinataria. | Implementar el fan-out en el consumidor de Outbox cuando exista BB-04, o exponer un comando de evento que genere las 32 entregas; mientras tanto, documentar que la publicación es por tienda |
| A2 | Media | Carrera "consultar y luego insertar" en idempotencia (`FiscalService.requestDocument`, `ErpIntegrationService.registerConfirmedSale`, `PosIntegrationService.publish`): dos solicitudes simultáneas con la misma clave pueden fallar con error de integridad (HTTP 500) en vez de devolver el registro existente | Capturar `DataIntegrityViolationException` y devolver el registro existente; agregar prueba concurrente por clave repetida |
| A3 | Media-alta | `ErpIntegrationService.runBatch` selecciona ventas `PENDING` sin reclamarlas atómicamente: la ventana programada y una ejecución manual concurrente pueden incluir las mismas ventas en dos lotes (ERP-02 exige no regenerar de forma duplicada) | Reclamar las ventas con `UPDATE ... WHERE status = 'PENDING'` (o bloqueo pesimista) antes de generar el archivo |
| A4 | Media | Las ventas incluidas en un lote `DEAD_LETTER` quedan atadas a ese lote y no vuelven a `PENDING`; no hay procedimiento de recuperación | Definir reproceso documentado o reencolar las ventas del lote fallido |
| A5 | Baja | El XML del lote ERP se arma por concatenación de cadenas, a diferencia del XML fiscal que usa StAX | Reutilizar un generador con escape o validar el contenido antes de persistir |
| A6 | Baja | El CSV no escapa campos; el riesgo es bajo porque los campos son `UUID`, números, ISO-8601 y moneda, pero `currency` no valida longitud | Validar `currency` (3 letras ISO) en el borde de la API |
| A7 | Conocida | El despacho asíncrono ocurre en memoria después de persistir; una caída del proceso entre ambos pasos pierde la ejecución (no el hecho de negocio) | Cerrada por BB-04/Outbox; ya documentada en ADR-010 y en el checklist de Andy |

### 3.3 Decisión

**Corregir antes de cerrar las tarjetas**, sin bloquear la integración:
A3 es el hallazgo más relevante (duplicidad de lote ERP); A1 y A2 deben quedar
resueltos o convertidos en limitación aceptada por el equipo. A5–A7 son
mejoras o limitaciones ya declaradas.

Los ADR-010 y ADR-012 coinciden con el código revisado y se aprueban desde la
perspectiva de resiliencia y protección de datos; su estado permanece
`Proposed` hasta que Jairo (y Kevin en ADR-012) registren su aprobación.

## 4. Revisión del esquema e invariantes de inventario (Jorge Pumay)

### 4.1 Fortalezas verificadas

- `tryReserve`/`tryRelease` son un único `UPDATE ... WHERE` atómico; no existe
  ventana leer-decidir-escribir para la última unidad.
- `CHECK (quantity_reserved <= quantity_on_hand)`, no negativos, `UNIQUE`
  `(product_id, location_id)` y `UNIQUE (idempotency_key)` como red de
  seguridad.
- Prueba de concurrencia real: 8 solicitudes simultáneas por la última unidad
  → exactamente una reserva, el resto `409` (`InventoryConcurrencyIntegrationTest`).
- Expiración con `saveAndFlush` documentado y auto-inyección `@Lazy` para no
  perder `@Transactional`; `tryRelease` idempotente por `CHECK`.
- La limitación de idempotencia con la misma clave simultánea está declarada
  con honestidad y es distinta del escenario E1 de CDU-02.

### 4.2 Hallazgos

| # | Severidad | Hallazgo | Recomendación |
|---|---|---|---|
| J1 | Media (latente) | `releaseExpiredReservations` procesa todo el barrido en una sola transacción: un conflicto de bloqueo optimista (por ejemplo, una reserva confirmada mientras se barre) aborta el lote completo y las demás reservas vencidas no se liberan | Procesar cada reserva en su propia transacción o capturar `OptimisticLockingFailureException` por reserva; añadir prueba de confirmación concurrente cuando exista ORD-04 |
| J2 | Baja | `sgo-logic.inventory.reservation-ttl` y `expiration-sweep-interval` no aparecen en `.env.example`, Compose ni README, aunque el checklist de entrega lo exige para variables nuevas | Externalizarlas en la rama de inventario (ver desviación D3 de la matriz de trazabilidad) |
| J3 | Baja | `reserveAtBestOrigin` ordena candidatos en memoria tras leer todos los ítems del producto | Aceptable con 34 ubicaciones; documentar el límite si el catálogo crece |

### 4.3 Decisión

**Aceptar con observaciones.** El esquema y las invariantes cumplen CDU-02,
ADR-011 y ADR-014. J1 debe registrarse como riesgo para ORD-04; J2 se corrige
en la misma línea de inventario.

## 5. Coherencia documentación ↔ código

La matriz completa está en `docs/qa/trazabilidad-cun-mvp.md` §5. Resumen:

- **D1:** el stack describe módulos por capas (`api/domain/application/adapter`)
  y el código usa paquetes planos.
- **D2:** el stack prescribe React 19/TypeScript/Vite y el frontend es
  HTML/CSS/JS estático.
- **D3:** variables de inventario no externalizadas.
- **D4:** JUnit 5 (plan) vs JUnit Jupiter 6.0.3 (BOM de Spring Boot 4.1.1).
- **D5:** `integration` mezcla adaptadores ERP/POS de Andy con el futuro
  Outbox de Edson; usar subpaquetes al implementar BB-04.

Ninguna desviación implica código huérfano: todas tienen respaldo en un CDU,
un EaC, un ADR o una tarjeta, y deben ratificarse o corregirse antes del
release candidate.

## 6. Artefactos de trazabilidad generados por esta revisión

- `docs/diagrams/modelo-datos.md`: cierra el artefacto de modelo de datos
  exigido por la tabla de penalización.
- `docs/qa/trazabilidad-cun-mvp.md`: mapea los doce CUN/CDU a código, datos,
  despliegue, EaC, ADR y pruebas.

## 7. Hallazgo crítico de la prueba manual y corrección (A8)

La prueba de humo contra el stack real de Docker Compose (no solo la suite
automatizada) detectó que **todos los flujos asíncronos quedaban atascados**
en su estado inicial:

| Flujo | Síntoma observado | Estado que nunca alcanzaba |
|---|---|---|
| Fiscal SAT | `submissionAttempts = 1`, sin código de falla | `APPROVED` |
| ERP | lote `PENDING` con `attempts = 1` | `DELIVERED` |
| POS | entrega `PENDING` con `attempts = 1` | `ACKNOWLEDGED` |
| Despacho | guía no emitida, reintento programado muerto | `LABEL_ISSUED` |
| Notificaciones | solicitud `PENDING` con `attempts = 1` | `SENT` |

**Causa raíz:** los servicios guardaban la misma entidad desconectada dos
veces (marca de intento y resultado). `repository.save` devuelve la copia
gestionada con la `@Version` incrementada, pero el código ignoraba el retorno
y el segundo guardado usaba la versión obsoleta, lanzando
`ObjectOptimisticLockingFailureException` en el hilo asíncrono. El hecho de
negocio quedaba persistido, pero el flujo nunca terminaba y no se agendaba el
reintento. Las pruebas unitarias con repositorios en memoria no lo detectaban
porque no existe `@Version` real.

**Corrección:** reasignar la copia devuelta por `save` en
`FiscalService`, `ErpIntegrationService`, `PosIntegrationService`,
`ShipmentService` y `NotificationService`, capturando los identificadores
antes de programar reintentos. El mock de `NotificationServiceTest` también
devolvía `null` en `save`, lo que ocultaba el comportamiento real.

**Regresión:** `AsyncIntegrationPersistenceTest` (5 pruebas con PostgreSQL y
RabbitMQ reales de Testcontainers y ejecutores asíncronos reales) espera el
estado terminal de los cinco flujos. Sin la corrección, las cinco pruebas
fallan por tiempo de espera; con ella, `clean verify` queda en 65/65.

**Impacto en EaC:** sin esta corrección, EaC-04 (pago/SAT), EaC-06 (POS),
EaC-05 (LogiExpress) y EaC-08 (notificaciones) no podían demostrarse en el
stack real aunque el código pareciera correcto.
