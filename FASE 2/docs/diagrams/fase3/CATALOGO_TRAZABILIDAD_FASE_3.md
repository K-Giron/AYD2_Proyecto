# Catálogo de trazabilidad — vistas arquitectónicas de Fase 3

**Corte documental:** 14 de octubre de 2026
**Fuente editable:** `NUEVO-vistas-arquitectonicas-fase3.drawio`  
**Decisiones principales:** ADR-016 y ADR-017

Este catálogo evita que el diagrama afirme componentes o despliegues que no
existen. Las vistas representan el árbol local ejecutable: `credit` permanece
dentro del monolito modular y usa la misma base PostgreSQL, con propiedad lógica
de sus tablas.

## Exportaciones vigentes

- [`01-Componentes-Fase-3.jpg`](./01-Componentes-Fase-3.jpg)
- [`02-Secuencia-Saga-Dual.jpg`](./02-Secuencia-Saga-Dual.jpg)
- [`03-Datos-Credito.jpg`](./03-Datos-Credito.jpg)
- [`04-Despliegue-Fase-3.jpg`](./04-Despliegue-Fase-3.jpg)

Los SVG anteriores fueron sustituidos por estas exportaciones. La fuente
editable continúa siendo `NUEVO-vistas-arquitectonicas-fase3.drawio`.

| Requisito | Vista | Elemento implementado | Evidencia ejecutable |
|---|---|---|---|
| Crédito base para todo cliente registrado | Componentes / datos | perfil individual idempotente de Q2,500; migración V4011 | pruebas de perfil, registro y migración |
| Ofrecer crédito con saldo y condiciones válidas | Componentes / secuencia | `PaymentOptionsService` → `CreditPort.evaluate` | `PaymentOptionsServiceTest` |
| Mantener PayKlik ante rechazo, falla o demora | Secuencia | timeout de 2 s y fallback `PAYKLIK` | `PaymentOptionsServiceTest` |
| Conceder sin cobro PayKlik ficticio | Componentes / datos | `CreditService`, `credit_operations`, referencia fiscal neutral | `OrderJourneysE2ETest` |
| Reintento sin efectos duplicados | Secuencia / datos | llave idempotente, unicidad y advisory locks transaccionales | `CreditServiceIntegrationTest`, `OrderJourneysE2ETest` |
| Rechazo sin deuda y continuación PayKlik | Secuencia | `CREDIT_REJECTED` → nueva ejecución `PAYKLIK` | `OrderJourneysE2ETest` |
| Resultado auditable y notificable | Componentes | Outbox → RabbitMQ → Inbox → `notifications` | `CreditNotificationIntegrationTest` (reintento/DLQ), `CreditNotificationOutageE2ETest` |
| Explicación de la decisión consultable | Componentes / datos | `CreditExplanationService` (`GET /credit/operations/{operationId}`) | `CreditExplanationIntegrationTest`, `CreditSecurityTest` |
| PayKlik se conserva con crédito caído o lento | Secuencia | fallback de opciones con límite de tiempo | `CreditDegradationE2ETest`, `PaymentOptionsIntegrationTest` |
| Cliente solo opera sus carritos/órdenes | Componentes | claim JWT `customer_id` + `CustomerAccessGuard` | `SecurityAuthorizationTest` |
| Despliegue coherente | Despliegue | frontend, backend modular, PostgreSQL, RabbitMQ y servicios existentes | CI `38674105134`, deploy `38669072485`, smoke de nube 11/11 y plataforma 17/17 |
| Rendimiento verificable | Despliegue / secuencia | cinco escenarios de carga y cinco de estrés | reporte Fase 3; carga en umbral, STRESS-03 en 0 % y límite STRESS-04 documentado |

## Límites explícitos

- La proyección histórica demostrativa no sustituye la fuente productiva que
  debe acordar Jorge con los dueños de datos.
- Cada cuenta registrada posee un `customerId` persistente y un claim JWT
  `customer_id`; carrito, órdenes, historial y crédito se aíslan en backend. La
  federación con un proveedor de identidad continúa como evolución productiva.
- La vista de despliegue representa la misma topología Compose ejecutada en la
  VM GCP. Pipeline, seis imágenes, health check, backup y rollback están
  implementados; la publicación de `v2` a `v4` continúa pendiente.
- ADR-016 y ADR-017 fueron ratificados como `Accepted` el 14 de octubre de 2026
  contra implementación, pruebas, CI y comportamiento desplegado.
