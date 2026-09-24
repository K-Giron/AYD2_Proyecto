# Checklist de entrega de módulo — Kevin Girón

Copia de `module-delivery-checklist.md` completada para esta entrega. Un campo pendiente impide afirmar que la integración terminó; no se rellena con evidencia inventada.

## Identificación

- Tarjeta Kanban: pendiente de actualizar en Trello (`MegaStore - Fase 2`); esta entrega cubre LOG-01, LOG-02, LOG-03, NOT-01, QRY-01, INC-01 y UI-01.
- Módulo y propietario: `logistics`, `notifications`, `query`, `incidents`, `frontend` — Kevin Girón.
- Rama o PR: `feature/kevin-logistica-consulta-notificaciones-incidencias` (local, sin publicar; sin PR todavía).
- Revisor distinto del autor: **pendiente**. No se acepta como `Done` sin uno (regla explícita de §8.3 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`).
- Contratos implementados y versión: puerto de despacho de `saga-required-ports.md` (borrador `PROPOSED`); sobre de eventos v1 **no** se usa todavía (Query se alimenta por API temporal, no por evento, según se documenta en el propio código).

## Límites arquitectónicos

- [x] El cambio permanece dentro del módulo propietario (cada tabla, entidad y servicio nuevo vive en su propio paquete de módulo).
- [x] No accede a repositorios, entidades, adaptadores o tablas de otro módulo.
- [x] Las dependencias entre módulos usan una API pública o evento aprobado — en este incremento, ninguno de mis módulos depende de otro módulo de negocio (verificado por `ModularArchitectureTest`).
- [x] No introduce ciclos según Spring Modulith (`ApplicationModules.of(...).verify()` pasa).
- [x] No mueve reglas de negocio a `shared` (`SharedClockConfig` y `type = OPEN` son infraestructura técnica, no lógica de negocio).
- [ ] **No modifica `pom.xml`, Compose o configuración común sin justificarlo — con excepciones documentadas:**
  - Se modificó `docker-compose.yml` para agregar el servicio `frontend`: justificado explícitamente por ADR-009 ("El frontend se agregará cuando Kevin entregue la base de UI").
  - Se modificó `shared/package-info.java` (`type = OPEN`) y `ModularArchitectureTest.java`: justificado porque, sin este cambio, ningún módulo puede usar `shared.http` (formato común de error, `X-Correlation-Id`) tal como exige este mismo checklist. **Pendiente de aprobación de Jairo Gómez (arquitecto integrador) antes de integrar a `develop`.**
  - `pom.xml` no se modificó.

## Datos y configuración

- [x] Cada migración pertenece al módulo y tiene prueba o procedimiento de validación (`V1001`-`V1004`, verificadas por `./mvnw clean verify` contra PostgreSQL real vía Testcontainers).
- [x] No se incluyeron credenciales reales.
- [x] Las variables nuevas (`FRONTEND_PORT`) aparecen en `.env.example` y en el README.
- [x] No se registran cuerpos, tokens, PAN, CVV ni datos sensibles (estos módulos no manejan datos de pago).

## API y eventos

- [x] Endpoints bajo `/api/v1` y JSON `camelCase`.
- [x] UTC/ISO 8601, UUID y formato común de error (`ApiError` reutilizado de `shared.http`).
- [x] `X-Correlation-Id` se propaga (verificado manualmente en la prueba de humo del 2026-09-22).
- [x] Operaciones reintentables definen `Idempotency-Key` (`POST /api/v1/shipments`).
- [ ] Eventos usan nombre, versión y sobre común acordados — **no aplica todavía**: ninguno de estos módulos publica ni consume eventos reales; Query se alimenta por un endpoint de ingesta temporal, documentado como tal hasta que el Outbox (BB-04) exista.
- [x] Cambios incompatibles tienen nueva versión o ADR — no hubo cambios incompatibles sobre contratos ya aceptados (no existían contratos aceptados que romper).

## Verificación

- [x] `./mvnw clean verify` termina correctamente — `BUILD SUCCESS`, 24/24 pruebas, 2026-09-22, ejecutado en contenedor `eclipse-temurin:25.0.4_7-jdk` con Testcontainers reales (Postgres 17.11, RabbitMQ 4.3.5).
- [x] Las pruebas positivas, negativas y de concurrencia relevantes están incluidas — positivas y negativas sí (14 pruebas nuevas cubren éxito, timeout/DLQ, indisponibilidad de proveedor, idempotencia, casos no encontrados). **No hay prueba de concurrencia** para estos módulos en este incremento (a diferencia de INV-02/INV-05 de Jorge, que sí la exigen explícitamente); se documenta como limitación.
- [ ] `./tests/smoke/verify-platform.sh` termina correctamente — **no se ejecutó el script formal** porque no contempla el nuevo servicio `frontend` (fallaría o lo ignoraría). En su lugar se hizo una verificación manual equivalente: `docker compose up --build -d` con los 4 servicios en `healthy`, seguida de llamadas reales a `POST /api/v1/shipments` (201), `POST /api/v1/query/order-summary` + `GET .../summary` (200), `POST /api/v1/incidents` (201) y `POST /api/v1/notifications` (202), más el caso negativo de consulta a orden inexistente (404). Pendiente: actualizar `tests/smoke/verify-platform.sh` para que valide también `frontend`.
- [x] Se adjunta salida o evidencia reproducible (bitácora del 2026-09-22, esta checklist y `QA-03`/`QA-05`/`DEMO-01`).
- [x] Se documentan limitaciones y trabajo pendiente (README §"Limitaciones actuales", bitácora, y cada uno de los documentos de `docs/qa/`).

## Resultado

- Decisión: **Corregir** — no `Aceptar` todavía. Motivos: falta revisor distinto del autor, faltan las dos aprobaciones de Jairo sobre los cambios a `shared`/`ModularArchitectureTest`, y `verify-platform.sh` necesita actualizarse para el nuevo servicio `frontend`.
- Observaciones: el código funcional está completo y probado (unitario + integración real + smoke manual); lo que falta es proceso de integración del equipo, no implementación.
- Fecha y evidencia: 2026-09-22; ver `BITACORA_FASE_2.md` (entrada de Kevin Girón) y `docs/qa/`.
