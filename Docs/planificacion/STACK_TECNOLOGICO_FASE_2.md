# Stack tecnológico y organización técnica, Proyecto 1 Fase 2

**Sistema:** SGO-Logic  
**Equipo:** Grupo 5  
**Integrantes:** Jairo Gómez, Jorge Pumay, Edson Sosa, Andy Sánchez y Kevin Girón  
**Estado:** Base tecnológica definida para la construcción del MVP  
**Decisión formal relacionada:** ADR-009, pendiente de incorporarse al DDA  
**Base arquitectónica:** [DDA.md](../ProyectoFase1/Docs/DDA.md)  
**Plan de ejecución:** [PLANIFICACION_FASE_2.md](./PLANIFICACION_FASE_2.md)  
**Responsabilidades:** [RESPONSABILIDADES_INTEGRANTES_FASE_2.md](./RESPONSABILIDADES_INTEGRANTES_FASE_2.md)

---

## 1. Propósito

Este documento establece la base tecnológica común para construir el MVP de SGO-Logic. Su objetivo es que los cinco integrantes utilicen las mismas herramientas, convenciones, límites modulares y mecanismos de integración.

La selección materializa la arquitectura aceptada en la Fase 1: un monolito modular, desplegable como una unidad, con puertos y adaptadores, persistencia relacional, publicación confiable mediante Outbox, mensajería asíncrona y simuladores de los sistemas externos.

Este documento define tecnología y organización del código. El ADR-009 deberá registrar formalmente la decisión, alternativas evaluadas, consecuencias y relación con los drivers arquitectónicos.

## 2. Principios de la solución

1. Se construirá un solo backend desplegable, organizado por capacidades de negocio.
2. Cada módulo será propietario de su lógica y sus datos.
3. Un módulo no podrá utilizar repositorios, entidades JPA ni tablas privadas de otro módulo.
4. La comunicación interna se realizará mediante API pública de módulo o eventos.
5. Las integraciones externas se aislarán detrás de puertos y adaptadores.
6. Los cambios de negocio y sus mensajes salientes se registrarán en una misma transacción mediante Outbox.
7. Los consumidores de mensajes deberán ser idempotentes.
8. Todo servicio se ejecutará localmente mediante Docker Compose.
9. Las pruebas y evidencias formarán parte de cada entrega, no una actividad posterior.
10. No se almacenarán PAN completo, CVV, secretos ni credenciales en código, base de datos, mensajes o logs.

## 3. Stack tecnológico aprobado

### 3.1 Backend

| Elemento | Selección | Uso previsto |
|---|---|---|
| Lenguaje | Java 25 LTS | Desarrollo del backend y trabajadores asíncronos |
| Framework | Spring Boot 4.1.x | Configuración, API, validación, persistencia, seguridad y observabilidad |
| Modularidad | Spring Modulith 2.1.x | Declaración, verificación y pruebas de límites modulares |
| Construcción | Maven 3.9.x mediante Maven Wrapper | Compilación reproducible y administración de dependencias |
| API | Spring Web MVC y REST con JSON | Entrada omnicanal y endpoints de consulta |
| Validación | Jakarta Bean Validation | Validación consistente de comandos y DTO |
| Persistencia | Spring Data JPA y Hibernate | Repositorios, transacciones y mapeo relacional |
| Migraciones | Flyway | Creación y evolución versionada del esquema |
| Mensajería | Spring AMQP | Publicadores y consumidores RabbitMQ |
| Resiliencia | Resilience4j | Timeout, Circuit Breaker, reintento controlado y aislamiento de fallas |
| Seguridad | Spring Security | Autenticación, autorización por roles y protección de endpoints |
| Documentación de API | OpenAPI y Swagger UI | Contratos ejecutables y prueba manual de contingencia |
| Observabilidad | Spring Boot Actuator y Micrometer | Health checks, métricas y datos operativos |
| Logs | SLF4J y Logback con salida JSON | Correlación y diagnóstico por orden, evento e integración |

Se utilizarán las versiones estables compatibles fijadas en `pom.xml`. El equipo no actualizará versiones mayores durante la fase sin una revisión del arquitecto integrador y una ejecución completa de las pruebas.

### 3.2 Persistencia y mensajería

| Elemento | Selección | Uso previsto |
|---|---|---|
| Base de datos | PostgreSQL 17.x | Datos de negocio, auditoría, idempotencia, Outbox y vistas de consulta |
| Broker | RabbitMQ 4.3.x con consola Management | Eventos, colas de trabajo, reintentos y mensajes fallidos |
| Formato de eventos | JSON UTF-8 versionado | Intercambio asíncrono entre productores y consumidores |
| Semántica de entrega | Al menos una vez | Publicación confiable con deduplicación en consumidores |
| Persistencia local | Volúmenes Docker | Conservación de datos entre reinicios del entorno |

RabbitMQ no sustituye al Outbox. El cambio de negocio y el registro Outbox se confirman en PostgreSQL dentro de la misma transacción. Un dispatcher independiente publica después el mensaje en RabbitMQ.

### 3.3 Interfaz web

| Elemento | Selección | Uso previsto |
|---|---|---|
| Runtime | Node.js 24 LTS | Construcción y ejecución local del frontend |
| Librería | React 19 | Interfaz web del MVP |
| Lenguaje | TypeScript con modo estricto | Tipado de componentes, contratos y servicios |
| Herramienta de construcción | Vite, versión estable fijada en `package-lock.json` | Servidor de desarrollo y compilación de producción |
| Estilos | Bootstrap 5.3 y CSS propio mínimo | Diseño responsivo y consistente |
| Peticiones HTTP | `fetch` encapsulado en servicios | Comunicación con la API REST |
| Pruebas | Vitest y React Testing Library | Componentes y flujos críticos |

No se incorporarán Redux, Next.js ni una biblioteca adicional de estado durante el MVP. El estado se mantendrá local o mediante Context cuando realmente sea compartido.

### 3.4 Simulación, pruebas y operación

| Elemento | Selección | Uso previsto |
|---|---|---|
| Simulación de terceros | Adaptadores internos configurables de Spring | PayKlik, SAT, LogiExpress y notificaciones sin servicios externos |
| Simulación legacy | Persistencia y directorios montados en Docker | Facturas, acuses POS e intercambio CSV/XML con ERP-Core |
| Pruebas unitarias | JUnit 5, AssertJ y Mockito | Reglas, estados y servicios de aplicación |
| Pruebas modulares | Spring Modulith Test | Dependencias permitidas y módulos aislados |
| Pruebas de integración | Spring Boot Test y Testcontainers | PostgreSQL y RabbitMQ reales durante pruebas |
| Pruebas API | MockMvc o REST Assured | Contratos, seguridad y errores HTTP |
| Pruebas de carga | k6 | Línea base `T`, carga `8T`, latencia y tasa de errores |
| Contenedores | Docker y Docker Compose | Entorno reproducible de desarrollo y demostración |
| Integración continua | GitHub Actions | Compilación, pruebas, validación modular y construcción de imágenes |

Prometheus, Grafana, WireMock, OpenTelemetry y plataformas externas de monitoreo quedan fuera del alcance obligatorio. La evidencia se obtendrá mediante Actuator, logs estructurados, auditoría, RabbitMQ Management, consultas controladas de Outbox/Inbox y reportes exportados por k6.

### 3.5 Clasificación por prioridad

| Clasificación | Tecnologías y capacidades |
|---|---|
| Obligatorio | Java, Spring Boot, Spring Modulith, PostgreSQL, Flyway, RabbitMQ, React, TypeScript, Docker Compose, k6, seguridad, auditoría y logs correlacionados |
| Condicionado al caso de uso | Resilience4j, consumidores RabbitMQ, generación CSV/XML y simuladores internos |
| Fuera del alcance | Prometheus, Grafana, WireMock, Kubernetes, Kafka, Redis, Keycloak y servicios reales de terceros |

Una herramienta condicionada se utiliza únicamente en el flujo que la necesita. Por ejemplo, Resilience4j se aplica a PayKlik y LogiExpress, no a llamadas internas normales del monolito.

## 4. Justificación contra el DDA

| Decisión o driver | Respuesta del stack |
|---|---|
| ADR-001, monolito modular | Spring Boot produce un solo artefacto y Spring Modulith verifica las fronteras internas |
| ADR-002, publicación confiable | PostgreSQL conserva negocio y Outbox en la misma transacción; RabbitMQ realiza la entrega asíncrona |
| ADR-003, resiliencia de pagos | Resilience4j, Outbox, idempotencia y reconciliación controlan timeout y resultado indeterminado |
| ADR-004, integraciones reintentables | RabbitMQ, colas de reintento, backoff, DLQ y adaptadores idempotentes |
| ADR-005, coordinación del flujo | Saga orquestada dentro del módulo `orders` |
| ADR-006, observabilidad | Actuator, Micrometer, RabbitMQ Management, auditoría y logs JSON correlacionados |
| ADR-007, confirmación fiscal | Cola fiscal y estado `FISCAL_PENDING` hasta obtener aprobación simulada de SAT |
| ADR-008, notificaciones desacopladas | Outbox, cola propia y trabajador de notificaciones |
| EaC-01 a EaC-03, desempeño y escala | Índices PostgreSQL, pool de conexiones y pruebas reproducibles con k6 |
| EaC-04 y EaC-05, fallas externas | Adaptadores internos configurables permiten inyectar latencia, timeout, rechazo y caída |
| EaC-07, trazabilidad | Auditoría inmutable, correlation ID, event ID y logs estructurados |
| EaC-09, mantenibilidad | Tecnologías conocidas, un solo backend y ejecución mediante Compose |
| EaC-11, portabilidad | Imágenes Docker, configuración externa y servicios sin dependencia de infraestructura propietaria |
| EaC-12, seguridad | Spring Security, roles, sanitización y pruebas de ausencia de PAN/CVV |

## 5. Topología ejecutable

```text
Navegador
   |
   v
Frontend React
   |
   v
API Spring Boot
   |
   +--> Módulos de negocio
   |       |
   |       +--> PostgreSQL
   |       |      +--> esquemas lógicos
   |       |      +--> auditoría
   |       |      +--> Outbox
   |       |
   |       +--> Dispatcher Outbox
   |               |
   |               v
   |            RabbitMQ
   |               |
   |               +--> trabajadores idempotentes
   |                         |
   |                         v
   |                    puertos y adaptadores
   |
   +--> Actuator, logs y métricas internas

Adaptadores simulados dentro del backend
   +--> PayKlik: aprobación, rechazo, timeout o caída
   +--> SAT: creación, aprobación y almacenamiento de factura
   +--> LogiExpress: guía, tracking o timeout
   +--> Notificaciones: registro de envío o falla
   +--> ERP-Core: archivo CSV/XML en volumen local
   +--> POS: 32 acuses lógicos persistidos
```

La topología física contiene únicamente cuatro servicios permanentes: backend, frontend, PostgreSQL y RabbitMQ. Los terceros simulados son implementaciones internas de los puertos, no contenedores independientes.

## 6. Estructura oficial del repositorio

```text
FASE 2/
├── backend/
│   ├── .mvn/
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/com/megastore/sgologic/
│       │   │   ├── SgoLogicApplication.java
│       │   │   ├── orders/
│       │   │   ├── inventory/
│       │   │   ├── payments/
│       │   │   ├── fiscal/
│       │   │   ├── logistics/
│       │   │   ├── notifications/
│       │   │   ├── incidents/
│       │   │   ├── query/
│       │   │   ├── audit/
│       │   │   ├── security/
│       │   │   ├── integration/
│       │   │   └── shared/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-local.yml
│       │       ├── application-test.yml
│       │       └── db/migration/
│       └── test/
│           └── java/com/megastore/sgologic/
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   ├── components/
│   │   ├── features/
│   │   │   ├── auth/
│   │   │   ├── checkout/
│   │   │   ├── orders/
│   │   │   ├── tracking/
│   │   │   └── incidents/
│   │   ├── services/
│   │   ├── types/
│   │   └── styles/
│   ├── package.json
│   ├── package-lock.json
│   ├── vite.config.ts
│   └── Dockerfile
├── simulator-data/
│   ├── erp/
│   │   └── output/
│   ├── pos/
│   │   └── acknowledgements/
│   └── notifications/
│       └── sent/
├── tests/
│   ├── e2e/
│   ├── k6/
│   ├── security/
│   └── data/
├── docs/
│   ├── adr/
│   ├── contracts/
│   │   ├── openapi/
│   │   ├── events/
│   │   └── files/
│   ├── evidence/
│   │   ├── functional/
│   │   ├── performance/
│   │   ├── resilience/
│   │   └── security/
│   └── diagrams/
├── .github/workflows/
├── .env.example
├── .gitignore
├── docker-compose.yml
└── README.md
```

`BITACORA_FASE_2.md` permanece en la raíz general del repositorio, según lo solicitado por el enunciado.

## 7. Estructura interna de cada módulo backend

Cada módulo de negocio seguirá la misma organización:

```text
orders/
├── api/
│   ├── OrderFacade.java
│   ├── OrderView.java
│   └── event/
├── domain/
│   ├── model/
│   ├── service/
│   └── exception/
├── application/
│   ├── command/
│   ├── query/
│   ├── port/
│   │   ├── in/
│   │   └── out/
│   └── service/
├── adapter/
│   ├── in/
│   │   ├── web/
│   │   └── messaging/
│   └── out/
│       ├── persistence/
│       ├── messaging/
│       └── external/
└── config/
```

Reglas de dependencias:

- `domain` no depende de Spring, JPA, RabbitMQ ni HTTP.
- `application` coordina casos de uso y depende de puertos, no de adaptadores concretos.
- `adapter.in` traduce entradas HTTP o mensajes hacia casos de uso.
- `adapter.out` implementa persistencia, mensajería y llamadas externas.
- `api` contiene únicamente contratos permitidos para otros módulos.
- Las entidades JPA permanecen dentro de `adapter.out.persistence`.
- `shared` contiene solo tipos realmente transversales, como identificadores, reloj y errores comunes.
- Está prohibido convertir `shared` en un contenedor de lógica de negocio sin dueño.

## 8. Propiedad lógica de datos

| Esquema lógico | Módulo propietario | Escritura permitida |
|---|---|---|
| `orders` | Órdenes | Solo módulo `orders` |
| `inventory` | Inventario | Solo módulo `inventory` |
| `payments` | Pagos | Solo módulo `payments` |
| `fiscal` | Facturación y SAT | Solo módulo `fiscal` |
| `logistics` | Despacho y tracking | Solo módulo `logistics` |
| `notifications` | Notificaciones | Solo módulo `notifications` |
| `incidents` | Incidencias y devoluciones | Solo módulo `incidents` |
| `query` | Vista consolidada | Solo módulo `query`, alimentado por eventos |
| `audit` | Evidencia auditable | Servicio transversal de auditoría |
| `integration` | Outbox, inbox e idempotencia | Infraestructura de integración controlada |

Las tablas pueden residir físicamente en una instancia PostgreSQL, pero su propiedad permanece separada. Una consulta que necesite información de varios módulos utilizará eventos para mantener el modelo de lectura de `query`; no realizará uniones directas sobre tablas privadas.

## 9. Responsabilidades técnicas por integrante

### 9.1 Jairo Gómez

**Rol:** Scrum Master, arquitecto integrador y Release Manager.

**Propiedad técnica:**

- Inicialización de Spring Boot, Maven Wrapper y Spring Modulith.
- Módulos `orders` y coordinación de la Saga.
- Configuración general de la API y manejo común de errores.
- `docker-compose.yml`, Dockerfile del backend y configuración por ambiente.
- Pipeline de GitHub Actions.
- Integración E2E, ramas `develop`, `release`, `main` y release final.
- ADR-009 y consolidación arquitectónica.

**Directorios principales:**

```text
backend/src/main/java/com/megastore/sgologic/orders/
backend/src/main/java/com/megastore/sgologic/shared/
backend/src/main/resources/application*.yml
.github/workflows/
docker-compose.yml
docs/adr/
tests/e2e/
```

**Entregas técnicas:** backend ejecutable, contratos base, estados de orden, Saga, Compose, CI y prueba del camino feliz.

**Revisor principal:** Kevin Girón.

### 9.2 Jorge Pumay

**Rol:** dueño de consistencia operacional.

**Propiedad técnica:**

- Diseño relacional inicial y revisión de migraciones.
- Módulo `inventory`.
- Stock de 34 ubicaciones.
- Reserva atómica, expiración y liberación idempotente.
- Selección del origen de despacho.
- Pruebas de concurrencia y restricciones de integridad.

**Directorios principales:**

```text
backend/src/main/java/com/megastore/sgologic/inventory/
backend/src/main/resources/db/migration/
backend/src/test/java/com/megastore/sgologic/inventory/
tests/data/inventory/
docs/contracts/events/inventory/
```

**Entregas técnicas:** esquema inicial, datos semilla, puertos de inventario, reservas consistentes y evidencia de ausencia de sobreventa.

**Revisor principal:** Edson Sosa.

### 9.3 Edson Sosa

**Rol:** Security & Quality Champion.

**Propiedad técnica:**

- Módulos `payments`, `audit` y `security`.
- Infraestructura compartida de Outbox, Inbox, dispatcher e idempotencia.
- Configuración de RabbitMQ, reintentos y DLQ junto con Jairo.
- Adaptador PayKlik y reconciliación de pagos.
- Autenticación, autorización por roles y sanitización.
- Pruebas de carga, auditoría y seguridad.

**Directorios principales:**

```text
backend/src/main/java/com/megastore/sgologic/payments/
backend/src/main/java/com/megastore/sgologic/audit/
backend/src/main/java/com/megastore/sgologic/security/
backend/src/main/java/com/megastore/sgologic/integration/
backend/src/test/java/com/megastore/sgologic/payments/
tests/k6/
tests/security/
docs/evidence/performance/
docs/evidence/security/
```

**Entregas técnicas:** Outbox reutilizable, pago resiliente, trazabilidad, roles y evidencia de carga y protección de datos.

**Revisor principal:** Andy Sánchez.

### 9.4 Andy Sánchez

**Rol:** dueño de integración fiscal y legacy.

**Propiedad técnica:**

- Módulo `fiscal`.
- Adaptadores y contratos para SAT, ERP-Core y POS.
- Generación fiscal XML, CUI y serie simulados.
- Estado `FISCAL_PENDING` y procesamiento de respuesta SAT.
- Lotes CSV/XML para ERP y publicación lógica a 32 POS.
- Infraestructura común de simuladores y escenarios configurables de terceros.
- README de instalación y despliegue.

**Directorios principales:**

```text
backend/src/main/java/com/megastore/sgologic/fiscal/
backend/src/main/java/com/megastore/sgologic/integration/legacy/
backend/src/test/java/com/megastore/sgologic/fiscal/
backend/src/main/java/com/megastore/sgologic/integration/simulator/
simulator-data/
docs/contracts/files/
docs/contracts/openapi/external/
README.md
```

**Entregas técnicas:** confirmación fiscal simulada, archivos ERP, distribución POS, simuladores configurables y guía reproducible de despliegue.

**Revisor principal:** Jorge Pumay.

### 9.5 Kevin Girón

**Rol:** Product Owner operativo, Developer y QA Coordinator.

**Propiedad técnica:**

- Módulos `logistics`, `notifications`, `query` e `incidents`.
- Adaptador LogiExpress y trabajador de notificaciones.
- Vista consolidada de órdenes.
- Interfaz React del MVP.
- Métricas, paneles y correlación E2E.
- Matriz de pruebas EaC, pruebas de fallas y guion de demostración.

**Directorios principales:**

```text
backend/src/main/java/com/megastore/sgologic/logistics/
backend/src/main/java/com/megastore/sgologic/notifications/
backend/src/main/java/com/megastore/sgologic/query/
backend/src/main/java/com/megastore/sgologic/incidents/
frontend/
docs/evidence/
```

**Entregas técnicas:** despacho, tracking, avisos, consulta, devolución, interfaz demostrable y matriz de evidencias.

**Revisor principal:** Jairo Gómez.

## 10. Archivos compartidos y control de cambios

| Archivo o área | Dueño de integración | Revisión requerida |
|---|---|---|
| `backend/pom.xml` | Jairo | Dueño del componente que solicita la dependencia |
| `docker-compose.yml` | Jairo | Andy para simuladores, Edson para broker, Kevin para observabilidad |
| `application.yml` | Jairo | Dueño de cada propiedad agregada |
| `db/migration` | Jorge | Dueño del módulo y Edson si afecta auditoría, Outbox o seguridad |
| Contratos OpenAPI | Jairo | Productor y consumidor |
| Contratos de eventos | Edson | Productor, consumidor y Jairo |
| Infraestructura de simuladores | Andy | Dueño del puerto simulado |
| Frontend | Kevin | Jairo para contrato API |
| README | Andy | Prueba de instalación realizada por Jorge |
| Evidencias | Kevin | Dueño de la prueba correspondiente |

Ningún integrante modificará silenciosamente un archivo compartido para resolver únicamente su rama. Los cambios de contrato deben acordarse entre productor y consumidor antes de programarse.

## 11. Convenciones de API

- Prefijo de endpoints: `/api/v1`.
- Cuerpos y respuestas: JSON en `camelCase`.
- Fechas e instantes: ISO 8601 en UTC.
- Identificadores: UUID generados por el sistema.
- Operaciones reintentables: encabezado `Idempotency-Key`.
- Correlación: encabezado `X-Correlation-Id`; si no existe, la API lo genera.
- Autenticación: `Authorization: Bearer <token>`.
- Paginación: `page`, `size` y `sort` donde corresponda.
- Errores: estructura común con `code`, `message`, `timestamp`, `path` y `correlationId`.
- Contratos: OpenAPI versionado dentro de `docs/contracts/openapi`.

Roles mínimos:

| Rol | Alcance principal |
|---|---|
| `CLIENT` | Carrito, checkout, consulta propia e incidencia propia |
| `SUPPORT` | Consulta de órdenes e incidencias autorizadas |
| `LOGISTICS` | Despacho, guía y tracking |
| `FINANCE` | Pago, facturación, conciliación y auditoría autorizada |

Para el MVP se utilizarán usuarios de demostración con contraseñas cifradas mediante BCrypt. No se implementará un servidor OAuth propio ni se almacenarán tokens en el repositorio.

## 12. Convenciones de eventos y mensajería

Todo evento deberá incluir un sobre común:

```json
{
  "eventId": "uuid",
  "eventType": "PaymentRequested",
  "eventVersion": 1,
  "occurredAt": "2026-09-10T18:00:00Z",
  "correlationId": "uuid",
  "causationId": "uuid",
  "aggregateType": "Order",
  "aggregateId": "uuid",
  "payload": {}
}
```

Convenciones operativas:

- Exchange principal: `sgo.events` de tipo topic.
- Routing key: `<modulo>.<evento_en_snake_case>`, por ejemplo `payments.payment_requested`.
- Cola principal: `sgo.<consumidor>.<proposito>.q`.
- Cola de mensajes fallidos: nombre de la cola principal con sufijo `.dlq`.
- Todo consumidor registra `eventId` en Inbox antes de aplicar el efecto.
- Un evento duplicado devuelve éxito sin repetir el efecto.
- Cada cambio incompatible incrementa `eventVersion`.
- Un mensaje agotado conserva payload, causa, número de intentos y correlation ID.

### 12.1 Topología mínima de RabbitMQ

Para evitar complejidad accidental se utilizará un solo exchange y cuatro colas funcionales:

| Cola | Responsabilidad | Dueño principal |
|---|---|---|
| `sgo.payments.q` | Pago pendiente, reintento y conciliación simulada | Edson |
| `sgo.fiscal.q` | Creación y confirmación fiscal simulada | Andy |
| `sgo.legacy.q` | Generación ERP y entrega lógica a POS | Andy |
| `sgo.operations.q` | Logística y notificaciones desacopladas | Kevin |

Cada cola tendrá una DLQ con el mismo nombre y sufijo `.dlq`. No se crearán colas para llamadas internas síncronas, consultas normales ni operaciones que no requieran preservación o reintento.

RabbitMQ se utiliza para demostrar la arquitectura, no para representar proveedores reales. Los consumidores viven dentro del mismo backend modular y llaman adaptadores simulados.

### 12.2 Contrato de simulación

Los simuladores se activarán con el perfil Spring `simulated`. Cada puerto mantendrá el contrato que una integración real necesitaría, pero su adaptador responderá localmente y de manera controlada.

```yaml
sgo:
  simulators:
    payklik:
      outcome: APPROVED
      delay-ms: 200
    sat:
      outcome: APPROVED
      delay-ms: 300
    logiexpress:
      outcome: SUCCESS
      delay-ms: 200
    notifications:
      outcome: SUCCESS
```

Resultados configurables:

| Simulador | Comportamiento mínimo | Evidencia almacenada |
|---|---|---|
| PayKlik | `APPROVED`, `REJECTED`, `TIMEOUT` y `UNAVAILABLE` | Intento, referencia simulada, resultado, duración y correlación, sin PAN ni CVV |
| SAT | Crear, aprobar o rechazar una factura simulada; también permitir timeout | XML, CUI, serie, estado, instante y `orderId` |
| LogiExpress | Crear guía, devolver tracking o simular timeout | Guía, estado, intentos y correlación |
| Notificaciones | Registrar envío simulado o falla | Canal, destinatario enmascarado, plantilla, resultado e intentos |
| ERP-Core | Generar CSV/XML en un volumen local | Lote, órdenes incluidas, checksum, estado y archivo |
| POS | Crear 32 entregas lógicas y sus acuses simulados | `posId`, `eventId`, estado, intentos y acuse |

No se levantarán servidores independientes para estos sistemas. El objetivo es demostrar puertos reemplazables, persistencia, asincronía, reintentos e idempotencia.

### 12.3 Alcance fiscal simulado

El MVP no se conectará con la SAT. El módulo fiscal deberá:

1. Crear la factura asociada a la orden.
2. Generar y almacenar el XML fiscal.
3. Asignar CUI y serie simulados.
4. Conservar el estado de la factura y su correlación.
5. Procesar la creación mediante la cola fiscal para demostrar el flujo asíncrono.
6. Permitir configurar aprobación, rechazo o timeout para las pruebas.

No se implementarán firma electrónica real, certificación fiscal, credenciales de SAT ni consumo de sus servicios productivos.

### 12.4 Alcance de pagos, ERP y POS

- PayKlik será un puerto con respuesta simulada. El sistema solo almacenará referencias y resultados ficticios.
- Los casos de timeout y caída preservarán el intento mediante Outbox y activarán el flujo de reintento.
- ERP-Core será representado por archivos CSV/XML generados en `simulator-data/erp/output`.
- Los POS serán representados por 32 destinos lógicos persistidos; no se ejecutarán 32 aplicaciones.
- La entrega repetida del mismo `eventId` no podrá duplicar un lote, acuse o efecto.
- La documentación indicará expresamente que todos los contratos y resultados son simulados y no productivos.

## 13. Migraciones y datos

- Flyway será la única forma autorizada de modificar esquemas compartidos.
- Hibernate validará el esquema, pero no lo creará ni actualizará automáticamente.
- Se utilizará `spring.jpa.hibernate.ddl-auto=validate` fuera de pruebas unitarias.
- Cada migración tendrá un nombre único con el formato `VYYYYMMDDHHMM__descripcion.sql`.
- Una migración aplicada no se editará; cualquier corrección se realizará en una migración nueva.
- Los datos semilla de demostración serán reproducibles y no incluirán información real.
- Cada tabla tendrá propietario documentado, clave primaria, restricciones necesarias e índices justificados.

## 14. Seguridad mínima obligatoria

- Autenticación mediante JWT firmado localmente para el MVP.
- Autorización por rol y propiedad del recurso.
- Contraseñas almacenadas únicamente como hash BCrypt.
- Secretos inyectados mediante variables de ambiente.
- `.env.example` contiene nombres y ejemplos no sensibles; `.env` no se versiona.
- Validación de entrada en todos los endpoints públicos.
- Consultas parametrizadas mediante JPA o SQL preparado.
- CORS limitado al origen del frontend configurado.
- Sanitización de logs y mensajes.
- Cero apariciones de PAN completo y CVV en base de datos, eventos, logs y evidencias.
- Respuestas 401 para identidad ausente o inválida y 403 para permisos insuficientes.

## 15. Observabilidad

Cada petición y mensaje deberá conservar:

- `correlationId` para seguir el flujo completo.
- `orderId` cuando exista una orden asociada.
- `eventId` para procesamiento asíncrono.
- Nombre del módulo y operación.
- Resultado, duración y código de error sanitizado.

Métricas mínimas:

- Latencia y tasa de errores por endpoint.
- Número de órdenes por estado.
- Profundidad y antigüedad máxima del Outbox.
- Mensajes publicados, procesados, reintentados y enviados a DLQ.
- Estado del Circuit Breaker por dependencia.
- Duración y errores de PayKlik, SAT y LogiExpress.
- Tiempo de respuesta de la consulta consolidada.

La evidencia académica deberá exportarse a `docs/evidence`. No se aceptará como única evidencia una pantalla temporal de consola. Cada prueba guardará comando, configuración, datos, resultado y conclusión.

## 16. Estrategia de pruebas

| Nivel | Objetivo | Responsable |
|---|---|---|
| Unitarias | Reglas, invariantes, transiciones y errores | Dueño de cada módulo |
| Modulares | Fronteras y dependencias permitidas | Jairo con todos los dueños |
| Persistencia | Restricciones, migraciones y repositorios | Jorge y dueño del módulo |
| Mensajería | Outbox, Inbox, duplicados, reintentos y DLQ | Edson y consumidores |
| Contratos | API, eventos, XML y CSV | Productor y consumidor |
| Integración externa | Éxito, rechazo, timeout, latencia y caída | Andy y dueño del puerto |
| Seguridad | 401, 403, propiedad y fuga de datos | Edson |
| Rendimiento | Línea base `T`, `8T`, p50, p95, p99 y errores | Edson con Kevin |
| E2E | Camino feliz y recuperación ante fallas | Jairo con todo el equipo |
| Aceptación | EaC, evidencia y demostración | Kevin con todo el equipo |

Pruebas mínimas para aceptar un módulo:

1. Camino exitoso.
2. Entrada inválida.
3. Regla de negocio o transición inválida.
4. Dependencia no disponible, si aplica.
5. Repetición idempotente, si aplica.
6. Autorización permitida y denegada.
7. Evidencia observable mediante logs, métricas o auditoría.

### 16.1 Paquete mínimo de evidencia para la entrega

| Evidencia | Resultado que debe mostrar | Ubicación |
|---|---|---|
| Arranque limpio | Los cuatro servicios levantan con health checks correctos | `docs/evidence/functional` |
| Camino feliz | Carrito, reserva, pago, factura, despacho, notificación, ERP y POS | `docs/evidence/functional` |
| Pago con timeout | Orden e intento preservados, reintento visible y sin doble cobro | `docs/evidence/resilience` |
| Evento duplicado | Segundo procesamiento sin efecto duplicado | `docs/evidence/resilience` |
| Factura simulada | XML, CUI, serie, estado y vínculo con la orden almacenados | `docs/evidence/functional` |
| ERP simulado | Archivo CSV/XML y lote trazable generados | `docs/evidence/functional` |
| POS simulados | 32 destinos lógicos con acuse, reintento o DLQ | `docs/evidence/resilience` |
| Fallo de notificación | Orden confirmada aunque el aviso falle | `docs/evidence/resilience` |
| Seguridad | Respuestas 401/403 y búsqueda sin PAN completo ni CVV | `docs/evidence/security` |
| Carga con k6 | Configuración de `T` y `8T`, p50, p95, p99, errores y conclusión | `docs/evidence/performance` |

Cada evidencia deberá incluir el escenario, la configuración utilizada, el comando de ejecución, el resultado y una conclusión honesta. Las capturas pueden acompañar el resultado, pero no reemplazan los reportes o datos reproducibles.

## 17. Servicios de Docker Compose

| Servicio | Función | Exposición esperada |
|---|---|---|
| `backend` | API, módulos y trabajadores | API y Actuator |
| `frontend` | Interfaz React compilada | Puerto web |
| `postgres` | Persistencia relacional | Solo red interna, excepto desarrollo local |
| `rabbitmq` | Broker y consola de administración | AMQP y consola local |

Todos los servicios deberán contar con health check cuando la imagen lo permita. `backend` no se considerará listo hasta que PostgreSQL y RabbitMQ estén disponibles. Las imágenes deberán fijar versión y nunca utilizar `latest`.

k6 no necesita permanecer activo dentro de Compose. Se ejecutará bajo demanda desde `tests/k6` contra la API levantada y guardará el resumen en `docs/evidence/performance`.

## 18. Flujo Git e integración continua

Ramas permanentes:

- `main`: versión estable entregable.
- `develop`: integración diaria.
- `release`: preparación y estabilización de la entrega.

Ramas temporales:

- `feature/<id>-descripcion` desde `develop`.
- `hotfix/<id>-descripcion` cuando corresponda.

Formato obligatorio de commits:

```text
#carnet: mensaje en infinitivo
```

Validaciones mínimas del pipeline:

1. Compilar backend.
2. Ejecutar pruebas unitarias.
3. Verificar módulos con Spring Modulith.
4. Ejecutar pruebas de integración seleccionadas.
5. Compilar frontend.
6. Ejecutar pruebas de frontend.
7. Construir imágenes Docker.
8. Rechazar secretos o archivos de ambiente versionados accidentalmente.

Ningún PR se fusionará con el pipeline en rojo. El autor no aprobará su propio cambio.

## 19. Definition of Done técnica

Una tarjeta se considera terminada únicamente cuando:

- cumple su criterio funcional;
- respeta el límite del módulo;
- no accede a tablas privadas de otro módulo;
- incluye pruebas automáticas proporcionales al riesgo;
- conserva idempotencia si procesa mensajes o reintentos;
- aplica autenticación y autorización cuando corresponde;
- produce logs y métricas con correlación;
- actualiza contrato o migración cuando aplica;
- incluye evidencia reproducible;
- funciona dentro de Docker Compose;
- supera el pipeline de integración continua;
- fue revisada por otro integrante;
- cuenta con documentación suficiente para integrarse y demostrarse.

## 20. Tecnologías fuera del alcance

Durante esta fase no se incorporarán:

- microservicios;
- Kubernetes;
- Kafka;
- Redis sin evidencia de necesidad;
- Keycloak;
- aplicaciones móviles nativas;
- Elasticsearch;
- Event Sourcing;
- CQRS completo;
- servicios reales de nube;
- Prometheus, Grafana, WireMock y suites de observabilidad adicionales;
- bases de datos independientes por integrante;
- acceso directo entre esquemas privados.

Cualquiera de estas incorporaciones requiere un nuevo ADR, evidencia de necesidad y aprobación del equipo. No podrá desplazar trabajo P0 o P1.

## 21. Criterios para ratificar ADR-009

La decisión se considerará formalmente ratificada cuando:

1. Los cinco integrantes puedan construir el backend y frontend desde un ambiente limpio.
2. Docker Compose levante PostgreSQL, RabbitMQ, backend y frontend.
3. Una migración Flyway se aplique automáticamente.
4. Un endpoint cree un registro de negocio y su entrada Outbox en la misma transacción.
5. El dispatcher publique un evento de prueba.
6. Un consumidor procese el mensaje sin duplicar el efecto al recibirlo dos veces.
7. Spring Modulith no reporte dependencias modulares prohibidas.
8. Swagger UI permita inspeccionar el contrato inicial.
9. Actuator exponga salud y métricas sin revelar secretos.
10. El pipeline ejecute correctamente sobre una rama de prueba.

## 22. Resultado esperado

Con esta base, cada integrante puede desarrollar una capacidad vertical sin convertir el sistema en cinco soluciones aisladas. El equipo comparte infraestructura y contratos, conserva la propiedad lógica de cada módulo y puede demostrar los drivers arquitectónicos definidos en el DDA mediante pruebas reproducibles.

El stack seleccionado mantiene una complejidad proporcional al alcance académico, permite ejecutar todo localmente y deja una ruta de evolución futura sin comprometer la entrega de la Fase 2.

## 23. Referencias técnicas oficiales

- [Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- [Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Modulith Reference](https://docs.spring.io/spring-modulith/reference/)
- [PostgreSQL Versioning Policy](https://www.postgresql.org/support/versioning/)
- [RabbitMQ Release Information](https://www.rabbitmq.com/release-information)
- [React 19](https://react.dev/blog/2024/12/05/react-19)
- [Node.js Releases](https://nodejs.org/en/about/previous-releases)
- [Vite Guide](https://vite.dev/guide/)
- [Grafana k6 Documentation](https://grafana.com/docs/k6/latest/)
