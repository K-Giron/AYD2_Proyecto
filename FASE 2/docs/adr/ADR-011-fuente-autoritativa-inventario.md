# ADR-011 — Fuente lógica autoritativa de inventario para el MVP

- **Fecha:** 2026-09-22
- **Estado:** Proposed
- **Responsable del borrador:** Jorge Pumay
- **Aprobado por:** Kevin Girón (2026-10-14)
- **Aprobación pendiente:** Jairo Gómez, Edson Sosa y Andy Sánchez
- **Resuelve:** DEC-03 del Gate 0 (Planificación Fase 2)

## Contexto

El DDA (§2.2, CDU-02, EaC-03) exige que SGO-Logic verifique disponibilidad y reserve inventario de forma atómica para evitar el doble compromiso de una misma unidad, consolidando de forma lógica las existencias de 2 centros de distribución y 32 tiendas físicas. El DDA declaró explícitamente en §2.6 ("Fuente autoritativa y mecanismo de actualización del inventario") que la fuente física real queda fuera de la Fase 1 y debe confirmarse con Negocio y Logística antes de producción; solo exige que exista una fuente lógica única y reserva atómica.

Para la Fase 2, el enunciado pide un MVP funcional y reproducible en Docker Compose, no una integración con un sistema de inventario físico real de MegaStore, que no existe en este contexto académico. El módulo `inventory` (ADR-001, capacidad de negocio "Inventario y Reservas"; ADR-009, backend modular con Spring Modulith) necesita una fuente de datos concreta para implementar CDU-02 y demostrar EaC-03 antes de que BB-03 (modelo relacional) pueda construirse.

## Drivers relacionados

- CUN-02 / CDU-02: verificar y reservar inventario, evitando doble compromiso.
- EaC-03: escalabilidad 8x sin degradar EaC-01/EaC-02.
- EaC-07: auditabilidad de cada reserva y liberación.
- EaC-09: mantenibilidad con el stack acordado en ADR-009.
- R1: no modificar ni sustituir ningún sistema legado (aquí no se integra ninguno, por lo que R1 se cumple trivialmente).
- Pregunta abierta del DDA §2.6 sobre la fuente autoritativa de inventario.

## Decisión propuesta

Para el MVP de Fase 2, el **módulo `inventory` de SGO-Logic es la única fuente lógica autoritativa de inventario**. No se integra con ningún sistema externo de inventario real:

- El esquema lógico `inventory` (propiedad exclusiva del módulo `inventory`, según ADR-001 y §8 de STACK_TECNOLOGICO_FASE_2) contiene el stock disponible y reservado por producto y por ubicación.
- Se cargan **datos semilla simulados para 34 ubicaciones** (2 centros de distribución + 32 tiendas) mediante migración Flyway versionada (BB-03), representando existencias iniciales razonables para demostrar el camino feliz y los escenarios de concurrencia de INV-02/INV-05.
- Toda verificación y reserva de inventario (CDU-02) se resuelve contra esta única fuente lógica, dentro de una transacción atómica en PostgreSQL, sin depender de ningún sistema externo.
- Otros módulos (`orders`, `logistics`) consultan disponibilidad exclusivamente a través del puerto público del módulo (`InventoryPort` / `api` del módulo `inventory`), nunca accediendo a sus tablas o repositorios internos, conforme a la regla de dependencias de §7 de STACK_TECNOLOGICO_FASE_2.
- Esta decisión es una simplificación explícita del MVP: no reemplaza ni cierra la pregunta abierta del DDA sobre cuál será el sistema de registro físico definitivo en producción; esa decisión sigue siendo responsabilidad de Negocio y Logística según lo declarado en el DDA.

## Alternativas evaluadas

1. **Módulo `inventory` como fuente lógica única con datos semilla simulados (propuesta):** permite demostrar reserva atómica, prevención de doble compromiso y escalabilidad sin inventar una integración externa inexistente; coherente con el alcance académico y no viola R1 porque no se integra ningún sistema. Limita la fidelidad frente al mecanismo real de actualización física que tendría MegaStore en producción.
2. **Simular una integración con un sistema externo de inventario ficticio (tipo ERP/WMS):** inventa un contrato y un sistema que el DDA nunca definió ni autorizó (§2.6 lo deja expresamente abierto); añade complejidad y riesgo de penalización por introducir algo no trazable al DDA. Descartada.
3. **Consultar cada ubicación por separado en tiempo real sin fuente única:** contradice directamente CDU-02 ("fuente autoritativa de inventario") y hace imposible garantizar la reserva atómica exigida por EaC-03. Descartada.

## Ventajas

- Desbloquea BB-03, INV-01, INV-02 e INV-04 sin depender de decisiones pendientes fuera del alcance de la Fase 2.
- La reserva atómica y la prevención de doble compromiso (CDU-02, EaC-03) se pueden demostrar de forma reproducible en Docker Compose con datos deterministas.
- No viola R1 ni inventa un contrato externo no autorizado por el DDA.

## Consecuencias

- El mecanismo real de sincronización física de inventario entre las 32 tiendas y los 2 centros de distribución de MegaStore queda fuera de este MVP; se documenta como limitación honesta, no como brecha oculta.
- Los datos de las 34 ubicaciones son semilla simulada, no inventario real; deben marcarse claramente como tales en la demo y en la documentación (README, DEMO-01).
- El esquema `inventory` queda como dependencia directa de BB-03; cualquier cambio de su modelo tras el cierre de este ADR requiere revisión conforme a la política de contratos (`contract-status.md`).

## Riesgos

- Que otro módulo intente acceder directamente al esquema `inventory` en lugar de usar el puerto público, rompiendo la propiedad lógica de datos de ADR-001.
- Que la demo presente los datos semilla como si fueran inventario real de MegaStore, generando una afirmación no sustentada por el DDA.
- Que INV-02 se implemente sin transacción atómica real (p. ej. verificar y reservar en dos pasos no atómicos), invalidando la prueba de concurrencia de INV-05.

## Criterios de ratificación

- BB-03 implementa el esquema `inventory` con las restricciones e índices necesarios para soportar reserva atómica.
- INV-01 carga los datos semilla de 34 ubicaciones mediante migración Flyway versionada y documentada.
- INV-02 demuestra, mediante prueba de concurrencia (INV-05), que dos solicitudes simultáneas por la última unidad producen exactamente una reserva confirmada.
- El README y la documentación de demo declaran explícitamente que los datos de inventario son simulados.
- Al menos tres integrantes registran su aprobación antes de que este ADR pase a `Accepted`.

## Actualización Fase 2 (2026-10-13)

La decisión se mantiene **para la reserva atómica** (CDU-02, EaC-03): el
módulo `inventory` sigue siendo la única fuente con la que la Saga reserva,
confirma y libera. Fase 2 agregó, con responsabilidades separadas (ADR-015):

| Fuente | Qué es autoritativa | Cómo se actualiza |
|---|---|---|
| `inventory` (monolito) | Reservas y existencias **vendibles por la web**, en la ubicación `CD-WEB` (almacén propio de la tienda online) | Solo entra mercadería por folio recibido del almacén central (`onlinestore`); sale por venta confirmada o por traslado |
| `inventory-service` (almacén central, base propia) | Existencias del almacén central y **stock propio de cada punto de venta** (POS-01, POS-02…, POS-ONLINE) | Folios de entrada, salida, ajuste y traslado; facturas que suben las tiendas; venta web reflejada por `inventorysync` |

Consecuencias:
- Las 34 ubicaciones sembradas (CD-01, CD-02, ST-01…ST-32) siguen como datos
  simulados para demostrar el algoritmo de ADR-014; la web vende desde `CD-WEB`.
- Ningún sistema lee tablas de otro: el monolito y el central se hablan solo
  por API y eventos, con reintentos y bandeja de fallidos (`inventorysync`).
- La pregunta abierta del DDA §2.6 (sistema físico definitivo) sigue abierta.
  El central es el candidato natural, pero eso lo decide Negocio.

Evidencia: `OnlineStoreIntegrationTest` (recepción → CD-WEB → compra con
reserva; dos compradores concurrentes por la última unidad producen una sola
reserva) e `InventoryConcurrencyIntegrationTest`.

## Referencias

- DDA §2.2 (alcance), §2.6 (fuente autoritativa de inventario, pregunta abierta), CDU-02, EaC-03, EaC-07.
- ADR-001 (monolito modular, propiedad lógica de datos por módulo).
- ADR-009 (stack y topología ejecutable).
- PLANIFICACION_FASE_2.md, DEC-03.
- STACK_TECNOLOGICO_FASE_2.md §7 y §8 (estructura del módulo y propiedad lógica de datos).
