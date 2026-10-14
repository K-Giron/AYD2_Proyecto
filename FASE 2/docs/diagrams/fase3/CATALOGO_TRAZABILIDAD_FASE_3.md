# Catálogo de trazabilidad — vistas arquitectónicas de Fase 3

**Corte documental:** 14 de octubre de 2026
**Fuente editable:** `NUEVO-vistas-arquitectonicas-fase3.drawio`  
**Decisión principal:** ADR-016

Este catálogo evita que el diagrama afirme componentes o despliegues que no
existen. Las vistas representan el árbol local ejecutable: `credit` permanece
dentro del monolito modular y usa la misma base PostgreSQL, con propiedad lógica
de sus tablas.

| Requisito | Vista | Elemento implementado | Evidencia ejecutable |
|---|---|---|---|
| Ofrecer crédito solo a elegibles | Componentes / secuencia | `PaymentOptionsService` → `CreditPort.evaluate` | `PaymentOptionsServiceTest` |
| Mantener PayKlik ante rechazo, falla o demora | Secuencia | timeout de 2 s y fallback `PAYKLIK` | `PaymentOptionsServiceTest` |
| Conceder sin cobro PayKlik ficticio | Componentes / datos | `CreditService`, `credit_operations`, referencia fiscal neutral | `OrderJourneysE2ETest` |
| Reintento sin efectos duplicados | Secuencia / datos | llave idempotente, unicidad y advisory locks transaccionales | `CreditServiceIntegrationTest`, `OrderJourneysE2ETest` |
| Rechazo sin deuda y continuación PayKlik | Secuencia | `CREDIT_REJECTED` → nueva ejecución `PAYKLIK` | `OrderJourneysE2ETest` |
| Resultado auditable y notificable | Componentes | Outbox → RabbitMQ → Inbox → `notifications` | `CreditNotificationIntegrationTest` (reintento/DLQ), `CreditNotificationOutageE2ETest` |
| Explicación de la decisión consultable | Componentes / datos | `CreditExplanationService` (`GET /credit/operations/{operationId}`) | `CreditExplanationIntegrationTest`, `CreditSecurityTest` |
| PayKlik se conserva con crédito caído o lento | Secuencia | fallback de opciones con límite de tiempo | `CreditDegradationE2ETest`, `PaymentOptionsIntegrationTest` |
| Cliente solo opera sus carritos/órdenes | Componentes | claim JWT `customer_id` + `CustomerAccessGuard` | `SecurityAuthorizationTest` |
| Despliegue coherente | Despliegue | frontend, backend modular, PostgreSQL, RabbitMQ y servicios existentes | CI `38616631224`, deploy `38618417187`, smoke externo verde |

## Límites explícitos

- La proyección histórica demostrativa no sustituye la fuente productiva que
  debe acordar Jorge con los dueños de datos.
- El claim firmado identifica al cliente canónico de demostración. Un catálogo
  real de cuentas individuales o un proveedor de identidad depende del frente
  de seguridad de Edson.
- La vista de despliegue representa la misma topología Compose ejecutada en la
  VM GCP. Pipeline, seis imágenes, health check, backup y rollback están
  implementados; la publicación de `v2` a `v4` continúa pendiente.
- ADR-016 fue ratificado como `Accepted` el 14 de octubre de 2026 contra
  implementación, pruebas, CI y comportamiento desplegado.
