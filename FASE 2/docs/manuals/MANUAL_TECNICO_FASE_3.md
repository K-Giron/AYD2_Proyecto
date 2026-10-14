# Manual Técnico - SGO-Logic Fase 3

**Responsable:** Andy Sánchez  
**Revisor asignado:** Kevin Girón  
**Estado:** Borrador técnico verificable  
**Última actualización:** 2026-10-13  
**Alcance:** construcción, configuración, ejecución, pruebas, operación y
recuperación de SGO-Logic.

## 1. Propósito y regla de veracidad

Este manual permite preparar y operar SGO-Logic sin depender del conocimiento
oral de sus autores. Distingue el comportamiento comprobado del trabajo aún no
integrado. Un elemento marcado `PENDIENTE` no debe presentarse como disponible.

Al momento de crear esta versión, `develop` contiene la plataforma funcional de
Fase 2 y la planificación de Fase 3, pero todavía no contiene el módulo de
crédito propio ni sus endpoints definitivos. Las secciones correspondientes se
cerrarán contra el SHA o release que integre esa rebanada.

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

**Estado de esta ampliación:** PENDIENTE de integración por sus responsables.

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

### 3.2 Evolución objetivo de Fase 3

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

Los recursos finales, proveedor, URL, DNS, backup y costo son responsabilidad
del frente de Kevin y permanecen `PENDIENTES` en este manual hasta recibir
evidencia. No se inventan tamaños ni garantías de disponibilidad.

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

Variables nuevas de crédito: `PENDIENTES` del contrato definitivo. Deben
documentarse aquí antes de `v4`, incluyendo reglas, versión, timeouts y límites,
sin registrar secretos.

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

1. Confirmar el SHA/tag y que los endpoints de crédito estén integrados.
2. Recibir de Kevin una URL estable y autorización para generar carga.
3. Sembrar un dataset determinista y aislado.
4. Copiar `.env.performance.example` como `.env.performance`.
5. Ajustar las rutas al contrato real y nunca guardar secretos.

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

## 10. CI/CD

### 10.1 Estado comprobado

El workflow actual `.github/workflows/qa-integracion-base.yml` ejecuta:

1. Checkout del repositorio.
2. Java 25.
3. `clean verify` del backend.
4. Publicación de reportes Surefire.
5. Construcción y smoke de Compose después de las pruebas.

### 10.2 Gate requerido para Fase 3

```text
build -> unitarias/integración -> imágenes -> deploy -> health -> smoke
```

`deploy` debe depender de todos los trabajos de prueba. Si cualquier prueba
falla, no debe ejecutarse. Las imágenes deben etiquetarse por release y SHA,
no únicamente como `latest`.

**Estado:** publicación de imágenes, deploy, smoke en nube y rollback están
`PENDIENTES` de Kevin y de revisión cruzada por Andy.

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

URL, región, recursos, acceso y procedimiento final: `PENDIENTES` de Kevin.

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

El procedimiento específico de nube y la compatibilidad de migraciones están
`PENDIENTES` del runbook de Kevin. No debe declararse rollback completo hasta
probar restauración de datos y compatibilidad con el tag anterior.

## 13. Smoke independiente en nube

Andy debe ejecutarlo sin acceso al host, usando solo URL y cuentas de prueba.
Las cinco rutas mínimas serán:

1. Readiness de la plataforma.
2. Autenticación.
3. Consulta de opciones/elegibilidad.
4. Checkout mediante crédito o PayKlik.
5. Consulta de orden/auditoría o notificación.

Las rutas 3 y 4 están `PENDIENTES` del contrato final. El reporte debe indicar
fecha, SHA/tag, URL sin credenciales, códigos HTTP y conclusión.

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
| Crédito propio y elegibilidad | PENDIENTE de integración local | Jorge/Jairo |
| Ruta dual completa y eventos | PENDIENTE | Jairo/Edson |
| Endpoints definitivos para k6 | PENDIENTE | Jairo/Jorge/Edson |
| Cinco ejecuciones de carga | PENDIENTE de ambiente estable | Andy |
| Cinco ejecuciones de estrés | PENDIENTE de autorización/ambiente | Andy/Kevin |
| Análisis antes/después | PENDIENTE de primeras mediciones | Andy |
| Nube y URL | PENDIENTE | Kevin |
| Deploy, rollback y cuatro releases | PENDIENTE de evidencia | Kevin |
| Manual revisado por no autor | PENDIENTE | Kevin |

## 17. Criterio de cierre del manual

El Manual Técnico queda final únicamente cuando:

- Describe el commit/tag entregado, no una arquitectura futura.
- Contiene todas las variables nuevas sin secretos.
- Otra persona ejecuta instalación y smoke siguiendo solo este documento.
- Enlaza resultados reales de las 25 pruebas.
- Incluye URL, operación, backup y rollback verificados.
- Kevin registra su revisión cruzada.

