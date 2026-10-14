# Plan de ejecución y cierre — Fase 3 de SGO-Logic

**Equipo:** Grupo 5  
**Periodo de cierre replanificado:** 12 al 14 de octubre de 2026
**Entrega:** 14 de octubre de 2026  
**Calificación:** 17 de octubre de 2026  
**Fuente:** [Enunciado de Fase 3](../../Proyecto%20Fase%203_convertido.md)  
**Esfuerzo de referencia del enunciado:** 75 horas  
**Corte de validación del repositorio:** 12 de octubre de 2026
**Objetivo operativo:** completar crédito propio, evolución arquitectónica,
CI/CD, despliegue en nube, cuatro releases, 25 pruebas mínimas y documentación,
sin degradar la ruta existente de PayKlik.

---

## 1. Lectura ejecutiva del enunciado

Fase 3 no se completa únicamente agregando un botón de crédito. La entrega debe
probar que SGO-Logic puede incorporar una segunda ruta de pago sin romper lo ya
construido y que el equipo puede llevarla hasta un ambiente de nube mediante un
proceso repetible.

Los resultados obligatorios son:

1. Corregir la documentación de Fases 1 y 2 con el feedback real recibido.
2. Diseñar e implementar la evaluación de crédito propio “Compra ahora, paga
   en 30 días”.
3. Mantener PayKlik como ruta independiente y funcional.
4. Usar el comportamiento histórico del cliente para determinar elegibilidad.
5. Resolver la evaluación con rapidez, trazabilidad y una explicación
   auditable.
6. Notificar al cliente el resultado sin bloquear la finalización de la orden.
7. Actualizar DDA, actores, drivers, escenarios de calidad, ADR y diagramas.
8. Implementar como mínimo cinco pruebas unitarias, cinco de integración,
   cinco E2E, cinco de carga y cinco de estrés.
9. Construir un pipeline con etapas `build`, `test` y `deploy`; si las pruebas
   fallan, el despliegue no debe ejecutarse.
10. Empaquetar frontend y backend con Docker e incluirlos en CI/CD.
11. Desplegar en nube la arquitectura documentada.
12. Aplicar git-flow y producir por lo menos cuatro releases.
13. Entregar Manual Técnico y Manual de Usuario en Markdown.
14. Mantener evidencia de contribuciones sustanciales de los cinco integrantes.

### 1.1 Verificación enunciado versus plan

Esta matriz evita tratar como obligatorio algo que el enunciado no pide y evita
omitir requisitos que sí son calificables.

| Requisito literal | Cobertura en este plan | Evidencia de cierre |
|---|---|---|
| Corregir documentación de Fases 1 y 2 con feedback real | J3-01, J3-05 y registro documental | Observación, acción, archivo, revisor y SHA |
| Diseñar e implementar la nueva arquitectura de crédito | ADR de Fase 3, módulo `credit`, opciones de pago y Saga dual | Código, migración, diagramas y demo de ambas rutas |
| Mínimo cuatro releases y git-flow | Sección 6 | Ramas `release/fase3-vN`, tags `v1` a `v4` y notas |
| CI/CD `build`, `test`, `deploy` en GitHub o GitLab | KEV3-04 y sección 8 | Runs donde un fallo de test impide `deploy` |
| Despliegue en nube de la arquitectura documentada | KEV3-01 a KEV3-08 | URL, topología, health, smoke y rollback |
| Docker en frontend y backend dentro del CI/CD | KEV3-03/04 | Imágenes etiquetadas por tag y SHA |
| Cinco pruebas por cada uno de los cinco tipos | Sección 5 | 25 casos distintos con reporte automatizado |
| Manual Técnico y Manual de Usuario en Markdown | AND3-05 y J3-06 | Archivos navegables desde el README |
| Commits sustanciales de todos; merge no cuenta | Secciones 6, 9 y 14 | Matriz integrante → commits → entregables |
| Auxiliar agregado y entrega por UEDI | Checklist final | Colaborador verificado y enlace entregado |

### 1.2 Estado inicial comprobado al 12 de octubre

- La base de Fase 2 compila y sus suites locales registran 290 pruebas verdes:
  227 del backend, 40 de inventario central, 15 de tienda física, 4 de PayKlik
  y 4 de LogiExpress.
- Docker Compose es válido y ya existen Dockerfiles para los componentes. No se
  deben reconstruir desde cero; se endurecen y se incorporan al pipeline.
- Ya existen Saga reentrante, Outbox/RabbitMQ, Inbox, auditoría, correlación,
  notificaciones no bloqueantes con reintento/DLQ y aislamiento de PayKlik.
- El tag `v2.0.0` identifica un corte previo de Fase 2. Los cuatro tags de Fase 3 serán
  `v1`, `v2`, `v3` y `v4`, disponibles y diferenciados en las notas de release.
- `main` es ancestro de `develop` y está 15 commits detrás en el corte. Fase 3
  debe ramificarse desde `develop`; la primera release promoverá ese baseline
  completo a `main` sin reescribir ni omitir el historial de Fase 2.
- No existe todavía implementación de crédito ni release de Fase 3. Por ello el
  primer gate del 12 de octubre es obligatorio y no se presenta trabajo futuro
  como terminado.
- El enunciado convertido y sus imágenes deben versionarse para que el vínculo
  `Fuente` de este documento funcione en GitHub.

## 2. Decisión arquitectónica de trabajo

La ampliación se implementará como una **arquitectura híbrida**: decisión
síncrona y rápida en checkout, Saga orquestada para el ciclo de la orden y
arquitectura dirigida por eventos para notificación, auditoría y proyecciones.
Este uso explícito de eventos de crédito constituye la evolución arquitectónica
de Fase 3 y convive con el monolito modular y la infraestructura de mensajería
construida en Fase 2. El ADR de Fase 3 debe distinguir entre el estilo
arquitectónico y los mecanismos existentes (Outbox, RabbitMQ, Inbox y DLQ):

```text
Checkout
   |
   v
PaymentOptionService (síncrono, con timeout)
   |-----------------------|
   v                       v
CreditEligibilityPort      PayKlikPort
   |                       |
   v                       v
credit module          payments module
   |
   +--> CreditDecisionRecorded (Outbox)
   +--> CreditGranted / CreditRejected (eventos)
                         |
                         v
                 notifications + audit
```

Principios que no se pueden romper:

- El módulo de crédito no consulta directamente tablas de ERP, pagos, órdenes
  ni incidencias. Consume un contrato o una proyección local.
- La historia proveniente de ERP se incorpora mediante adaptador y eventos
  idempotentes.
- La evaluación usa reglas explícitas, versionadas y explicables; no se inventa
  inteligencia artificial ni un puntaje opaco.
- La opción de crédito solo aparece cuando el cliente es elegible.
- La ruta PayKlik no depende de la disponibilidad del crédito.
- Una notificación fallida no revierte una orden ya confirmada.
- Toda decisión registra cliente, regla, versión, entradas relevantes, resultado,
  razón, instante y `correlationId`, sin exponer datos sensibles.
- La fecha de vencimiento es 30 días después de conceder el crédito y debe
  persistirse como dato de negocio, no calcularse únicamente en la interfaz.
- La referencia fiscal no puede seguir llamándose exclusivamente
  `paymentAttemptId`: la ruta de crédito debe entregar un identificador de
  operación financiera equivalente y el contrato fiscal debe generalizarlo sin
  crear un intento PayKlik ficticio.
- La Saga conserva estados explícitos para crédito o una abstracción documentada
  de autorización financiera. No debe registrar un crédito concedido como si
  PayKlik hubiera cobrado al cliente.

Esta propuesta se congela mediante un ADR antes de combinar las ramas de
implementación. Si el equipo elige otra forma, debe conservar las mismas
fronteras y actualizar este plan, el DDA y las pruebas antes de programar.

### 2.1 Decisiones funcionales que deben cerrarse en el primer gate

El enunciado indica las variables de decisión, pero no sus umbrales. El equipo
debe aprobar y versionar estos valores; no se permite que cada desarrollador
invente reglas distintas.

| Decisión | Valor que debe aprobar el equipo | Valor inicial recomendado para datos de demo |
|---|---|---|
| Historia suficiente | Ventana y mínimo de compras completadas | Últimos 12 meses y al menos 3 compras |
| Puntualidad | Máximo de pagos vencidos | Ningún crédito vencido activo |
| Devoluciones | Umbral que reduce o niega elegibilidad | Rechazar si la tasa supera 30 % |
| Incidentes | Severidad/cantidad aceptable | Rechazar con 2 o más incidentes críticos abiertos |
| Límite | Fórmula y tope del crédito | 40 % de compras históricas, con tope configurable |
| Datos insuficientes | Resultado y razón pública | `REJECTED`, código `INSUFFICIENT_HISTORY` |
| Crédito vigente | Política de una nueva concesión | No conceder si existe obligación vencida |
| Tiempo de decisión | Umbral medible y fallback | p95 ≤ 1 s; a los 2 s ocultar crédito y conservar PayKlik |
| Ingesta histórica | Archivo, API o dataset controlado proveniente del ERP | Contrato versionado e idempotente con compras, puntualidad, devoluciones e incidentes |

Los valores de la tercera columna son una propuesta de trabajo, no hechos del
enunciado. La decisión final debe aparecer en ADR, configuración, pruebas y
Manual de Usuario con la misma versión de reglas.

## 3. Reparto equitativo

La equidad se calcula por horas enfocadas y dificultad, no por número de
tarjetas. La reestimación descuenta capacidades ya implementadas en Fase 2 y
conserva para cada persona un frente técnico de alta responsabilidad.

| Integrante | Frente principal | Carga | Responsabilidad de mayor riesgo |
|---|---|---:|---|
| Jairo Gómez | Checkout, Saga y arquitectura funcional | **22 h** | Convivencia de crédito y PayKlik sin romper órdenes |
| Jorge Pumay | Datos históricos y motor de elegibilidad | **22 h** | Decisión crediticia consistente y explicable |
| Edson Sosa | Integración, seguridad, auditoría y pruebas funcionales | **20 h** | Cinco integraciones y cinco E2E reproducibles |
| Andy Sánchez | Rendimiento, estrés y manual técnico | **18 h** | Diez escenarios no funcionales y reporte de recuperación |
| Kevin Girón | Nube, Docker, CI/CD y releases | **18 h** | Despliegue automático, rollback y cuatro releases |
| **Total** | | **100 h** | Esfuerzo restante del equipo, incluyendo revisión y evidencia |

Las 75 horas del enunciado son una referencia académica cuya unidad no se
aclara (persona o equipo); no se usan para justificar omisiones. La estimación
restante incluye implementación, corrección, revisión cruzada, documentación y
evidencia. Las tareas se ejecutan en paralelo y se reduce únicamente alcance
cosmético, nunca un entregable literal.

## 4. Responsabilidades por integrante

### 4.1 Jairo Gómez — arquitectura funcional y orquestación

**Alcance:** evolución funcional de checkout y Saga, coherencia arquitectónica,
trazabilidad y documentación orientada al usuario.

El desglose operativo, contratos y secuencia diaria se encuentran en
[PLAN_EJECUCION_JAIRO_FASE_3.md](PLAN_EJECUCION_JAIRO_FASE_3.md).

| ID | Resultado verificable | Horas | Dependencias | Terminado cuando |
|---|---|---:|---|---|
| J3-01 | Convertir el enunciado y el feedback real en backlog, actores, reglas y criterios de aceptación | 2 | Feedback de revisiones | No quedan requisitos ambiguos sin decisión o responsable |
| J3-02 | Diseñar el contrato de opciones de pago y la selección Crédito/PayKlik | 3 | ADR de Fase 3 | El contrato está versionado y revisado por Jorge y Edson |
| J3-03 | Integrar la opción elegible en checkout sin mostrarla a clientes rechazados | 4 | API de elegibilidad de Jorge | Checkout conserva compatibilidad con PayKlik |
| J3-04 | Extender la Saga para confirmar o rechazar ambas rutas y mantener compensaciones | 5 | Crédito de Jorge; integración de Edson; referencia fiscal generalizada por Andy | Reintentos no duplican crédito, pago, reserva ni orden |
| J3-05 | Actualizar DDA: alcance, actores, drivers, EaC, ADR, CDU, componentes y secuencia de decisión | 4 | Evidencia de los dueños | DDA coincide con código y despliegue; no presenta planes como hechos |
| J3-06 | Redactar Manual de Usuario y guion de demostración funcional | 2 | UI y flujos estables | Un usuario ejecuta crédito aprobado, rechazo y PayKlik sin asistencia técnica |
| J3-07 | Coordinación, dos revisiones cruzadas y cierre de trazabilidad | 2 | Aportes de todos | Cada requisito apunta a código, prueba y evidencia |
| **Total** | | **22 h** | | |

Entradas requeridas para completar este frente:

- Kevin entrega URL de nube, evidencia del pipeline, rollback y tags.
- Andy entrega resultados de carga y estrés.
- Edson entrega reportes de integración y E2E.
- Jorge entrega reglas, datos y pruebas unitarias del crédito.

### 4.2 Jorge Pumay — dominio de crédito y elegibilidad

| ID | Resultado verificable | Horas | Dependencias | Terminado cuando |
|---|---|---:|---|---|
| JOR3-01 | Modelo y migraciones de perfil, historial resumido, decisión y cuenta de crédito | 4 | Contrato de datos | Flyway parte de base limpia y de base Fase 2 |
| JOR3-02 | Adaptador/proyección idempotente del historial ERP: compras, puntualidad, devoluciones e incidentes | 4 | Contrato ERP | Reprocesar el mismo dato no duplica ni altera totales |
| JOR3-03 | Motor de reglas configurable, versionado y explicable | 5 | JOR3-01/02 | Cada decisión devuelve `APPROVED`/`REJECTED`, límite y razones |
| JOR3-04 | Ciclo de crédito: concesión, vencimiento a 30 días y estados mínimos | 3 | Motor de reglas | Se impide una segunda concesión incompatible y se registra vencimiento |
| JOR3-05 | Cinco o más pruebas unitarias del dominio | 4 | JOR3-03/04 | Suite verde y casos límite documentados |
| JOR3-06 | Documentación del modelo y revisión del checkout de Jairo | 2 | J3-03 | Esquema, invariantes y contrato coinciden |
| **Total** | | **22 h** | | |

### 4.3 Edson Sosa — integración, seguridad y pruebas funcionales

| ID | Resultado verificable | Horas | Dependencias | Terminado cuando |
|---|---|---:|---|---|
| EDS3-01 | Revisar la ruta de crédito y proteger idempotencia | 3 | Contrato J3-02; dominio Jorge | Reintento conserva una sola decisión y un solo efecto financiero |
| EDS3-02 | Conectar eventos de decisión a notificación no bloqueante existente | 2 | Outbox/RabbitMQ | Falla de notificación no revierte la orden y queda reintentable |
| EDS3-03 | Extender roles, autorización y sanitización al módulo de crédito | 2 | Endpoints definidos | Matriz 401/403 aprobada y sin información crediticia sensible en logs |
| EDS3-04 | Integrar la decisión con la auditoría existente | 2 | Evento de decisión | Se reconstruye quién, cuándo, regla, resultado y razón |
| EDS3-05 | Cinco o más pruebas de integración | 4 | Servicios integrados | Suite automatizada verde con dependencias reales de prueba |
| EDS3-06 | Cinco o más pruebas E2E | 5 | Rebanada completa y Compose | Flujos por API/UI pasan desde ambiente limpio |
| EDS3-07 | Evidencia QA y revisión de seguridad | 2 | Resultados anteriores | Reporte enlaza caso, ejecución, resultado y defecto corregido |
| **Total** | | **20 h** | | |

### 4.4 Andy Sánchez — carga, estrés y Manual Técnico

Andy es el propietario del diseño y ejecución de carga/estrés. Kevin proporciona
el ambiente estable y los datos de acceso requeridos para medirlo.

| ID | Resultado verificable | Horas | Dependencias | Terminado cuando |
|---|---|---:|---|---|
| AND3-00 | Generalizar el contrato fiscal de intento PayKlik a operación financiera | 2 | Contratos PayKlik/crédito | Factura acepta referencia PayKlik o crédito sin crear intentos ficticios |
| AND3-01 | Ampliar harness k6 existente, datos reproducibles y captura de métricas | 1 | Endpoints estables | Existe comando único, umbrales y limpieza documentada |
| AND3-02 | Implementar y ejecutar cinco pruebas de carga | 3 | Ambiente de Kevin | Reporte contiene VUs, duración, throughput, p95/p99 y errores |
| AND3-03 | Implementar y ejecutar cinco pruebas de estrés | 4 | Carga estable | Se identifica límite, degradación y comportamiento de recuperación |
| AND3-04 | Analizar cuellos de botella y repetir después de ajustes | 2 | Resultados iniciales | Informe distingue observación, causa inferida y corrección medida |
| AND3-05 | Redactar Manual Técnico completo | 3 | Aportes de todos | Incluye arquitectura, requisitos, variables, operación y pruebas |
| AND3-06 | Smoke test independiente del ambiente de nube | 1 | URL y credenciales de Kevin | Valida salud y cinco rutas críticas sin acceso al host |
| AND3-07 | Correcciones y revisión cruzada de la evidencia de CI/CD | 2 | Pipeline Kevin | Comandos y resultados son reproducibles por un no autor |
| **Total** | | **18 h** | | |

### 4.5 Kevin Girón — despliegue, CI/CD y releases

Kevin es propietario del despliegue, del pipeline y de la administración de los
cuatro releases. Andy realiza la validación cruzada de este frente.

| ID | Resultado verificable | Horas | Dependencias | Terminado cuando |
|---|---|---:|---|---|
| KEV3-01 | Confirmar proveedor y documentar topología/costo | 1 | Crédito o cuenta disponible | ADR registra proveedor, alternativa y límites; opción base: VM Ubuntu + Compose |
| KEV3-02 | Proveer infraestructura de nube, red y secretos | 3 | KEV3-01 | Ambiente accesible sin secretos versionados |
| KEV3-03 | Endurecer Dockerfiles existentes y Compose de producción | 2 | Build estable | Imágenes reproducibles, health checks y persistencia definidos |
| KEV3-04 | GitHub Actions: build → test → imágenes → deploy | 5 | Secrets y Docker | `deploy` depende de `test`; un test fallido impide desplegar |
| KEV3-05 | Health check posterior, backup y rollback a release anterior | 2 | Pipeline desplegando | Falla del health check revierte o detiene promoción con evidencia |
| KEV3-06 | Administrar git-flow y publicar `v1`, `v2`, `v3`, `v4` | 3 | Gates de release | Tags y releases apuntan a `main` y contienen notas/evidencia |
| KEV3-07 | Smoke E2E en nube y entrega del ambiente a Andy | 1 | Despliegue verde | URL, usuarios de prueba y dataset entregados |
| KEV3-08 | Runbook de despliegue y operación | 1 | Pipeline final | Otra persona puede consultar logs, reiniciar y revertir |
| **Total** | | **18 h** | | |

## 5. Catálogo mínimo de las 25 pruebas

No basta con tener 25 métodos que prueben lo mismo. Los casos deben cubrir
riesgos diferentes y generar evidencia automatizada.

### 5.1 Unitarias — Jorge

| ID | Caso |
|---|---|
| UT-01 | Cliente puntual y con volumen suficiente resulta elegible |
| UT-02 | Cliente moroso resulta rechazado con razón explícita |
| UT-03 | Límite exacto de una regla produce el resultado definido, sin ambigüedad |
| UT-04 | Exceso de devoluciones/incidencias reduce o niega elegibilidad |
| UT-05 | Concesión calcula vencimiento a 30 días y monto permitido |

### 5.2 Integración — Edson

| ID | Caso |
|---|---|
| IT-01 | Evento/adaptador ERP actualiza la proyección crediticia una sola vez |
| IT-02 | Checkout consulta elegibilidad y expone las opciones correctas |
| IT-03 | Crédito aprobado confirma orden, conserva reserva y registra auditoría |
| IT-04 | Crédito rechazado no crea obligación ni afecta la ruta PayKlik |
| IT-05 | Evento de resultado crea notificación y soporta reintento/DLQ |

### 5.3 End to End — Edson

| ID | Caso |
|---|---|
| E2E-01 | Cliente elegible compra a crédito y recibe fecha de pago |
| E2E-02 | Cliente no elegible completa la compra mediante PayKlik |
| E2E-03 | Evaluación indisponible no congela el checkout ni daña PayKlik |
| E2E-04 | Repetición con la misma llave idempotente no duplica crédito ni orden |
| E2E-05 | Notificación caída deja la orden confirmada y el mensaje reintentable |

### 5.4 Carga — Andy

| ID | Caso |
|---|---|
| LOAD-01 | Consulta concurrente de opciones/elegibilidad en carga esperada |
| LOAD-02 | Mezcla de checkout crédito y PayKlik en carga esperada |
| LOAD-03 | Ingesta sostenida de historia ERP sin duplicados |
| LOAD-04 | Consumo de eventos y creación de notificaciones |
| LOAD-05 | Consulta de estado y auditoría durante compras concurrentes |

### 5.5 Estrés — Andy

| ID | Caso |
|---|---|
| STRESS-01 | Rampa progresiva hasta encontrar el primer umbral de degradación |
| STRESS-02 | Pico súbito de checkout por encima de la carga objetivo |
| STRESS-03 | Saturación del motor de elegibilidad sin bloquear PayKlik |
| STRESS-04 | Backlog de eventos/notificaciones y drenaje posterior |
| STRESS-05 | Recuperación: salud, errores y latencia después de retirar la sobrecarga |

Cada reporte debe registrar commit/tag, ambiente, dataset, fecha, comando,
resultado, métricas, capturas o artefactos y conclusión. Una prueba sin evidencia
no se contabiliza.

## 6. Plan de cuatro releases

| Release | Fecha límite | Contenido mínimo | Gate |
|---|---|---|---|
| `v1` | 12 oct, primer corte | Enunciado versionado, feedback registrado, ADR, contratos, migración inicial y pipeline `build/test` | Compila, pruebas preexistentes verdes y DDA base actualizado |
| `v2` | 12 oct, segundo corte | Proyección histórica, motor de elegibilidad, opciones de checkout y primer flujo de crédito | 5 unitarias nuevas + integración mínima verde |
| `v3` | 13 oct | Saga dual, eventos, notificaciones, seguridad, auditoría y E2E | 5 E2E verdes; imágenes Docker publicables |
| `v4` | 14 oct | Nube, pipeline completo, resultados de carga/estrés, manuales, DDA y evidencia final | 25 pruebas, deploy verde, smoke externo y rollback documentado |

Flujo obligatorio para cada release:

```text
feature/* desde develop
    -> PR con revisión y CI verde
    -> develop
    -> release/fase3-vN
    -> validación del gate
    -> main
    -> tag vN + notas de release
```

Reglas:

- Cada integrante produce commits sustanciales propios con formato
  `#carnet: mensaje en infinitivo`.
- Un merge de PR no sustituye los commits sustanciales exigidos.
- No se reescribe historial para simular participación.
- `main` solo recibe una rama `release/fase3-vN` que haya pasado su gate.
- `hotfix/*` nace de `main` y vuelve tanto a `main` como a `develop`.
- Las operaciones Git y publicaciones se ejecutan durante el proyecto por sus
  responsables; este documento no las realiza.

## 7. Cronograma diario y paralelización

### 12 de octubre — Gate 0, `v1` y `v2`

- Todos: registrar disponibilidad, carné, acceso y feedback real; versionar el
  enunciado; congelar reglas, contrato fiscal generalizado y contrato de eventos.
- Jairo: ADR/borrador de DDA, contrato de opciones, ruta de la Saga y frontend.
- Jorge: esquema, proyección, reglas de elegibilidad y cinco unitarias.
- Edson: contratos de eventos, seguridad, auditoría y primeras integraciones.
- Andy: generalización de la referencia fiscal, protocolo de carga/estrés y dataset.
- Kevin: proveedor de nube, secretos, ramas git-flow y pipeline `build/test`.
- Primer corte `v1`: contratos y migración compilan sobre la línea base verde.
- Segundo corte `v2`: historia → elegibilidad → opción visible → concesión mínima.
- Salida: ningún requisito P0 ambiguo, cinco unitarias verdes y PayKlik sin regresión.

### 13 de octubre — Release `v3`

- Saga de dos rutas, notificación, seguridad y auditoría completas.
- Cinco E2E verdes desde ambiente limpio.
- Despliegue candidato en nube a cargo de Kevin.
- Andy ejecuta las cinco pruebas de carga y cinco de estrés únicamente después
  del smoke verde; publica resultados y deja margen para una corrección medida.
- Manuales y DDA quedan al menos al 80 %, sin esperar al día de entrega.

### 14 de octubre — Release `v4` y entrega

- Andy repite solo los escenarios afectados por correcciones y cierra el análisis;
  no inicia las diez pruebas por primera vez el día de entrega.
- Kevin ejecuta pipeline, deploy final, health check, rollback ensayado y `v4`.
- Jairo cierra DDA/Manual de Usuario con evidencia recibida.
- Andy cierra Manual Técnico.
- Todos ejecutan un ensayo de defensa de 10–15 minutos.

Después del congelamiento funcional del 14 de octubre solo se permiten defectos
de entrega, configuración o documentación. No se agregan reglas de negocio ni
cambios de arquitectura ese día.

## 8. CI/CD y despliegue esperado

La implementación base propuesta usa GitHub Actions porque el repositorio ya
contiene un workflow de Fase 2. Kevin puede cambiar el proveedor solo mediante
ADR y sin reducir los gates.

```text
Pull request
  -> lint/validación de archivos
  -> build de backend, inventory-service, store-service, payklik-service y logiexpress-service
  -> pruebas unitarias
  -> pruebas de integración
  -> build de las imágenes de la topología y, como mínimo obligatorio, frontend/backend

release/* o tag
  -> repetir gates anteriores
  -> publicar imágenes con tag inmutable
  -> deploy a nube
  -> health check
  -> smoke E2E
  -> conservar evidencia / habilitar rollback
```

El pipeline debe garantizar:

- `deploy` usa `needs`/dependencias sobre todos los trabajos de prueba.
- No existen secretos en YAML, Dockerfile, Compose, Markdown o imágenes.
- Las imágenes se identifican por release y SHA, nunca solo por `latest`.
- Frontend y backend se construyen con Docker.
- Las migraciones se ejecutan de manera controlada antes de promover tráfico.
- Existe persistencia para bases/colas y estrategia básica de backup.
- El health check verifica algo más que un puerto abierto.
- El rollback usa el tag anterior y documenta las limitaciones de migraciones.
- El Compose de nube ejecuta la misma topología que muestra el diagrama. Si un
  componente se mantiene simulado o fuera del host, el DDA lo declara; no se
  dibuja como desplegado algo que el pipeline no promociona.

### 8.1 Criterios mínimos de rendimiento

| Medición | Criterio inicial de aceptación |
|---|---|
| Consulta de opciones/elegibilidad | p95 ≤ 1 s y menos de 1 % de error en carga esperada |
| Checkout mixto crédito/PayKlik | p95 ≤ 5 s para la respuesta sin esperar notificación; menos de 1 % de error técnico |
| Falla o timeout de elegibilidad | En ≤ 2 s PayKlik sigue disponible y la orden no queda congelada |
| Idempotencia | 0 créditos, pagos u órdenes duplicados |
| Backlog de notificaciones | Se drena después de retirar la carga, sin revertir órdenes |
| Recuperación de estrés | Health verde y tasa de error normal en ≤ 5 min |

Estos umbrales se confirman contra la capacidad real de la VM antes de ejecutar.
El generador k6 corre fuera de la VM medida; ambiente, VUs, duración, dataset y
SHA forman parte del reporte.

## 9. Documentación y evidencia obligatoria

| Artefacto | Responsable | Revisor | Contenido mínimo |
|---|---|---|---|
| Registro de feedback F1/F2 | Jairo | Kevin | Observación literal, acción, archivo y evidencia |
| DDA actualizado | Jairo | Jorge + Edson | Actores, drivers, EaC, ADR, CDU, datos, componentes y despliegue |
| Diagramas editables/exportados | Jairo coordina; cada dueño aporta | Kevin | Crédito, eventos, nube y trazabilidad con implementación |
| Manual de Usuario | Jairo | Edson | Crédito aprobado/rechazado, PayKlik, consulta y mensajes de error |
| Manual Técnico | Andy | Kevin | Requisitos, arquitectura, variables, ejecución, pruebas y operación |
| Reporte de pruebas funcionales | Edson | Jorge | 5 integración + 5 E2E y defectos corregidos |
| Reporte de rendimiento | Andy | Kevin | 5 carga + 5 estrés, métricas, límites y recuperación |
| Pipeline y despliegue | Kevin | Andy | Runs, imágenes, URL, health, secretos, rollback |
| Releases | Kevin | Todo el equipo | `v1`–`v4`, notas y matriz de contribuciones |
| Bitácora/Kanban | Cada integrante actualiza lo suyo; Kevin consolida | Jairo verifica trazabilidad | Fechas, horas, bloqueos, decisiones y capturas |

El feedback real de las revisiones de Fase 1 y 2 debe adjuntarse o transcribirse.
Si no está disponible, se registra como bloqueo; no se inventa retroalimentación.

## 10. Definition of Done

Una tarea está `Done` solamente cuando:

1. El código está en su rama y tiene commits sustanciales del autor.
2. Existe revisión de una persona distinta.
3. Compila desde un checkout limpio.
4. Sus pruebas automatizadas pasan.
5. No introduce secretos ni acceso directo a datos privados de otro módulo.
6. Actualiza contrato, migración, DDA o manual cuando corresponde.
7. La evidencia indica SHA o release.
8. El pipeline conserva el artefacto o reporte.
9. El responsable funcional acepta el criterio de negocio.
10. La tarjeta de Kanban contiene enlace a PR, prueba y documento.

Una release está terminada únicamente si, además:

- `main` coincide con el tag publicado.
- El ambiente objetivo responde a health y smoke test.
- Existe una ruta de rollback documentada.
- No hay defectos P0/P1 abiertos ni pruebas obligatorias fallando.

## 11. Dependencias y política de bloqueos

| Bloqueo | Dueño de resolver | Continuidad permitida | Lo que no debe ocurrir |
|---|---|---|---|
| No hay feedback F1/F2 | Kevin obtiene evidencia; Jairo registra | Avanzar código contra enunciado | Inventar feedback |
| No hay cuenta/crédito de nube | Kevin | Preparar Compose productivo y pipeline hasta deploy | Cambiar de propietario sin ajustar capacidad y cronograma |
| Elegibilidad retrasada | Jorge | Stub detrás de `CreditEligibilityPort` | Consultar sus tablas desde orders |
| Referencia fiscal sigue acoplada a PayKlik | Andy + Jairo | Completar crédito hasta concesión | Crear un intento PayKlik ficticio o facturar sin trazabilidad |
| Integración/E2E inestable | Edson | Aislar caso y conservar release anterior | Desactivar tests para desplegar |
| Ambiente no estable para rendimiento | Kevin | Andy termina scripts/dataset localmente | Crear un segundo ambiente improvisado sin trazabilidad |
| Carga/estrés retrasados | Andy | Kevin conserva ambiente y métricas | Omitir escenarios o sustituir mediciones por estimaciones |
| Pipeline/deploy retrasados | Kevin | Equipo sigue con PR y Compose local | Desplegar manualmente sin gate, evidencia ni rollback |

Todo bloqueo de más de dos horas enfocadas se registra. Si atraviesa un daily,
se reduce alcance cosmético o se hace trabajo en pareja dentro del mismo frente;
el cambio de responsable requiere un ajuste explícito de carga, dependencias y
cronograma aprobado por el equipo.

## 12. Riesgos prioritarios

| Riesgo | Prob. | Impacto | Mitigación | Dueño |
|---|---|---|---|---|
| No contar con proveedor/credenciales de nube | Alta | Crítico | Resolver en Gate 0; usar VM sencilla compatible con Compose | Kevin |
| Cuatro releases en tres días | Alta | Alto | Dos cortes pequeños el 12, gates automáticos y cero cambios cosméticos P0 | Kevin |
| Historia ERP insuficiente o inconsistente | Media | Alto | Dataset determinista y política explícita de “datos insuficientes” | Jorge |
| Crédito bloquea PayKlik | Media | Crítico | Puertos separados, timeout y E2E-03 | Jairo + Edson |
| Doble concesión por reintentos | Media | Crítico | Idempotency key, restricción única e IT/E2E-04 | Jorge + Edson |
| Notificación revierte la orden | Media | Alto | Evento after-commit, Outbox y DLQ | Edson |
| Pruebas de estrés dañan datos o ambiente | Media | Alto | Dataset aislado, límites, ventana y limpieza | Andy |
| Secretos expuestos | Media | Crítico | GitHub Secrets, escaneo y rotación | Kevin + Edson |
| Documentación contradice el código | Alta | Alto | Evidencia por release y revisión cruzada | Jairo |
| El estilo dirigido por eventos ya existía en Fase 2 | Alta | Alto | ADR declara con honestidad la infraestructura previa y demuestra el delta nuevo: decisiones/eventos/proyección/notificación de crédito | Jairo + Edson |

## 13. Checklist final de entrega

- [ ] Feedback real de Fases 1 y 2 registrado y atendido.
- [ ] Crédito elegible visible solo para clientes aprobados.
- [ ] PayKlik continúa funcionando de forma independiente.
- [ ] Facturación usa una referencia financiera válida para PayKlik o crédito, sin intentos ficticios.
- [ ] Decisión explicable, auditable e idempotente.
- [ ] Vencimiento a 30 días persistido.
- [ ] Notificación no bloqueante y recuperable.
- [ ] DDA y diagramas actualizados.
- [ ] Cinco pruebas unitarias verdes.
- [ ] Cinco pruebas de integración verdes.
- [ ] Cinco pruebas E2E verdes.
- [ ] Cinco pruebas de carga ejecutadas y documentadas.
- [ ] Cinco pruebas de estrés ejecutadas y documentadas.
- [ ] Frontend y backend empaquetados con Docker.
- [ ] Pipeline `build` → `test` → `deploy` con gate efectivo.
- [ ] Arquitectura desplegada en nube y accesible.
- [ ] Health check, smoke test y rollback demostrados.
- [ ] Releases `v1`, `v2`, `v3` y `v4` publicados mediante git-flow.
- [ ] Manual Técnico en Markdown.
- [ ] Manual de Usuario en Markdown.
- [ ] Cada integrante tiene commits sustanciales y puede defender su trabajo.
- [ ] URL/repositorio entregados por UEDI.
- [ ] Auxiliar correspondiente agregado al repositorio.
- [ ] Los cinco integrantes tienen preparado el ensayo de calificación.

---

## 14. Gobernanza del reparto

Para conservar una distribución equilibrada, el equipo aplica estas reglas:

1. Cada frente mantiene un responsable principal y un revisor cruzado.
2. Infraestructura de nube, Docker productivo, secretos, pipeline, rollback y
   releases forman el frente de DevOps a cargo de Kevin.
3. Diseño, ejecución y análisis de carga y estrés forman el frente de rendimiento
   a cargo de Andy.
4. La evidencia producida por cada frente se entrega al responsable documental
   para mantener DDA, manuales y trazabilidad, sin duplicar su ejecución.
5. Un bloqueo activa trabajo en pareja dentro del frente o reducción de alcance
   no obligatorio; no provoca reasignaciones automáticas.
6. Cualquier cambio de responsable requiere recalcular horas, dependencias y
   fechas, y actualizar este documento antes de continuar.
