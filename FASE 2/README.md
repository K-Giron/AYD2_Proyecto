# SGO-Logic — Fase 2

## Objetivo

Materializar el DDA de la Fase 1 como un MVP reproducible de gestión de órdenes y logística omnicanal. Este incremento crea el backbone técnico; no implementa todavía flujos de negocio.

## Arquitectura

El backend es un único JAR organizado como monolito modular. Cada paquete directo bajo `com.megastore.sgologic` es un módulo Spring Modulith con identidad estable. Cada módulo será propietario de su lógica y datos; la colaboración futura será mediante APIs públicas o eventos, nunca mediante repositorios, entidades JPA, adaptadores o tablas privadas ajenas. `shared` contiene solo infraestructura transversal mínima.

Docker Compose conecta los tres servicios mediante una red bridge dedicada al proyecto. La red no se comparte con otros proyectos; sus puertos publicados permiten el acceso local requerido. En un ambiente real, PostgreSQL y RabbitMQ deben permanecer sin exposición pública.

## Estructura

```text
FASE 2/
├── backend/          Spring Boot, módulos y pruebas
├── simulator-data/   Salidas reproducibles futuras de simuladores
├── tests/            E2E, k6, seguridad y datos
├── docs/             ADR, contratos, evidencia y diagramas
├── .env.example
├── .gitignore
└── docker-compose.yml
```

El frontend no se crea en este incremento. Se agregará cuando Kevin Girón entregue la base de UI.

## Tecnologías y versiones

| Elemento | Versión |
|---|---|
| Java / Eclipse Temurin | 25 / 25.0.4_7 en Docker |
| Spring Boot | 4.1.1 |
| Spring Modulith | 2.1.1 |
| Maven | 3.9.11 |
| Maven Wrapper | 3.3.4 (scripts del proyecto) |
| PostgreSQL | 17.11, Alpine 3.23 |
| RabbitMQ | 4.3.5 Management, Alpine |
| Testcontainers | 2.0.5, administrado por Spring Boot |
| JUnit Jupiter | 6.0.3, administrado por Spring Boot |
| AssertJ | 3.27.7, administrado por Spring Boot |
| Empaquetado | JAR |

## Propietarios

| Responsable | Módulos o áreas |
|---|---|
| Jairo Gómez | plataforma, `orders`, Saga e integración E2E |
| Jorge Pumay | `inventory`, reservas y modelo relacional |
| Edson Sosa | `payments`, `audit`, `security`, Outbox e infraestructura de integración |
| Andy Sánchez | `fiscal`, SAT, ERP, POS y simuladores |
| Kevin Girón | `logistics`, `notifications`, `query`, `incidents` y frontend |

`integration` es un área compartida bajo la propiedad técnica definida para Edson y la coordinación de archivos comunes de Jairo. En este incremento solo se declara el módulo.

## Requisitos locales

- JDK 25 para ejecución fuera de Docker.
- Docker Engine y Docker Compose.
- `curl` o `wget`, `tar` y `sha512sum` para la primera descarga del wrapper en Linux/macOS.
- No se requiere Maven global.

## Variables de ambiente

Copie manualmente `.env.example` como `.env` solo si desea personalizar el entorno; `.env` está ignorado y nunca debe versionarse. Las variables disponibles son:

- `BACKEND_PORT`
- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_HOST`, `POSTGRES_PORT`
- `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT`
- `RABBITMQ_MANAGEMENT_PORT`
- `SPRING_PROFILES_ACTIVE`

Los valores predeterminados son exclusivamente locales y ficticios.

## Pruebas y construcción

Desde `FASE 2/backend`:

```bash
./mvnw clean verify
```

La verificación incluye pruebas unitarias, MVC, arquitectura modular e integración con contenedores reales de PostgreSQL y RabbitMQ. Docker debe estar activo para ejecutar la suite completa.

## Ejecución con Docker Compose

Desde `FASE 2`:

```bash
docker compose config
docker compose up --build -d
docker compose ps
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/api/v1/platform/status
docker compose down
```

No use `docker compose down -v` salvo que se quiera eliminar deliberadamente los datos locales. La API queda en `8080`, RabbitMQ Management en `15672` y PostgreSQL en `5432` por defecto. PostgreSQL se expone solo para comodidad local; en un ambiente real debe permanecer interno.

## Ejecución desde IntelliJ IDEA

1. Abra `FASE 2/backend/pom.xml` como proyecto Maven.
2. Seleccione un JDK 25.
3. Levante dependencias desde `FASE 2` con `docker compose up -d postgres rabbitmq`.
4. Defina `SPRING_PROFILES_ACTIVE=local` y, si cambió valores, las variables correspondientes.
5. Ejecute `com.megastore.sgologic.SgoLogicApplication`.

## Ejecución mediante Maven

Con PostgreSQL y RabbitMQ disponibles:

```bash
cd "FASE 2/backend"
./mvnw spring-boot:run
```

## Imagen Docker

Desde `FASE 2`:

```bash
docker build -t sgo-logic:phase2-backbone backend
```

El Dockerfile compila mediante el wrapper, usa etapas separadas, ejecuta con usuario no root y comprueba readiness mediante Actuator.

## Endpoints técnicos

- `GET /api/v1/platform/status`: nombre, estado, timestamp UTC y correlation ID.
- `GET /actuator/health`: salud agregada.
- `GET /actuator/health/liveness`: probe de vida.
- `GET /actuator/health/readiness`: probe de disponibilidad.
- `GET /actuator/info`: información pública mínima.

Actuator expone únicamente `health` e `info`.

## Alcance de este incremento

Se prepararon la estructura oficial, backend Spring Boot, módulos, configuración local/test, correlación HTTP, errores comunes, endpoint técnico, Dockerfile, Compose, documentación, contratos preliminares y pruebas iniciales.

Continúan pendientes: esquema y migraciones de negocio, carrito, estados ejecutables, Saga, compensaciones, E2E funcional, inventario, pagos, auditoría funcional, seguridad/JWT, Outbox/Inbox/retries/DLQ, fiscal, integraciones legacy, logística, notificaciones, consulta, incidencias, frontend, simuladores y GitHub Actions.

BB-02 permanece parcial porque ADR-009 está `Proposed`. BB-05 también permanece parcial hasta integrar frontend y componentes compartidos pendientes. Los contratos de estados, eventos y API son propuestas sujetas a aprobación del equipo.

## Limitaciones actuales

- El endpoint de plataforma no representa funcionalidad de negocio ni sustituye Actuator.
- No existen entidades JPA ni tablas de negocio; Hibernate solo valida lo que esté mapeado.
- RabbitMQ está disponible, pero no existen productores, consumidores ni topología funcional.
- No hay autenticación ni autorización hasta BB-07.
- El entorno Compose es local y no define redundancia, capacidad, RTO, RPO ni topología productiva.
- La planificación menciona JUnit 5, pero el BOM de Spring Boot 4.1.1 resuelve JUnit Jupiter 6.0.3. No se sobrescribió manualmente el BOM; el equipo debe ratificar esta diferencia o cambiar de versión de plataforma mediante ADR.
