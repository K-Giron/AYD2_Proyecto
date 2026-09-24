# Convenciones de API v1

**Estado del contrato:** `PROPOSED`

- Prefijo de API: `/api/v1`.
- Cuerpos y respuestas: JSON con propiedades `camelCase`.
- Fechas e instantes: ISO 8601 en UTC.
- Identificadores de dominio: UUID.
- Operaciones funcionales reintentables: encabezado `Idempotency-Key`; su semántica definitiva sigue pendiente.
- Correlación: encabezado `X-Correlation-Id`. La API reutiliza un UUID válido recibido o genera uno y lo devuelve.
- Error común: `code`, `message`, `timestamp`, `path` y `correlationId`.

El único endpoint declarado en este incremento es el técnico `GET /api/v1/platform/status`. No se proponen endpoints funcionales de módulos ajenos ni se declara autenticación hasta BB-07/ADR-013.
