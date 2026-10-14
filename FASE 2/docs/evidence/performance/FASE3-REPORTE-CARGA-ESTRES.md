# Reporte de rendimiento - Fase 3

**Responsable:** Andy Sánchez  
**Estado:** Arnés alineado con contratos reales; pendiente de ejecución y métricas  
**Zona horaria de la evidencia:** America/Guatemala

## 1. Propósito

Este reporte consolida los cinco escenarios de carga y los cinco escenarios de
estrés exigidos en Fase 3. Un caso solo cambia a `EJECUTADO` cuando conserva el
resumen k6, el commit o tag medido, el ambiente, el dataset y una conclusión
basada en métricas. No se aceptan valores estimados como resultados.

## 2. Identificación de la ejecución

| Campo | Valor |
|---|---|
| Fecha y hora | PENDIENTE |
| Commit SHA | PENDIENTE |
| Release/tag | PENDIENTE |
| Ambiente | PENDIENTE |
| URL base | PENDIENTE, sin credenciales |
| Topología/recursos | PENDIENTE, suministrado por Kevin |
| Dataset | PENDIENTE |
| Limpieza aplicada | PENDIENTE |
| Configuración k6 | `tests/k6/phase3/.env.performance`, no versionada |
| Directorio de resultados | `tests/k6/phase3/results/` |

El ambiente entregado para la medición es una VM GCP `e2-standard-2` en
`us-central1-a` (2 vCPU, 8 GB). La guía prescribe disco de 30 GB, dato que debe
confirmarse al capturar recursos. La URL se registrará sin incluir cuentas,
contraseñas ni tokens. La ejecución debe medir el tag que Kevin autorice; no se
asume que `v1` será el candidato final.

## 3. Criterios comunes

- Reportar throughput, p50, p95, p99 y tasa de error.
- Separar rechazo funcional de crédito de error técnico.
- Confirmar que los reintentos no dupliquen decisiones, órdenes ni efectos.
- Durante estrés, registrar el primer nivel donde se incumple un umbral.
- En recuperación, comprobar salud, latencia y tasa de error después de retirar
  la sobrecarga.
- Correlacionar los resultados con CPU, memoria, conexiones de base, profundidad
  de cola y logs cuando el ambiente los exponga.
- Conservar `ALLOW_MUTATIONS=false` y `ALLOW_STRESS=false` fuera de la ventana
  aprobada; cada corrida usa un `RUN_ID` único.

## 4. Pruebas de carga

| ID | Escenario | Perfil final | Throughput | p50 | p95 | p99 | Errores | Estado |
|---|---|---:|---:|---:|---:|---:|---:|---|
| LOAD-01 | Opciones/elegibilidad concurrente | PENDIENTE | - | - | - | - | - | NO EJECUTADO |
| LOAD-02 | Checkout mixto crédito/PayKlik | PENDIENTE | - | - | - | - | - | NO EJECUTADO |
| LOAD-03 | Historial ERP idempotente | PENDIENTE | - | - | - | - | - | NO EJECUTADO |
| LOAD-04 | Eventos y notificaciones | PENDIENTE | - | - | - | - | - | NO EJECUTADO |
| LOAD-05 | Estado de orden y auditoría | PENDIENTE | - | - | - | - | - | NO EJECUTADO |

### Evidencia y conclusión por caso

#### LOAD-01

- Archivo de resumen k6: PENDIENTE
- Resultado del umbral: PENDIENTE
- Observación: PENDIENTE
- Conclusión: PENDIENTE

#### LOAD-02

- Archivo de resumen k6: PENDIENTE
- Proporción crédito/PayKlik: PENDIENTE
- Resultado del umbral: PENDIENTE
- Conclusión: PENDIENTE

#### LOAD-03

- Archivo de resumen k6: PENDIENTE
- Eventos enviados/repetidos: PENDIENTE
- Duplicados detectados: PENDIENTE
- Conclusión: PENDIENTE

#### LOAD-04

- Archivo de resumen k6: PENDIENTE
- Notificaciones aceptadas/entregadas/fallidas: PENDIENTE
- Profundidad máxima de cola: PENDIENTE
- Conclusión: PENDIENTE

#### LOAD-05

- Archivo de resumen k6: PENDIENTE
- Resultado de consulta de estado: PENDIENTE
- Resultado de consulta de auditoría: PENDIENTE
- Conclusión: PENDIENTE

## 5. Pruebas de estrés

| ID | Escenario | Pico alcanzado | Primer límite | p95 en límite | Error máximo | Recuperación | Estado |
|---|---|---:|---:|---:|---:|---|---|
| STRESS-01 | Rampa progresiva | - | - | - | - | - | NO EJECUTADO |
| STRESS-02 | Pico súbito de checkout | - | - | - | - | - | NO EJECUTADO |
| STRESS-03 | Crédito saturado y control PayKlik | - | - | - | - | - | NO EJECUTADO |
| STRESS-04 | Backlog y drenaje de notificaciones | - | - | - | - | - | NO EJECUTADO |
| STRESS-05 | Salud y latencia tras sobrecarga | - | - | - | - | - | NO EJECUTADO |

### Evidencia y conclusión por caso

#### STRESS-01

- Archivo de resumen k6: PENDIENTE
- Nivel donde aparece degradación: PENDIENTE
- Recurso correlacionado: PENDIENTE
- Conclusión: PENDIENTE

#### STRESS-02

- Archivo de resumen k6: PENDIENTE
- Latencia antes/durante/después del pico: PENDIENTE
- Órdenes perdidas o duplicadas: PENDIENTE
- Conclusión: PENDIENTE

#### STRESS-03

- Archivo de resumen k6: PENDIENTE
- Resultado del crédito bajo saturación: PENDIENTE
- p95/p99 y errores de PayKlik durante la saturación: PENDIENTE
- Evidencia de independencia entre rutas: PENDIENTE
- Conclusión: PENDIENTE

#### STRESS-04

- Archivo de resumen k6: PENDIENTE
- Backlog máximo: PENDIENTE
- Tiempo de drenaje: PENDIENTE
- Mensajes perdidos o duplicados: PENDIENTE
- Conclusión: PENDIENTE

#### STRESS-05

- Archivo de resumen k6: PENDIENTE
- Tiempo hasta recuperar `UP`: PENDIENTE
- p95/p99 en ventana de recuperación: PENDIENTE
- Error en ventana de recuperación: PENDIENTE
- Conclusión: PENDIENTE

## 6. Cuellos de botella y segunda medición

| Observación medida | Causa inferida | Evidencia que sustenta la inferencia | Ajuste | Antes | Después | Responsable |
|---|---|---|---|---:|---:|---|
| PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | - | - | PENDIENTE |

No se debe declarar una causa como hecho si solo existe correlación. En ese
caso se registra como hipótesis y se diseña una segunda medición controlada.

## 7. Recuperación y defectos

| ID | Defecto o degradación | Severidad | Acción | Repetición | Resultado |
|---|---|---|---|---|---|
| PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |

## 8. Veredicto

**PENDIENTE DE EJECUCIÓN.** El crédito, checkout dual, contratos y ambiente ya
están disponibles, pero todavía no existe evidencia para aprobar o rechazar los
umbrales. Este estado debe mantenerse hasta medir un commit o tag autorizado.

