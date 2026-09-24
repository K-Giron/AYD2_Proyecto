# Checklist de entrega de módulo — Edson Sosa (BB-07)

Copia de `module-delivery-checklist.md` completada con evidencia local. No declara aceptación ni actualiza Trello.

## Identificación

- Tarjeta: BB-07 (autenticación, autorización y sanitización) + DEC-06/ADR-013.
- Módulo y propietario: `security` — Edson Sosa.
- Rama o PR: `docs/rev-edson-cruzadas`; sin push ni PR por indicación del equipo.
- Revisor distinto del autor: Jairo, Andy y Kevin, pendientes (ADR-013 exige su revisión).
- Contratos implementados: `ADR-013` (`Proposed`) y convenciones HTTP de `shared.http`.

## Límites arquitectónicos

- [x] El cambio permanece dentro del módulo `security` y de la UI de Kevin (cambio declarado para su revisión).
- [x] No accede a repositorios, entidades ni tablas de otros módulos.
- [x] No introduce dependencias entre módulos de negocio; `ModularArchitectureTest` sigue en verde.
- [x] No mueve reglas de negocio a `shared`.
- [x] `pom.xml` cambió solo para agregar `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` y `spring-boot-starter-webmvc-test` (pruebas), justificado por BB-07.

## Datos y configuración

- [x] Sin tablas nuevas: las cuentas demo viven en configuración local (`SGO_SECURITY_*`) y las contraseñas se codifican con BCrypt al arrancar.
- [x] No se incluyeron credenciales reales; las de demo son ficticias y locales.
- [x] Las variables nuevas aparecen en `.env.example`, Compose, `application.yml` y README.
- [x] No se registran contraseñas, tokens, PAN ni CVV; `SensitiveDataSanitizer` enmascara PAN/CVV y las rutas de los 401/403.

## API y eventos

- [x] `POST /api/v1/auth/login` bajo `/api/v1`, JSON `camelCase` y `ApiError` común.
- [x] `X-Correlation-Id` se propaga y aparece en las respuestas 401/403.
- [x] Sin token → `401 UNAUTHENTICATED`; rol insuficiente → `403 FORBIDDEN`; credenciales inválidas → `401 INVALID_CREDENTIALS`.
- [x] El token incluye `sub`, `roles`, `iss`, `iat`, `exp` y `jti`, firmado con HMAC-SHA256.

## Verificación

- [x] `./mvnw clean verify` con Java 25: `BUILD SUCCESS`, 85 pruebas, 0 fallos, 0 errores, 0 omitidas.
- [x] Pruebas positivas (login, acceso por rol), negativas (401/403) y de sanitización.
- [x] Smoke en Compose: login de los cuatro roles, 401/403 reales y frontend servido con login real.
- [x] Se adjunta evidencia en la bitácora y en `docs/qa/trazabilidad-cun-mvp.md`.
- [x] Limitaciones documentadas: cuentas demo, sin refresh token, sin revocación ni límite de intentos; TLS no implementado localmente.

## Resultado

- Decisión: **Corregir/Revisar antes de integrar**.
- Pendiente: revisión de la matriz de autorización y del cambio de UI por Jairo, Andy y Kevin; ratificación de ADR-013.
- Evidencia: `docs/adr/ADR-013-autenticacion-jwt-y-roles.md`, `BITACORA_FASE_2.md` (2026-09-23) y reportes Surefire.
