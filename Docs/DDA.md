# Plantilla Documento de Definición de Arquitectura (DDA)
## SGO-Logic — Sistema de Gestión de Órdenes y Logística Omnicanal · MegaStore

| Campo | Valor |
|---|---|
| Curso | Análisis y Diseño de Sistemas 2A |
| Grupo | `AYD2_<seccion>_2S2025_PROYECTO_G<#>` |
| Fase | 1 |
| Versión | 0.1 (borrador) |
| Última actualización | `DD/MM/AAAA` |
| Responsable de esta versión | `Nombre` |

> Leyenda de estado:  Completo ·  En progreso ·  Pendiente
> Cada sección indica quién es el dueño principal según el reparto de trabajo del equipo.

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
**Estado:**  Pendiente · **Dueño:** Persona 5 (Rol E)

> Párrafo de 150–250 palabras: qué es MegaStore, cuál es el problema, qué propone SGO-Logic, y una síntesis de la arquitectura elegida. Se escribe **al final**, cuando ya existe el resto del documento — no antes.

`[TODO: redactar]`

---

## 2. Identificación del Caso de Negocio
**Estado:**  En progreso · **Dueño:** Persona 2 (Rol B)

### 2.1 Diagrama de Casos de Uso de Negocio (CUN) — alto nivel
`[TODO: insertar imagen del diagrama, ej. ![CUN](./diagramas/cun-alto-nivel.png)]`

### 2.2 Primera descomposición (macro-procesos)
Agrupar los CUN en procesos de negocio principales:

| Macro-proceso | CUN incluidos |
|---|---|
| Venta omnicanal | Gestionar carrito omnicanal, Verificar y reservar inventario, Procesar pago |
| Postventa | Gestionar devoluciones y reembolsos |
| Cumplimiento fiscal | Emitir documentación fiscal, Sincronizar venta con ERP |
| Logística | Gestionar despacho, Integrar con LogiExpress, Notificar eventos a POS |
| Atención al cliente | Consultar estado de orden, Notificar al cliente |
| Gobernanza | Auditar operaciones |

### 2.3 Lista de CUN identificados (insumo para 2.1)
1. Gestionar carrito omnicanal
2. Verificar y reservar inventario
3. Procesar pago
4. Emitir documentación fiscal
5. Sincronizar venta con ERP
6. Notificar eventos a POS
7. Gestionar despacho
8. Integrar con LogiExpress
9. Consultar estado de orden
10. Gestionar devoluciones y reembolsos
11. Notificar al cliente
12. Auditar operaciones

`[TODO: convertir 2.3 en el diagrama real de 2.1 y completar la descomposición si aplica una tercera categoría]`

---

## 3. Stakeholders y Preocupaciones Arquitectónicas
**Estado:**  Completo (validar formato) · **Dueño:** Persona 1 (Rol A), con insumo de todo el equipo

| # | Stakeholder | Tipo | Preocupación principal | Poder de decisión |
|---|---|---|---|---|
| 1 | Carlos Méndez — Gerente de TI | Humano | Continuidad operativa, escalabilidad en Black Friday, mantenibilidad interna | Alto (veto técnico) |
| 2 | Ana Lucía Fuentes — Directora de Finanzas | Humano | Trazabilidad total, cumplimiento fiscal SAT | Alto (veto explícito) |
| 3 | Roberto Quezada — Jefe de Logística | Humano | Reducir errores de despacho 70%, visibilidad de órdenes | Medio |
| ... | `[completar con los 18 identificados en el análisis previo]` | | | |

`[TODO: pegar aquí la tabla completa de 18 stakeholders ya trabajada]`

---

## 4. Características del Sistema Priorizadas
**Estado:**  Pendiente · **Dueño:** Persona 1 (Rol A) + Persona 3 (Rol C)

> Lista corta (5–8 ítems) de las capacidades/cualidades que definen el "éxito" del sistema desde la perspectiva del negocio, en lenguaje no técnico. Se deriva de los drivers priorizados en la sección 8, pero se escribe para audiencia ejecutiva.

**Ejemplo de formato:**
| Característica | Por qué importa al negocio |
|---|---|
| El sistema nunca pierde una venta aunque falle la pasarela de pago | Evita repetir la pérdida de $150,000 del Black Friday pasado |
| Toda operación es rastreable en menos de 1 segundo | Evita multas y observaciones en auditorías de la SAT |

`[TODO: completar lista]`

---

## 5. Drivers — Requisitos Funcionales
**Estado:**  En progreso · **Dueño:** Persona 2 (Rol B)

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
Actor(es): Cliente, Pasarela de pago (PayKlik), Sistema de auditoría
Descripción breve: Permite cobrar una orden confirmada usando el método de pago
  elegido por el cliente, garantizando que la orden no se pierda si PayKlik falla.
Precondiciones:
  - El carrito fue confirmado y el inventario está reservado
Flujo principal:
  1. El cliente selecciona método de pago (tarjeta, transferencia, billetera digital)
  2. El sistema envía la solicitud de cobro a PayKlik
  3. PayKlik responde con el resultado (aprobado/rechazado)
  4. El sistema registra el intento (orden, canal, fecha, resultado) para auditoría
  5. Si es aprobado, se confirma la orden y se dispara el evento de facturación
Flujos alternativos:
  A1. El cliente cambia de método de pago antes de confirmar
Flujo de excepción:
  E1. PayKlik no responde o supera el umbral de latencia:
      el sistema activa el circuit breaker, encola el intento y notifica
      al cliente que el pago está en proceso, sin perder la orden
Postcondiciones:
  - La orden queda en estado "Pagada" o "Pago pendiente de reintento"
  - Existe un registro auditable del intento de cobro
Relacionado con EaC: EaC-4 (resiliencia de pago), EaC-7 (trazabilidad)
```

### 5.3 CDU pendientes de expandir
`[TODO: repetir la plantilla 5.1 para los CDU priorizados: Gestionar carrito, Verificar inventario,
Consultar estado de orden, Gestionar despacho, Gestionar devoluciones, y los restantes según tiempo disponible]`

---

## 6. Drivers de Calidad (EaC)
**Estado:**  En progreso · **Dueño:** Persona 3 (Rol C)

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
Respuesta: El sistema detecta la falla mediante un circuit breaker, encola el intento
  para reintento posterior y no bloquea ni pierde la orden del cliente
Medida de respuesta: 0% de órdenes perdidas por falla del proveedor de pago;
  reintento exitoso o notificación al cliente en menos de 60 segundos
Reflejado en diagrama de despliegue como: Componente "Circuit Breaker" + cola de
  reintentos entre el Módulo de Pago y el conector externo hacia PayKlik
```

### 6.3 Tabla resumen de EaC identificados (completar cada uno con 6.1)
| ID | Atributo | Resumen del escenario | Medida |
|---|---|---|---|
| EaC-01 | Rendimiento | Cliente consulta estado de orden | ≤ 5 s |
| EaC-02 | Rendimiento | Finanzas recupera evidencia de transacción | < 1 s |
| EaC-03 | Escalabilidad | Demanda Black Friday | Soportar 8x tráfico normal |
| EaC-04 | Disponibilidad/Resiliencia | Falla de PayKlik | No perder la orden *(ver ejemplo arriba)* |
| EaC-05 | Confiabilidad | Timeout de LogiExpress (2%) | Reintento con backoff |
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
 **Dueño:** Persona 3 (Rol C)

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

---

## 8. Priorización de Drivers
**Estado:** **Dueño:** Persona 1 (Rol A)

| Prioridad | Driver | Valor | Riesgo | Urgencia | Impacto arquitectónico |
|---|---|---|---|---|---|
| 1 | EaC-04 (resiliencia de pagos) | Alto | Alto | Alta | Alto |
| 2 | EaC-03 (escalabilidad 8x) | Alto | Alto | Alta | Alto |
| 3 | R1 (no tocar ERP/POS) | Alto | Medio | Alta | Alto |
| 4 | EaC-07 (trazabilidad) | Alto | Alto (veto) | Media | Alto |
| 5 | CUN-01 (carrito omnicanal) | Alto | Medio | Media | Medio |

`[TODO: completar el resto de drivers de la lista de priorización]`

---

## 9. Matriz de Trazabilidad
**Estado:** 🔲 Pendiente · **Dueño:** Persona 1 (Rol A)

| Stakeholder | Preocupación | Driver relacionado | Decisión/ADR | Vista donde aparece | Mecanismo de validación |
|---|---|---|---|---|---|
| Ana Lucía Fuentes | Trazabilidad fiscal | EaC-07 | ADR-002 (Outbox/Event Store) | Diagrama de Componentes, Despliegue | Prueba: reconstrucción de una transacción en < 1s |
| Carlos Méndez | Escalabilidad Black Friday | EaC-03 | ADR-001 (arquitectura general) | Diagrama de Despliegue | Prueba de carga simulando 8x tráfico |
| `[...]` | | | | | |

`[TODO: completar una fila por cada driver de la sección 8, mínimo]`

---

## 10. Decisiones Arquitectónicas (ADRs)
**Estado:**  En progreso · **Dueño:** Persona 3 (Rol C)

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

### 10.2 Ejemplo resuelto — ADR-001

```
# ADR-001: Estilo arquitectónico general de SGO-Logic

## Estado
Propuesto

## Contexto
MegaStore no puede modificar ERP-Core ni los POS (R1), tiene un equipo con
experiencia en Java/.NET y Docker básico (R5), debe operar on-premises al
inicio (R6) y necesita resistir 8x demanda en Black Friday (EaC-03) sin
perder ventas si falla el proveedor de pago (EaC-04).

## Decisión
Se adopta un monolito modular (bounded contexts: Carrito, Pago, Inventario,
Despacho, Facturación) con una capa de integración asíncrona basada en
mensajería, que desacopla SGO-Logic de todos los sistemas externos y legacy
(PayKlik, LogiExpress, POS, ERP-Core, proveedor de notificaciones).

## Alternativas consideradas
1. Monolito modular simple (sin capa de eventos) — más simple, pero no
   resuelve bien EaC-04/EaC-05 porque las llamadas a proveedores externos
   seguirían siendo síncronas
2. Microservicios completos — mejor escalado independiente, pero alto
   costo de adopción para el nivel de experiencia del equipo (R5) y riesgo
   alto en un cronograma corto
3. Monolito modular + capa de integración por eventos (elegida) — balance
   entre mantenibilidad (R5, R9) y resiliencia (EaC-04, EaC-05, EaC-06)

## Consecuencias
Positivas:
- Permite Circuit Breaker y colas de reintento hacia proveedores externos
- Los bounded contexts facilitan una futura migración a microservicios
- Alcanzable por el equipo dado su stack conocido

Negativas / riesgos aceptados:
- El equipo debe aprender conceptos de mensajería asíncrona (curva de
  aprendizaje menor que microservicios, pero no nula)
- Requiere disciplina para no acoplar los módulos internos entre sí

## Drivers relacionados
EaC-03, EaC-04, EaC-05, EaC-06, EaC-07, EaC-11, R1, R5, R6
```

### 10.3 ADRs pendientes de redactar
`[TODO: ADR-002 Outbox/Event Store para trazabilidad, ADR-003 Circuit Breaker hacia PayKlik,
ADR-004 estrategia de reintentos hacia LogiExpress/POS, ADR-005 State pattern + Saga para
ciclo de vida de la orden]`

---

## 11. Vistas Arquitectónicas
**Estado:** 🔲 Pendiente · **Dueño:** Persona 4 (Rol D)

### 11.1 Diagrama de Contexto
`[TODO: insertar imagen]`
Descripción: `[cómo SGO-Logic interactúa con clientes, personal interno y sistemas externos]`

### 11.2 Diagrama de Componentes
`[TODO: insertar imagen]`
Descripción: `[módulos internos, relaciones e interfaces, según ADR-001]`

### 11.3 Diagrama de Despliegue
`[TODO: insertar imagen]`
Descripción: `[artefactos físicos; debe mostrar explícitamente los EaC — circuit breaker, colas, reintentos]`

### 11.4 Diagrama de Bloques
`[TODO: insertar imagen]`
Descripción: `[vista ejecutiva del estilo arquitectónico completo, para stakeholders no técnicos]`

---

## 12. Prototipo / Mockups
**Estado:** 🔲 Pendiente · **Dueño:** Persona 5 (Rol E)

- Flujo principal a mostrar: iniciar compra en un canal → completar en otro → consultar estado → generar devolución
- `[TODO: enlazar mockups, ej. Figma link]`

---

## 13. Anexos

### 13.1 Glosario
| Término | Definición |
|---|---|
| CUN | Caso de Uso de Negocio |
| CDU | Caso de Uso de Sistema (expandido) |
| EaC | Escenario de Atributo de Calidad |
| ADR | Architecture Decision Record |
| `[...]` | |

### 13.2 Referencias
- Enunciado del proyecto: `Proyecto_Fase_1.pdf`
- `[otras fuentes que usen: libros, documentación de patrones, etc.]`