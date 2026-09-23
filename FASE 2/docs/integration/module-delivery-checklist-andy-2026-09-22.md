# Checklist de entrega de módulo — Andy Sánchez

Copia de `module-delivery-checklist.md` completada con evidencia local. No declara aceptación ni actualiza Trello.

## Identificación

- Tarjetas: DEC-01, DEC-04, BB-06, FIS-01, FIS-02, FIS-03, ERP-01, ERP-02, POS-01 y DOC-02.
- Módulos y propietario: `fiscal`, integraciones ERP/POS y contratos simulados — Andy Sánchez.
- Rama o PR: trabajo local sobre `develop`; sin commit, push ni PR por instrucción de Andy.
- Revisor distinto del autor: Edson Sosa, pendiente.
- Contratos: ADR-010 y ADR-012 en estado `Proposed`; puerto fiscal de ORD-03 todavía pendiente de ratificación por Jairo.

## Límites arquitectónicos

- [x] Cada agregado permanece dentro de su módulo propietario.
- [x] No accede a repositorios, entidades ni tablas privadas de otros módulos.
- [x] Las referencias a Orders/Payments/Eventos son UUID o contratos de puerto.
- [x] Spring Modulith no detecta ciclos ni dependencias prohibidas.
- [x] `shared` solo recibe el enum técnico común `SimulatorMode`.
- [x] Los cambios comunes de configuración se justifican por BB-06, ERP-01 y ADR-012.

## Datos y configuración

- [ ] Las migraciones `V2001`–`V2003` tienen revisión estática, pero falta ejecutarlas contra PostgreSQL real por ausencia de Docker local.
- [x] No se incluyeron credenciales reales.
- [x] Modos, latencias, tasas, tiendas POS y ventana ERP aparecen en `.env.example`, Compose y `application.yml`.
- [x] PayKlik solo acepta una referencia segura y rechaza valores que parezcan PAN o incluyan CVV.

## API y eventos

- [x] Endpoints nuevos bajo `/api/v1`, JSON `camelCase`, UUID e instantes UTC.
- [x] Se reutilizan correlación y formato común de error.
- [x] Fiscal exige `Idempotency-Key`; ERP deduplica por orden y POS por evento+tienda.
- [ ] El sobre común y Outbox no se conectan todavía porque BB-04 no existe en el árbol local.
- [x] Los contratos no aceptados permanecen rotulados `Proposed`.

## Verificación

- [ ] `./mvnw clean verify`: pendiente con Java 25 y Docker.
- [x] Compilación local: `BUILD SUCCESS` con Java 21 como compatibilidad de fuentes.
- [x] Suite sin contenedores: 40/40 verdes; incluye las 16 pruebas nuevas y los módulos existentes.
- [ ] `./tests/smoke/verify-platform.sh`: pendiente; Docker no está instalado.
- [x] Limitaciones y pasos pendientes registrados en FIS-03, README, ADR y bitácora.

## Resultado

- Decisión: **Corregir/Revisar antes de integrar**.
- Pendiente: revisión de Edson, ratificación de contratos por Jairo/equipo, ejecución Java 25 + Docker y conexión con BB-04/ORD-03.
- Evidencia: `docs/qa/FIS-03-pruebas-fiscal-erp-pos.md` y `BITACORA_FASE_2.md`.
