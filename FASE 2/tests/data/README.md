# Datos reproducibles de pruebas

Este directorio contiene los datos versionados y no sensibles de ORD-05.

- `ord-05-deterministic-fixtures.json`: usuarios demo, productos semilla y valores estables de checkout usados por los cinco recorridos E2E.
- `credit-history-demo.csv`: historial ficticio del ERP para el crédito propio (contrato `credit-history-v1`), con las fechas expresadas como «días atrás» para que siempre caiga en la ventana de 12 meses.

## Crédito propio: clientes de demostración

- **Contrato:** `credit-history-v1` · **Propietario:** Jorge Pumay · **Consumidores:** `CreditDemoDatasetTest`, E2E de crédito y carga.
- **Carga:** `./scripts/load-credit-demo-history.sh` (idempotente; `--print` solo muestra el lote). **Restablecimiento:** `./scripts/reset-demo-data.sh --yes`.
- Todos los IDs y datos son ficticios; no hay información real ni sensible.

| Cliente (`bbbbbbbb-0000-4000-8000-…`) | Historia | Decisión esperada |
|---|---|---|
| `…0001` | 5 compras, 3 pagos puntuales, sin devoluciones | `APPROVED` (`ELIGIBLE`, límite 1600.00) |
| `…0002` | 2 compras | `REJECTED` (`INSUFFICIENT_HISTORY`) |
| `…0003` | 4 compras, 2 devoluciones (50 %) | `REJECTED` (`HIGH_RETURN_RATE`) |
| `…0004` | 4 compras, 2 incidentes críticos | `REJECTED` (`CRITICAL_INCIDENTS`) |
| `…0005` | 4 compras, 1 de 3 pagos puntual | `REJECTED` (`LATE_PAYMENT_HISTORY`) |

Para ver un rechazo desde la interfaz del cliente, arranca el backend con `SGO_SECURITY_CLIENT_CUSTOMER_ID` apuntando a uno de estos clientes.

Cada conjunto futuro deberá indicar:

- versión del contrato;
- propietario;
- propósito y prueba consumidora;
- IDs ficticios y deterministas;
- precondiciones;
- resultado esperado;
- mecanismo de restablecimiento;
- confirmación de que no contiene información real o sensible.

No se deben crear tablas ni datos artificiales únicamente para ocultar una dependencia pendiente.
