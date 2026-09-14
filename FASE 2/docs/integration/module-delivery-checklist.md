# Checklist de entrega de un módulo

Completar una copia por entrega. Un campo pendiente impide afirmar que la integración terminó, pero no debe rellenarse con evidencia inventada.

## Identificación

- Tarjeta Kanban: pendiente.
- Módulo y propietario: pendiente.
- Rama o PR: pendiente.
- Revisor distinto del autor: pendiente.
- Contratos implementados y versión: pendiente.

## Límites arquitectónicos

- [ ] El cambio permanece dentro del módulo propietario.
- [ ] No accede a repositorios, entidades, adaptadores o tablas de otro módulo.
- [ ] Las dependencias entre módulos usan una API pública o evento aprobado.
- [ ] No introduce ciclos según Spring Modulith.
- [ ] No mueve reglas de negocio a `shared`.
- [ ] No modifica `pom.xml`, Compose o configuración común sin justificarlo.

## Datos y configuración

- [ ] Cada migración pertenece al módulo y tiene prueba o procedimiento de validación.
- [ ] No se incluyeron credenciales reales.
- [ ] Las variables nuevas aparecen en `.env.example` y README.
- [ ] No se registran cuerpos, tokens, PAN, CVV ni datos sensibles.

## API y eventos

- [ ] Endpoints bajo `/api/v1` y JSON `camelCase`.
- [ ] UTC/ISO 8601, UUID y formato común de error.
- [ ] `X-Correlation-Id` se propaga.
- [ ] Operaciones reintentables definen `Idempotency-Key`.
- [ ] Eventos usan nombre, versión y sobre común acordados.
- [ ] Cambios incompatibles tienen nueva versión o ADR.

## Verificación

- [ ] `./mvnw clean verify` termina correctamente.
- [ ] Las pruebas positivas, negativas y de concurrencia relevantes están incluidas.
- [ ] `./tests/smoke/verify-platform.sh` termina correctamente.
- [ ] Se adjunta salida o evidencia reproducible.
- [ ] Se documentan limitaciones y trabajo pendiente.

## Resultado

- Decisión: pendiente (`Aceptar`, `Corregir` o `Bloquear`).
- Observaciones: pendiente.
- Fecha y evidencia: pendiente.
