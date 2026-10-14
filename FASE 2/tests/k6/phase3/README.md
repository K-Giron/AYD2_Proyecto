# Fase 3 - pruebas de carga y estrés

Este directorio corresponde al frente de Andy Sánchez (`AND3-01` a
`AND3-04`). Contiene cinco escenarios de carga y cinco de estrés. Los scripts
no afirman resultados: la evidencia solo se cierra después de ejecutarlos
contra un commit o release identificable.

## Estado del contrato

Al crear este arnés, el módulo de crédito todavía no estaba integrado en la
copia local de `develop`. Por eso las rutas nuevas están centralizadas en
variables de ambiente. Antes de la ejecución final, deben igualarse con el
contrato publicado por los responsables de crédito, checkout e integración.

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

## Configuración

1. Copiar `.env.performance.example` como `.env.performance`.
2. Ajustar `BASE_URL`, rutas y credenciales del ambiente.
3. Confirmar que los UUID consultados existen en el dataset.
4. No versionar `.env.performance` ni secretos.

El dataset determinista se prepara, una vez publicado el endpoint de historial,
con:

```bash
docker run --rm --add-host host.docker.internal:host-gateway \
  --env-file .env.performance -v "$PWD:/scripts:ro" \
  grafana/k6 run /scripts/prepare-dataset.js
```

Genera cuatro cohortes repetibles (cliente puntual, comportamiento medio,
moroso/con incidencias y datos insuficientes) con UUID e idempotencia estables.
La limpieza debe ejecutarse restaurando el snapshot/base efímera acordada con
Kevin; no se proporciona un borrado genérico que pueda apuntar por error a un
ambiente compartido.

Las rutas de crédito y checkout incluidas son provisionales. Si el contrato
final cambia, solo se actualiza el archivo de ambiente; los scripts conservan
el propósito y las métricas.

## Ejecución

Windows PowerShell:

```powershell
./run-phase3.ps1 -Suite load
./run-phase3.ps1 -Suite stress
./run-phase3.ps1 -Suite all
```

Linux/macOS:

```bash
chmod +x run-phase3.sh
./run-phase3.sh load
./run-phase3.sh stress
./run-phase3.sh all
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

El smoke externo independiente se ejecuta con:

```bash
docker run --rm --add-host host.docker.internal:host-gateway \
  --env-file .env.performance -v "$PWD:/scripts:ro" \
  grafana/k6 run /scripts/smoke-phase3-cloud.js
```

## Reglas de seguridad

- Ejecutar estrés únicamente en un ambiente autorizado y aislado.
- No usar clientes, correos, tarjetas ni historiales reales.
- No apuntar estos scripts a producción sin una ventana aprobada.
- Detener la corrida si aparecen errores persistentes, pérdida de salud o
  crecimiento no controlado de colas.
