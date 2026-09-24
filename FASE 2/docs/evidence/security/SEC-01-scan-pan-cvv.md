# SEC-01 — Pruebas de autorización y fuga de PAN/CVV

**Responsable:** Edson Sosa
**Fecha:** 2026-09-23
**Tarjeta:** SEC-01 (depende de BB-07 y PAY-01)
**Sustento:** CDU-03 paso 3, EaC-12, R4 y ADR-013

## 1. Objetivo

Demostrar con evidencia automatizada que:

1. Los roles restringen los endpoints y que sin token la API responde `401`.
2. No aparecen PAN completo (13–19 dígitos) ni marcadores CVV/CVC en base de
   datos, mensajes persistidos del Outbox, estados de auditoría ni logs.

## 2. Método

`SensitiveDataLeakScanTest` genera actividad real contra el stack de pruebas
(PostgreSQL 17 y RabbitMQ 4.3 reales mediante Testcontainers):

- Pago aprobado con referencia segura y dos intentos con PAN (`4111111111111111`)
  y CVV (`cvv=123`) que deben rechazarse con `400`.
- Incidencia registrada y resuelta, despacho con guía y tracking, notificación
  no bloqueante, documento fiscal asíncrono, consulta de auditoría y un `401`
  sin token.

Después escanea:

- **Base de datos:** todas las columnas de texto/JSON del esquema `public`
  (incluye `outbox_event.payload`, `audit_events.before_state/after_state`,
  `payment_attempts`, `payment_reconciliations`, etc.) con las expresiones
  `[0-9]{13,19}` y `(?i)\y(cvv|cvc)\y`.
- **Logs:** captura el appender raíz de Logback durante la actividad y aplica
  los mismos patrones.
- **No vacuidad:** exige más de 20 columnas escaneadas, logs no vacíos, un
  único intento persistido (los de PAN/CVV no se guardan) y eventos en Outbox
  y auditoría.

`SecurityAuthorizationTest.roleMatrixRestrictsEndpoints` cubre además la
matriz 401/403 por grupo de endpoints.

## 3. Resultado

| Verificación | Resultado |
|---|---|
| `./mvnw clean verify` (Java 25 + Testcontainers) | `BUILD SUCCESS`, 118 pruebas, 0 fallos |
| PAN/CVV en base, Outbox, auditoría y logs | 0 apariciones |
| Intento con PAN o CVV | `400 INVALID_PAYMENT_REQUEST`; no se persiste |
| Sin token | `401 UNAUTHENTICATED` |
| Rol insuficiente | `403 FORBIDDEN` (cliente en fiscal/query/notificaciones; logística en pagos/auditoría; finanzas en despachos/pagos; soporte en fiscal) |

## 4. Limitaciones

- El escaneo cubre la base de datos de pruebas y los payloads persistidos del
  Outbox; no inspecciona las colas de RabbitMQ en vuelo ni un agregador de
  logs externo (fuera del alcance del MVP).
- La alerta de conciliación y los errores se registran como log y evidencia
  auditable; no hay canal externo de notificaciones.
- El escaneo de logs se ejecuta en el mismo JVM de la prueba; en un ambiente
  real debe repetirse sobre la retención centralizada.

## 5. Conclusión

EaC-12 queda **Cubierto** para el MVP: el sistema no almacena PAN completo ni
CVV, rechaza esas referencias en el borde, enmascara los datos sensibles antes
de un log y restringe los endpoints por rol.
