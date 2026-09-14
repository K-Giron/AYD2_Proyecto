# ADR-009 — Stack y topología ejecutable de SGO-Logic

- **Fecha:** 2026-09-14
- **Estado:** Proposed
- **Responsable del borrador:** Jairo Gómez
- **Aprobación pendiente:** Jorge Pumay, Edson Sosa, Andy Sánchez y Kevin Girón

## Contexto

ADR-001 acepta un monolito modular con puertos, adaptadores y mensajería, pero deliberadamente no fija productos. La Fase 2 necesita una base reproducible que el equipo pueda ejecutar localmente sin modificar ERP-Core ni POS y sin introducir todavía funciones que dependen de contratos o módulos ajenos.

## Drivers relacionados

- R1: no modificar ERP-Core ni POS.
- R2 y EaC-10: respetar límites de inversión y operación.
- R5 y EaC-09: usar tecnología mantenible por el equipo.
- R6 y EaC-11: configuración externa y portabilidad desde on-premises.
- EaC-03: permitir validación posterior de carga a `8T`.
- EaC-04 a EaC-08: preparar aislamiento, persistencia y mensajería sin implementar aún esos flujos.

## Decisión propuesta

Construir un único backend JAR con Java y Spring Boot, delimitado mediante Spring Modulith. Usar PostgreSQL como persistencia relacional, RabbitMQ como broker y Docker Compose para el entorno local. Mantener configuración externalizada, imágenes versionadas y módulos propietarios de sus datos. En este incremento el broker solo forma parte del backbone: Outbox, consumidores y políticas de entrega se decidirán e implementarán en sus tarjetas correspondientes.

## Tecnologías y versiones

| Tecnología | Versión fijada |
|---|---|
| Java / Eclipse Temurin | 25; imagen `25.0.4_7` |
| Spring Boot | 4.1.1 |
| Spring Modulith | 2.1.1 |
| Apache Maven | 3.9.11 |
| Maven Wrapper | 3.3.4, scripts compatibles con Maven 3.9.11 |
| PostgreSQL | 17.11 (`postgres:17.11-alpine3.23`) |
| RabbitMQ | 4.3.5 Management (`rabbitmq:4.3.5-management-alpine`) |
| Testcontainers | 2.0.5, administrado por Spring Boot 4.1.1 |
| JUnit Jupiter | 6.0.3, administrado por Spring Boot 4.1.1 |
| AssertJ | 3.27.7, administrado por Spring Boot 4.1.1 |

No se fijan versiones individuales de dependencias administradas por los BOM de Spring Boot o Spring Modulith.

## Topología propuesta

```text
cliente HTTP -> backend Spring Boot modular -> PostgreSQL
                              |
                              +-------------> RabbitMQ
```

Compose contiene `backend`, `postgres` y `rabbitmq` en una red interna. Expone localmente la API, la consola Management y PostgreSQL para desarrollo. En un ambiente real PostgreSQL y AMQP permanecerían internos. El frontend se agregará cuando Kevin entregue la base de UI.

## Alternativas evaluadas

1. **Java/Spring Boot + PostgreSQL + RabbitMQ:** alineado con R5, transacciones relacionales y mensajería requerida; añade operación de broker.
2. **.NET con la misma persistencia y broker:** técnicamente viable y conocido por el equipo, pero dividiría el stack del backend acordado y no aporta una ventaja demostrada para este MVP.
3. **Microservicios desde el inicio:** permite despliegue independiente, pero aumenta coordinación distribuida, costo y operación; ADR-001 ya lo descarta para la primera etapa.
4. **Base o broker embebidos para desarrollo:** simplifican el arranque aislado, pero reducen fidelidad y contradicen la verificación con PostgreSQL/RabbitMQ reales.

## Ventajas

- Un artefacto y un comando de entorno local.
- Límites modulares verificables automáticamente.
- Stack conocido y portable mediante contenedores.
- Pruebas de integración sobre los mismos motores elegidos.

## Consecuencias

- El equipo debe operar PostgreSQL y RabbitMQ incluso en desarrollo integrado.
- La modularidad exige disciplina: no se permiten accesos a repositorios, entidades o adaptadores ajenos.
- El único desplegable puede requerir escalado conjunto; la capacidad real se decidirá después de medir `T` y `8T`.

## Riesgos

- Divergencia entre contratos propuestos y decisiones posteriores del Gate 0.
- Complejidad accidental si se incorpora infraestructura asíncrona antes de BB-03/BB-04.
- Confundir un entorno local de tres servicios con la topología final de producción.
- Imágenes o dependencias obsoletas si se actualizan sin repetir la verificación completa.
- La planificación solicita JUnit 5, mientras el BOM elegido de Spring Boot 4.1.1 administra JUnit Jupiter 6.0.3; forzar JUnit 5 rompería la regla de no sobrescribir versiones administradas sin una decisión explícita.

## Criterios de ratificación

- Los integrantes construyen el backend con el wrapper desde un ambiente limpio.
- Spring Modulith verifica los límites declarados.
- Compose levanta PostgreSQL, RabbitMQ y backend con health checks verdes.
- Actuator expone solo `health` e `info` sin secretos.
- Se acuerdan las versiones, responsabilidades y consecuencias.
- El equipo ratifica JUnit Jupiter 6.0.3 o acuerda mediante ADR una combinación distinta de Spring Boot y JUnit.
- Las limitaciones del incremento y la incorporación futura del frontend quedan explícitas.

ADR-009 requiere la aprobación registrada de **al menos tres integrantes** antes de cerrarse. Mientras no exista esa evidencia conservará el estado `Proposed`; BB-02 no puede marcarse terminado.

## Relación con ADR-001

Este ADR materializa tecnológicamente el estilo aceptado en ADR-001. No modifica el monolito modular, la propiedad lógica de datos, los puertos/adaptadores ni la prohibición de acceso cruzado. Cualquier contradicción deberá resolverse mediante revisión del equipo, no mediante una modificación silenciosa.
