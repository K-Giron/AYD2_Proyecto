# Documento de Definición de Arquitectura (DDA)
## SGO-Logic — Sistema de Gestión de Órdenes y Logística Omnicanal · MegaStore

| Campo | Valor |
|---|---|
| Curso | Análisis y Diseño de Sistemas 2A |
| Grupo | `AYD2_<seccion>_2S2025_PROYECTO_G<#>` |
| Fase | 1 |
| Versión | 0.5 (borrador de trabajo) |
| Última actualización | 26/08/2026 |
| Responsable de esta versión | Persona 4 — Rol D: Modelado y Vistas (sección 11) |

> Leyenda de estado: Pendiente | En progreso | Borrador completo (requiere validación) | Completo | Propuesto (aplica a decisiones aún no aceptadas)
> Cada sección indica quién es el dueño principal según el reparto de trabajo del equipo.
> Las decisiones marcadas como **propuestas** no se consideran aprobadas hasta ser revisadas por el equipo y por la Persona 3, responsable de los ADR.

---

## Índice
1. [Resumen Ejecutivo](#1-resumen-ejecutivo)
2. [Identificación del Caso de Negocio](#2-identificación-del-caso-de-negocio)
3. [Stakeholders y Preocupaciones Arquitectónicas](#3-stakeholders-y-preocupaciones-arquitectónicas)
4. [Características del Sistema Priorizadas](#4-características-del-sistema-priorizadas)
5. [Drivers — Requisitos Funcionales](#5-drivers--requisitos-funcionales)
6. [Drivers de Calidad (EaC)](#6-drivers-de-calidad-eac)
7. [Drivers de Restricción](#7-drivers-de-restricción)
8. [Priorización de Drivers](#8-priorización-de-drivers)
9. [Matriz de Trazabilidad](#9-matriz-de-trazabilidad)
10. [Decisiones Arquitectónicas (ADRs)](#10-decisiones-arquitectónicas-adrs)
11. [Vistas Arquitectónicas](#11-vistas-arquitectónicas)
12. [Prototipo / Mockups](#12-prototipo--mockups)
13. [Anexos](#13-anexos)

---

## 1. Resumen Ejecutivo
**Estado:** Borrador; pendiente de validación final cuando ADR-001 y las secciones 5 y 6 queden cerradas | **Dueño:** Persona 5 (Rol E)

MegaStore es una cadena regional de retail con 32 tiendas en Guatemala, El Salvador y Costa Rica y una facturación anual de USD 450 millones, cuyo comercio electrónico crece un 15 % cada año. Su operación actual está fragmentada: cada tienda administra inventario local, el canal en línea opera por separado, la disponibilidad se confirma por teléfono y Finanzas concilia ventas manualmente desde hojas de cálculo, generando devoluciones de hasta siete días hábiles y exposición a errores de despacho y a pérdidas ya cuantificadas, como los USD 150,000 de un solo evento de indisponibilidad de la pasarela de pago.

SGO-Logic se propone como una capa de orquestación e integración omnicanal que permite iniciar, continuar y completar una compra desde cualquier canal sin modificar ERP-Core ni los POS existentes, cuya sustitución la Junta Directiva descartó. La arquitectura recomendada (ADR-001, propuesto) es un **monolito modular** organizado por capacidades de negocio, rodeado de una **capa de integración por puertos, adaptadores y mensajería** que aísla a SGO-Logic de PayKlik, LogiExpress, la SAT, los POS y el ERP. Este estilo protege los drivers críticos priorizados: continuidad de la orden ante fallas de PayKlik, escalabilidad de hasta 8x en Black Friday, trazabilidad completa para las auditorías de la SAT y mantenibilidad con las capacidades de Java, .NET y Docker del equipo interno, dentro del presupuesto aprobado y con una ruta planificada de evolución hacia la nube.

`[Persona 5: revisar y ajustar este párrafo cuando ADR-001 se acepte formalmente y las secciones 5 (CDU) y 6 (EaC) estén completas, para asegurar que ningún dato quede desactualizado.]`

---

## 2. Identificación del Caso de Negocio
**Estado:** Borrador completo; pendiente de validación del equipo | **Dueño:** Persona 2 (Rol B), con delimitación de alcance por Persona 1 (Rol A)

### 2.1 Situación actual y objetivo de negocio

MegaStore opera 32 tiendas físicas en Guatemala, El Salvador y Costa Rica y un canal de comercio electrónico que representa el 25 % de sus ventas. Sus aplicaciones y procedimientos evolucionaron de forma separada: las tiendas conservan ventas e inventario localmente, el comercio electrónico maneja información independiente, Logística confirma existencias por teléfono y Finanzas concilia archivos de hojas de cálculo. Esta fragmentación retrasa cambios y devoluciones hasta siete días hábiles, dificulta conocer el inventario disponible y expone a la organización a pérdidas, errores de despacho e inconsistencias fiscales.

El objetivo de SGO-Logic es habilitar una operación omnicanal en la que el cliente pueda iniciar, continuar y completar una compra desde tienda, web, aplicación móvil o centro de atención con una experiencia consistente. El sistema debe unificar la orquestación de órdenes, inventario, pagos, facturación, despacho, consulta, devolución, notificación y auditoría, integrándose con los sistemas existentes sin sustituirlos.

### 2.2 Sistema de interés y alcance

**Sistema de interés:** SGO-Logic es la capa de orquestación e integración omnicanal situada entre los canales de venta y los sistemas internos o externos existentes. Su frontera incluye la lógica necesaria para mantener la continuidad de la intención de compra y el ciclo de vida de la orden; no incluye la implementación interna de los sistemas con los que se integra.

**Dentro del alcance:**

- Orquestar el carrito y la orden entre web, aplicación móvil, tienda física y centro de atención.
- Verificar y reservar inventario antes de confirmar una venta, y liberar la reserva cuando la compra no concluye.
- Orquestar pagos por los métodos habilitados para cada canal y conservar cada intento asociado con la orden, el canal, la fecha y el resultado.
- Generar y enviar la documentación fiscal en XML a la SAT antes de finalizar la orden, conservando CUI y serie autorizada.
- Sincronizar resúmenes de ventas con ERP-Core mediante CSV o XML en intervalos de 30 minutos.
- Publicar eventos hacia las 32 instancias POS con manejo de errores y reintentos.
- Orquestar el despacho desde un centro de distribución, una tienda o recogida por el cliente, incluida la modificación del método de entrega después de la compra.
- Integrarse con LogiExpress para generar guías y recibir estados de entrega.
- Proveer consulta consolidada de órdenes, pagos, despachos, entregas, devoluciones e incidencias.
- Gestionar devoluciones y reembolsos conservando su relación con la venta original.
- Emitir notificaciones de confirmación, preparación, tránsito, entrega e incidencias sin bloquear la operación principal.
- Mantener trazabilidad suficiente para reconstruir quién realizó una operación, cuándo ocurrió y qué cambió.

**Fuera del alcance:**

- Modificar o reemplazar ERP-Core sobre AS/400.
- Modificar o reemplazar las 32 instancias POS.
- Modificar los servicios internos de PayKlik, LogiExpress, SAT o del proveedor de notificaciones.
- Implementar en esta fase el sistema productivo: la Fase 1 entrega definición arquitectónica, vistas, justificación y prototipo de alto nivel.

**Interfaces que atraviesan la frontera:**

| Entidad externa | Interacción conocida | Condición arquitectónica derivada del caso |
|---|---|---|
| ERP-Core | SGO-Logic entrega resúmenes de venta en CSV o XML | Procesamiento en intervalos de 30 minutos; ERP-Core no se modifica |
| POS de las 32 tiendas | Reciben eventos emitidos por SGO-Logic | Deben existir manejo de errores y reintentos; los POS no se modifican |
| PayKlik | Solicitudes de pago mediante REST | Latencia aproximada de 800 ms y hasta 5 s en alta demanda; el contrato vigente debe respetarse |
| LogiExpress | Generación de guías y recepción de estados mediante REST | Aproximadamente una de cada 50 solicitudes termina en timeout |
| SAT | Envío de factura electrónica en XML | La factura debe emitirse antes de finalizar la orden y conservar CUI y serie autorizada |
| Proveedor de notificaciones | Envío de SMS, correo y push | Su indisponibilidad no debe bloquear venta, pago, facturación o despacho |

### 2.3 Supuestos y preguntas arquitectónicas abiertas

Los siguientes puntos no están definidos por el enunciado. No se adoptará una solución irreversible hasta obtener evidencia o registrar un supuesto explícito en un ADR:

| Tema abierto | Por qué afecta la arquitectura | Responsable de aclaración |
|---|---|---|
| Fuente autoritativa y mecanismo de actualización del inventario | Determina consistencia de reservas, prevención de doble compromiso y diseño de integración con tiendas y centros de distribución | Persona 2 con stakeholders de Negocio y Logística |
| Significado exacto de “procesar pagos sin depender del proveedor externo” | El contrato de PayKlik sigue vigente y no se describe un segundo adquirente; debe aclararse si se espera aceptar otros métodos, conservar la orden pendiente o incorporar un proveedor alterno | Personas 2 y 3 con Finanzas y Operaciones |
| Contrato técnico de integración con SAT y comportamiento durante su indisponibilidad | La factura es previa al cierre de la orden, por lo que su disponibilidad condiciona la transición de estado y las compensaciones | Persona 3 con Finanzas |
| Formato, protocolo y garantías disponibles en cada POS | Define adaptadores, idempotencia, confirmaciones y estrategia de reintento | Personas 2 y 4 con TI |
| Volumen normal, concurrencia y distribución del tráfico por canal | “8x” es una proporción, pero falta una línea base para dimensionar y diseñar una prueba de carga reproducible | Persona 3 con TI |
| Objetivos de disponibilidad, recuperación y pérdida de datos | Sin RTO, RPO y disponibilidad objetivo no puede justificarse redundancia ni recuperación ante desastres | Persona 3 con TI y Gerencia |
| Jurisdicciones y controles concretos de protección de datos | MegaStore opera en tres países; deben confirmarse obligaciones aplicables, retención, cifrado y residencia de datos | Persona 3 con Legal/Finanzas |
| Nombres de estado del ciclo de vida de la orden usados en el prototipo (Confirmada, Pago aprobado, En preparación, En tránsito, Entregada) | El prototipo de Persona 5 (§12) los usa para ilustrar la consulta de estado, pero el ciclo de vida completo todavía no tiene un CDU o ADR aceptado — ADR-005 (coordinación y estados) sigue pendiente de redactar (§10.3) | Personas 2 y 3, revisado por Persona 5 |

### 2.4 Diagrama de Casos de Uso de Negocio (CUN) — alto nivel

![Core del negocio de SGO-Logic](./diagramas/cun-alto-nivel.svg)

[Fuente versionable](./diagramas/cun-alto-nivel.dot) | [PNG para presentación](./diagramas/cun-alto-nivel.png)

El core del negocio se modela como un único Caso de Uso de Negocio, "Operar venta y logística omnicanal de MegaStore", que resume en una sola frase el alcance descrito en 2.2. Se distinguen actores de negocio internos (Cliente, Personal de tienda, Personal de atención, Logística, Finanzas) de actores externos existentes (ERP-Core, POS, PayKlik, LogiExpress, SAT, proveedor de notificaciones), consistentes con las entidades externas de 3.1. Todavía no hay actores operativos ni relaciones entre casos de uso: esos aparecen recién en los CDU expandidos (Sección 5), siguiendo el mismo criterio con el que Persona 4 modeló el Diagrama de Contexto (11.1).

**Notación:** el óvalo de doble borde representa un CUN y la etiqueta "(negocio)" marca actores de negocio, notación complementaria declarada explícitamente por no disponer de la diagonal de UML de negocio en la herramienta de diagramado (mismo criterio que `Docs/diagramas/README.md`).

### 2.5 Primera descomposición (macroprocesos)

![Primera descomposición del core de SGO-Logic](./diagramas/cun-primera-descomposicion.svg)

[Fuente versionable](./diagramas/cun-primera-descomposicion.dot) | [PNG para presentación](./diagramas/cun-primera-descomposicion.png)

El core se descompone en los 12 CUN de 2.6, agrupados en seis macro-procesos de negocio. Cada CUN queda asociado a los actores de negocio que efectivamente lo originan o lo reciben, y cada macro-proceso corresponde a una línea de acción distinguible del negocio (venta, cumplimiento fiscal, logística, atención, postventa y gobernanza), no a una agrupación técnica de sistema:

| Macro-proceso | CUN incluidos | Actores de negocio asociados |
|---|---|---|
| Venta omnicanal | Gestionar carrito omnicanal, Verificar y reservar inventario, Procesar pago | Cliente, Personal de tienda, Personal de atención, Logística, PayKlik |
| Postventa | Gestionar devoluciones y reembolsos | Cliente, Personal de atención, Personal de tienda |
| Cumplimiento fiscal | Emitir documentación fiscal, Sincronizar venta con ERP | Finanzas, SAT, ERP-Core |
| Logística | Gestionar despacho, Integrar con LogiExpress, Notificar eventos a POS | Logística, LogiExpress, POS |
| Atención al cliente | Consultar estado de orden, Notificar al cliente | Cliente, Personal de atención, Proveedor de notificaciones |
| Gobernanza | Auditar operaciones | Finanzas, SAT |

Ningún CUN queda sin macro-proceso ni sin al menos un actor de negocio asociado; esa cobertura se reutiliza en la matriz de trazabilidad (Sección 9) y en el diagrama de Componentes de Persona 4 (11.2), donde CUN-01 a CUN-03 orientan el módulo de Carrito/Órdenes, CUN-04/CUN-05 el módulo de Facturación, CUN-06 a CUN-08 el módulo de Despacho, CUN-09/CUN-11 el módulo de Notificaciones/Consulta, CUN-10 el módulo de Postventa y CUN-12 el módulo de Auditoría.

### 2.6 Lista de CUN identificados (insumo para 2.4)
1. **CUN-01:** Gestionar carrito omnicanal
2. **CUN-02:** Verificar y reservar inventario
3. **CUN-03:** Procesar pago
4. **CUN-04:** Emitir documentación fiscal
5. **CUN-05:** Sincronizar venta con ERP
6. **CUN-06:** Notificar eventos a POS
7. **CUN-07:** Gestionar despacho
8. **CUN-08:** Integrar con LogiExpress
9. **CUN-09:** Consultar estado de orden
10. **CUN-10:** Gestionar devoluciones y reembolsos
11. **CUN-11:** Notificar al cliente
12. **CUN-12:** Auditar operaciones

`[Persona 2: diagramas 2.4/2.5 incorporados. Pendiente de validación del equipo: confirmar que ningún CUN quedó fuera de alcance y que los actores de negocio coinciden con los que use Persona 4 en Contexto/Componentes.]`

---

## 3. Stakeholders y Preocupaciones Arquitectónicas
**Estado:** Borrador completo; pendiente de validación del equipo | **Dueño:** Persona 1 (Rol A), con insumo de todo el equipo

Para este DDA, un stakeholder es una persona, grupo u organización con intereses sobre SGO-Logic o capacidad de afectar sus decisiones. ERP-Core, POS y los servicios de terceros se documentan como **entidades externas del contexto**, no como stakeholders humanos; sus condiciones técnicas se conservan en la frontera del sistema y en las matrices de trazabilidad. La agrupación no pretende imponer una cifra cerrada: debe refinarse si el equipo identifica propietarios, operadores o responsables contractuales adicionales.

| # | Stakeholder | Categoría | Preocupaciones arquitectónicas | Influencia o autoridad |
|---|---|---|---|---|
| 1 | Junta Directiva | Gobierno corporativo | Preservar ERP-Core y POS; controlar el alcance y proteger la inversión existente | Alta; su decisión sobre los sistemas legados es no negociable |
| 2 | Dirección | Patrocinio estratégico | Experiencia omnicanal consistente y capacidad de evolución regional | Alta; define el objetivo estratégico |
| 3 | Carlos Méndez, Gerente de TI | Operación y tecnología | Continuidad, monitoreo de dependencias externas, escalabilidad para Black Friday y mantenibilidad interna | Alta; responsable de operar la solución y rechaza cambios a ERP/POS |
| 4 | Ana Lucía Fuentes, Directora de Finanzas | Finanzas y cumplimiento | Trazabilidad, evidencia de transacciones, conciliación y cumplimiento fiscal | Alta; posee veto explícito sobre decisiones que reduzcan trazabilidad o cumplimiento |
| 5 | Roberto Quezada, Jefe de Logística | Operación logística | Visibilidad oportuna de inventario y órdenes; reducción del 70 % de errores de despacho | Media; responsable del resultado logístico |
| 6 | Directora de Comercialización | Negocio y experiencia de cliente | Carrito y experiencia consistentes entre canales, sin silos | Media; representa el objetivo omnicanal comercial |
| 7 | Jefe de Operaciones | Continuidad del negocio | Evitar pérdida de ventas durante caídas y picos de demanda | Media; aporta evidencia del impacto operativo |
| 8 | Gerencia | Gobierno financiero | Inversión máxima de USD 150,000 y operación de hasta USD 1,200 mensuales sin aprobación adicional | Alta; aprueba presupuesto y excepciones |
| 9 | Clientes | Usuario externo | Continuidad del carrito, rapidez, pagos disponibles, estado visible, devoluciones y notificaciones consistentes | Alto interés; influencia indirecta por experiencia e ingresos |
| 10 | Personal de atención al cliente | Usuario interno | Vista consolidada de compra, pago, despacho, entrega, devolución e incidencias | Alto interés; influencia operativa |
| 11 | Equipo interno de desarrollo y operación | Construcción y operación | Tecnología conocida, facilidad de adopción, despliegue, diagnóstico, mantenimiento y evolución | Media; condiciona la viabilidad de la solución |
| 12 | SAT | Regulador | Facturación electrónica, conservación de CUI y serie, auditabilidad y cumplimiento normativo | Alta; obligaciones no negociables |

### 3.1 Entidades externas relevantes

| Entidad | Rol en el contexto | Riesgo o restricción asociada |
|---|---|---|
| ERP-Core (AS/400) | Plataforma contable-financiera receptora de resúmenes de venta | No se modifica; integración por CSV/XML cada 30 minutos |
| POS de las 32 tiendas | Sistemas locales receptores de eventos de SGO-Logic | No se modifican; requieren manejo de errores y reintentos |
| PayKlik | Pasarela para pagos con tarjeta | Contrato por tres años, latencia variable e indisponibilidad observada |
| LogiExpress | Generación de guías y estados de entrega | Contrato por dos años y timeout aproximado en 2 % de solicitudes |
| Servicio de facturación electrónica de SAT | Receptor de XML fiscal | Respuesta variable y condición previa al cierre de la orden |
| Proveedor de SMS, correo y push | Distribución de notificaciones | Disponibilidad fuera del control de MegaStore; no debe bloquear el flujo principal |

---

## 4. Características del Sistema Priorizadas
**Estado:** Borrador completo; pendiente de validación del equipo | **Dueño:** Persona 1 (Rol A) + Persona 3 (Rol C)

| Característica | Por qué importa al negocio |
|---|---|
| Continuidad de la orden ante fallas de la pasarela de pago | Protege los ingresos y evita repetir pérdidas como los USD 150,000 ocasionados por 45 minutos de indisponibilidad en el último Black Friday |
| Capacidad para soportar hasta ocho veces el tráfico normal | Permite atender el pico comercial de noviembre sin degradar las consultas ni la recuperación de evidencia |
| Experiencia y carrito unificados entre tienda, web, aplicación y centro de atención | Cumple el objetivo estratégico de operar sin silos y conservar la intención de compra al cambiar de canal |
| Trazabilidad completa y evidencia recuperable en menos de un segundo | Permite reconstruir quién hizo qué y cuándo, reduce conciliaciones manuales y protege el cumplimiento ante auditorías de la SAT |
| Integración sin modificar ERP-Core ni POS | Preserva la inversión y respeta una decisión no negociable de la Junta Directiva |
| Despacho e inventario visibles y confiables | Ayuda a reducir en 70 % los envíos incorrectos, cuyo costo actual ronda USD 25 por caso |
| Operación mantenible por el equipo interno y dentro del presupuesto | Reduce riesgo de adopción y evita depender de competencias o costos operativos no aprobados |
| Evolución planificada desde infraestructura local hacia nube | Aprovecha la infraestructura existente y mantiene una ruta futura para elasticidad, recuperación y crecimiento regional |

---

## 5. Drivers — Requisitos Funcionales
**Estado:** Borrador completo para los 6 CDU priorizados (5.2 a 5.7); pendiente de validación del equipo y de expandir el resto según tiempo disponible | **Dueño:** Persona 2 (Rol B)

### 5.1 Plantilla de Caso de Uso Expandido (CDU)

```
ID: CDU-XX
Nombre:
Derivado de (CUN):
Actor(es):
Descripción breve:
Precondiciones:
Flujo principal:
  1. ...
  2. ...
Flujos alternativos:
  A1. ...
Flujo de excepción:
  E1. ...
Postcondiciones:
Relacionado con EaC: (ej. EaC-4, EaC-1)
```

### 5.2 Ejemplo resuelto — CDU-03: Procesar Pago

```
ID: CDU-03
Nombre: Procesar pago de una orden
Derivado de (CUN): Procesar pago
Actor(es): Cliente; PayKlik cuando el método seleccionado sea tarjeta
Descripción breve: Permite registrar y procesar el método de pago elegido,
  manteniendo la orden y la trazabilidad del intento cuando el proveedor no
  responda. Los actores de otros métodos de pago deben confirmarse.
Precondiciones:
  - El carrito fue confirmado y el inventario está reservado
Flujo principal:
  1. El cliente selecciona método de pago (tarjeta, transferencia, billetera digital)
  2. El sistema valida que el método esté disponible para el canal y crea un
     intento asociado con la orden, canal y fecha
  3. Para tarjeta, el sistema envía a PayKlik una solicitud con un identificador
     idempotente, sin almacenar el PAN completo ni el CVV
  4. PayKlik responde con el resultado aprobado o rechazado
  5. El sistema registra el resultado y, si fue aprobado, cambia la orden a
     "Pago aprobado" e inicia la emisión fiscal
  6. La orden no se marca como finalizada hasta cumplir la condición fiscal
Flujos alternativos:
  A1. El cliente cambia de método de pago antes de confirmar
  A2. El método seleccionado no utiliza PayKlik:
      el sistema invoca el adaptador correspondiente, una vez definido
Flujo de excepción:
  E1. PayKlik no responde o supera el umbral de latencia:
      el sistema conserva la orden como "Pago pendiente de confirmación" y
      registra el resultado como indeterminado. Antes de reintentar debe consultar
      o conciliar el resultado anterior para evitar un cobro duplicado. El mecanismo
      definitivo de idempotencia, consulta y reintento se decidirá en ADR-003
Postcondiciones:
  - La orden queda en estado "Pago aprobado", "Pago rechazado" o
    "Pago pendiente de confirmación"
  - Existe un registro auditable del intento de cobro
Relacionado con EaC: EaC-4 (resiliencia de pago), EaC-7 (trazabilidad)
```

### 5.3 CDU-01: Gestionar carrito omnicanal

```
ID: CDU-01
Nombre: Gestionar carrito omnicanal
Derivado de (CUN): Gestionar carrito omnicanal
Actor(es): Cliente; Personal de tienda o Personal de atención cuando actúan
  en representación del cliente
Descripción breve: Permite iniciar, modificar y conservar un carrito de
  compra asociado al cliente, reconocible desde cualquier canal, para que
  la compra pueda continuar o completarse en un canal distinto al de inicio.
Precondiciones:
  - El cliente está identificado o puede identificarse en el canal
Flujo principal:
  1. El cliente inicia una intención de compra en un canal (web, app,
     tienda o centro de atención)
  2. El sistema identifica al cliente o solicita su identificación para
     asociar el carrito a su cuenta
  3. El cliente agrega, modifica o elimina productos del carrito
  4. Para cada producto agregado, el sistema invoca la verificación de
     disponibilidad (CDU-02: Verificar y reservar inventario) antes de
     confirmarlo en el carrito
  5. El sistema conserva el carrito asociado al cliente, independiente del
     canal de origen
  6. El cliente decide continuar la compra desde el mismo canal o desde
     otro distinto
  7. El cliente confirma el carrito para iniciar el pago (CDU-03: Procesar
     pago de una orden)
Flujos alternativos:
  A1. El cliente cambia de canal a mitad de la compra: el sistema recupera
      el carrito vigente en el nuevo canal sin pedir que reingrese los
      artículos
  A2. El cliente elimina productos o abandona el carrito: el inventario
      reservado para esos productos se libera según el mecanismo definido
      en CDU-02
Flujo de excepción:
  E1. Un producto deja de estar disponible mientras el cliente decide: el
      sistema informa la indisponibilidad y ofrece alternativas, o mantiene
      el carrito sin ese ítem, sin bloquear el resto de la compra
Postcondiciones:
  - Existe un carrito único y vigente asociado al cliente, reconocible
    desde cualquier canal, con los productos y su estado de disponibilidad
    actualizados
Relacionado con EaC: EaC-01 (consulta ≤ 5 s), EaC-03 (escalabilidad 8x),
  EaC-08 (no bloqueado por falla de notificaciones)
```

### 5.4 CDU-02: Verificar y reservar inventario

```
ID: CDU-02
Nombre: Verificar y reservar inventario
Derivado de (CUN): Verificar y reservar inventario
Actor(es): Sistema (invocado por CDU-01 y por CDU-07); Personal de
  logística como consultante de disponibilidad
Descripción breve: Verifica existencia de un producto en una ubicación y
  reserva la cantidad solicitada para evitar que la misma unidad se
  comprometa con más de un cliente, liberando la reserva cuando la compra
  no concluye.
Precondiciones:
  - Existe un carrito o una orden con productos solicitados
Flujo principal:
  1. El módulo de Carrito/Órdenes solicita verificar disponibilidad de un
     producto en una ubicación (tienda o centro de distribución)
  2. El sistema consulta la fuente autoritativa de inventario (fuente
     exacta abierta; ver 2.3)
  3. Si hay existencia suficiente, el sistema reserva la cantidad
     solicitada y la marca como comprometida para esa orden
  4. El sistema responde con la confirmación de reserva y la ubicación
     desde la que se reservó
  5. Si la compra no concluye dentro de un tiempo acorde con la operación
     comercial, el sistema libera automáticamente la reserva
Flujos alternativos:
  A1. No hay existencia en la ubicación preferida: el sistema evalúa otra
      ubicación (tienda o centro de distribución) antes de declarar
      indisponibilidad
Flujo de excepción:
  E1. Dos solicitudes intentan reservar la última unidad disponible casi al
      mismo tiempo: el sistema debe garantizar que solo una reserva se
      confirme y la otra reciba indisponibilidad, evitando el doble
      compromiso de la misma unidad
Postcondiciones:
  - El inventario reservado queda comprometido y visible para el resto del
    sistema
  - Una reserva no concluida se libera en un tiempo acorde con la
    operación comercial
Relacionado con EaC: EaC-01 (consulta ≤ 5 s), EaC-03 (escalabilidad 8x)
```

### 5.5 CDU-07: Gestionar despacho

```
ID: CDU-07
Nombre: Gestionar despacho
Derivado de (CUN): Gestionar despacho
Actor(es): Personal de logística; Cliente (elige o cambia el método de
  entrega)
Descripción breve: Determina el origen y el método de despacho de una
  orden (centro de distribución, tienda o recogida por el cliente),
  coordina su preparación y mantiene su estado visible hasta la entrega.
Precondiciones:
  - La orden tiene el inventario reservado (CDU-02) y su pago aprobado o en
    proceso de aprobación según la política vigente
Flujo principal:
  1. El sistema determina las opciones de despacho disponibles para la
     orden: centro de distribución, tienda o recogida por el cliente
  2. El cliente o el sistema selecciona el método de entrega según
     disponibilidad e inventario reservado
  3. El sistema asigna la orden a la ubicación de despacho correspondiente
     y notifica a Logística qué preparar
  4. Personal de logística prepara el pedido y actualiza el estado de
     despacho
  5. El sistema genera la guía de envío mediante integración con
     LogiExpress (CDU-08) cuando el método de entrega lo requiere
  6. El sistema actualiza el estado de la orden conforme avanza el
     despacho (preparación, tránsito, entrega)
Flujos alternativos:
  A1. El cliente cambia el método de entrega después de confirmada la
      compra: el sistema reevalúa la ubicación de despacho y ajusta la
      reserva de inventario si es necesario
Flujo de excepción:
  E1. LogiExpress no confirma la generación de la guía o responde con
      timeout: el sistema reintenta según la política que defina ADR-004
      (pendiente) sin bloquear la preparación física del pedido
Postcondiciones:
  - La orden queda asociada a un método y una ubicación de despacho, con
    estado visible y trazable hasta la entrega
Relacionado con EaC: EaC-05 (confiabilidad LogiExpress), EaC-06
  (confiabilidad hacia POS/tienda), EaC-07 (trazabilidad)
```

### 5.6 CDU-09: Consultar estado de orden

```
ID: CDU-09
Nombre: Consultar estado de orden
Derivado de (CUN): Consultar estado de orden
Actor(es): Cliente; Personal de atención al cliente
Descripción breve: Entrega una vista consolidada del estado de una orden
  (compra, pago, despacho, entrega y, si aplica, devolución o incidencia)
  desde cualquier canal, sin que el solicitante deba consultar cada área
  por separado.
Precondiciones:
  - La orden existe en el sistema, independientemente del canal donde se
    originó
Flujo principal:
  1. El cliente o el personal de atención solicita el estado de una orden
     desde cualquier canal
  2. El sistema recupera el estado consolidado de la orden: compra, pago,
     despacho, entrega y, si aplica, devolución o incidencia
  3. El sistema muestra el resultado dentro del tiempo máximo aceptable
     para el negocio
Flujos alternativos:
  A1. La orden tiene una incidencia registrada: el sistema incluye la
      incidencia en la respuesta junto con el estado general
Flujo de excepción:
  E1. Alguno de los subsistemas de origen del dato (pago, despacho) no
      responde a tiempo: el sistema muestra la última información conocida
      indicando que puede no estar actualizada, en vez de fallar toda la
      consulta
Postcondiciones:
  - El solicitante obtiene una vista consolidada del estado de la orden sin
    necesidad de consultar cada canal o área por separado
Relacionado con EaC: EaC-01 (consulta ≤ 5 s), EaC-07 (trazabilidad)
```

### 5.7 CDU-10: Gestionar devoluciones y reembolsos

```
ID: CDU-10
Nombre: Gestionar devoluciones y reembolsos
Derivado de (CUN): Gestionar devoluciones y reembolsos
Actor(es): Cliente; Personal de atención al cliente; Personal de tienda
Descripción breve: Registra una devolución referenciando la venta
  original, calcula el reembolso o cambio correspondiente y conserva la
  relación entre ambos para auditoría y conciliación fiscal.
Precondiciones:
  - Existe una venta original confirmada y facturada (CDU-04: Emitir
    documentación fiscal)
Flujo principal:
  1. El cliente o el personal de atención inicia una devolución
     referenciando la orden o venta original
  2. El sistema recupera el historial completo de la compra (pago,
     despacho, entrega) para validar la elegibilidad de la devolución
  3. El personal de atención o de tienda registra el motivo y los
     artículos a devolver
  4. El sistema calcula el reembolso o cambio correspondiente y lo asocia
     a la venta original
  5. El sistema actualiza el inventario disponible con los artículos
     devueltos, cuando aplica
  6. El sistema genera la documentación fiscal de la devolución cuando la
     operación lo requiere (relacionado con CDU-04)
  7. El sistema notifica al cliente el resultado de la devolución
     (relacionado con CUN-11: Notificar al cliente)
Flujos alternativos:
  A1. La devolución corresponde solo a una parte de la orden: el sistema
      mantiene la relación entre la devolución parcial y la venta original
      completa
Flujo de excepción:
  E1. La venta original no se encuentra o no cumple condición de
      elegibilidad: el sistema informa el motivo y no continúa con el
      reembolso hasta que el caso se resuelva manualmente
Postcondiciones:
  - La devolución queda registrada y vinculada a la venta original, con el
    reembolso o cambio procesado y la evidencia disponible para auditoría
Relacionado con EaC: EaC-02 (evidencia < 1 s), EaC-07 (trazabilidad)
```

### 5.8 CDU pendientes de expandir
`[TODO: repetir la plantilla 5.1 para el resto de CUN según tiempo disponible: Emitir documentación
fiscal, Sincronizar venta con ERP, Notificar eventos a POS, Integrar con LogiExpress, Notificar al
cliente, Auditar operaciones. Los cinco priorizados en la Sección 8.2 y en la matriz de la Sección 9
ya quedaron expandidos en 5.2 a 5.7.]`

---

## 6. Drivers de Calidad (EaC)
**Estado:** En progreso | **Dueño:** Persona 3 (Rol C)

### 6.1 Plantilla de Escenario de Atributo de Calidad

```
ID: EaC-XX
Atributo de calidad:
Fuente del estímulo:
Estímulo:
Artefacto:
Ambiente:
Respuesta:
Medida de respuesta:
Reflejado en diagrama de despliegue como:
```

### 6.2 Ejemplo resuelto — EaC-04

```
ID: EaC-04
Atributo de calidad: Disponibilidad / Resiliencia
Fuente del estímulo: Pasarela de pago externa (PayKlik)
Estímulo: PayKlik cae o supera el umbral aceptable de latencia durante un pico de demanda
Artefacto: Módulo de Pago de SGO-Logic
Ambiente: Operación normal y picos de demanda (ej. Black Friday, hasta 8x tráfico normal)
Respuesta: El sistema detecta la falla mediante límites de tiempo y circuit breaker,
  conserva la orden y el intento con resultado indeterminado, y programa conciliación
  o reintento idempotente sin bloquear ni perder la orden del cliente
Medida de respuesta: 0 órdenes perdidas por falla del proveedor de pago;
  el tiempo objetivo de conciliación, reintento o notificación debe ser acordado
Reflejado en diagrama de despliegue como: adaptador de PayKlik con Circuit Breaker,
  persistencia del intento, cola de conciliación/reintentos y monitoreo de mensajes fallidos
```

### 6.3 Tabla resumen de EaC identificados (completar cada uno con 6.1)
| ID | Atributo | Resumen del escenario | Medida |
|---|---|---|---|
| EaC-01 | Rendimiento | Cliente consulta estado de orden | ≤ 5 s |
| EaC-02 | Rendimiento | Finanzas recupera evidencia de transacción | < 1 s |
| EaC-03 | Escalabilidad | Demanda Black Friday | Soportar 8x tráfico normal |
| EaC-04 | Disponibilidad/Resiliencia | Falla de PayKlik | No perder la orden *(ver ejemplo arriba)* |
| EaC-05 | Confiabilidad | Timeout de LogiExpress (2%) | Despacho no bloqueado; política y umbral de reintento por acordar |
| EaC-06 | Confiabilidad | Envío de eventos a POS | Entrega eventual garantizada |
| EaC-07 | Auditabilidad | Auditoría anual SAT | Reconstruir quién/cuándo/qué |
| EaC-08 | Disponibilidad parcial | Falla proveedor de notificaciones | Operación principal continúa |
| EaC-09 | Mantenibilidad | Operación por equipo interno | Stack Java/.NET/Docker básico |
| EaC-10 | Costo | Infraestructura en operación | ≤ $1,200/mes |
| EaC-11 | Portabilidad | Crecimiento futuro | On-prem → migración planificada a nube |
| EaC-12 | Seguridad | Datos de pago | Sin PAN completo ni CVV almacenado |

`[TODO: expandir cada fila con el formato completo de 6.1]`

---

## 7. Drivers de Restricción
**Estado:** Borrador completo; pendiente de validación de Persona 3 | **Dueño:** Persona 3 (Rol C) | **Formalización:** Persona 1 (Rol A)

| # | Restricción | Origen |
|---|---|---|
| R1 | No modificar ni reemplazar ERP-Core ni los POS | Decisión de Junta Directiva |
| R2 | Presupuesto: $150,000 inversión / $1,200 mensual sin aprobación adicional | Gerencia |
| R3 | Respetar contratos vigentes: PayKlik (3 años), LogiExpress (2 años) | Legal/Contractual |
| R4 | Cumplir normativa de protección de datos y facturación electrónica SAT | Regulatorio |
| R5 | Stack del equipo: Java, .NET, Docker básico | Capacidad del equipo interno |
| R6 | Despliegue inicial on-premises, con ruta de migración a nube | Decisión estratégica de Dirección/TI |
| R7 | Ruta viable de pagos/facturación desde el primer mes; Black Friday en noviembre no se mueve | Calendario comercial |
| R8 | Notación UML 2.5.1 | Cátedra |
| R9 | Minimizar el consumo energético de la infraestructura | Objetivo organizacional de sostenibilidad y costo |
| R10 | Repositorio privado de GitHub con nomenclatura `AYD2_<seccion>_2S2025_PROYECTO_G<#>` | Cátedra |

---

## 8. Priorización de Drivers
**Estado:** Borrador completo; pendiente de aprobación del equipo | **Dueño:** Persona 1 (Rol A)

### 8.1 Criterio utilizado

La prioridad combina cuatro dimensiones solicitadas por el enunciado:

- **Valor:** contribución a ingresos, experiencia omnicanal, cumplimiento o eficiencia operativa.
- **Riesgo:** pérdida o daño probable si el driver no se atiende; se consideran evidencia histórica, fallas conocidas y autoridad de veto.
- **Urgencia:** cercanía de una fecha o necesidad no desplazable, especialmente Black Friday y la ruta de pagos/facturación del primer mes.
- **Impacto arquitectónico:** grado en que el driver condiciona fronteras, estilo, integración, despliegue o mecanismos transversales.

La tabla es una priorización relativa, no una exclusión: los drivers que no aparecen en el grupo crítico continúan siendo obligatorios cuando el enunciado los establece.

### 8.2 Drivers críticos priorizados

| Prioridad | Driver | Valor | Riesgo | Urgencia | Impacto arquitectónico | Justificación basada en evidencia |
|---|---|---|---|---|---|---|
| 1 | EaC-04 — resiliencia de pagos | Alto | Alto | Alta | Alto | La caída de PayKlik durante 45 minutos produjo pérdidas estimadas en USD 150,000; la orden no puede perderse si el proveedor falla |
| 2 | EaC-03 — escalabilidad hasta 8x | Alto | Alto | Alta | Alto | Black Friday tiene fecha fija en noviembre y se espera hasta ocho veces la demanda normal sin degradar consultas ni evidencia |
| 3 | R1 — no modificar ERP-Core ni POS | Alto | Alto | Alta | Alto | Es una decisión no negociable de Junta que define la frontera del sistema y obliga a integrar sin alterar los legados |
| 4 | EaC-07 — auditabilidad y trazabilidad | Alto | Alto | Alta | Alto | Finanzas posee autoridad de veto y las transacciones sin respaldo pueden causar multas o inconsistencias fiscales |
| 5 | CUN-01 — gestionar carrito omnicanal | Alto | Medio | Alta | Alto | Es la capacidad que materializa el objetivo estratégico: iniciar en un canal y finalizar en otro sin perder la intención de compra |
| 6 | EaC-05 y EaC-06 — confiabilidad con LogiExpress y POS | Alto | Alto | Media | Alto | El timeout de LogiExpress ocurre aproximadamente en 1 de cada 50 solicitudes y los POS requieren manejo de errores y reintentos |
| 7 | R5 / EaC-09 — stack conocido y mantenibilidad interna | Medio | Alto | Media | Alto | Una alternativa fuera de Java, .NET y Docker básico añade capacitación, costo y riesgo de operación |
| 8 | EaC-11 / R6 — evolución on-premises a nube | Medio | Medio | Baja | Alto | El despliegue local inicial es obligatorio, pero la arquitectura debe permitir elasticidad, recuperación y expansión regional futuras |

### 8.3 Orden de atención del resto de drivers

Una vez protegidos los ocho grupos críticos, el siguiente orden de trabajo es: **cumplimiento y seguridad** (EaC-02, EaC-12, R4), **operación logística y consulta** (CUN-02, CUN-07 a CUN-10, EaC-01), **integraciones y continuidad parcial** (CUN-04 a CUN-06, CUN-08, CUN-11, EaC-08, R3), y **costo, sostenibilidad y gobernanza del entregable** (EaC-10, R2, R7 a R10). CUN-12 y EaC-07 atraviesan todos los grupos mediante auditoría.

---

## 9. Matriz de Trazabilidad
**Estado:** Precargada; faltan validación de ADR y referencias finales a diagramas | **Dueño:** Persona 1 (Rol A)

La matriz parte de la evidencia disponible y relaciona tanto stakeholders como entidades externas que imponen condiciones de integración. `Propuesto` se utiliza únicamente para ADR-001, cuyo borrador ya existe. Los demás ADR se marcan como `Pendiente` hasta que tengan contenido revisable. Las vistas y validaciones son objetivos de trazabilidad, no evidencia de implementación terminada.

| Fuente de la preocupación | Preocupación o condición | Driver relacionado | Decisión/ADR | Vista prevista | Validación prevista |
|---|---|---|---|---|---|
| Junta Directiva y Dirección | Omnicanalidad sin modificar ERP/POS | CUN-01, R1 | ADR-001 propuesto | Contexto, Componentes, Bloques | Revisión de frontera sin cambios requeridos en ERP-Core o POS; recorrido del flujo entre canales |
| Carlos Méndez, TI | Continuidad, monitoreo, escalabilidad y mantenibilidad interna | EaC-03, EaC-09, EaC-11, R1, R5, R6 | ADR-001 propuesto; ADR de observabilidad pendiente | Componentes, Despliegue, Bloques | Diseño de prueba de carga a 8x; revisión operativa; verificación de monitoreo de dependencias |
| Ana Lucía Fuentes, Finanzas | Trazabilidad, evidencia rápida y cumplimiento fiscal | EaC-02, EaC-07, R4, CUN-04, CUN-12 | ADR-002 pendiente: publicación confiable y registro auditable | Componentes, Despliegue | Reconstrucción de quién, cuándo y qué cambió; recuperación de evidencia en menos de un segundo |
| Roberto Quezada, Logística | Visibilidad de inventario y órdenes; reducir 70 % los errores | CUN-02, CUN-07, CUN-08, EaC-05 | ADR-004 pendiente: reintentos; ADR-005 pendiente: coordinación y estados | Contexto, Componentes, Despliegue | Escenario de despacho y cambio de entrega; indicador de errores comparado con la línea base |
| Directora de Comercialización | Experiencia y carrito unificados, sin silos | CUN-01, EaC-01 | ADR-001 propuesto; decisión de entrada omnicanal pendiente | Contexto, Componentes, Bloques | Recorrido del prototipo entre canales; consulta en cinco segundos o menos |
| Jefe de Operaciones | Evitar pérdida de ventas en caídas y picos | EaC-03, EaC-04, R7 | ADR-001 propuesto; ADR-003 pendiente: resiliencia de pago | Componentes, Despliegue | Carga a 8x y falla sostenida de PayKlik; verificar que la orden persiste y puede continuar o recuperarse |
| Gerencia | Inversión y operación dentro del presupuesto | R2, EaC-10, R9 | ADR-001 propuesto; evaluación de costos pendiente | Despliegue, Bloques | Estimación de inversión no mayor a USD 150,000 y operación no mayor a USD 1,200 mensuales, o excepción aprobada |
| Clientes | Continuidad del carrito, pago confiable, consulta y notificación | CUN-01, CUN-03, CUN-09 a CUN-11, EaC-01, EaC-04, EaC-08, EaC-12 | ADR-003 y ADR-005 pendientes; decisión de notificación pendiente | Contexto, Componentes, Prototipo | Flujo omnicanal; consulta en cinco segundos o menos; falla de notificación no bloqueante; ausencia de PAN completo y CVV almacenados |
| Personal de atención al cliente | Vista consolidada de orden, pago, entrega y postventa | CUN-09, CUN-10, EaC-01, EaC-07 | ADR-002 y ADR-005 pendientes | Contexto, Componentes, Prototipo | Consulta integral en cinco segundos o menos y trazabilidad de devolución hasta la venta original |
| Equipo interno de desarrollo y operación | Tecnología conocida, mantenibilidad y adopción viable | EaC-09, R5, R6 | ADR-001 propuesto | Componentes, Despliegue | Revisión de arquitectura por el equipo; ejercicio de despliegue y mantenimiento con las capacidades declaradas |
| ERP-Core (entidad externa) | Recibir CSV/XML cada 30 minutos sin cambios internos | CUN-05, R1 | ADR-002 pendiente; decisión de adaptador ERP pendiente | Contexto, Componentes, Despliegue | Generación, reintento y procesamiento de archivo dentro de una ventana de 30 minutos |
| POS (entidades externas) | Recepción confiable de eventos sin modificar las instalaciones | CUN-06, EaC-06, R1 | ADR-002 y ADR-004 pendientes | Contexto, Componentes, Despliegue | Error de entrega y reintento; comprobar entrega eventual e idempotencia en el receptor o adaptador |
| PayKlik (entidad externa) | Integración REST bajo latencia o indisponibilidad | CUN-03, EaC-04, EaC-12, R3 | ADR-003 pendiente: aislamiento de fallas de pago | Contexto, Componentes, Despliegue | Latencia de cinco segundos y falla sostenida; comprobar orden recuperable y ausencia de PAN completo y CVV almacenados |
| LogiExpress (entidad externa) | Generación de guías y estados pese a timeouts | CUN-08, EaC-05, R3 | ADR-004 pendiente: reintento con backoff | Contexto, Componentes, Despliegue | Al menos 2 % de timeouts simulados; comprobar reintento sin bloquear la preparación del despacho |
| SAT | Factura XML, CUI/serie y evidencia auditable | CUN-04, CUN-12, EaC-02, EaC-07, R4 | ADR-002 pendiente; decisión de integración fiscal pendiente | Contexto, Componentes, Despliegue | Validación de XML y CUI/serie; evidencia en menos de un segundo; reconstrucción de la operación |
| Proveedor de SMS, correo y push | Su indisponibilidad no debe bloquear la operación principal | CUN-11, EaC-08 | Decisión de notificación asíncrona pendiente de ADR | Contexto, Componentes, Despliegue | Inyección de falla del proveedor mientras venta, pago y despacho continúan; reintento o registro de entrega fallida |

### 9.1 Matriz de cobertura de drivers críticos

Esta segunda lectura verifica que la priorización de la sección 8 no deje un driver crítico sin decisión, vista o validación prevista.

| Driver crítico | Stakeholders principales | Decisión/ADR | Vista prevista | Validación prevista |
|---|---|---|---|---|
| EaC-04 — resiliencia de pagos | Jefe de Operaciones, Clientes, TI, PayKlik | ADR-003 pendiente, apoyado por ADR-001 propuesto | Componentes, Despliegue | Falla sostenida y latencia alta de PayKlik; orden persistida y recuperable |
| EaC-03 — escalabilidad 8x | TI, Jefe de Operaciones | ADR-001 propuesto | Despliegue, Bloques | Carga equivalente a ocho veces el tráfico normal sin degradar EaC-01/EaC-02 |
| R1 — no modificar ERP/POS | Junta Directiva, TI, ERP-Core, POS | ADR-001 propuesto | Contexto, Componentes | Revisión de interfaces y alcance sin cambios en los sistemas legados |
| EaC-07 — auditabilidad | Finanzas, SAT, Atención al cliente | ADR-002 pendiente | Componentes, Despliegue | Reconstrucción de actor, instante y cambio de una operación; evidencia en menos de un segundo |
| CUN-01 — carrito omnicanal | Comercialización, Cliente | ADR-001 propuesto; decisión de entrada omnicanal pendiente | Contexto, Componentes, Prototipo | Iniciar una compra en un canal y continuarla o completarla en otro |
| EaC-05 / EaC-06 — entrega confiable | Logística, LogiExpress, POS | ADR-004 pendiente | Componentes, Despliegue | Timeouts y errores simulados con reintento, idempotencia y entrega eventual |
| R5 / EaC-09 — mantenibilidad interna | TI, Equipo interno | ADR-001 propuesto | Componentes, Despliegue | Revisión y ejercicio operativo por el equipo interno |
| EaC-11 / R6 — evolución a nube | Dirección, TI | ADR-001 propuesto | Despliegue, Bloques | Revisión de portabilidad y ruta de migración sin obligar migración inmediata |

### 9.2 Pendientes para cerrar las matrices

- Persona 3 debe aceptar, cambiar o rechazar ADR-001 y asignar el contenido definitivo de ADR-002 a ADR-005.
- Persona 4 proporcionó las rutas versionadas `Docs/diagramas/contexto.svg`, `componentes.svg`, `despliegue.svg` y `bloques.svg`, junto con sus fuentes `.dot`; Persona 1 debe enlazar esas rutas en cualquier referencia genérica restante de las matrices.
- Cada responsable debe conservar evidencia de los mecanismos de validación; en Fase 1 puede ser un diseño de prueba o criterio de aceptación verificable cuando aún no exista implementación.

---

## 10. Decisiones Arquitectónicas (ADRs)
**Estado:** En progreso | **Dueño:** Persona 3 (Rol C)

### 10.1 Plantilla de ADR

```
# ADR-XXX: <Título de la decisión>

## Estado
Propuesto | Aceptado | Reemplazado por ADR-YYY

## Contexto
¿Qué problema u obligación de negocio motiva esta decisión? ¿Qué drivers están en juego?

## Decisión
¿Qué se decidió, en una o dos frases claras?

## Alternativas consideradas
1. Alternativa A — pros / contras
2. Alternativa B — pros / contras
3. Alternativa elegida — por qué gana

## Consecuencias
Positivas:
- ...
Negativas / riesgos aceptados:
- ...

## Drivers relacionados
EaC-XX, RX, CUN-XX
```

### 10.2 ADR-001 — Estilo arquitectónico general de SGO-Logic

> **Autor del borrador:** Persona 1 (Rol A) · **Responsable de revisión y aprobación:** Persona 3 (Rol C)

#### Estado

**Propuesto.** Este ADR es un borrador de apoyo y no representa todavía un acuerdo definitivo del equipo.

#### Contexto

SGO-Logic debe integrar una operación omnicanal sin modificar ERP-Core ni las 32 instancias POS (R1). La solución inicia en infraestructura local y debe conservar una ruta de evolución a nube (R6, EaC-11). El equipo que la operará posee experiencia principalmente en Java, .NET y Docker básico (R5, EaC-09), mientras que la organización impone límites de inversión y operación (R2, EaC-10).

El diseño también debe soportar hasta ocho veces el tráfico normal en Black Friday (EaC-03), conservar la orden y permitir su recuperación cuando PayKlik falle (EaC-04), tolerar timeouts de LogiExpress (EaC-05), entregar eventos confiablemente a POS (EaC-06) y conservar trazabilidad completa (EaC-07). Estas condiciones favorecen límites internos claros y desacoplamiento de las integraciones, pero la capacidad actual del equipo y el despliegue inicial no justifican asumir desde el comienzo toda la complejidad operativa de una arquitectura de microservicios.

#### Decisión propuesta

Adoptar un **monolito modular** para el núcleo de SGO-Logic, organizado por capacidades de negocio: Carrito y Órdenes, Inventario y Reservas, Pagos, Facturación, Despacho, Postventa, Notificaciones y Auditoría. Los límites deberán validarse durante el modelado del dominio; esta lista no presupone que cada módulo sea automáticamente un *bounded context*. Cada módulo será propietario de su modelo y de las operaciones sobre sus datos, y la comunicación entre módulos se realizará mediante contratos explícitos, sin acceso directo a estructuras internas de otro módulo.

Rodear el núcleo con una **capa de integración mediante puertos y adaptadores**, complementada con mensajería para las interacciones que admitan procesamiento asíncrono. La arquitectura orientada a eventos se aplica a integraciones y cambios de estado que necesitan desacoplamiento; no convierte todas las operaciones en eventos ni elimina las respuestas síncronas necesarias.

Los modos de interacción se definirán según la semántica del negocio:

- **Síncrono y condicionado:** operaciones cuya respuesta determina una transición inmediata, como la autorización de pago o la aceptación fiscal, mientras el caso mantenga que la factura debe emitirse antes de finalizar la orden.
- **Asíncrono confiable:** propagación hacia POS, generación programada hacia ERP-Core, notificaciones y otros efectos que pueden completarse después sin perder el hecho de negocio.
- **Proceso de larga duración:** ciclo completo de la orden, con estados persistidos, reintentos idempotentes y compensaciones explícitas cuando un paso no pueda completarse.

El primer despliegue mantendrá un solo artefacto para el núcleo y una frontera transaccional local. Si se usa una base de datos compartida físicamente, cada módulo tendrá propiedad lógica de su esquema o tablas; el acceso cruzado se realizará mediante contratos del módulo. La publicación de eventos deberá coordinarse con la transacción de negocio para evitar cambios confirmados sin mensaje o mensajes sin cambio confirmado.

El ADR define el estilo y las fronteras, no una tecnología concreta. La selección de lenguaje, broker, base de datos y herramientas de despliegue deberá evaluarse por separado contra costo, capacidades del equipo y medidas de los EaC.

#### Alternativas consideradas

| Alternativa | Ventajas | Desventajas y riesgos | Evaluación |
|---|---|---|---|
| Monolito modular sin capa de eventos | Menor complejidad inicial; despliegue y diagnóstico simples | Mantiene dependencia temporal con proveedores; dificulta entrega eventual, reintentos y aislamiento de fallas | Descartada porque atiende R5, pero responde débilmente a EaC-04, EaC-05 y EaC-06 |
| Microservicios completos | Escalado y despliegue independientes por dominio; mayor aislamiento potencial | Aumenta operación distribuida, observabilidad, consistencia eventual, infraestructura y capacitación | Descartada para la primera etapa por R2, R5, R6 y EaC-09; puede reconsiderarse durante la evolución |
| Monolito modular con integración orientada a eventos y adaptadores | Equilibra mantenibilidad, resiliencia, trazabilidad y evolución; aísla legados y proveedores | Introduce mensajería, consistencia eventual en ciertos flujos y disciplina de modularidad | **Propuesta**, por responder mejor al conjunto de drivers críticos con complejidad proporcional |

#### Consecuencias

**Positivas:**

- ERP-Core, POS y proveedores externos quedan desacoplados mediante adaptadores, respetando R1 y R3.
- Las integraciones pueden incorporar Circuit Breaker, límites de tiempo, reintentos con backoff, idempotencia y publicación confiable sin contaminar la lógica central.
- Los límites modulares crean una ruta para extraer servicios independientes solo cuando la demanda o la operación lo justifiquen.
- Un registro de auditoría diseñado explícitamente, complementado por eventos e intentos de integración, puede apoyar trazabilidad, conciliación y diagnóstico.
- El núcleo permanece abordable con las capacidades tecnológicas declaradas por el equipo.

**Negativas y riesgos aceptados:**

- El equipo debe aprender y operar mensajería asíncrona, además de observar colas, reintentos y mensajes fallidos.
- La consistencia eventual obliga a definir estados intermedios, idempotencia y compensaciones en procesos como pago, facturación y despacho.
- Un único desplegable del núcleo puede requerir escalar más capacidad de la estrictamente necesaria durante picos; deberá verificarse contra EaC-03 y el límite de costo.
- La modularidad depende de reglas verificables de dependencia; sin ellas, el núcleo puede degradarse a un monolito fuertemente acoplado.
- El broker, la persistencia y los adaptadores se convierten en elementos operativos que requieren métricas, alertas, manejo de mensajes fallidos y procedimientos de recuperación.

#### Criterios de validación de la decisión

- Las vistas de Componentes y Despliegue muestran límites modulares, adaptadores, mensajería y mecanismos de resiliencia coherentes.
- La vista de Componentes identifica contratos provistos y requeridos, propietario de datos y dirección de cada dependencia; no muestra accesos cruzados a datos internos.
- Una revisión de arquitectura demuestra que ERP-Core y POS no requieren modificaciones.
- El diseño de prueba cubre carga de 8x, caída de PayKlik, timeouts de LogiExpress, reintentos hacia POS y reconstrucción auditable.
- La estimación de inversión y operación respeta R2 o documenta la aprobación necesaria.
- El equipo interno confirma que puede desplegar, observar y mantener la alternativa con capacitación razonable.

#### Drivers relacionados

CUN-01, CUN-03 a CUN-06, CUN-08, CUN-11 y CUN-12; EaC-03 a EaC-12; R1 a R7 y R9.

### 10.3 ADRs pendientes de redactar

- **ADR-002:** publicación confiable y registro auditable. Debe comparar Outbox, Event Store y otras alternativas; no tratarlas como si fueran el mismo patrón.
- **ADR-003:** aislamiento de fallas y continuidad de órdenes ante PayKlik.
- **ADR-004:** entrega confiable y estrategia de reintentos hacia LogiExpress, POS y ERP-Core.
- **ADR-005:** coordinación y estados del ciclo de vida de la orden; comparar Saga orquestada con alternativas y justificar compensaciones.
- **ADR pendiente de numeración:** observabilidad de sistemas externos y operación de colas.
- **ADR pendiente de numeración:** notificaciones no bloqueantes.

---

## 11. Vistas Arquitectónicas
**Estado:** Borrador completo; pendiente de validación del equipo y de la decisión final sobre ADR-001 | **Dueño:** Persona 4 (Rol D)

Las cuatro vistas se generan desde fuentes Graphviz versionables y se publican en SVG para conservar legibilidad al ampliar. Se declara **UML 2.5.1 simplificado** para Componentes y Despliegue: los estereotipos, fronteras, interfaces y tipos de relación se indican explícitamente. Contexto y Bloques emplean notación complementaria declarada, apropiada para comunicar la frontera del sistema y la visión ejecutiva. La guía de notación, los comandos de regeneración y las decisiones abiertas se conservan en [`Docs/diagramas/README.md`](./diagramas/README.md).

### 11.1 Diagrama de Contexto

![Diagrama de Contexto de SGO-Logic](./diagramas/contexto.svg)

[Fuente versionable](./diagramas/contexto.dot) | [PNG para presentación](./diagramas/contexto.png)

La frontera central identifica SGO-Logic como capa de orquestación e integración. A la izquierda se separan usuarios, canales y áreas organizacionales; a la derecha aparecen las seis entidades externas existentes. ERP-Core, POS y proveedores se integran sin presentarlos como componentes internos ni como sistemas que deban modificarse.

**Criterios de aceptación:**

- Delimita claramente SGO-Logic como sistema de interés y mantiene fuera de la frontera a ERP-Core, POS, PayKlik, LogiExpress, SAT y el proveedor de notificaciones.
- Muestra los cuatro canales, sus usuarios principales y las áreas de TI, Finanzas, Logística, Dirección, Gerencia y Comercialización.
- Rotula propósito y dirección; solo afirma REST para PayKlik y LogiExpress, CSV/XML para ERP-Core, XML para SAT y eventos hacia POS donde existe evidencia en el caso.
- Marca como pendiente de definición el contrato del canal tienda, en vez de inventarlo.

### 11.2 Diagrama de Componentes

![Diagrama de Componentes de SGO-Logic](./diagramas/componentes.svg)

[Fuente versionable](./diagramas/componentes.dot) | [PNG para presentación](./diagramas/componentes.png)

La vista materializa el ADR-001 **mientras permanezca propuesto**: un núcleo modular desplegable, una API omnicanal y una capa de puertos, adaptadores y mensajería. Cada módulo declara el propietario lógico de sus datos. La publicación confiable aparece como responsabilidad explícita, pero su mecanismo se deja sujeto a ADR-002 para no confundir Outbox, Event Store u otras alternativas.

**Criterios de aceptación:**

- Representa Carrito, Órdenes y Coordinación, Inventario y Reservas, Pagos, Facturación, Despacho, Postventa, Notificaciones y Auditoría/Consulta.
- Distingue contratos síncronos con línea sólida y mensajes/eventos asíncronos con línea discontinua; las políticas transversales se muestran con línea punteada.
- Señala propiedad lógica de carrito, orden, reserva, intento de pago, factura, despacho, devolución, notificación y evidencia; prohíbe acceso directo a datos internos de otro módulo.
- Ubica explícitamente publicación confiable, broker, idempotencia y los seis adaptadores externos requeridos.

### 11.3 Diagrama de Despliegue

![Diagrama de Despliegue de SGO-Logic](./diagramas/despliegue.svg)

[Fuente versionable](./diagramas/despliegue.dot) | [PNG para presentación](./diagramas/despliegue.png)

El contorno azul representa el despliegue inicial **on-premises**. El número de instancias, los productos y la redundancia permanecen abiertos hasta contar con ADR y evidencia de pruebas. El contorno verde discontinuo muestra únicamente una ruta futura a nube; no afirma que esa infraestructura exista en la primera etapa.

| Driver de calidad | Elemento visible en la vista |
|---|---|
| EaC-01 — consulta ≤ 5 s | Entrada segura y ruta al artefacto SGO-Logic |
| EaC-02 — evidencia < 1 s | Registro y lectura de auditoría con objetivo explícito |
| EaC-03 — demanda 8x | Instancias `N` dimensionadas por prueba de carga y observabilidad de capacidad |
| EaC-04 — falla de PayKlik | Timeout, Circuit Breaker, intento persistido, broker y conciliación/reintento |
| EaC-05 — timeout de LogiExpress | Adaptador con retry/backoff, cola y mensajes fallidos |
| EaC-06 — entrega a POS | Broker, trabajador de integración, reintento y entrega eventual |
| EaC-07 — auditabilidad | Registro de quién, cuándo y qué cambió |
| EaC-08 — falla de notificaciones | Cola y consumidor no bloqueante |
| EaC-09 — mantenibilidad | Un desplegable modular y operación/telemetría centralizada para el equipo interno |
| EaC-10 — costo | Nota de validación contra el límite de USD 1,200 mensuales, sin inventar capacidad |
| EaC-11 — evolución | Ruta futura a nube separada mediante contorno discontinuo |
| EaC-12 — datos de pago | Adaptador de pago y restricción de no persistir PAN completo ni CVV; controles concretos por decidir |

**Criterios de aceptación:**

- Distingue canales, entrada segura, entorno de ejecución, artefactos, base operacional, auditoría, broker, mensajes fallidos, intercambio ERP y observabilidad.
- Hace visibles los doce EaC sin convertir medidas abiertas en afirmaciones inventadas.
- Mantiene protocolos conocidos y marca como pendientes la selección de productos, redundancia, cifrado, retención y contrato del canal tienda.
- No fija réplicas, capacidad, RTO, RPO ni disponibilidad; `N` debe resultar de la validación de carga a ocho veces el tráfico normal.

### 11.4 Diagrama de Bloques

![Diagrama de Bloques de SGO-Logic](./diagramas/bloques.svg)

[Fuente versionable](./diagramas/bloques.dot) | [PNG para presentación](./diagramas/bloques.png)

Esta vista resume para el comité cómo la experiencia omnicanal atraviesa la entrada unificada, el núcleo modular y la integración confiable con legados y proveedores. Auditoría, seguridad y observabilidad aparecen como confianza transversal; el despliegue local y la ruta a nube se muestran como operación evolutiva. El resultado se expresa en términos de continuidad de ingresos, cumplimiento fiscal, reducción de errores logísticos y experiencia consistente.

**Criterios de aceptación:**

- Mantiene el estilo propuesto visible sin presentar productos tecnológicos ni topología física detallada.
- Relaciona capacidades con continuidad de ingresos, trazabilidad fiscal, reducción de errores de despacho, experiencia consistente y evolución sin romper legados.
- Conserva la misma frontera: los sistemas legados y proveedores se integran y no se reemplazan.
- Usa el mismo núcleo modular, la misma capa de integración y los mismos controles transversales de las otras tres vistas.

---

## 12. Prototipo / Mockups
**Estado:** Borrador completo; pendiente de contrastar contra los CDU expandidos de Persona 2 | **Dueño:** Persona 5 (Rol E)

Wireframes no funcionales del flujo principal: iniciar compra en la app móvil → continuar y pagar en la web → consultar estado desde cualquier canal → generar una devolución desde el centro de atención. Cada pantalla anota el CUN y el EaC que ilustra, para mantener trazabilidad con las secciones 2, 6 y 9.

[`Docs/prototipo/mockups.html`](./prototipo/mockups.html) — abrir directamente en el navegador. Detalle en [`Docs/prototipo/README.md`](./prototipo/README.md).

**Criterios de aceptación:**

- No contiene lógica, datos reales ni backend; es explícitamente un mockup de alto nivel.
- Cubre el flujo completo exigido por el enunciado en un solo recorrido (carrito → pago → consulta → devolución).
- Cada pantalla referencia al menos un CUN y, cuando aplica, el EaC que motiva su diseño (ej. mensaje de pago pendiente para EaC-04).
- Pendiente: revisar consistencia con los CDU expandidos de la sección 5 una vez estén completos, y decidir si se sustituyen los datos de ejemplo.

---

## 13. Anexos

### 13.1 Glosario
| Término | Definición |
|---|---|
| CUN | Caso de Uso de Negocio |
| CDU | Caso de Uso de Sistema (expandido) |
| EaC | Escenario de Atributo de Calidad |
| ADR | Architecture Decision Record |
| Stakeholder | Persona, grupo u organización con intereses sobre el sistema o capacidad de afectar sus decisiones |
| Entidad externa | Sistema o servicio fuera de la frontera de SGO-Logic con el que existe una integración |
| Driver arquitectónico | Requisito funcional, atributo de calidad o restricción que influye de manera significativa en la arquitectura |
| Puerto y adaptador | Enfoque que aísla el núcleo de una tecnología o sistema externo mediante contratos y adaptadores reemplazables |
| Outbox | Patrón de publicación confiable que registra el cambio de negocio y el mensaje saliente en una misma transacción local |
| Event Store | Almacén de eventos que conserva la secuencia de cambios como fuente de estado; no es sinónimo de Outbox |
| Circuit Breaker | Mecanismo que interrumpe temporalmente llamadas a una dependencia que está fallando para evitar fallas en cascada |
| Saga | Coordinación de una transacción de negocio distribuida mediante pasos y acciones de compensación |

### 13.2 Referencias

- [Enunciado de la Fase 1](../Enunciado_Proyecto_Fase_1_SGO-Logic.md)
- [Análisis arquitectónico y plan de trabajo](../SGO-Logic_Analisis_y_Plan_Fase1.md)
- [Plan de acción de Persona 1](../Persona1_Plan_de_Accion_SGO-Logic.md)
- [Tablero Kanban de la Fase 1](./Tablero_Kanban_Fase1.md)
- [Arquitectura de Software — Unidad 1](./Unidad1MD/Arquitectura%20de%20Software_convertido.md)
- [Arquitectura de Software 2 — Unidad 1](./Unidad1MD/Arquitectura%20de%20Software2_convertido.md)
- [Identificación de Casos de Uso de Negocio — Unidad 1](./Unidad1MD/NT%20Identificación%20de%20CDU%20de%20Negocio_convertido.md)
- [Modelado de Drivers Funcionales — Unidad 1](./Unidad1MD/CDU%20Negocio%20-%20Modelado%20de%20Drivers%20RF_convertido.md)

### 13.3 Bitácora de acuerdos y validaciones

| Elemento | Estado actual | Responsable de confirmar | Evidencia de cierre esperada |
|---|---|---|---|
| Alcance y frontera de SGO-Logic | Borrador completo | Todo el equipo, facilitado por Persona 1 | Acuerdo registrado y ausencia de objeciones abiertas |
| Clasificación de stakeholders, entidades externas y preocupaciones | Borrador completo | Todo el equipo | Tablas validadas; altas, bajas o agrupaciones justificadas |
| Características y drivers priorizados | Borrador completo | Personas 1 y 3 + equipo | Priorización aprobada con valor, riesgo, urgencia e impacto |
| ADR-001 | Propuesto | Persona 3 + equipo | Estado cambiado a Aceptado o decisión alternativa documentada |
| Matrices de trazabilidad | Precargadas | Persona 1, con insumos de Personas 2, 3 y 4 | ADR y rutas finales de vistas incorporados; mecanismos de validación revisados |
| Vistas de Contexto, Componentes, Despliegue y Bloques | Borrador completo | Personas 2 y 3 + equipo | Actores, EaC y ADR contrastados; observaciones resueltas y vistas aprobadas |

### 13.4 Responsabilidades, dependencias y bloqueos

Esta sección representa el estado observable en este repositorio. Una **dependencia** indica que un artefacto final necesita otro insumo; un **bloqueo** existe cuando ya no queda trabajo preliminar útil que pueda realizarse sin ese insumo. Los responsables deben actualizar el estado cuando exista evidencia nueva.

| Integrante | Responsabilidad principal | Evidencia disponible | Trabajo que puede realizar ahora | Pendiente para cerrar | Depende de |
|---|---|---|---|---|---|
| Persona 1 — Facilitación y Trazabilidad | Alcance, stakeholders, priorización, matrices, coherencia del DDA, Kanban y repositorio | Alcance, clasificación de stakeholders, características, priorización, matrices precargadas y borrador de ADR-001 | Coordinar validaciones, mantener el tablero, registrar acuerdos, revisar formato y preparar estructura de exposición | Cerrar matrices con artefactos reales y ejecutar revisión final de coherencia | Persona 2: CUN/CDU; Persona 3: EaC/ADR; Persona 4: vistas; equipo: aprobación de alcance y prioridades |
| Persona 2 — Negocio y Drivers Funcionales | Actores, CUN, primera descomposición y CDU expandidos | Lista preliminar de 12 CUN, macroprocesos, plantilla CDU y ejemplo CDU-03 | Elaborar el diagrama CUN, validar actores y expandir CDU priorizados | Diagramas CUN/CDU definitivos y relación CDU–EaC | Equipo: validación del alcance; Persona 3: coordinación de EaC asociados |
| Persona 3 — Calidad, Restricciones y ADR | EaC completos, restricciones y decisiones arquitectónicas | Resumen de 12 EaC, ejemplo EaC-04, restricciones y borrador de ADR-001 preparado por Persona 1 | Formalizar EaC prioritarios, revisar restricciones y decidir sobre ADR-001 | EaC completos; ADR-001 aceptado o sustituido; ADR-002 a ADR-005 y decisiones de observabilidad/notificaciones | Equipo: priorización; Personas 2 y stakeholders: aclaraciones de negocio; TI/Finanzas: medidas abiertas |
| Persona 4 — Modelado y Vistas | Contexto, Componentes, Despliegue y Bloques | Cuatro diagramas incorporados en SVG y PNG, fuentes `.dot`, notación declarada, correspondencia EaC-despliegue y decisiones abiertas documentadas | Coordinar revisión cruzada con Personas 2 y 3 y ajustar las vistas si cambian actores, EaC o ADR | Aprobación del equipo y sustitución de supuestos por decisiones aceptadas | Persona 2: validación final de actores/CUN; Persona 3: EaC y ADR aceptados |
| Persona 5 — Documentación, Prototipo y QA | Resumen ejecutivo, ensamblado editorial, mockups, QA y presentación | Esqueleto del DDA y flujo principal indicado; no hay resumen ni prototipo enlazado | Iniciar mockups, plantilla editorial y estructura del guion | Resumen ejecutivo final, prototipo, QA integral y presentación consolidada | Personas 1 a 4: artefactos estabilizados; todo el equipo: ensayo |

#### 13.4.1 Dependencias que requieren seguimiento

| ID | Entregable pendiente | Responsable | Insumo requerido | Proveedor del insumo | Impacto si se retrasa |
|---|---|---|---|---|---|
| DEP-01 | Diagrama de Contexto definitivo | Persona 4 | Actores y relaciones confirmados | Persona 2 | Retrasa vista de contexto, trazabilidad y consolidación |
| DEP-02 | Diagrama de Componentes definitivo | Persona 4 | ADR-001 aceptado y contratos funcionales principales | Personas 2 y 3 | Retrasa diseño de despliegue y validación de modularidad |
| DEP-03 | Diagrama de Despliegue definitivo | Persona 4 | EaC formalizados y ADR de resiliencia/integración | Persona 3 | Impide demostrar que los atributos de calidad se reflejan en despliegue |
| DEP-04 | Matrices de trazabilidad definitivas | Persona 1 | CDU, EaC, ADR y rutas de diagramas definitivos | Personas 2, 3 y 4 | Impide demostrar cobertura completa de drivers |
| DEP-05 | QA y DDA consolidado | Persona 5 | Secciones y vistas estabilizadas | Personas 1, 2, 3 y 4 | Concentra errores e inconsistencias cerca de la entrega |
| DEP-06 | Presentación y defensa final | Persona 5, coordinada por Persona 1 | DDA, vistas, decisiones y prototipo consolidados | Todo el equipo | Impide ensayar argumentos y distribuir tiempos con evidencia real |

#### 13.4.2 Mapa resumido de dependencias

```text
Persona 1: alcance y priorización validados
    |-- habilita a Persona 2: CUN y CDU
    `-- habilita a Persona 3: EaC y ADR

Persona 2: actores, CUN y CDU -----> Persona 4: Contexto y Componentes
Persona 3: EaC y ADR --------------> Persona 4: Componentes y Despliegue

Persona 2 + Persona 3 + Persona 4 -> Persona 1: cierre de trazabilidad
Persona 1 + Persona 2 + Persona 3 + Persona 4 -> Persona 5: QA y consolidación
Todo el equipo --------------------> Presentación y defensa
```

### 13.5 Criterios de cierre de Persona 1

| Tarea de Persona 1 | Estado actual | Acción necesaria | Dependencia para cerrar |
|---|---|---|---|
| Delimitar alcance y sistema de interés | Borrador completo | Facilitar aprobación y registrar fecha, participantes, observaciones y resultado | Todo el equipo |
| Identificar stakeholders, entidades externas y preocupaciones | Borrador completo | Validar agrupaciones y corregir altas o bajas justificadas | Todo el equipo |
| Priorizar características y drivers | Borrador completo | Confirmar el orden y documentar desacuerdos o cambios | Persona 3 y todo el equipo |
| Formalizar restricciones en el DDA | Borrador completo | Obtener revisión técnica y aceptación del inventario | Persona 3 |
| Preparar ADR-001 | Borrador propuesto completo | Entregar para revisión; registrar aceptación, modificación o sustitución | Persona 3 y equipo |
| Precargar matrices de trazabilidad | Completado | Mantenerlas sincronizadas mientras cambien drivers y decisiones | Sin dependencia para la precarga |
| Cerrar matrices de trazabilidad | Pendiente | Sustituir decisiones y vistas previstas por ADR aprobados y enlaces reales; comprobar cobertura bidireccional | Personas 2, 3 y 4 |
| Mantener el tablero Kanban | Estructura documental preparada | Crear o actualizar el tablero en la herramienta elegida y mover tarjetas únicamente con evidencia | Avances reportados por todo el equipo |
| Mantener coherencia del DDA | En progreso continuo | Revisar vocabulario, IDs, referencias, estados y contradicciones en cada integración | Personas 2, 3, 4 y 5 |
| Coordinar revisión y ensayo | Pendiente | Aplicar checklist final, distribuir tiempos y verificar que todos puedan defender cualquier sección | DDA y prototipo consolidados |

Persona 1 termina su responsabilidad cuando el alcance y la priorización están aprobados, las matrices apuntan a artefactos reales, no existen contradicciones conocidas entre drivers, ADR y vistas, los bloqueos están visibles y la revisión final queda registrada. La edición visual del documento corresponde a Persona 5; la responsabilidad sobre coherencia y trazabilidad permanece en Persona 1.

### 13.6 Estructura preliminar de la presentación de 45 minutos

Esta distribución puede prepararse antes de finalizar el contenido. Los nombres reales deben sustituir los roles cuando el equipo confirme el orden.

| Tiempo | Responsable principal | Contenido | Evidencia esperada |
|---|---|---|---|
| 0–3 min | Persona 1 | Apertura, objetivo, alcance y agenda | Frontera de SGO-Logic y propósito de la propuesta |
| 3–10 min | Persona 2 | Situación actual, actores, CUN y CDU priorizados | Diagrama CUN y ejemplo de CDU trazado |
| 10–16 min | Persona 1 | Stakeholders, preocupaciones, priorización y trazabilidad | Matrices y criterios de prioridad |
| 16–24 min | Persona 3 | EaC, restricciones y decisiones arquitectónicas | Escenarios medibles y ADR aceptados |
| 24–34 min | Persona 4 | Contexto, Componentes, Despliegue y Bloques | Cuatro vistas coherentes y relación con EaC |
| 34–40 min | Persona 5 | Prototipo y recorrido omnicanal | Flujo principal de los mockups |
| 40–45 min | Persona 5 con cierre de Persona 1 | Consecuencias, riesgos, próximos pasos y conclusión ejecutiva | Trade-offs, preguntas abiertas y mensaje de valor |

Para la ronda de preguntas de cinco minutos, cada integrante debe poder explicar al menos un driver, una decisión, una vista relacionada y la consecuencia de cambiarla.
