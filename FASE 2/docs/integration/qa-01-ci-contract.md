# QA-01 — Contrato de integración continua

## Alcance implementado

El workflow `.github/workflows/qa-integracion-base.yml` separa la validación en dos jobs:

1. `test-backend`: Java 25, caché Maven, `clean verify`, Spring Modulith, Testcontainers y reportes Surefire.
2. `smoke-platform`: construcción y arranque de Compose, tres health checks, Actuator, status y correlation ID.

El segundo job depende del primero. Compose se apaga incluso cuando ocurre un fallo y nunca se usa `down -v`.

## Secretos opcionales en GitHub

Configurar en `Settings → Secrets and variables → Actions`:

| Secreto | Contenido esperado |
|---|---|
| `SGO_POSTGRES_DB` | Nombre ficticio de base de CI |
| `SGO_POSTGRES_USER` | Usuario exclusivo de CI |
| `SGO_POSTGRES_PASSWORD` | Contraseña no productiva de CI |
| `SGO_RABBITMQ_USER` | Usuario exclusivo de CI |
| `SGO_RABBITMQ_PASSWORD` | Contraseña no productiva de CI |

No usar credenciales productivas. Como los servicios existen únicamente durante la ejecución de CI y no se despliegan, el workflow dispone de valores ficticios de respaldo. Si un secreto no existe, utiliza el respaldo correspondiente en lugar de abortar. Cuando el secreto sí existe, GitHub enmascara su valor. `docker compose config` se ejecuta en modo silencioso para no imprimir la interpolación.

El fallo observado `Falta configurar el secreto asociado a RABBITMQ_PASSWORD` significaba que `SGO_RABBITMQ_PASSWORD` no estaba disponible para el job. Ya no bloquea el pipeline, aunque se recomienda corregir el nombre del secreto.

## Disparadores

- Pull requests que modifican backend, smoke test, Compose, `.env.example` o el workflow.
- Push a `develop` o `main` sobre esas rutas.
- Ejecución manual mediante `workflow_dispatch`.

## Criterio de cierre

QA-01 solo puede marcarse terminado cuando exista una ejecución verde enlazada desde la bitácora o tarjeta Kanban. Configurar los secretos sigue siendo recomendable, pero no es requisito técnico para estos contenedores efímeros sin despliegue.
