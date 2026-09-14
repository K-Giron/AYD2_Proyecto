# QA-01 — Contrato de integración continua

## Alcance implementado

El workflow `.github/workflows/qa-integracion-base.yml` ejecuta:

1. Java 25 con caché Maven.
2. `./mvnw --batch-mode clean verify`.
3. Pruebas de Spring Modulith y Testcontainers.
4. Construcción y arranque de Docker Compose.
5. Health checks de PostgreSQL, RabbitMQ y backend.
6. Verificación HTTP de Actuator, status y correlation ID.
7. Publicación de reportes Surefire.
8. `docker compose down` incluso cuando ocurre un fallo, sin eliminar volúmenes.

## Secretos requeridos en GitHub

Configurar en `Settings → Secrets and variables → Actions`:

| Secreto | Contenido esperado |
|---|---|
| `SGO_POSTGRES_DB` | Nombre ficticio de base de CI |
| `SGO_POSTGRES_USER` | Usuario exclusivo de CI |
| `SGO_POSTGRES_PASSWORD` | Contraseña no productiva de CI |
| `SGO_RABBITMQ_USER` | Usuario exclusivo de CI |
| `SGO_RABBITMQ_PASSWORD` | Contraseña no productiva de CI |

No usar credenciales productivas. El workflow valida únicamente presencia y GitHub enmascara los valores; `docker compose config` se ejecuta en modo silencioso para no imprimir la interpolación.

## Disparadores

- Pull requests que modifican backend, smoke test, Compose, `.env.example` o el workflow.
- Push a `develop` o `main` sobre esas rutas.
- Ejecución manual mediante `workflow_dispatch`.

## Criterio de cierre

QA-01 solo puede marcarse terminado cuando los secretos estén configurados y exista una ejecución verde enlazada desde la bitácora o tarjeta Kanban.
