# Responsabilidades por integrante - Proyecto 1, Fase 2

**Sistema:** SGO-Logic  
**Equipo:** Grupo 5  
**Periodo de ejecución:** 10 al 23 de septiembre de 2026  
**Plan general:** [PLANIFICACION_FASE_2.md](./PLANIFICACION_FASE_2.md)  
**Base arquitectónica:** [DDA.md](../ProyectoFase1/Docs/DDA.md)  
**Tablero Kanban:** [MegaStore - Fase 2](https://trello.com/b/uyoJQ4RH/megastore-fase-2)

---

## 1. Propósito

Este documento convierte el backlog general en responsabilidades individuales ejecutables. Para cada integrante establece qué debe producir, cuánto tiempo enfocado debería requerir, de quién depende, a quién bloquea y qué acción debe tomar el equipo si el trabajo no avanza.

Las horas son estimaciones de trabajo enfocado, no horas calendario ni compromisos rígidos. Incluyen diseño, programación, pruebas propias y documentación inmediata. Cada integrante debe registrar sus horas reales en la bitácora para ajustar la carga sin ocultar desviaciones.

## 2. Reglas de ejecución

- Cada tarea tiene un solo dueño responsable de llevarla hasta `Done`.
- El dueño puede recibir apoyo, pero conserva la responsabilidad de integración y documentación.
- Nadie espera a que otro termine una implementación completa si ya existe un contrato congelado que permita usar un simulador o adaptador temporal.
- Un bloqueo se registra en Kanban en cuanto impide avanzar durante dos horas enfocadas.
- Un bloqueo que atraviesa un daily se considera crítico y activa el protocolo de la sección 10.
- No se desbloquea una tarea consultando directamente las tablas privadas de otro módulo.
- Un cambio incompatible de contrato requiere aprobación del arquitecto integrador y actualización del ADR correspondiente.
- P0 y P1 tienen prioridad sobre cualquier refinamiento visual P2.

## 3. Cadena de dependencias principal

```text
Jairo: contratos, entorno y aplicación ejecutable
                   ↓
Jorge: inventario y reserva atómica
                   ↓
Edson: pago, Outbox, auditoría y seguridad
                   ↓
Andy: confirmación fiscal e integraciones legacy
                   ↓
Kevin: despacho, consulta, notificación e interfaz
                   ↓
Jairo + todos: Saga, E2E, pruebas EaC y release
```

La representación anterior muestra la ruta del camino feliz, no una orden para trabajar secuencialmente. Los cinco integrantes comienzan en paralelo usando los contratos acordados el 10 de septiembre.

## 4. Jairo Gómez

**Rol:** Scrum Master, arquitecto integrador y Release Manager  
**Objetivo:** mantener ejecutable la plataforma común y ensamblar el flujo completo sin apropiarse del código interno de los demás módulos.  
**Carga estimada:** 49 a 51 horas enfocadas.

### 4.1 Tareas y tiempos

| ID | Responsabilidad y resultado | Horas | Fecha objetivo | Depende de | Bloquea a |
|---|---|---:|---|---|---|
| GES-01 | Kickoff, disponibilidad, Gate 0, tablero, captura inicial y registro de feedback real | 4 | 10 sep | Todos entregan disponibilidad y observaciones | Todo el equipo |
| BB-01 | Crear carpeta `FASE 2`, bitácora, README inicial y plantillas ADR | 2 | 10 sep | Acceso al repositorio | Todos |
| BB-02 | Redactar ADR-009 de stack y topología ejecutable | 3 | 10 sep | Acuerdo técnico de al menos tres integrantes | Todos |
| BB-05 | Docker Compose, configuración y health checks | 4 | 12 sep | ADR-009 | Todos los módulos y DOC-02 |
| ORD-01 | API de carrito recuperable entre canales | 4 | 14 sep | BB-03 y contratos | ORD-03, UI-01 y QA-04 |
| ORD-02 | Orden y máquina de estados | 4 | 14 sep | Contratos de estado y AUD-01 mínimo | ORD-03, FIS-02 y QRY-01 |
| ORD-03 | Saga del camino feliz | 7 | 15 sep | INV-01, PAY-01, FIS-01 y LOG-01 | ORD-05, QA-04 y demo integrada |
| ORD-04 | Compensaciones de cancelación y reserva | 3 | 17 sep | INV-02 y PAY-02 | QA-05 |
| ORD-05 | Datos reproducibles y suite E2E | 4 | 18 sep | Todas las líneas P0 | QA-02, QA-04 y DEMO-01 |
| QA-01 | Integración continua para compilación y pruebas | 3 | 12 sep | Repositorio y stack | Todos los PR |
| QA-04 | Automatizar el camino feliz completo | 3 | 20 sep | ORD-05 | Release candidate |
| DOC-01 | Consolidar DDA, diagramas y ADR-009 a ADR-013 | 4 | 21 sep | Evidencia de cada dueño | Release candidate |
| DOC-03 | Consolidar bitácora, Kanban y burndown | 2 | 21 sep | Registro diario de todos | Cierre académico |
| REL-01 | Integrar `develop`, `release`, `main` y crear el tag final | 4 | 21 al 23 sep | CI verde y aprobaciones | Entrega final |

### 4.2 Entregas que debe realizar a otros

- Antes de terminar el 10 de septiembre: stack, estructura de módulos, estados, eventos y política de cambios congelados.
- Antes de terminar el 12 de septiembre: ambiente local reproducible y CI mínimo.
- El 15 de septiembre: primera Saga integrada en `develop`.
- El 18 de septiembre: conjunto de datos y comando único para ejecutar el E2E.
- El 21 de septiembre: release candidate y DDA coherente con el código.

### 4.3 Si Jairo se retrasa

- Kevin facilita el daily y mantiene el tablero para que Scrum Master no sea un punto único de falla.
- Andy mantiene temporalmente Docker Compose si BB-05 queda bloqueado.
- Cada dueño continúa contra contratos versionados y simuladores locales.
- Nadie espera un merge central: el revisor asignado puede aprobar PR si CI está verde.
- Si ORD-03 se retrasa más de un daily, se organiza una sesión de integración con Jorge, Edson, Andy y Kevin, manteniendo a Jairo como dueño final de la Saga.

---

## 5. Jorge Pumay

**Rol:** dueño de consistencia operacional  
**Objetivo:** garantizar que el inventario y las reservas no permitan sobreventa y que exista un origen de despacho explicable.  
**Carga estimada:** 45 a 47 horas enfocadas.

### 5.1 Tareas y tiempos

| ID | Responsabilidad y resultado | Horas | Fecha objetivo | Depende de | Bloquea a |
|---|---|---:|---|---|---|
| DEC-03 | Cerrar fuente lógica de inventario y ADR-011 | 3 | 10 sep | Aprobación de Gate 0 | BB-03 e INV-01 |
| BB-03 | Modelo relacional, migración, restricciones e índices iniciales | 6 | 11 sep | Contratos y ADR-011 | Jairo, Edson, Andy y Kevin |
| INV-01 | Stock de dos centros y 32 tiendas | 4 | 13 sep | BB-03 | ORD-03 e INV-04 |
| INV-02 | Reserva atómica e idempotente | 7 | 15 sep | INV-01 | ORD-04 e INV-05 |
| INV-03 | Expiración configurable y liberación automática | 4 | 17 sep | INV-02 y Outbox | Pruebas de recuperación y auditoría |
| INV-04 | Selección explicable de origen de despacho | 5 | 15 sep | INV-01 | LOG-01 y camino feliz |
| INV-05 | Pruebas de concurrencia y consistencia | 5 | 19 sep | INV-02 e INV-03 | Cierre de P1 y QA-02 |
| REV-ANDY | Revisar migraciones y PR de Facturación/Legacy | 3 | Durante la fase | Entregas de Andy | Calidad de FIS, ERP y POS |
| EAC-09 | Ejecutar despliegue cruzado usando únicamente README | 4 | 20 sep | BB-05 y DOC-02 | Evidencia de mantenibilidad |
| DOC-JORGE | Documentar datos, invariantes, pruebas y limitaciones | 2 | Con cada PR | Sus propias tareas | DOC-01 y DOC-02 |
| CER-JORGE | Daily, revisiones e integración | 3 | Diario | Disponibilidad del equipo | Seguimiento del equipo |

### 5.2 Entregas que debe realizar a otros

- El 10 de septiembre: decisión de fuente lógica de inventario.
- El 11 de septiembre: migración inicial revisada.
- El 13 de septiembre: consulta de stock utilizable mediante puerto.
- El 15 de septiembre: reserva y selección de origen integrables.
- El 19 de septiembre: evidencia de que no existe stock negativo, doble compromiso ni doble liberación.

### 5.3 Si Jorge se retrasa

- El equipo usa primero la interfaz de inventario congelada, nunca acceso directo a su esquema.
- Jairo puede usar un `InventoryPort` simulado para continuar ORD-01 y ORD-02.
- Kevin puede usar un origen de despacho fijo y claramente marcado como simulador hasta recibir INV-04.
- Edson apoya únicamente la transacción y prueba concurrente de INV-02, sin asumir todo el módulo.
- Si BB-03 no está listo el 11 de septiembre, se reduce la migración a las tablas mínimas del camino feliz y las tablas secundarias pasan a P1.

---

## 6. Edson Sosa

**Rol:** Security & Quality Champion  
**Objetivo:** preservar la intención de pago, evitar duplicados y proporcionar evidencia auditable sin almacenar datos sensibles.  
**Carga estimada:** 49 a 51 horas enfocadas.

### 6.1 Tareas y tiempos

| ID | Responsabilidad y resultado | Horas | Fecha objetivo | Depende de | Bloquea a |
|---|---|---:|---|---|---|
| DEC-02 | Definir protocolo de línea base `T` y prueba `8T` | 2 | 10 sep | Ambiente objetivo acordado | QA-02 |
| DEC-06 | Definir autenticación, roles y ADR-013 | 3 | 10 sep | Stack y revisión de Andy | BB-07 y todos los endpoints |
| BB-04 | Outbox transaccional, dispatcher, reintento y DLQ | 7 | 12 sep | BB-03 | Pagos, SAT, ERP, POS, LogiExpress y notificaciones |
| BB-07 | Autenticación, autorización y sanitización | 4 | 12 sep | ADR-009 y ADR-013 | PAY-01, API y SEC-01 |
| PAY-01 | Puerto PayKlik e intentos de pago | 4 | 14 sep | BB-06 y BB-07 | ORD-03 y PAY-02 |
| PAY-02 | Timeout, circuit breaker y pago indeterminado | 6 | 16 sep | BB-04 y PAY-01 | ORD-04, PAY-03 y QA-05 |
| PAY-03 | Conciliación e idempotencia del cobro | 4 | 17 sep | PAY-02 | Cierre de EaC-04 |
| AUD-01 | Registro transversal de actor, instante y cambio | 5 | 15 sep | BB-03 y correlación | ORD-02, QRY-01 y todos los módulos |
| AUD-02 | Consulta indexada de evidencia | 3 | 18 sep | AUD-01 y datos de prueba | EaC-02 y demo |
| SEC-01 | Pruebas 401/403 y fuga de PAN/CVV | 3 | 19 sep | BB-07 y PAY-01 | EaC-12 y release |
| QA-02 | Pruebas de carga `T` y `8T` | 5 | 20 sep | ORD-05, QRY-01 y ambiente estable | Cierre de EaC-01 a EaC-03 |
| REV-JORGE | Revisar esquema e invariantes de Inventario | 2 | Durante la fase | PR de Jorge | BB-03 e INV-02 |
| DOC-EDSON | Documentación, daily e integración | 3 | Diario | Sus propios resultados | DOC-01 y matriz EaC |

### 6.2 Entregas que debe realizar a otros

- El 10 de septiembre: roles y protocolo de carga acordados.
- El 12 de septiembre: Outbox reutilizable y seguridad mínima.
- El 14 de septiembre: contrato de pago exitoso disponible.
- El 17 de septiembre: falla y recuperación de PayKlik demostrables.
- El 20 de septiembre: reporte de carga, auditoría y seguridad reproducible.

### 6.3 Si Edson se retrasa

- Andy y Kevin continúan con productores y consumidores falsos que respeten el esquema de evento congelado.
- Jairo puede usar un `PaymentPort` simulado exitoso para integrar la primera Saga, pero la rebanada no pasa a P1 sin PAY-02 y PAY-03.
- Andy apoya BB-04 porque sus integraciones son las principales consumidoras del mecanismo.
- Las pruebas de carga no comienzan sobre un ambiente inestable; Kevin prepara datos y consultas mientras Edson estabiliza la plataforma.
- Nunca se reemplaza el Outbox por una publicación directa solo para acelerar la demo.

---

## 7. Andy Sánchez

**Rol:** dueño de integración fiscal y legacy  
**Objetivo:** confirmar fiscalmente la orden y entregar información a SAT, ERP y POS sin modificar ni fingir los sistemas reales.  
**Carga estimada:** 46 a 48 horas enfocadas.

### 7.1 Tareas y tiempos

| ID | Responsabilidad y resultado | Horas | Fecha objetivo | Depende de | Bloquea a |
|---|---|---:|---|---|---|
| DEC-01 | Resolver transporte asíncrono a SAT con confirmación condicionante y ADR-010 | 3 | 10 sep | Revisión de Jairo | FIS-02 y estados de orden |
| DEC-04 | Contratos simulados y ADR-012 | 3 | 10 sep | Dueños de cada puerto | BB-06 y todas las integraciones |
| BB-06 | Simuladores de PayKlik, SAT, ERP, POS, LogiExpress y notificaciones | 5 | 12 sep | ADR-012 | Edson, Andy, Kevin y QA-05 |
| FIS-01 | XML fiscal, CUI y serie simulados | 4 | 14 sep | BB-03 | ORD-03 y FIS-02 |
| FIS-02 | Solicitud SAT asíncrona y estado `FISCAL_PENDING` | 6 | 15 sep | BB-04, BB-06 y FIS-01 | ORD-03, LOG-01 y confirmación de orden |
| ERP-01 | Consolidación CSV/XML con intervalo configurable | 4 | 17 sep | Órdenes confirmadas | ERP-02 y demo |
| ERP-02 | Entrega, reintento e idempotencia del lote | 3 | 18 sep | BB-04 y ERP-01 | Cierre de integración ERP |
| POS-01 | Emisión lógica a 32 POS, acuse, reintento y deduplicación | 6 | 18 sep | BB-04 y BB-06 | EaC-06 y demo |
| FIS-03 | Pruebas de XML, SAT, ERP y POS | 4 | 19 sep | FIS-01, FIS-02, ERP y POS | Matriz EaC y release |
| DOC-02 | README completo de instalación y despliegue | 4 | 21 sep | Aportes de todos | EAC-09 y entrega |
| REV-EDSON | Revisar seguridad, pagos y sanitización | 2 | Durante la fase | PR de Edson | PAY y SEC |
| DOC-ANDY | Documentación, daily e integración | 3 | Diario | Sus propios resultados | DOC-01 y DOC-02 |

### 7.2 Entregas que debe realizar a otros

- El 10 de septiembre: contratos de terceros y decisión SAT.
- El 12 de septiembre: simuladores configurables para éxito, rechazo, latencia, timeout y caída.
- El 15 de septiembre: confirmación fiscal integrable.
- El 18 de septiembre: lote ERP y 32 POS simulados.
- El 21 de septiembre: README probado por una persona no autora.

### 7.3 Si Andy se retrasa

- Edson usa simuladores mínimos propios para PayKlik mientras se completa BB-06.
- Jairo integra FIS-01 mediante un `FiscalPort` falso, pero una orden no puede marcarse `CONFIRMED` sin respuesta aprobada.
- Kevin mantiene LOG-01 contra una orden confirmada de prueba hasta recibir FIS-02.
- Jorge revisa y apoya los problemas de persistencia fiscal o de lotes.
- Si POS o ERP se retrasan, se reduce su interfaz visual, no la entrega confiable, idempotencia ni evidencia requerida.

---

## 8. Kevin Girón

**Rol:** Product Owner operativo, Developer y QA Coordinator  
**Objetivo:** materializar la experiencia visible del MVP y coordinar que cada escenario de calidad tenga evidencia verificable.  
**Carga estimada:** 49 a 51 horas enfocadas.

### 8.1 Tareas y tiempos

| ID | Responsabilidad y resultado | Horas | Fecha objetivo | Depende de | Bloquea a |
|---|---|---:|---|---|---|
| DEC-05/07 | Matriz EaC sin requisitos inventados y registro del feedback real | 2 | 10 sep | Información del equipo | Gate 0 y backlog final |
| BB-08 | Logs, métricas y correlación E2E | 4 | 12 sep | IDs congelados | QRY-01, QA-03 y diagnóstico |
| LOG-01 | Crear despacho desde el origen asignado | 4 | 15 sep | INV-04 y FIS-02 | ORD-03 y LOG-02 |
| LOG-02 | Guía y tracking mediante LogiExpress simulado | 4 | 16 sep | BB-06 y LOG-01 | LOG-03, QRY-01 y demo |
| LOG-03 | Reintentos 2/8/32 segundos y DLQ | 4 | 17 sep | BB-04 y LOG-02 | EaC-05 y QA-05 |
| NOT-01 | Notificaciones no bloqueantes | 4 | 17 sep | BB-04 y simulador | EaC-08 y demo |
| QRY-01 | Consulta consolidada menor o igual a cinco segundos | 5 | 18 sep | Eventos de todas las líneas | UI-01, QA-02 y QA-04 |
| INC-01 | Incidencia o devolución vinculada a la venta | 4 | 18 sep | QRY-01 y AUD-01 | UI-01 y demo funcional |
| UI-01 | Interfaz mínima para checkout, consulta y devolución | 4 | 19 sep | ORD-01, QRY-01 e INC-01 | DEMO-01 |
| QA-03 | Matriz EaC, prueba, resultado y evidencia | 4 | 20 sep | Resultados de todos | DOC-01 y release |
| QA-05 | Pruebas coordinadas de fallas externas | 4 | 20 sep | PAY-02, FIS-02, LOG-03 y NOT-01 | Release candidate |
| DEMO-01 | Guion, datos, contingencia y dos ensayos | 4 | 22 sep | QA-04, QA-05 y UI-01 | Entrega final |
| PO-QA | Priorización, aceptación, daily y coordinación QA | 3 | Diario | Evidencia del equipo | Flujo Kanban y cierre |

### 8.2 Entregas que debe realizar a otros

- El 10 de septiembre: prioridades P0/P1 y feedback real trazado.
- El 12 de septiembre: formato común de correlación y evidencia.
- El 17 de septiembre: despacho, tracking y notificación integrables.
- El 20 de septiembre: matriz completa de los 12 EaC y pruebas de fallas.
- El 22 de septiembre: demo ensayada y plan de contingencia.

### 8.3 Si Kevin se retrasa

- Jairo asume temporalmente la priorización del backlog, pero no acepta sus propias tareas sin revisor.
- El equipo usa Swagger o una colección HTTP como interfaz de contingencia; nunca se sacrifica el flujo API o la evidencia por terminar estilos visuales.
- Jairo puede integrar un `ShippingPort` temporal para la Saga hasta recibir LOG-01.
- Edson ayuda a estructurar resultados de QA y correlación, sin asumir la interfaz.
- Si QA-03 no está actualizada diariamente, ninguna tarjeta que afirme cumplir un EaC pasa a `Done`.

---

## 9. Matriz de bloqueo entre integrantes

| Si se retrasa | Elemento crítico | Personas afectadas | Continuidad temporal permitida | Límite |
|---|---|---|---|---|
| Jairo | Stack, Compose, estados o Saga | Todos | Contratos versionados, ejecución local parcial y simuladores | Un daily |
| Jorge | Migración, stock, reserva u origen | Jairo, Edson, Andy y Kevin | Repositorio falso detrás de `InventoryPort` y datos semilla mínimos | No se acepta como evidencia final |
| Edson | Outbox, pago, auditoría o seguridad | Todos | Broker o `PaymentPort` simulado conforme al contrato | No publicar directo ni omitir seguridad |
| Andy | Simuladores, SAT, ERP o POS | Jairo, Edson y Kevin | `FiscalPort` y terceros falsos con respuestas versionadas | No confirmar orden sin aprobación fiscal |
| Kevin | Despacho, consulta, QA o UI | Jairo y cierre del equipo | `ShippingPort` simulado y demo mediante API | UI puede reducirse, QA no |

## 10. Protocolo cuando una tarea bloquea al equipo

### Nivel 1 - Bloqueo menor, menos de dos horas

1. El dueño documenta error, entrada, salida esperada y último cambio conocido.
2. Revisa contrato, logs, health checks y prueba mínima reproducible.
3. Solicita apoyo a su revisor principal sin reasignar todavía la tarea.

### Nivel 2 - Bloqueo registrado, entre dos horas y un daily

1. Mover la tarjeta a `Blocked`.
2. Registrar causa, impacto, persona que puede desbloquear y siguiente revisión.
3. Crear un simulador detrás del puerto acordado para que los consumidores continúen.
4. El dueño y su revisor realizan una sesión de hasta 60 minutos.
5. El Scrum Master reorganiza el WIP y evita que alguien inicie trabajo P2.

### Nivel 3 - Bloqueo crítico, atraviesa un daily

1. El arquitecto integrador convoca a los dueños de productor y consumidor.
2. Se decide entre corregir, reducir alcance interno, revertir el último cambio o versionar el contrato.
3. Si cambia una decisión arquitectónica, se actualiza el ADR antes del merge.
4. Se reasigna únicamente trabajo no iniciado o una subtarea claramente delimitada.
5. El Product Owner ajusta prioridad y registra el impacto en fecha y entregable.

### Nivel 4 - Riesgo para la entrega, más de 24 horas

1. Congelar P2 y cualquier mejora estética.
2. Conservar el flujo P0 mediante puertos y simuladores.
3. Formar pareja temporal entre dueño y revisor hasta recuperar la ruta crítica.
4. Mantener obligatoriamente seguridad, trazabilidad, idempotencia y confirmación fiscal.
5. Documentar con honestidad cualquier EaC no alcanzado y su evidencia parcial.

## 11. Revisión diaria de responsabilidades

Durante el daily, cada integrante debe informar:

1. Tarea e ID que terminó o avanzó.
2. Horas reales consumidas frente a la estimación.
3. Evidencia funcional o numérica obtenida.
4. Dependencia que necesita durante las siguientes 24 horas.
5. Persona que quedará bloqueada si no termina su entrega.
6. Acción temporal acordada para evitar la espera.

El Scrum Master actualiza diariamente esta tabla de control:

| Integrante | Tarea activa | Horas estimadas restantes | Entrega próxima | Bloquea a | Estado/acción |
|---|---|---:|---|---|---|
| Jairo |  |  |  |  |  |
| Jorge |  |  |  |  |  |
| Edson |  |  |  |  |  |
| Andy |  |  |  |  |  |
| Kevin |  |  |  |  |  |

## 12. Criterio de cierre individual

Un integrante concluye su responsabilidad cuando:

- todas sus tarjetas P0 y P1 están en `Done` o tienen una limitación formalmente aceptada;
- sus pruebas automatizadas y negativas están ejecutadas;
- entregó contratos, migraciones y datos necesarios a sus consumidores;
- otra persona revisó su código;
- su trabajo corre dentro de Docker Compose;
- actualizó README, ADR, DDA y evidencia cuando corresponde;
- registró su retrospectiva individual;
- puede explicar cómo sus decisiones implementan los CUN, EaC, restricciones y ADR asignados.

El cierre individual no ocurre si el código funciona únicamente en la computadora del autor o si otro integrante todavía depende de una entrega no documentada.
