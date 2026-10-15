# Manual Técnico - SGO-Logic Fase 3

**Responsable:** Andy Sánchez  
**Revisor asignado:** Kevin Girón  
**Estado:** Actualizado con resultados k6 locales y smoke GCP; pendiente de revisión

**Última actualización:** 2026-10-14  
**Alcance:** construcción, configuración, ejecución, pruebas, operación y
recuperación de SGO-Logic.

## 1. Propósito y regla de veracidad

Este manual permite preparar y operar SGO-Logic sin depender del conocimiento
oral de sus autores. Distingue el comportamiento comprobado del trabajo aún no
integrado. Un elemento marcado `PENDIENTE` no debe presentarse como disponible.

La versión descrita integra crédito propio, Saga dual, historial ERP, CI/CD y
despliegue en Google Cloud. Las cinco pruebas de carga y las cinco de estrés se
ejecutaron localmente sobre la base `5a7cdd1`; el reporte conserva los resúmenes,
las limitaciones y la segunda medición controlada. El smoke externo de GCP quedó
aprobado; el ajuste del cuello de botella de notificaciones permanece abierto.

## 2. Alcance funcional

### 2.1 Capacidades existentes

- Autenticación JWT y roles de cliente, soporte, logística y finanzas.
- Carrito, checkout, órdenes y Saga con la ruta PayKlik.
- Inventario, facturación fiscal, despacho, notificaciones y auditoría.
- Integraciones con ERP/POS, tienda física, almacén central, PayKlik y
  LogiExpress.
- PostgreSQL, RabbitMQ, Outbox/Inbox, reintentos e idempotencia.
- Frontend web servido por nginx y API Spring Boot.

### 2.2 Ampliación de Fase 3

- Evaluación explicable de elegibilidad crediticia.
- Opción "Compra ahora, paga en 30 días" solo para clientes elegibles.
- Persistencia del vencimiento a 30 días y del resultado auditable.
- Convivencia independiente con PayKlik.
- Notificación no bloqueante de aprobación o rechazo.
- Despliegue en nube mediante pipeline con gates.
- Cinco pruebas de cada tipo: unitarias, integración, E2E, carga y estrés.

**Estado de esta ampliación:** integrada. Las diez ejecuciones no funcionales,
el análisis antes/después y el smoke externo están documentados. Permanecen
pendientes la repetición de notificaciones después de un ajuste y la revisión
del manual por una persona no autora.

## 3. Arquitectura

### 3.1 Vista actual

El núcleo es un monolito modular Spring Boot. Los módulos colaboran mediante
APIs públicas, puertos y eventos, evitando leer repositorios o tablas privadas
de otros módulos. PostgreSQL conserva el estado operacional y RabbitMQ
transporta eventos publicados mediante Outbox e ingeridos mediante Inbox.

```text
Navegador
   |
   v
nginx / frontend ----> API Spring Boot
                           |       |
                           v       v
                       PostgreSQL RabbitMQ
                           |
        +------------------+-------------------+
        |                  |                   |
        v                  v                   v
 inventory-service   store-service      proveedores externos
                                         ERP / PayKlik /
                                         LogiExpress / SAT
```

### 3.2 Flujo financiero de Fase 3

```text
Checkout
   |
   v
Selección de opción de pago
   |-----------------------------|
   v                             v
Crédito propio                 PayKlik
   |                             |
   v                             v
Decisión explicable          Pago tradicional
   |
   +--> auditoría
   +--> notificación no bloqueante
   +--> vencimiento a 30 días si se concede
```

La indisponibilidad del crédito no debe congelar PayKlik. La notificación no
debe revertir una orden ya confirmada.

## 4. Componentes desplegables

El archivo `docker-compose.yml` define actualmente:

| Componente | Función | Puerto local predeterminado |
|---|---|---:|
| `frontend` | nginx y experiencia web | 8081 |
| `backend` | API y monolito modular | 8080 |
| `postgres` | Datos del monolito | 5432 |
| `rabbitmq` | Mensajería y consola de administración | 5672 / 15672 |
| `inventory-service` | Almacén central | 8082 |
| `inventory-db` | Datos del almacén | 5434 |
| `store-service` | Sistema de tienda POS-01 | 8084 |
| `store-db` | Datos locales de la tienda | 5435 |
| `logiexpress-service` | Proveedor logístico simulado/propio | 8085 |
| `logiexpress-db` | Datos de LogiExpress | 5436 |
| `payklik-service` | Pasarela PayKlik simulada/propia | 8086 |
| `payklik-db` | Datos de PayKlik | 5437 |

Los puertos son para desarrollo. En nube, las bases y RabbitMQ no deben quedar
expuestos públicamente.

## 5. Requisitos

### 5.1 Desarrollo local

- Git.
- Docker Engine y Docker Compose.
- JDK 25 para ejecutar el backend fuera de contenedores.
- Navegador moderno.
- PowerShell 7 o Bash para los scripts auxiliares.
- k6 mediante la imagen `grafana/k6` para rendimiento.

### 5.2 Recursos y nube

La bitácora del ambiente entregado confirma Google Cloud, proyecto
`sgo-logic-g5-202010844`, zona `us-central1-a`, VM `e2-standard-2` (2 vCPU,
8 GB), 4 GB de swap e IP pública `136.65.180.39`. La guía de provisión define
Ubuntu 24.04 y disco de 30 GB; esos dos valores deben contrastarse con la VM en
la evidencia final. El puerto 80 sirve la tienda, consola y API. Los
procedimientos de respaldo, restauración y rollback están en
`docs/operacion/RUNBOOK.md`.

## 6. Configuración

1. Copiar `FASE 2/.env.example` como `FASE 2/.env`.
2. Cambiar todos los valores locales al salir del entorno de desarrollo.
3. Mantener `.env`, llaves y credenciales fuera del repositorio.
4. Proporcionar secretos de nube mediante el mecanismo del pipeline.

Grupos principales de variables:

| Grupo | Uso |
|---|---|
| `POSTGRES_*`, `RABBITMQ_*` | Persistencia y broker |
| `SGO_SECURITY_*` | JWT, cuentas y roles |
| `SGO_OUTBOX_*` | Publicación y reintentos de eventos |
| `SGO_PAYKLIK_*` | Timeout, pool y circuit breaker |
| `SGO_PAYMENT_RECONCILIATION_*` | Conciliación de pagos indeterminados |
| `SGO_SIMULATORS_*` | Éxito, rechazo, latencia, timeout o caída |
| `INVENTORY_*`, `STORE_*` | Almacén central y tienda física |
| `LOGIEXPRESS_*`, `PAYKLIK_*` | Servicios externos desplegados localmente |
| `SGO_MAIL_*` | Proveedor de correo; vacío usa simulación local |

Variables principales de crédito:

| Variable | Predeterminado | Uso |
|---|---:|---|
| `SGO_CREDIT_INITIAL_LIMIT` | `2500.00` | Línea inicial individual por cliente |
| `SGO_CREDIT_MINIMUM_ON_TIME_RATE` | `0.80` | Puntualidad mínima |
| `SGO_CREDIT_MAXIMUM_RETURN_RATE` | `0.30` | Devoluciones máximas |
| `SGO_CREDIT_MAXIMUM_CRITICAL_INCIDENTS` | `1` | Incidentes críticos máximos |
| `SGO_CREDIT_MAXIMUM_LIMIT` | `5000.00` | Tope de la línea |
| `SGO_CREDIT_LIMIT_RATIO` | `0.40` | Fracción de compras de 12 meses |
| `SGO_CREDIT_TERM_DAYS` | `30` | Plazo de la obligación |
| `SGO_CREDIT_RULE_VERSION` | `credit-rules-v1` | Versión auditable |
| `SGO_CREDIT_HISTORY_WINDOW_MONTHS` | `12` | Ventana histórica |
| `SGO_CREDIT_OPTIONS_TIMEOUT_MS` | `2000` | Timeout de opciones |
| `SGO_CREDIT_OPTIONS_CALL_POOL_SIZE` | `4` | Hilos de evaluación |

## 7. Construcción y ejecución local

Desde `FASE 2`:

```bash
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

La plataforma está lista cuando los health checks requeridos muestran
`healthy`. Accesos locales:

- Frontend: `http://localhost:8081`
- API: `http://localhost:8080`
- Readiness: `http://localhost:8080/actuator/health/readiness`
- RabbitMQ Management: `http://localhost:15672`

Para detener sin eliminar los volúmenes:

```bash
docker compose down
```

No ejecutar `docker compose down -v` salvo que se haya aprobado eliminar los
datos locales.

## 8. Construcción y pruebas automatizadas

### 8.1 Backend

Desde `FASE 2/backend`:

```bash
./mvnw --batch-mode clean verify
```

El comando debe finalizar correctamente antes de desplegar. Incluye las
pruebas configuradas por Maven; algunas integraciones requieren Docker.

### 8.2 Smoke local

Desde `FASE 2`:

```bash
./tests/smoke/verify-platform.sh
```

El smoke compila, valida Compose, levanta servicios, espera health checks y
comprueba Actuator y correlación HTTP.

### 8.3 Matriz mínima de Fase 3

| Tipo | Cantidad mínima | Responsable | Evidencia final |
|---|---:|---|---|
| Unitarias | 5 | Jorge | Reporte automatizado |
| Integración | 5 | Edson | Reporte automatizado |
| E2E | 5 | Edson | Reporte automatizado |
| Carga | 5 | Andy | Reporte k6 |
| Estrés | 5 | Andy | Reporte k6 y recuperación |

## 9. Rendimiento con k6

Los scripts están en `tests/k6/phase3`. Antes de ejecutarlos:

1. Confirmar el SHA/tag medido.
2. Recibir de Kevin una URL estable y autorización para generar carga.
3. Sembrar un dataset determinista y aislado.
4. Copiar `.env.performance.example` como `.env.performance`.
5. Habilitar `ALLOW_MUTATIONS` y `ALLOW_STRESS` únicamente en la ventana
   autorizada; nunca guardar secretos.

Ejecución en PowerShell:

```powershell
./tests/k6/phase3/run-phase3.ps1 -Suite load
./tests/k6/phase3/run-phase3.ps1 -Suite stress
```

Ejecución en Bash:

```bash
./tests/k6/phase3/run-phase3.sh load
./tests/k6/phase3/run-phase3.sh stress
```

Los resultados crudos quedan en `tests/k6/phase3/results/`. La conclusión se
registra en `docs/evidence/performance/FASE3-REPORTE-CARGA-ESTRES.md` con
throughput, p50, p95, p99, errores, recursos, límite y recuperación.

La ejecución local del 14 de octubre de 2026 completó los cinco casos de carga
y los cinco de estrés. STRESS-03 se repitió con identificadores aislados y pasó
con 0 % de errores; STRESS-04 encontró un backlog máximo de 5,566 que no drenó
en la ventana de dos minutos. Estos resultados locales no representan la
capacidad de la VM de producción. El smoke externo se documenta por separado.

## 10. CI/CD

### 10.1 Pipeline integrado

El workflow `.github/workflows/ci-cd.yml` ejecuta matrices de build y pruebas
para los cinco proyectos Java, valida el frontend y ambos Compose, publica
reportes, construye seis imágenes con tags inmutables y permite desplegar solo
después de que los gates anteriores concluyen correctamente.

### 10.2 Gate de Fase 3

```text
build -> unitarias/integración -> imágenes -> deploy -> health -> smoke
```

`deploy` debe depender de todos los trabajos de prueba. Si cualquier prueba
falla, no debe ejecutarse. Las imágenes deben etiquetarse por release y SHA,
no únicamente como `latest`.

**Estado:** pipeline, imágenes, deploy, health y smoke están implementados; el
tag `v1` existe en `main`. Andy aún debe adjuntar la revisión del run y Kevin
debe conservar evidencia del ensayo de rollback y de los tags `v2` a `v4`.

## 11. Despliegue en nube

Antes de aceptar el despliegue se requiere evidencia de:

- Proveedor y topología documentados.
- Red y exposición mínima.
- Secretos fuera del repositorio y de las imágenes.
- Persistencia y backup.
- Migraciones controladas.
- Health check posterior al despliegue.
- Smoke externo de cinco rutas críticas.
- Rollback al tag anterior.

URL pública: `http://136.65.180.39`. La consola está en `/staff.html`; almacén,
POS-01, LogiExpress y PayKlik usan los puertos `8082`, `8084`, `8085` y `8086`.
No se documentan contraseñas ni llaves en este manual.

## 12. Operación

### 12.1 Comprobación de salud

```bash
curl --fail --silent http://localhost:8080/actuator/health/readiness
docker compose ps
```

### 12.2 Diagnóstico

```bash
docker compose logs --no-color --tail=200 backend
docker compose logs --no-color --tail=200 rabbitmq
docker compose logs --no-color --tail=200 postgres
```

Buscar el `correlationId` de la operación afectada. No copiar tokens, secretos,
datos financieros ni información personal en tickets o evidencias.

### 12.3 Reinicio controlado

```bash
docker compose restart backend
docker compose ps
curl --fail --silent http://localhost:8080/actuator/health/readiness
```

Un reinicio no sustituye investigar la causa. Después se verifican colas,
Outbox, Inbox y operaciones pendientes.

### 12.4 Backup, restauración y rollback

`deploy/scripts/backup.sh` conserva respaldos comprimidos; `deploy.sh` registra
el tag previo y ejecuta rollback automático si health o smoke fallan. El
procedimiento manual está en `docs/operacion/RUNBOOK.md`. No debe declararse el
rollback como ensayado hasta guardar la evidencia de volver a un tag anterior y
restaurar el actual.

## 13. Smoke independiente en nube

Andy debe ejecutarlo sin acceso al host, usando solo URL y cuentas de prueba.
Las cinco rutas mínimas serán:

1. Estado público de la plataforma (`/api/v1/platform/status`); Actuator no se
   publica mediante nginx en producción.
2. Autenticación.
3. Consulta de opciones/elegibilidad.
4. Checkout mediante crédito o PayKlik.
5. Consulta de orden/auditoría o notificación.

El script `tests/k6/phase3/smoke-phase3-cloud.js` usa las rutas definitivas:
crea carrito, agrega producto, consulta opciones, hace checkout, ejecuta la Saga
y consulta la orden. El reporte debe indicar fecha, SHA/tag, URL sin
credenciales, códigos HTTP y conclusión.

**Resultado 2026-10-14:** aprobado contra la URL pública de GCP con 11/11
comprobaciones, 0 % de errores, p95 HTTP de 331.21 ms y recorrido completo en
2.06 s. Actuator permanece interno; el arnés usa
`/api/v1/platform/status` para el estado público.

## 14. Revisión cruzada de CI/CD

Andy aprueba la evidencia de Kevin únicamente si:

- Build y pruebas preceden al despliegue.
- Una prueba fallida impide desplegar.
- Frontend y backend se construyen con Docker.
- No aparecen secretos en YAML, logs ni artefactos.
- Health y smoke se ejecutan después del deploy.
- Las imágenes usan SHA/tag inmutable.
- Se demuestra rollback al release anterior.
- Existen cuatro releases trazables en `main`.

La revisión estática y sus pendientes están registrados en
`docs/evidence/devops/REV-ANDY-2026-10-14-CICD.md`.

## 15. Seguridad

- No almacenar PAN ni CVV.
- No versionar `.env`, tokens, llaves, contraseñas o respaldos.
- Aplicar mínimo privilegio por endpoint.
- Sanitizar logs y reportes k6.
- Usar datos sintéticos para carga y estrés.
- Ejecutar estrés solo en ambientes autorizados.
- Rotar cualquier secreto que aparezca accidentalmente en una salida.

## 16. Limitaciones y pendientes

| Elemento | Estado | Dueño |
|---|---|---|
| Crédito propio y elegibilidad | INTEGRADO | Jorge/Jairo |
| Ruta dual completa y eventos | INTEGRADO | Jairo/Edson |
| Endpoints definitivos para k6 | ALINEADOS EN EL ARNÉS | Andy |
| Cinco ejecuciones de carga | EJECUTADAS EN LOCAL; observaciones documentadas | Andy |
| Cinco ejecuciones de estrés | EJECUTADAS EN LOCAL; STRESS-04 abierto | Andy |
| Análisis antes/después | COMPLETADO para STRESS-03 | Andy |
| Nube y URL | DISPONIBLE; smoke independiente APROBADO 11/11 | Kevin/Andy |
| Deploy y release `v1` | IMPLEMENTADOS | Kevin |
| Rollback ensayado y releases `v2`–`v4` | PENDIENTE de evidencia | Kevin |
| Manual revisado por no autor | PENDIENTE | Kevin |

## 17. Criterio de cierre del manual

El Manual Técnico queda final únicamente cuando:

- Describe el commit/tag entregado, no una arquitectura futura.
- Contiene todas las variables nuevas sin secretos.
- Otra persona ejecuta instalación y smoke siguiendo solo este documento.
- Enlaza resultados reales de las 25 pruebas.
- Incluye URL, operación, backup y rollback verificados.
- Kevin registra su revisión cruzada.

