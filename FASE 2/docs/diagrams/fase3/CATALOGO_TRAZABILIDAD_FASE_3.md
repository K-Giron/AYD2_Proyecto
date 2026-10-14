# Catálogo de trazabilidad — vistas arquitectónicas de Fase 3

**Corte documental:** 13 de octubre de 2026  
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
| Resultado auditable y notificable | Componentes | Outbox → RabbitMQ → Inbox → `notifications` | pruebas de consumidor y E2E |
| Cliente solo opera sus carritos/órdenes | Componentes | claim JWT `customer_id` + `CustomerAccessGuard` | `SecurityAuthorizationTest` |
| Despliegue coherente | Despliegue | frontend, backend modular, PostgreSQL, RabbitMQ y servicios existentes | `docker compose config`, smoke local pendiente de cierre |

## Límites explícitos

- La proyección histórica demostrativa no sustituye la fuente productiva que
  debe acordar Jorge con los dueños de datos.
- El claim firmado identifica al cliente canónico de demostración. Un catálogo
  real de cuentas individuales o un proveedor de identidad depende del frente
  de seguridad de Edson.
- La vista de despliegue documenta Compose/local. La nube, pipeline, imágenes y
  rollback dependen de Kevin y no se declaran terminados aquí.
- El ADR permanece `Proposed` hasta revisión de Jorge, Edson, Andy y Kevin.
