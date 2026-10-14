# Reporte de rendimiento - Fase 3

**Responsable:** Andy Sánchez

**Estado:** Ejecución local y smoke externo completados; cuello de botella de notificaciones abierto

**Fecha:** 2026-10-14

**Zona horaria de la evidencia:** America/Guatemala

## 1. Propósito y alcance

Este reporte consolida cinco escenarios de carga y cinco de estrés de Fase 3.
Las mediciones se ejecutaron en el ambiente Docker local y no representan la
capacidad máxima de la VM de producción. No se realizó estrés contra GCP. El
smoke independiente sí se ejecutó contra la URL pública y terminó aprobado.

Se reportan por separado los errores técnicos, los rechazos funcionales
esperados y las limitaciones del dataset. Los archivos crudos de k6 están en
`tests/k6/phase3/results/`, directorio ignorado por Git; por ello, este documento
conserva las métricas consolidadas y los nombres exactos de los resúmenes.

## 2. Identificación de la ejecución

| Campo | Valor |
|---|---|
| Fecha y hora | 2026-10-14, 11:32 a 14:17 |
| Base de aplicación medida | `5a7cdd1cf4d4dfb3f249d92c4d830fac636578db` (`5a7cdd1`) |
| Rama | `feature/201904024-fase3-rendimiento` |
| Release/tag | Sin tag; medición local de la rama |
| Ambiente | Windows 11, Docker Desktop 29.8.2, Docker Compose, 12 contenedores |
| URL base desde k6 | `http://host.docker.internal:8080` |
| Límite visible por contenedor | 3.767 GiB |
| Dataset | 500 historiales sintéticos en cinco cohortes; inventario de prueba incrementado en 10,000 unidades |
| Identificador principal | `RUN_ID=20261014-local-02`; el ejecutor deriva un ID único por escenario |
| Configuración k6 | `tests/k6/phase3/.env.performance`, no versionada |
| Directorio de resultados | `tests/k6/phase3/results/` |
| Smoke externo | `http://136.65.180.39`, sin registrar credenciales |

La aplicación se midió sobre el SHA indicado. Durante la ejecución se
detectaron y corrigieron tres defectos del arnés: lectura del identificador de
ubicación, tratamiento de `stderr` de k6 en PowerShell y reutilización del mismo
`RUN_ID` entre escenarios. Esos cambios locales forman parte del cierre
documental posterior, no del SHA base de la aplicación.

No se ejecutó un borrado completo entre escenarios; se usaron datos sintéticos,
operaciones idempotentes e identificadores aislados. Esto permite reproducir la
prueba, pero el volumen acumulado puede influir en los escenarios posteriores.

## 3. Criterios comunes

- Registrar throughput, p50, p95, p99 y tasa de error.
- Separar rechazo funcional de crédito de error técnico.
- Confirmar que los reintentos no dupliquen decisiones, órdenes ni efectos.
- Durante estrés, registrar el primer nivel donde se incumple un umbral.
- En recuperación, comprobar salud, latencia y tasa de error al retirar la carga.
- Usar un `RUN_ID` único por escenario para aislar claves idempotentes.
- No guardar cuentas, contraseñas ni tokens en resultados o documentación.

## 4. Preparación y smoke

### 4.1 Preparación y smoke local

El primer smoke completó 20 de 21 comprobaciones. PayKlik superó el tiempo de
espera inicial, aunque la reconciliación aprobó la operación aproximadamente 30
segundos después. Al ampliar `SAGA_MAX_POLLS` a 90, la repetición terminó en
verde. Este resultado demuestra convergencia local, no un smoke de nube.

La preparación del dataset ejecutó 500 iteraciones y 504 solicitudes con 0 % de
errores. Alcanzó 25.62 req/s, p50 24.39 ms, p95 100.26 ms y p99 244.98 ms.

| Evidencia | Resultado |
|---|---|
| `20261014-113246-smoke-phase3-cloud-summary.json` | Smoke local aprobado |
| `20261014-113515-prepare-dataset-summary.json` | Dataset aprobado |

### 4.2 Smoke independiente en GCP

El primer intento externo, conservado en
`20261014-141055-smoke-phase3-cloud-summary.json`, completó carrito, producto,
opciones, checkout, Saga y consulta de orden, pero falló la comprobación de
`/actuator/health/readiness` con HTTP 404. El despliegue oculta deliberadamente
Actuator y nginx solo publica las rutas `/api/`; por tanto, no fue una caída del
sistema sino un contrato incorrecto del arnés externo.

Se cambió el valor predeterminado a `/api/v1/platform/status`, la misma ruta
pública utilizada por el smoke de despliegue. La repetición quedó registrada en
`20261014-141640-smoke-phase3-cloud-summary.json` con estos resultados:

- 11 de 11 comprobaciones correctas y 0 % de errores.
- 9 solicitudes HTTP, todas exitosas.
- Promedio 183.79 ms, p50 145.44 ms, p95 331.21 ms, p99 400.91 ms y máximo
  418.34 ms.
- Recorrido completo en 2.06 s: estado público, autenticación, carrito,
  producto, opciones, checkout PayKlik, Saga terminal y consulta de orden.

Conclusión: el smoke independiente de GCP queda **APROBADO**. La versión exacta
desplegada no se expone en el endpoint público, por lo que no se atribuye a un
tag no verificable.

## 5. Pruebas de carga

| ID | Escenario | Perfil | Throughput | p50 | p95 | p99 | Errores HTTP | Resultado |
|---|---|---:|---:|---:|---:|---:|---:|---|
| LOAD-01 | Opciones/elegibilidad concurrente | 10 VUs, 2 min | 35.69 req/s | 18.63 ms | 77.27 ms | 160.52 ms | 0 % | APROBADO |
| LOAD-02 | Checkout mixto crédito/PayKlik | 10 VUs, 2 min | 23.94 req/s | 148.22 ms | 928.38 ms | 1,303.22 ms | 2.05 % | UMBRALES APROBADOS CON OBSERVACIÓN |
| LOAD-03 | Historial ERP idempotente | 10 VUs, 2 min | 26.25 req/s | 88.36 ms | 342.22 ms | 609.16 ms | 0 % | APROBADO |
| LOAD-04 | Eventos y notificaciones | 10 VUs, 2 min | 31.39 req/s | 42.89 ms | 180.55 ms | 333.79 ms | 0 % | APROBADO |
| LOAD-05 | Estado de orden y auditoría | 10 VUs, 2 min | 121.80 req/s | 13.56 ms | 75.26 ms | 130.29 ms | 0 % | UMBRALES APROBADOS CON LIMITACIÓN DE DATOS |

### 5.1 Evidencia y conclusiones

#### LOAD-01

- Resumen: `20261014-113556-load-01-eligibility-summary.json`.
- Se realizaron 4,295 solicitudes y no hubo errores técnicos.
- Conclusión: elegibilidad estable bajo la carga objetivo.

#### LOAD-02

- Resumen: `20261014-113758-load-02-mixed-checkout-summary.json`.
- Se realizaron 2,933 solicitudes. Hubo 60 comprobaciones fallidas: 30 al abrir
  carrito, 12 al agregar producto, 17 en checkout y 1 al esperar la Saga.
- La tasa de error de la Saga etiquetada fue 0.05 %, menor al umbral de 1 %.
- Conclusión: los umbrales formales pasaron, pero las operaciones auxiliares del
  checkout requieren observarse de nuevo en un ambiente aislado.

#### LOAD-03

- Resumen: `20261014-114004-load-03-erp-history-summary.json`.
- Se realizaron 3,169 solicitudes sin errores.
- Conclusión: el ingreso idempotente de historial ERP fue estable.

#### LOAD-04

- Resumen: `20261014-114209-load-04-notifications-summary.json`.
- Se realizaron 3,797 solicitudes sin errores.
- Conclusión: el flujo de notificaciones soportó la carga normal; el límite se
  observó posteriormente en STRESS-04.

#### LOAD-05

- Resumen: `20261014-114413-load-05-state-audit-summary.json`.
- Se realizaron 14,688 solicitudes sin errores HTTP. Los umbrales de consulta
  de orden y auditoría pasaron.
- 4,720 iteraciones no encontraron una orden sembrada y 3,322 sí la encontraron;
  por eso la métrica operativa global marcó 16.07 % aunque los endpoints
  realmente invocados no fallaron.
- Conclusión: endpoints aprobados, con limitación del selector de datos. Debe
  prepararse un conjunto mayor de órdenes si se repite el caso.

## 6. Pruebas de estrés

| ID | Escenario | Pico | Throughput | p95 | p99 | Error HTTP | Resultado |
|---|---|---:|---:|---:|---:|---:|---|
| STRESS-01 | Rampa progresiva | 160 VUs | 102.97 req/s | 1,119.36 ms | 1,429.49 ms | 0.003 % | APROBADO |
| STRESS-02 | Pico súbito de checkout | 200 VUs | 101.60 req/s | 2,819.36 ms | 4,178.00 ms | 24.88 % | UMBRALES DEL ESCENARIO APROBADOS; SATURACIÓN AUXILIAR |
| STRESS-03 | Crédito saturado y control PayKlik, inicial | 170 VUs | 240.56 req/s | 1,545.59 ms | 2,350.11 ms | 46.99 % | NO VÁLIDO POR COLISIÓN DEL ARNÉS |
| STRESS-03R | Segunda medición controlada | 170 VUs | 121.56 req/s | 1,640 ms | 2,070 ms | 0 % | APROBADO |
| STRESS-04 | Backlog y drenaje de notificaciones | 160 + 2 VUs | 67.99 req/s | 872.45 ms | 2,045.47 ms | 28.01 % | NO APROBADO |
| STRESS-05 | Salud y latencia tras sobrecarga | 160 + 5 VUs | 94.56 req/s | 1,430.68 ms | 1,637.80 ms | 0.014 % | RECUPERACIÓN APROBADA |

### 6.1 Evidencia y conclusiones

#### STRESS-01

- Resumen: `20261014-132621-stress-01-progressive-limit-summary.json`.
- Se realizaron 31,913 solicitudes; solo una falló.
- Conclusión: la rampa hasta 160 VUs se mantuvo dentro de p95 < 3 s y error < 10 %.

#### STRESS-02

- Resumen: `20261014-133133-stress-02-checkout-spike-summary.json`.
- El pico llegó a 200 VUs. Fallaron 1,253 aperturas de carrito, 1,954 adiciones
  de producto y 597 checkout.
- Conclusión: el umbral etiquetado del escenario pasó, pero el 24.88 % de error
  HTTP global evidencia saturación de las operaciones auxiliares bajo un pico
  súbito. No debe interpretarse como capacidad sostenida.

#### STRESS-03 y repetición controlada

- Medición inicial: `20261014-133409-stress-03-credit-vs-payklik-summary.json`.
- Repetición: `20261014-135755-stress-03-credit-vs-payklik-summary.json`.
- La primera corrida reutilizó identificadores de STRESS-01. Esto produjo 27,156
  fallos al agregar producto y contaminó el resultado; además, PayKlik excedió
  sus umbrales con p95 2,053.08 ms y p99 3,030.70 ms.
- El ejecutor se corrigió para derivar un `RUN_ID` distinto por escenario y la
  repetición completó 29,233 solicitudes y 58,124 comprobaciones, todas exitosas.
- En la repetición, la presión de crédito tuvo 0 % de error y el control PayKlik
  tuvo promedio 523.94 ms, p50 382.45 ms, p95 1,470.14 ms y p99 1,972.71 ms,
  también con 0 % de error.
- Frente a la primera medición, PayKlik redujo p95 en 28.39 % y p99 en 34.91 %;
  el error HTTP global bajó de 46.99 % a 0 %.
- Conclusión: con datos aislados, crédito y PayKlik coexistieron dentro de los
  umbrales y sin errores. Esta es la medición válida del escenario.

#### STRESS-04

- Resumen: `20261014-133811-stress-04-notification-backlog-summary.json`.
- El backlog máximo observado fue 5,566. El drenaje no perdió comprobaciones y
  tuvo p50 2,240.82 ms, p95 3,446.82 ms y p99 3,923.08 ms, pero no completó la
  cola dentro de la ventana de recuperación de dos minutos.
- La creación tuvo 6,046 fallos y 15,476 éxitos, equivalente a 28 % de error.
- Conclusión: se encontró un cuello de botella real en capacidad de aceptación
  y drenaje de notificaciones. Permanece abierto y requiere ajuste y repetición.

#### STRESS-05

- Resumen: `20261014-134332-stress-05-recovery-summary.json`.
- Después de retirar la sobrecarga, la ventana de recuperación tuvo promedio
  14.47 ms, p50 12.58 ms, p95 38.10 ms, p99 62.73 ms, máximo 111.13 ms y 0 % de
  error en 2,340 comprobaciones.
- Conclusión: la plataforma recuperó salud y latencia dentro de los umbrales.

## 7. Cuellos de botella y segunda medición

| Observación | Causa o hipótesis | Ajuste/acción | Antes | Después | Estado |
|---|---|---|---:|---:|---|
| STRESS-03 produjo miles de fallos de carrito | Causa confirmada en el arnés: `RUN_ID` compartido entre scripts | ID único por escenario y repetición controlada | 46.99 % error HTTP; PayKlik p95 2,053.08 ms | 0 % error; PayKlik p95 1,470.14 ms | CERRADO |
| STRESS-04 no drenó la cola | Capacidad del flujo de notificaciones menor que la tasa de llegada | Medir consumidores, profundidad de cola y base; ajustar capacidad y repetir | Backlog 5,566; creación con 28 % de error | Pendiente | ABIERTO |
| STRESS-02 degradó operaciones auxiliares | Saturación durante el pico de 200 VUs | Correlacionar conexiones, CPU y stock; repetir con telemetría | 24.88 % error HTTP global | Pendiente | OBSERVACIÓN |
| LOAD-05 omitió consultas por falta de órdenes | Selector de datos insuficiente | Sembrar un conjunto explícito de órdenes consultables | 4,720 iteraciones sin orden | Pendiente | LIMITACIÓN |

Durante la prueba se observaron reintentos de sincronización de inventario de
`POS-ONLINE`. El incremento del dataset se aplicó al inventario del backend y no
necesariamente al stock independiente del servicio de inventario; se registra
como hipótesis de ruido operacional, no como causa confirmada de todos los
errores de checkout.

## 8. Recursos y recuperación

La captura realizada después de STRESS-05 muestra los 12 contenedores activos.
Es evidencia de recuperación posterior y no de consumo máximo durante el pico.
El consumo visible fue aproximadamente 2.3 GiB en total. Entre los procesos más
grandes estuvieron backend (556.3 MiB), store-service (373.2 MiB),
inventory-service (351.4 MiB), LogiExpress (313.3 MiB), PayKlik (287.9 MiB) y
PostgreSQL principal (207.2 MiB). El CPU estaba mayormente inactivo, coherente
con la finalización de la carga.

No se capturó una serie temporal de CPU, memoria, conexiones o cola durante el
pico. Por ello, no se atribuye el límite de STRESS-04 a un recurso específico.

## 9. Defectos y hallazgos

| ID | Hallazgo | Acción | Repetición | Resultado |
|---|---|---|---|---|
| PERF-01 | Dataset leía `location.id` en vez de `location.locationId` | Corregir campo | Dataset | CERRADO, 500 iteraciones |
| PERF-02 | PowerShell trataba `stderr` informativo de k6 como error fatal | Registrar salida y evaluar el código al final | Suite de estrés | CERRADO |
| PERF-03 | Un `RUN_ID` compartido contaminaba claves idempotentes | Derivar ID por escenario | STRESS-03R | CERRADO, 0 % errores |
| PERF-04 | Backlog de notificaciones no drenó en dos minutos | Ajustar capacidad y repetir | Pendiente | ABIERTO |
| PERF-05 | LOAD-05 no siempre encontraba órdenes | Mejorar dataset/selector | Pendiente opcional | ABIERTO |
| PERF-06 | Top-up no cubre necesariamente el stock de POS-ONLINE | Alinear preparación de ambos inventarios | Pendiente | OBSERVACIÓN |
| PERF-07 | El smoke externo consultaba Actuator, oculto por nginx | Usar `/api/v1/platform/status` y repetir | Smoke GCP | CERRADO, 11/11 |

## 10. Veredicto

Se ejecutaron las cinco pruebas de carga y las cinco de estrés en el ambiente
local. Los cinco escenarios de carga cumplieron sus umbrales formales, con
observaciones de datos en LOAD-02 y LOAD-05. STRESS-01 y la recuperación de
STRESS-05 aprobaron. La segunda medición controlada de STRESS-03 aprobó todos
los umbrales con 0 % de errores y confirmó la convivencia de crédito y PayKlik.

El resultado global queda **APROBADO CON OBSERVACIONES PARA EL AMBIENTE LOCAL**.
El smoke independiente contra GCP también queda **APROBADO**, con 11/11
comprobaciones y 0 % de errores. No se declara capacidad de producción:
STRESS-04 mantiene abierto un cuello de botella de notificaciones y las pruebas
de estrés se realizaron únicamente en local. No se recomienda ejecutar estrés
sobre la nube sin una nueva ventana autorizada.
