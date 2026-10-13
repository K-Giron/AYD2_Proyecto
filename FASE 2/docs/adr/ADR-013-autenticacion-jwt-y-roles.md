# ADR-013 — Autenticación JWT, roles y sanitización

- **Fecha:** 2026-09-23
- **Estado:** Proposed
- **Responsable:** Edson Sosa
- **Revisores:** Jairo Gómez, Andy Sánchez y Kevin Girón
- **Decisión previa:** DEC-06

## Contexto

El DDA no fija un mecanismo de autenticación, pero exige que los solicitantes
de evidencia estén autorizados (CDU-12, precondición) y que los datos de pago
nunca expongan PAN completo ni CVV (EaC-12, R4). La planificación de Fase 2
(DEC-06) pide roles `CLIENT`, `SUPPORT`, `LOGISTICS` y `FINANCE`, autorización
por endpoint y sanitización de logs. El equipo decidió JWT el 2026-09-23.

La UI de Fase 2 tenía un login simulado en el navegador porque el módulo
`security` no existía. Con esta decisión el login pasa a ser real y las
llamadas de la UI envían el token.

## Drivers relacionados

- CDU-12, precondición "el solicitante está autorizado"; CDU-03 paso 3.
- EaC-12: cero PAN completo/CVV en base, mensajes y logs.
- R4: normativa de protección de datos.
- ADR-001: módulos propietarios de su lógica; la seguridad es transversal.
- BB-07, SEC-01 y PAY-01.

## Decisión

1. **JWT sin estado** firmado con HMAC-SHA256; vigencia configurable
   (`SGO_SECURITY_JWT_TTL`, por defecto 8 h). El token incluye `sub`, `roles`,
   `iss`, `iat`, `exp` y `jti`.
2. **Login** en `POST /api/v1/auth/login` con usuario y contraseña. Las
   contraseñas se codifican con BCrypt en memoria al arrancar; nunca se
   registran ni se devuelven.
3. **Autorización por rol** en la cadena de filtros:

   | Recurso | Roles autorizados |
   |---|---|
   | `/api/v1/auth/login`, `/actuator/health*`, `/actuator/info`, `/api/v1/platform/**` | público |
   | `/api/v1/fiscal-documents/**`, `/api/v1/integrations/erp/**`, `/api/v1/simulators/**`, `/api/v1/audit/**` | `FINANCE` |
   | `GET /api/v1/shipments/**` (seguimiento, CDU-09) | `CLIENT`, `SUPPORT`, `LOGISTICS` |
   | Resto de `/api/v1/shipments/**` y `/api/v1/integrations/pos/**` | `LOGISTICS` |
   | `GET /api/v1/notifications/**` | `CLIENT`, `SUPPORT` |
   | `POST /api/v1/notifications` | `SUPPORT` |
   | `GET` y `POST /api/v1/incidents/**` | `CLIENT`, `SUPPORT` |
   | `PATCH /api/v1/incidents/**` (resolución) | `SUPPORT` |
   | `/api/v1/inventory/**` | `CLIENT`, `SUPPORT`, `LOGISTICS` |
   | `/api/v1/query/**` | `SUPPORT`, `LOGISTICS` |
   | Resto | cualquier token válido |

4. **Respuestas 401/403** usan el formato común `ApiError` con
   `correlationId`, códigos `UNAUTHENTICATED`, `INVALID_CREDENTIALS` y
   `FORBIDDEN`.
5. **Sanitización:** `SensitiveDataSanitizer` enmascara PAN (13–19 dígitos con
   separadores opcionales) y campos CVV/CVC antes de escribir un log; los
   eventos de seguridad registran la ruta enmascarada.
6. **Cuentas de demostración** configurables por variables `SGO_SECURITY_*`;
   no son cuentas productivas. La cuenta de personal interno
   (`staff@megastore.com`) acumula `SUPPORT` y `LOGISTICS` porque la consola
   de UI-01 cubre despacho y atención en una sola pantalla.
   `sgo.security.enabled=false` permite a las pruebas de negocio ejecutarse
   sin token; la prueba de seguridad lo activa explícitamente.
7. La UI deja de validar cuentas en el navegador: guarda el JWT y lo envía
   como `Authorization: Bearer`.

## Alternativas evaluadas

1. **Sesión con cookie:** requiere estado de servidor y protección CSRF;
   innecesaria para una API consumida por fetch y Postman.
2. **HTTP Basic:** simple, pero envía credenciales en cada petición y no
   modela roles con vigencia.
3. **Keycloak u otro IdP:** fuera del alcance del MVP y de R5; se mantiene
   como evolución futura.

## Consecuencias

- La API es stateless y el token viaja en cada petición.
- HS256 comparte un secreto simétrico: en producción debe migrarse a claves
  asimétricas o a un IdP sin reescribir la autorización por roles.
- No hay refresh token, revocación ni límite de intentos en el MVP; se
  documentan como limitaciones.
- Las cuentas demo viven en configuración local; cualquier ambiente real debe
  reemplazarlas y no versionar secretos.
- El frontend (UI-01, Kevin Girón) cambió su login simulado por el real; el
  cambio se somete a su revisión.

## Criterios de validación o ratificación

- Login válido devuelve token con roles; credenciales inválidas devuelven 401
  con `ApiError`.
- Sin token → 401; rol insuficiente → 403; rol correcto → 200.
- El sanitizador enmascara PAN/CVV y conserva UUID y fechas.
- SEC-01 escanea base, mensajes y logs en busca de PAN/CVV.
- Jairo, Andy y Kevin revisan la matriz de autorización antes de aceptar el ADR.

## Actualización Fase 2 (2026-10-13)

Roles y reglas que se agregaron con ADR-015, tal como están en
`SecurityConfig` y `application.yml` al 2026-10-13:

| Rol | Cuenta | Para qué |
|---|---|---|
| `ONLINE_STORE` | `tienda-online@megastore.com` (también tiene `SUPPORT`, `LOGISTICS` y `FINANCE`) | Administrar la tienda online: recepción, catálogo, traslados y bitácora (`/api/v1/online-store/**`) |
| `STORE` | `terminal-pos01@megastore.com` (también tiene `SUPPORT`) | El sistema de la tienda física confirma la entrega de un pedido web al cliente (`POST /api/v1/store-pickups/*/handoff`) |
| `POS_EVENTS` | `central-eventos@megastore.com` (cuenta de servicio, no de persona) | El almacén central entrega los eventos de stock de POS-ONLINE (`POST /api/v1/online-store/stock-events`) |

Reglas nuevas en la tabla de autorización:

| Recurso | Roles |
|---|---|
| `GET /api/v1/inventory/stock`, `GET /api/v1/online-catalog/**` | público (catálogo publicado) |
| `GET /api/v1/online-store/inventory`, `/movements` | `ONLINE_STORE`, `SUPPORT`, `LOGISTICS`, `FINANCE` |
| `POST /api/v1/online-store/stock-events` | `POS_EVENTS` |
| Resto de `/api/v1/online-store/**` | `ONLINE_STORE` |
| `GET /api/v1/invoices/orders/*` (factura del cliente) | `CLIENT`, `FINANCE`, `ONLINE_STORE` |
| `/api/v1/reports/**` (informe de ventas) | `FINANCE` |
| `POST /api/v1/store-pickups/*/handoff` | `STORE` |

Los sistemas propios tienen su propio control de acceso:
- `inventory-service` (almacén central) usa JWT propio con los roles
  `ALMACEN`, `FINANZAS`, `LOGISTICA` y `TIENDA` (una cuenta por punto de venta).
- `payklik-service` y `logiexpress-service` exigen `X-Api-Key` del comercio
  en su API.
- Sus portales y el de la tienda física (`store-service`) se usan dentro de
  la red interna, sin contraseña de operador. Es una decisión consciente del
  alcance académico, documentada en ADR-015.

EaC-12 sigue cubierto: PayKlik rechaza PAN y CVV (`SENSITIVE_DATA`) y nunca los
guarda. `SensitiveDataLeakScanTest` ahora confirma cada candidato con Luhn y
descarta UUID, para no fallar al azar (E5).

## Referencias

- `backend/src/main/java/com/megastore/sgologic/security/`
- `backend/src/test/java/com/megastore/sgologic/security/`
- `docs/contracts/openapi/api-conventions.md`
- `Docs/DDA.md` §3.16 (CDU-12), §4.2 (EaC-12)
