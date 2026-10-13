# Planificación de ejecución - Proyecto 1, Fase 2

**Sistema:** SGO-Logic - Sistema de Gestión de Órdenes y Logística Omnicanal  
**Equipo:** Grupo 5 - Jairo Gómez, Jorge Pumay, Edson Sosa, Andy Sánchez y Kevin Girón  
**Ventana de trabajo:** 10 al 23 de septiembre de 2026  
**Base arquitectónica:** `ProyectoFase1/Docs/DDA.md`  
**Enunciado:** `ProyectoFase2/Proyecto Fase 2_convertido.md`  
**Responsabilidades individuales:** [RESPONSABILIDADES_INTEGRANTES_FASE_2.md](./RESPONSABILIDADES_INTEGRANTES_FASE_2.md)
**Tablero Kanban:** [MegaStore - Fase 2](https://trello.com/b/uyoJQ4RH/megastore-fase-2)

---

## Evidencia de Sprint Planning y cierre de J6

**Responsable:** Jairo Gómez<br>
**Verificación:** 10 de octubre de 2026

El Sprint Backlog inicial está contenido en la sección 5 y asigna identificador,
descripción, dueño, dependencias y criterio de aceptación a cada tarjeta. La
captura siguiente corresponde al inicio observable del sprint: 54 tarjetas en
`To Do` y las otras cinco columnas vacías.

![Kanban al inicio del sprint](<../../FASE 2/docs/evidence/bitacora/2026-09-14_14-02.png>)

*Captura inicial del Kanban, registrada por Jairo Gómez el 14 de septiembre de
2026. Su propósito es demostrar el punto de partida; no prueba aceptación
técnica de las tarjetas.*

### Feedback de Fase 1

Se revisaron `Docs/`, `FASE 2/docs/`, la bitácora y la tarjeta DEC-07. No se
encontró una captura, rúbrica comentada, mensaje ni transcripción verificable
con observaciones del docente. Por esa razón no se crean IDs `OBS-F1-XX` ni se
atribuyen soluciones a comentarios inexistentes. DEC-07 queda documentada como
limitación de evidencia de entrada, con la siguiente trazabilidad:

| Fuente revisada | Resultado | Tratamiento |
|---|---|---|
| DDA y documentación de Fase 1 | No contiene retroalimentación del docente | Se conserva como línea base arquitectónica, no como feedback |
| `BITACORA_FASE_2.md` | Registra que la evidencia no estaba disponible | Se mantiene la limitación de forma explícita |
| Tarjeta Trello DEC-07 | Define el criterio, pero no adjunta observaciones reales | No se inventan observaciones ni tarjetas derivadas |

J6 se considera documentalmente cerrado porque el backlog y la captura inicial
son verificables y la ausencia de feedback fue registrada con honestidad. Si el
equipo recupera la fuente original, deberá sustituir esta limitación por la
matriz `OBS-F1-XX → solución → tarjeta → evidencia`.

---

## 1. Decisión de planificación

La Fase 2 se construirá como un **monolito modular**, tal como establece ADR-001, con una capa de integración basada en puertos/adaptadores, Outbox, broker y trabajadores asíncronos. El trabajo se divide en cinco líneas verticales de negocio. Cada línea incluye código, persistencia, adaptadores simulados, pruebas y documentación; ningún integrante recibe únicamente documentación o únicamente interfaz.

El MVP no intentará construir versiones reales de PayKlik, SAT, ERP-Core, POS, LogiExpress o del proveedor de notificaciones. Esos sistemas permanecerán fuera de la frontera de SGO-Logic y se representarán mediante simuladores con fallas configurables. Esto permite demostrar los EaC sin violar R1 ni inventar contratos productivos.

### 1.1 Arquitectura mínima de ejecución

- Un artefacto desplegable de SGO-Logic, organizado por módulos de negocio y sin acceso directo a las tablas internas de otro módulo.
- API omnicanal y una interfaz web mínima para ejecutar y observar el camino feliz, consultar órdenes y registrar incidencias/devoluciones.
- Base operacional relacional con migraciones, restricciones e índices.
- Tabla Outbox transaccional y broker para pagos pendientes, SAT, POS, LogiExpress, ERP y notificaciones.
- Trabajadores idempotentes, reintentos, backoff y bandeja de mensajes fallidos.
- Registro de auditoría inmutable desde la aplicación, correlacionado por orden, intento e integración.
- Contenedores para aplicación, persistencia, broker, entrada/balanceo y simuladores externos.
- Configuración externalizada; ninguna credencial dentro del repositorio.

La selección exacta de Java o .NET, framework, base de datos y broker debe cerrarse el 10 de septiembre mediante ADR-009. Una combinación coherente con R5 sería Java/Spring Boot, PostgreSQL y RabbitMQ, pero no se considera aceptada hasta que el equipo la registre.

### 1.2 Derivación desde el DDA

La división del trabajo no surge de preferencias personales. Cada línea implementa capacidades, decisiones y escenarios ya definidos en el DDA de la Fase 1.

| Línea de implementación | CUN principales | ADR de Fase 1 | EaC principales | Resultado integrado |
|---|---|---|---|---|
| Plataforma, Carrito y Órdenes | CUN-01 | ADR-001, ADR-005 | EaC-03, EaC-09, EaC-11 | Carrito recuperable, orden persistida y Saga coordinadora |
| Inventario y Asignación | CUN-02, CUN-07 | ADR-001, ADR-005 | EaC-03, EaC-07, EaC-09 | Stock lógico unificado, reserva atómica y origen de despacho |
| Pagos, Auditoría y Seguridad | CUN-03, CUN-12 | ADR-002, ADR-003, ADR-006 | EaC-02, EaC-04, EaC-07, EaC-12 | Pago resiliente, evidencia consultable y datos protegidos |
| Facturación e Integración Legacy | CUN-04, CUN-05, CUN-06 | ADR-002, ADR-004, ADR-007 | EaC-02, EaC-06, EaC-07 | Confirmación fiscal, lote ERP y entrega confiable a POS |
| Logística y Experiencia | CUN-07 a CUN-11 | ADR-004, ADR-005, ADR-006, ADR-008 | EaC-01, EaC-05, EaC-08 | Despacho, tracking, consulta, devolución y notificación |

---

## 2. Ajustes que deben resolverse antes de programar

| ID | Hallazgo | Resolución propuesta | Responsable | Evidencia requerida |
|---|---|---|---|---|
| DEC-01 | El DDA exige confirmación fiscal antes de finalizar; Fase 2 pide envío asíncrono a SAT. | Persistir la solicitud fiscal y publicarla asíncronamente, pero mantener la orden en `FISCAL_PENDING` hasta recibir aprobación. Así el hilo HTTP no se congela y tampoco se declara finalizada la orden. | Andy, con revisión de Jairo | ADR-010 y prueba de timeout SAT |
| DEC-02 | No existe línea base `T` para demostrar 8x. | Ejecutar una prueba controlada inicial, documentar `T` como tasa estable en el mismo ambiente y luego probar `8T`. No afirmar tráfico productivo. | Edson, con apoyo de Jairo | Protocolo de prueba con ambiente, datos, p50/p95/p99, errores y recursos |
| DEC-03 | La fuente física autoritativa de inventario sigue abierta. | Para el MVP, el módulo central de Inventario será la fuente lógica autoritativa y cargará stock simulado de 34 ubicaciones. | Jorge | ADR-011, migración y datos semilla |
| DEC-04 | No se conocen contratos reales de POS, ERP y terceros. | Definir contratos de simulación versionados; mantener puertos reemplazables y no presentar esos contratos como productivos. | Andy, con los dueños de cada puerto | OpenAPI, esquemas de eventos y ADR-012 |
| DEC-05 | La disponibilidad de 99.5 % aparece solo como ejemplo del enunciado. | No convertirla en requisito. Validar únicamente las medidas explícitas de EaC-01 a EaC-12. | Kevin | Matriz de pruebas sin requisito inventado |
| DEC-06 | El DDA no fija autenticación concreta, pero exige solicitantes autorizados y protección de datos. | Implementar autenticación mínima y roles `CLIENT`, `SUPPORT`, `LOGISTICS`, `FINANCE`, con autorización de endpoints y sanitización de logs. | Edson | ADR-013 y pruebas 401/403 |
| DEC-07 | Los archivos disponibles no contienen un registro verificable del feedback recibido en la Fase 1. | Transcribir únicamente las observaciones reales del docente, asignarles IDs `OBS-F1-XX` y enlazarlas con una tarjeta o una justificación de no aplicación. No inventar retroalimentación. | Jairo y Kevin | Tabla de observaciones en la bitácora y tarjetas trazadas |

Estos siete puntos forman el **Gate 0** y deben cerrarse el 10 de septiembre. ADR-009 registra el stack y la topología ejecutable; ADR-010 a ADR-013 registran las decisiones DEC-01, DEC-03, DEC-04 y DEC-06. DEC-02 se cierra mediante un protocolo de medición, DEC-05 mediante la matriz de trazabilidad y DEC-07 mediante evidencia del feedback real. Ninguna rama funcional debe diseñar contratos incompatibles antes de cerrar este gate.

---

## 3. Roles y reparto para cinco integrantes

El rol ágil no sustituye el trabajo técnico. Jairo facilita el flujo y conserva autoridad de integración; cada dueño de línea implementa y prueba su porción. Todo cambio crítico requiere revisión de otro integrante.

| Integrante | Rol de equipo | Propiedad técnica principal | Revisor principal |
|---|---|---|---|
| **Jairo Gómez** | Scrum Master, arquitecto integrador y Release Manager | Plataforma base, API, Carrito/Órdenes, Saga, flujo E2E, Docker Compose y ensamblado final | Kevin |
| **Jorge Pumay** | Developer, dueño de consistencia operacional | Inventario/Reservas, expiración automática y asignación de origen de despacho | Edson |
| **Edson Sosa** | Developer, Security & Quality Champion | Pagos resilientes, PayKlik simulado, auditoría, seguridad y pruebas de carga/seguridad | Andy |
| **Andy Sánchez** | Developer, dueño de integración fiscal/legacy | Facturación, SAT simulado, sincronización ERP y publicación a POS | Jorge |
| **Kevin Girón** | Product Owner operativo, Developer y QA Coordinator | Despacho/LogiExpress, Notificaciones, Consulta/Incidencias y UI mínima de demostración | Jairo |

### 3.1 Responsabilidades compartidas

- **Product Owner:** Kevin prioriza por valor y acepta el comportamiento funcional con base en el enunciado; no cambia drivers unilateralmente.
- **Scrum Master:** Jairo mantiene tablero, WIP, bloqueos, bitácora y ceremonias; no aprueba solo sus propias entregas.
- **Arquitectura:** Jairo coordina; las decisiones de contrato, datos y EaC se aprueban por al menos tres integrantes y se registran como ADR.
- **QA:** Kevin coordina el plan; cada dueño escribe las pruebas de su módulo y un revisor distinto aporta o ejecuta pruebas negativas.
- **Documentación:** cada dueño actualiza su ADR, API y sección del README en la misma rama que el código. Jairo consolida, pero no redacta todo al final.

### 3.2 Límites de propiedad

| Línea | Incluye | No incluye |
|---|---|---|
| Órdenes/Plataforma | Carrito unificado, creación de orden, estados, Saga, composición del camino feliz | Modificar tablas internas de pagos, inventario o facturación |
| Inventario/Asignación | Stock por 34 ubicaciones, reserva atómica, expiración y elección de origen | Crear guía o actualizar tracking |
| Pagos/Auditoría | Intentos, idempotencia, timeout/circuit breaker, conciliación, evidencia e índices | Guardar PAN completo/CVV o finalizar fiscalmente la orden |
| Fiscal/Legacy | XML, CUI/serie, `FISCAL_PENDING`, lotes ERP y eventos POS | Implementar AS/400/POS reales |
| Logística/Experiencia | Despacho, guía/tracking, avisos, consulta, incidencia/devolución e interfaz mínima | Acceso directo a datos privados de otros módulos |

### 3.3 Balance de carga estimado

La suma aproximada de backbone y línea técnica es: Jairo 36 puntos, Jorge 33, Edson 42, Andy 37 y Kevin 40. La diferencia es intencional: Jairo absorbe facilitación, merges y release; Kevin coordinación de QA/demo; Edson concentra los riesgos técnicos más altos y debe recibir apoyo de Jairo si BB-04 o PAY-02 exceden su estimación. En el checkpoint del 15 de septiembre se rebalancearán tareas pendientes, nunca código ya iniciado sin una transferencia explícita.

### 3.4 Capacidad y reserva

Las 50 horas indicadas en el enunciado son una estimación académica y no se convierten automáticamente en puntos. En el kickoff cada integrante registra su disponibilidad real hasta el 23 de septiembre. La capacidad individual se distribuye como referencia en 65 % construcción, 20 % integración y pruebas, y 15 % documentación, revisión y ceremonias.

El equipo mantiene una reserva mínima del 20 % de su capacidad para defectos e integración. Si una persona pierde disponibilidad, se reasignan primero tarjetas no iniciadas de P1; no se divide una misma modificación entre varias personas sin acordar previamente el contrato y el responsable final.

---

## 4. Contratos que se congelan primero

El equipo debe acordar y versionar estos contratos el 10 de septiembre. A partir de ahí, un cambio incompatible requiere ADR o nueva versión.

1. IDs y correlación: `customerId`, `cartId`, `orderId`, `paymentAttemptId`, `reservationId`, `invoiceId`, `shipmentId`, `eventId` e `idempotencyKey`.
2. Estados de orden: `CART`, `RESERVED`, `PAYMENT_PENDING`, `PAID`, `FISCAL_PENDING`, `CONFIRMED`, `PREPARING`, `DISPATCHED`, `DELIVERED`, `CANCELLED`, `RETURNED`.
3. Eventos: `OrderCreated`, `InventoryReserved`, `PaymentRequested`, `PaymentApproved`, `PaymentIndeterminate`, `InvoiceRequested`, `InvoiceApproved`, `OrderConfirmed`, `ShipmentRequested`, `ShipmentUpdated`, `NotificationRequested`, `ReturnRegistered`.
4. Errores comunes: validación, conflicto de stock, transición inválida, no autorizado, dependencia no disponible e idempotencia duplicada.
5. Responsabilidad de datos: solo el módulo propietario escribe sus entidades; otros módulos usan puertos o eventos.
6. Semántica de entrega: al menos una vez, consumidor idempotente y mensaje fallido tras agotar la política.

### 4.1 Estructura mínima del repositorio

El repositorio de la Fase 1 se conserva y se agrega la carpeta exigida por el enunciado. Los nombres internos pueden adaptarse al stack, pero las responsabilidades deben permanecer visibles.

```text
FASE 2/
├── src/                 Código organizado por módulos de negocio
├── tests/               Pruebas unitarias, integración, E2E, carga y seguridad
├── simulators/          PayKlik, SAT, ERP, POS, LogiExpress y notificaciones
├── docs/
│   ├── adr/             ADR-009 a ADR-013
│   ├── evidence/        Resultados de pruebas y capturas
│   └── contracts/       OpenAPI, eventos y esquemas versionados
├── docker-compose.yml
├── .env.example
└── README.md
BITACORA_FASE_2.md        Evidencia diaria ubicada en la raíz del repositorio
```

No se versionan secretos, archivos `.env` reales, datos sensibles, binarios generados ni dependencias descargadas.

---

## 5. Sprint Backlog inicial

Escala de esfuerzo: 1, 2, 3, 5 y 8 puntos. Los puntos permiten comparar complejidad dentro de este equipo; no representan horas.

### 5.0 Prioridad y línea de corte

La construcción se ejecuta en este orden para evitar cinco módulos aislados al final de la fase:

| Prioridad | Contenido | Fecha objetivo | Regla |
|---|---|---|---|
| P0 - Rebanada vertical | Gate 0, contratos, Compose, persistencia, Outbox mínimo y camino feliz desde carrito hasta consulta | 15 sep | Nada visual desplaza una dependencia de P0 |
| P1 - Evidencia arquitectónica | Fallas externas, concurrencia, idempotencia, auditoría, seguridad, ERP, POS y los doce EaC | 20 sep | La fase no se declara terminada sin evidencia o limitación documentada |
| P2 - Refinamiento | Estética adicional, paneles avanzados, endpoints auxiliares, optimizaciones no medidas o integraciones reales | Solo si P0 y P1 están verdes | Se elimina primero ante atraso y no forma parte del compromiso mínimo |

El alcance comprometido termina en P1. P2 no debe entrar a `In Progress` mientras exista una tarjeta P0 bloqueada o una prueba P1 crítica sin dueño.

### 5.1 Backbone compartido

| ID | Tarjeta | Dueño | Puntos | Dependencias | Criterio de aceptación |
|---|---|---:|---:|---|---|
| BB-01 | Crear estructura `FASE 2`, README, bitácora y plantillas ADR | Jairo | 2 | Ninguna | Estructura de §4.1 versionada, repositorio privado y accesos académicos verificados. |
| BB-02 | Registrar ADR-009 de stack y arquitectura ejecutable | Jairo | 3 | Gate 0 | Stack, alternativas, consecuencias y relación con ADR-001. |
| BB-03 | Modelo relacional y migración inicial | Jorge | 5 | Contratos | Claves, restricciones, propietarios e índices revisados por Edson. |
| BB-04 | Outbox, dispatcher, retry y DLQ reutilizables | Edson | 8 | BB-03 | Cambio de negocio + evento atómicos; reenvío no duplica efectos. |
| BB-05 | Docker Compose y configuración por ambiente | Jairo | 5 | BB-02 | Arranque desde cero con un comando documentado y health checks. |
| BB-06 | Simuladores configurables de terceros | Andy | 5 | Contratos | Éxito, rechazo, latencia, timeout y caída reproducibles. |
| BB-07 | Autenticación, roles y sanitización de logs | Edson | 5 | BB-02 | Casos 401/403 y escaneo sin PAN/CVV. |
| BB-08 | Observabilidad y correlación | Kevin | 5 | IDs congelados | Logs estructurados y métricas permiten seguir una orden E2E. |

### 5.2 Línea 1 - Jairo: Plataforma, Carrito, Órdenes y Saga

| ID | Tarjeta | Puntos | Dependencias | Criterio de aceptación |
|---|---|---:|---|---|
| ORD-01 | API de carrito unificado por cliente/canal | 5 | BB-03 | El mismo carrito se recupera desde dos canales simulados. |
| ORD-02 | Crear orden y máquina de estados | 5 | Contratos | Transiciones válidas; las inválidas retornan conflicto y se auditan. |
| ORD-03 | Orquestador Saga del camino feliz | 8 | INV-01, PAY-01, FIS-01, LOG-01 | Coordina reserva → pago → fiscal → despacho sin escribir datos ajenos. |
| ORD-04 | Compensaciones de reserva/cancelación | 3 | INV-02, PAY-02 | Fallo definitivo libera stock una sola vez y conserva evidencia. |
| ORD-05 | Suite E2E y datos de demo | 5 | Todas las líneas | Camino feliz completo repetible desde ambiente limpio. |

### 5.3 Línea 2 - Jorge: Inventario, Reservas y Asignación

| ID | Tarjeta | Puntos | Dependencias | Criterio de aceptación |
|---|---|---:|---|---|
| INV-01 | Inventario central de 2 CD + 32 tiendas | 5 | BB-03 | Consulta por producto/ubicación y stock disponible/reservado. |
| INV-02 | Reserva atómica e idempotente | 8 | INV-01 | Dos compras por la última unidad producen exactamente una reserva. |
| INV-03 | Expiración configurable y liberación | 5 | INV-02 | Reserva vencida se libera automáticamente y el evento queda auditado. |
| INV-04 | Algoritmo explicable de origen de despacho | 5 | INV-01 | Elige una ubicación con stock; criterio y limitaciones documentados. |
| INV-05 | Pruebas concurrentes y de consistencia | 5 | INV-02/03 | Sin stock negativo, doble compromiso ni doble liberación. |

### 5.4 Línea 3 - Edson: Pagos, Auditoría y Seguridad

| ID | Tarjeta | Puntos | Dependencias | Criterio de aceptación |
|---|---|---:|---|---|
| PAY-01 | Puerto/adaptador REST PayKlik e intentos de pago | 5 | BB-06/07 | Registra orden, canal, fecha, referencia y resultado sin PAN/CVV. |
| PAY-02 | Timeout, circuit breaker y estado indeterminado | 8 | BB-04, PAY-01 | A >5 s se preservan orden e intento y se evita falla en cascada. |
| PAY-03 | Conciliación/reintento idempotente ≤60 s | 5 | PAY-02 | El mismo intento no genera doble cobro y el primer trabajo se programa ≤60 s. |
| AUD-01 | Registro auditable transversal | 5 | BB-03 | Conserva actor, instante, cambio, resultado y correlación. |
| AUD-02 | Consulta indexada de evidencia | 3 | AUD-01 | Respuesta <1 s bajo el conjunto de datos declarado. |
| SEC-01 | Pruebas de autorización y fuga de datos | 3 | BB-07, PAY-01 | Cero PAN/CVV en BD, mensajes y logs; roles restringidos. |

### 5.5 Línea 4 - Andy: Facturación, SAT, ERP y POS

| ID | Tarjeta | Puntos | Dependencias | Criterio de aceptación |
|---|---|---:|---|---|
| FIS-01 | Generar XML fiscal con CUI y serie | 5 | BB-03 | XML válido para el contrato simulado y vinculado a la orden. |
| FIS-02 | Flujo SAT asíncrono condicionado | 8 | BB-04/06, FIS-01 | Timeout deja `FISCAL_PENDING`; aprobación permite confirmar la orden. |
| ERP-01 | Consolidación CSV/XML cada intervalo configurable | 5 | FIS-02 | Ventana demostrable reducida; producción documentada en 30 min. |
| ERP-02 | Entrega/reintento y trazabilidad del lote | 3 | BB-04, ERP-01 | Lote preservado, correlacionado y no regenerado de forma duplicada. |
| POS-01 | Fan-out lógico a 32 POS con libro idempotente | 8 | BB-04/06 | Simula 32 destinos, reintenta, deduplica y registra acuse/DLQ. |
| FIS-03 | Pruebas de XML, timeout SAT, lote y POS | 3 | FIS-01/02, ERP, POS | Evidencia automatizada de éxito y fallos. |

### 5.6 Línea 5 - Kevin: Despacho, Notificaciones, Consulta e Interfaz

| ID | Tarjeta | Puntos | Dependencias | Criterio de aceptación |
|---|---|---:|---|---|
| LOG-01 | Despacho desde origen asignado | 5 | INV-04, FIS-02 | Orden confirmada crea despacho con origen y método. |
| LOG-02 | Adaptador LogiExpress, guía y tracking | 5 | BB-06, LOG-01 | Guía/estados quedan correlacionados con la orden. |
| LOG-03 | Retry 2/8/32 s y DLQ ante timeout | 5 | BB-04, LOG-02 | 2 % o más de timeouts simulados no bloquean preparación. |
| NOT-01 | Notificaciones desacopladas | 5 | BB-04 | Caída del proveedor no revierte ni detiene la orden. |
| QRY-01 | Vista consolidada de orden | 5 | Eventos de todas las líneas | Estado completo ≤5 s y última información conocida ante fallo externo. |
| INC-01 | Incidencia/devolución mínima | 5 | QRY-01 | Registra motivo y artículos, vinculados a venta original y auditoría. |
| UI-01 | UI mínima para checkout, consulta e incidencia | 5 | ORD-01, QRY-01, INC-01 | Permite demostrar los flujos sin editar datos manualmente. |

### 5.7 QA, documentación y cierre

| ID | Tarjeta | Dueño | Colaboradores | Criterio de aceptación |
|---|---|---|---|---|
| QA-01 | Contrato e integración continua | Jairo | Todos | Cada merge ejecuta compilación, unitarias e integración. |
| QA-02 | Línea base `T` y carga `8T` | Edson | Todos | EaC-01/02 medidos con p50/p95/p99, errores y ambiente documentado. |
| QA-03 | Matriz EaC → prueba → resultado | Kevin | Todos | Los 12 EaC tienen evidencia o limitación honesta; sin afirmaciones vacías. |
| QA-04 | Prueba E2E camino feliz | Jairo | Todos | Ejecuta los 10 pasos del enunciado en orden observable. |
| QA-05 | Pruebas de fallas externas | Kevin | Edson, Andy | PayKlik, SAT, LogiExpress y notificaciones fallan sin pérdida silenciosa. |
| DOC-01 | Actualizar DDA y ADR-009 a ADR-013 | Jairo | Dueños de decisión | Diagramas, estados y decisiones coinciden con el código real. |
| DOC-02 | README de instalación/despliegue | Andy | Todos | Una persona no autora despliega desde cero siguiendo solo el README. |
| DOC-03 | Bitácora, capturas Kanban y burndown | Jairo | Todos | Evidencia inicial, diaria y final completa. |
| DEMO-01 | Guion, datos y ensayo cronometrado | Kevin | Todos | Cada integrante demuestra y explica su línea; plan de contingencia listo. |

---

## 6. Secuencia de trabajo del 10 al 23 de septiembre

| Fecha | Objetivo verificable | Trabajo principal | Gate de salida |
|---|---|---|---|
| **10 sep** | Inicio formal y contratos | Kickoff, roles, tablero, captura inicial, Gate 0, ADR-009, IDs, estados, eventos, errores y puertos | Decisiones asignadas, tablero real creado y contratos mínimos versionados. |
| **11–12 sep** | Backbone ejecutable | Compose, BD, broker, Outbox, seguridad, observabilidad y simuladores | Sistema vacío levanta desde cero; evento de prueba llega a consumidor idempotente. |
| **13–15 sep** | Primera rebanada vertical | Carrito, reserva, pago exitoso, XML/SAT exitoso, despacho y consulta | Camino feliz básico integrado en `develop`; no solo módulos aislados. |
| **16–17 sep** | Resiliencia | Pago indeterminado/conciliación, expiración, SAT pendiente, reintentos POS/ERP/LogiExpress y notificación | Escenarios de falla pasan con evidencia. |
| **18 sep** | MVP completo | Incidencia/devolución, UI mínima, auditoría, lote ERP y 32 POS simulados | Congelamiento funcional; lo faltante se prioriza o elimina explícitamente. |
| **19–20 sep** | Calidad arquitectónica | Carga `T/8T`, concurrencia, seguridad, latencias, recuperación y correcciones | Informe reproducible y matriz EaC con resultado real. |
| **21 sep** | Release candidate | DDA/ADR, diagramas, README, despliegue limpio, captura Kanban y burndown | Rama permanente `release` actualizada; cero defectos bloqueantes. |
| **22 sep** | Ensayo y contingencia | Dos demos completas, preguntas cruzadas, respaldo de datos/artefactos | Los cinco pueden defender arquitectura y resultados. |
| **23 sep** | Entrega | Correcciones menores, merge a `main`, tag y evidencia final | Código, documentación y demo accesibles antes del corte. |

### 6.1 Hitos de integración obligatorios

- **12 sep:** prueba de infraestructura y Outbox.
- **15 sep:** camino feliz básico completo.
- **17 sep:** fallas de terceros demostrables.
- **18 sep:** congelamiento de funcionalidades.
- **20 sep:** pruebas EaC cerradas.
- **21 sep:** release candidate.

No se acepta que cada integrante espere hasta el 18 de septiembre para integrar su módulo.

### 6.2 Ruta crítica

```text
Gate 0 y contratos
        ↓
Persistencia + Compose + Outbox + simuladores
        ↓
Carrito → Reserva → Pago → Facturación SAT → Despacho → Consulta
        ↓
Saga y prueba E2E
        ↓
Fallas, carga, seguridad y auditoría
        ↓
release → main → tag final
```

BB-03, BB-04 y BB-06 son habilitadores compartidos. ORD-03 y QA-04 forman el punto de convergencia. Cualquier bloqueo de más de un daily en esos elementos se atiende antes de iniciar trabajo P2.

---

## 7. Flujo Kanban y reglas de operación

Las columnas serán exactamente:

1. `To Do`
2. `Blocked`
3. `In Progress`
4. `Ready for Testing`
5. `Test/QA`
6. `Done`

### 7.1 Políticas

- WIP máximo: cinco tarjetas en `In Progress`, una principal por integrante.
- Cada tarjeta tiene un solo dueño, un revisor, driver/CUN relacionado, aceptación y enlace de evidencia.
- `Blocked` debe indicar causa, persona que puede desbloquear, impacto y próxima revisión.
- Una tarjeta no pasa a `Ready for Testing` sin pruebas del autor.
- Una tarjeta no pasa a `Done` sin revisión cruzada, merge a `develop`, documentación y trazabilidad actualizada.
- Backbone y riesgos arquitectónicos se terminan antes que mejoras visuales.
- El tablero se actualiza durante el daily, no al final de la semana.

### 7.2 Definition of Ready

Una tarjeta puede entrar a desarrollo cuando tiene alcance, dueño, dependencias, contrato afectado, criterio verificable, datos de prueba y driver asociado.

### 7.3 Definition of Done arquitectural

Una tarjeta está terminada únicamente si:

- compila y supera pruebas unitarias, integración y negativas relevantes;
- respeta los límites del monolito modular;
- no contiene secretos ni datos sensibles;
- produce logs/métricas correlacionables sin PAN/CVV;
- contempla idempotencia si consume mensajes o llama integraciones reintentables;
- tiene revisión de una persona distinta del autor;
- actualiza API/esquema, README, ADR/DDA y matriz de trazabilidad cuando corresponda;
- se ejecuta dentro de Docker Compose y aporta evidencia de aceptación.

---

## 8. Git-flow obligatorio

- El repositorio debe permanecer privado y contener la carpeta `FASE 2` dentro del repositorio utilizado en la Fase 1.
- Deben agregarse y verificarse los accesos académicos indicados en el enunciado: `aragonjc`, `AbdielOtzoy` y `Kjhg4589`.
- Ramas permanentes: `main`, `release` y `develop`.
- Toda feature nace de `develop`: `feature/<id>-descripcion-corta`.
- Correcciones urgentes: `hotfix/<id>-descripcion-corta`.
- No hacer commits funcionales directamente en `main`, `release` o `develop`.
- Formato de commit: `#carnet: mensaje en infinitivo`, sustituyendo `carnet` por el número real.
- Pull request pequeño, con tarjeta, alcance, pruebas, capturas/logs si aplican y revisor asignado.
- Al fusionar, eliminar la rama feature.
- El 21 de septiembre `develop` se integra en la rama permanente `release` para formar el release candidate.
- El 23 de septiembre `release` se integra en `main` y se etiqueta la entrega.

Protecciones recomendadas: un revisor obligatorio, CI en verde, conversación resuelta y prohibición de force-push en ramas principales.

---

## 9. Estrategia de validación de EaC

| EaC | Prueba de Fase 2 | Dueño | Resultado esperado |
|---|---|---|---|
| EaC-01 | Carga sobre consulta consolidada | Kevin/Edson | 100 % ≤5 s; reportar p50/p95/p99. |
| EaC-02 | Consulta de auditoría por orden/transacción | Edson | <1 s en el conjunto declarado. |
| EaC-03 | Línea base `T` y ejecución a `8T` | Edson/Jairo | Mantener EaC-01/02 y registrar recursos/errores. |
| EaC-04 | Latencia >5 s y caída sostenida de PayKlik | Edson | Cero órdenes perdidas; trabajo de conciliación ≤60 s. |
| EaC-05 | Inyectar al menos 2 % de timeouts LogiExpress | Kevin | Preparación continúa; 2/8/32 s y DLQ tras agotar. |
| EaC-06 | Deshabilitar POS y restaurarlo | Andy | Reintento persistente, deduplicación y acuse medible. |
| EaC-07 | Reconstruir una orden completa | Edson | Actor, instante y cambio en todos los pasos. |
| EaC-08 | Apagar proveedor de notificaciones | Kevin | Orden continúa; aviso queda pendiente/fallido. |
| EaC-09 | Despliegue por un revisor usando runbook | Andy/Jorge | Resultado y tiempo registrados; meta DDA ≤2 h. |
| EaC-10 | Inventario de recursos/costos | Jairo | Estimación explícita, sin superar límite ni fingir cotización. |
| EaC-11 | Segundo ambiente por configuración | Jairo | Mismo artefacto sin cambiar el núcleo. |
| EaC-12 | Escaneo de BD, cola y logs | Edson | Cero PAN completo/CVV; TLS simulado/documentado según ambiente. |

Una prueba que falla no se maquilla: se registra el resultado, se crea una tarjeta de corrección y, si no hay tiempo, se actualiza el ADR/DDA con la limitación real.

---

## 10. Daily, revisión y retrospectiva

### Daily de 15 minutos

Cada integrante registra en la bitácora:

1. Componente/patrón materializado ayer y resultado numérico o funcional.
2. Tarea de hoy y trazabilidad a CUN, EaC, restricción o ADR.
3. Impedimento técnico, responsable de desbloqueo e impacto.

Después del daily pueden quedar conversaciones técnicas, pero solo con quienes sean necesarios.

### Revisión diaria de integración

A las 20:00 o a una hora fija acordada:

- CI de `develop` debe estar verde.
- Se revisan cambios de contratos y migraciones.
- Se ejecuta al menos un smoke test E2E.
- Se mueve evidencia en el tablero y se actualiza burndown.

### Retrospectiva final individual

Cada persona responde las tres preguntas exigidas por el enunciado: decisiones/patrones que funcionaron, supuestos de Fase 1 que fallaron y mejoras concretas de mantenibilidad. La respuesta debe citar evidencia del código o de pruebas.

---

## 11. Guion mínimo de demostración

1. Levantar todo desde un ambiente limpio con el README.
2. Mostrar la frontera y contenedores activos.
3. Iniciar carrito en canal móvil simulado y recuperarlo desde web/soporte.
4. Lanzar órdenes concurrentes y demostrar una única reserva de la última unidad.
5. Completar pago exitoso sin exponer PAN/CVV.
6. Generar XML; SAT aprueba y la orden se confirma.
7. Asignar origen, generar guía y actualizar tracking.
8. Mostrar notificación, consulta consolidada e incidencia/devolución.
9. Ejecutar el lote ERP con intervalo reducido para demo y mostrar eventos/acuse de 32 POS.
10. Repetir una orden con PayKlik lento, SAT temporalmente caído, timeout LogiExpress y proveedor de notificaciones apagado.
11. Consultar la traza completa y mostrar resultados de carga/seguridad.

Los intervalos reducidos usados en demo deben estar parametrizados; la configuración objetivo de ERP seguirá siendo 30 minutos.

---

## 12. Riesgos y respuesta

| Riesgo | Señal temprana | Respuesta |
|---|---|---|
| Integración al final | Ramas viven >2 días sin PR | PR pequeño diario y smoke E2E desde el 12 sep. |
| Jairo se convierte en cuello de botella | Toda aprobación o redacción espera por él | Revisión cruzada fija y dueños documentan sus cambios. |
| Monolito se vuelve una masa acoplada | Un módulo consulta repositorios/tablas ajenas | Pruebas de arquitectura y puertos internos. |
| Outbox usado como simple cola sin atomicidad | Se guarda negocio y evento en transacciones distintas | Prueba de rollback y revisión de BB-04. |
| Doble cobro o doble reserva | Reintento crea otro efecto | Claves idempotentes y pruebas concurrentes antes de integrar. |
| Demo depende de internet/terceros | Se usan APIs públicas | Todos los terceros se simulan localmente. |
| UI consume tiempo crítico | UI empieza antes del camino feliz API | UI-01 se inicia tras QRY-01; Swagger/Postman queda como contingencia. |
| “8x” sin base defendible | No existe reporte de `T` al 19 sep | Congelar escenario y datos de carga el 18 sep. |
| Documentación contradice código | ADR/DDA se actualizan el último día | Documentación forma parte de DoD de cada tarjeta. |
| Pérdida de evidencia ágil | Bitácora o tablero pasan días sin cambios | El Scrum Master lo verifica diariamente y captura inicio/cierre. |

---

## 13. Criterio de éxito de la fase

La Fase 2 está lista para entrega cuando un integrante distinto del autor puede clonar/desplegar el sistema, ejecutar el camino feliz y los fallos principales, consultar evidencia auditable, reproducir las pruebas de EaC, explicar la correspondencia con ADR-001 a ADR-013 y verificar en el tablero/bitácora cómo evolucionó el trabajo.

La meta no es aparentar un sistema productivo completo. La meta es un MVP coherente, reproducible y defendible que demuestre con código y mediciones que la arquitectura de la Fase 1 funciona, y que documente con honestidad cualquier decisión que haya cambiado al confrontarla con la implementación.

### 13.1 Cobertura final del enunciado

| Entregable o exigencia | Evidencia planificada |
|---|---|
| Código completo | Ramas feature, PR revisados, CI y tag final en `main`. |
| Repositorio privado y accesos académicos | BB-01 y verificación de `aragonjc`, `AbdielOtzoy` y `Kjhg4589`. |
| README de instalación/despliegue | DOC-02 y prueba de despliegue por persona no autora. |
| Demo funcional E2E | ORD-05, QA-04 y DEMO-01. |
| Actualización del DDA mediante ADR | DOC-01, ADR-009 a ADR-013 y diagramas coherentes con el código. |
| Docker y `docker-compose.yml` | BB-05 y validación desde ambiente limpio. |
| Kanban con seis columnas | Sección 7, captura inicial del 10 sep y final del 23 sep. |
| Sprint Backlog inicial | Sección 5 trasladada a tarjetas con dueño y aceptación. |
| Feedback de la Fase 1 | DEC-07 y tarjetas `OBS-F1-XX` basadas únicamente en observaciones reales. |
| Bitácora Markdown | BB-01, actualización diaria y retrospectivas individuales. |
| Daily técnico obligatorio | Sección 10 y evidencia diaria por integrante. |
| Burndown o resumen consolidado | DOC-03 con completadas, pendientes y justificación. |
| Git-flow y commits profesionales | Sección 8 y protecciones de ramas. |
| Pruebas de calidad y seguridad | QA-02, QA-03, QA-05 y matriz de la sección 9. |
