# Fase 3 - pruebas de carga y estrés

Este directorio corresponde al frente de Andy Sánchez (`AND3-01` a
`AND3-04`). Contiene cinco escenarios de carga y cinco de estrés. Los scripts
no afirman resultados: la evidencia solo se cierra después de ejecutarlos
contra un commit o release identificable.

## Contratos medidos

El arnés está alineado con la rebanada integrada en `develop` el 14 de octubre:

- opciones: `GET /api/v1/carts/{cartId}/payment-options`;
- checkout: `POST /api/v1/carts/{cartId}/checkout`;
- Saga dual: `POST /api/v1/orders/{orderId}/saga`, rutas `PAYKLIK|OWN_CREDIT`;
- historial ERP: `POST /api/v1/credit/history`, contrato `credit-history-v1`;
- consulta: `GET /api/v1/orders/{orderId}/summary` y
  `GET /api/v1/audit/events?orderId=...`.

Las pruebas usan una cuenta interna para operar clientes sintéticos. La cuenta
`CLIENT` se reserva para el smoke, que toma el `customerId` firmado del login y
no acepta un UUID inventado desde variables de ambiente.

## Catálogo

| ID | Script | Propósito |
|---|---|---|
| LOAD-01 | `load-01-eligibility.js` | Consultas concurrentes de opciones/elegibilidad |
| LOAD-02 | `load-02-mixed-checkout.js` | Mezcla esperada de checkout con crédito y PayKlik |
| LOAD-03 | `load-03-erp-history.js` | Ingesta sostenida e idempotente de historial ERP |
| LOAD-04 | `load-04-notifications.js` | Creación sostenida de eventos/notificaciones |
| LOAD-05 | `load-05-state-audit.js` | Consulta de estado y auditoría durante carga |
| STRESS-01 | `stress-01-progressive-limit.js` | Rampa hasta el primer umbral de degradación |
| STRESS-02 | `stress-02-checkout-spike.js` | Pico súbito de checkout |
| STRESS-03 | `stress-03-credit-vs-payklik.js` | Saturación de crédito sin bloquear PayKlik |
| STRESS-04 | `stress-04-notification-backlog.js` | Formación de backlog y consulta de drenaje |
| STRESS-05 | `stress-05-recovery.js` | Recuperación de salud, errores y latencia |

## Requisitos

- Docker con acceso a `grafana/k6` o una instalación local de k6.
- SGO-Logic levantado y con un dataset aislado para pruebas.
- Endpoints de Fase 3 estables.
- Cuentas de prueba sin datos personales ni credenciales productivas.
- Autorización del responsable del ambiente antes de ejecutar estrés.

Los scripts que escriben datos se detienen salvo que `ALLOW_MUTATIONS=true`.
Los cinco escenarios de estrés exigen además `ALLOW_STRESS=true`.

## Configuración

1. Copiar `.env.performance.example` como `.env.performance`.
2. Ajustar `BASE_URL`, `RUN_ID` y las credenciales del ambiente.
3. Dejar `ALLOW_MUTATIONS=false` y `ALLOW_STRESS=false` hasta obtener la
   autorización de Kevin para la ventana de pruebas.
4. No versionar `.env.performance` ni secretos.

El dataset determinista se prepara con `ALLOW_MUTATIONS=true`:

```powershell
./run-phase3.ps1 -Suite dataset
```

Genera 500 clientes por defecto en cinco cohortes: elegible, historia
insuficiente, devoluciones altas, incidentes críticos y pagos tardíos. La
idempotencia real usa `sourceReference`; repetir la preparación no duplica los
hechos. Si Kevin autoriza surtir inventario para compras completas, se define
`INVENTORY_TOPUP` con las unidades a ingresar en `CD-01`; su valor por defecto
es `0`.

La limpieza debe ejecutarse restaurando el snapshot/base efímera acordada con
Kevin. El arnés no borra de forma genérica un ambiente compartido.

## Ejecución

Windows PowerShell:

```powershell
./run-phase3.ps1 -Suite load
./run-phase3.ps1 -Suite stress
./run-phase3.ps1 -Suite all
./run-phase3.ps1 -Suite smoke
```

El orden recomendado es `prepare-dataset.js`, suite `load` y luego suite
`stress`. `LOAD-05` consulta las órdenes que produjo `LOAD-02`, por lo que no
debe ejecutarse primero en un ambiente vacío.

Linux/macOS:

```bash
chmod +x run-phase3.sh
./run-phase3.sh dataset
./run-phase3.sh load
./run-phase3.sh stress
./run-phase3.sh all
./run-phase3.sh smoke
```

Para una sola prueba:

```bash
docker run --rm --add-host host.docker.internal:host-gateway \
  --env-file .env.performance \
  -v "$PWD:/scripts:ro" \
  grafana/k6 run /scripts/load-01-eligibility.js
```

## Evidencia obligatoria

Cada corrida debe registrar:

- SHA o tag medido.
- URL y descripción del ambiente, sin secretos.
- Fecha y zona horaria.
- Dataset y limpieza utilizada.
- Usuarios virtuales, duración y perfil de llegada.
- Throughput, p50, p95, p99 y porcentaje de errores.
- Uso de CPU, memoria, base de datos y colas cuando esté disponible.
- Primer límite observado y comportamiento posterior a la sobrecarga.
- Comparación antes/después si se aplicó un ajuste.

Los ejecutores guardan resúmenes en `results/`. Las conclusiones verificadas se
transcriben a `docs/evidence/performance/FASE3-REPORTE-CARGA-ESTRES.md`.

El smoke crea un carrito, consulta opciones, hace checkout, ejecuta la Saga y
consulta la orden. Requiere `ALLOW_MUTATIONS=true`; por defecto usa `PAYKLIK`
para no depender del saldo de crédito del cliente canónico.

## Reglas de seguridad

- Ejecutar estrés únicamente en un ambiente autorizado y aislado.
- No usar clientes, correos, tarjetas ni historiales reales.
- No apuntar estos scripts a producción sin una ventana aprobada.
- Detener la corrida si aparecen errores persistentes, pérdida de salud o
  crecimiento no controlado de colas.
