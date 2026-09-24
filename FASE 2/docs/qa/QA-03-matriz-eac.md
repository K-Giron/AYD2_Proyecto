# QA-03 — Matriz de Escenarios de Atributos de Calidad (EaC-01 a EaC-12)

| Campo | Valor |
|---|---|
| Responsable | Kevin Girón (PO-QA) |
| Fuente | [`Docs/DDA.md`](../../../Docs/DDA.md), §4.2 |
| Corte | 2026-09-22 |
| Estado | Instantánea viva — se actualiza conforme el equipo entrega evidencia, no una medición final |

## 1. Criterio de uso

- `Cubierto`: existe implementación y prueba automatizada o manual reproducible que ejercita el escenario descrito en el DDA.
- `Parcial`: existe implementación funcional, pero la medida de respuesta completa (carga, p95/p99, ventana de tiempo real) no está verificada.
- `Bloqueado`: depende de un módulo que otro integrante todavía no entrega; no se puede probar honestamente hasta entonces.
- `No evaluado`: requiere un ejercicio que no se ha realizado (ej. despliegue en nube, taller de runbook) y no depende de código faltante.
- Ninguna fila se marca `Cubierto` sin una evidencia reproducible enlazada. Si la evidencia es de esta sesión de trabajo, se cita el comando o la prueba exacta.

## 2. Matriz

| ID | Atributo (DDA) | Medida de respuesta (DDA) | Estado | Evidencia | Responsable del pendiente |
|---|---|---|---|---|---|
| EaC-01 | Rendimiento — consulta de orden | 100 % de consultas ≤ 5 s, con p50/p95/p99 | **Cubierto** | QA-02 con k6 sobre 500 proyecciones: `T` (10 VUs) 1 190 iteraciones, p50 3.35 ms / p95 5.88 ms / p99 7.10 ms; `8T` (80 VUs) 9 600 iteraciones, p50 2.07 ms / p95 4.22 ms / p99 8.30 ms; 0 % de errores y 100 % ≤ 5 s. Evidencia en `docs/evidence/performance/QA-02-linea-base-y-8T.md`. | Edson Sosa — cerrado en este incremento |
| EaC-02 | Rendimiento/Auditabilidad — evidencia de transacción | < 1 s | **Cubierto** | AUD-01/AUD-02 implementados: `GET /api/v1/audit/events` y exportación sobre `audit_events` (`V3002`) con índices por orden, agregado y correlación. `AuditQueryIntegrationTest.queryStaysUnderOneSecondWithDeclaredDataset` verifica < 1 s sobre 10 000 eventos (1 000 de una misma orden) y el smoke real respondió en ~63 ms. | Edson Sosa — cerrado en este incremento |
| EaC-03 | Escalabilidad — Black Friday 8x | Soportar 8x sin degradar EaC-01/EaC-02 | **Parcial** | QA-02 midió `8T` (80 VUs, ≈ 8.04× la tasa de `T`) sobre los endpoints consolidados disponibles sin degradar EaC-01 ni EaC-02 y con 0 % de errores. Falta el conjunto de módulos P0 (Orders y Saga, ORD-01 a ORD-05) y un ambiente con balanceo para escalabilidad horizontal real. | Edson Sosa (medición) y Jairo Gómez (ORD) |
| EaC-04 | Disponibilidad/Resiliencia — falla de PayKlik | 0 órdenes perdidas; reintento/conciliación ≤ 60 s | **Cubierto** | PAY-01 a PAY-03 implementados: el intento se conserva con orden, canal, fecha, referencia y resultado; timeout/caída/circuito abierto lo dejan `INDETERMINATE` y publican `PaymentIndeterminate`; la conciliación se programa dentro de los 60 s, reutiliza la misma Idempotency-Key (sin segundo cobro), reintenta con backoff y deja alerta auditable (`PAYMENT_RECONCILIATION_ALERT`) al agotar o superar 5 min. Evidencia: `PaymentIndeterminateIntegrationTest` (0 intentos perdidos, delay ≤ 60 s, circuito abierto), `PaymentReconciliationServiceTest` (misma llave, alerta) y `PaymentReconciliationIntegrationTest` (resolución y no duplicidad); smoke real de resolución y de bandeja de fallidos. | Edson Sosa — cerrado en este incremento |
| EaC-05 | Confiabilidad — timeout LogiExpress (2 %) | 0 pedidos bloqueados; hasta 3 reintentos (2/8/32 s) antes de mensajes fallidos | **Cubierto** | `SimulatedLogiExpressAdapter` reproduce ~2 % de timeout; `ShipmentServiceTest.exhaustsThreeRetriesThenSendsToDeadLetterQueue` verifica 1 intento inicial + 3 reintentos exactamente en la secuencia 2/8/32 s y el registro en `shipment_dead_letters`. Prueba en verde dentro de `./mvnw clean verify` (24/24, 2026-09-22). El adaptador ahora comparte los modos configurables de BB-06. | Kevin/Andy — revisión cruzada pendiente |
| EaC-06 | Confiabilidad — entrega eventual a 32 POS | Acuse ≤ 5 min; reintento persistente, alerta antes de 24 h | **Parcial** | POS-01 persiste entrega, acuse, intentos y deduplicación para tiendas 1–32; `PosIntegrationServiceTest` verifica éxito y 3 reintentos hasta `DEAD_LETTER`. Falta probar el límite de 5 minutos con infraestructura real, la alerta antes de 24 h y la recuperación durable mediante BB-04. | Andy (infraestructura/alerta) y Edson (BB-04) |
| EaC-07 | Auditabilidad — reconstrucción SAT | Reconstruir quién/cuándo/qué; evidencia < 1 s (EaC-02) | **Parcial** | AUD-01/AUD-02 registran y consultan actor, instante, acción, resultado, antes/después y correlación; `incidents` ya deja evidencia y el acceso a la evidencia queda registrado. Falta que pagos, fiscal y logística sumen sus acciones para reconstruir el ciclo completo de CDU-12. | Edson Sosa y dueños de cada módulo |
| EaC-08 | Disponibilidad parcial — falla del proveedor de notificaciones | 0 operaciones principales bloqueadas | **Cubierto** | `NotificationService.requestNotification` responde `202 Accepted` de inmediato y despacha en un `Executor` propio; `NotificationServiceTest` verifica el reintento único ante `NotificationProviderUnavailableException`. Prueba de humo real: `POST /api/v1/notifications` devolvió `202` sin esperar al proveedor simulado (2026-09-22). | Kevin — cerrado en este incremento |
| EaC-09 | Mantenibilidad — equipo interno | Despliegue con runbook ≤ 2 h; diagnóstico de falla inyectada ≤ 30 min | **Parcial** | `docker compose up --build` reproducible con health checks verdes (Jairo, BB-01/BB-05) y ahora incluye `frontend` (Kevin). No se ha cronometrado un despliegue por una persona no autora ni un diagnóstico de falla inyectada. | Jorge Pumay (EAC-09, tarea explícita en su tabla) |
| EaC-10 | Costo — infraestructura ≤ $1,200/mes | Costo operativo ≤ límite o excepción documentada | **No evaluado** | No existe despliegue en un ambiente medible con costo real; solo Docker Compose local. No se inventa una cifra. | Equipo, al planificar el despliegue real |
| EaC-11 | Portabilidad — on-premises a nube | Mismo artefacto contenedorizado en nube equivalente, sin reescribir el núcleo | **No evaluado** | El backend ya es un artefacto Docker único (BB-05), condición necesaria pero no ejercitada contra un entorno de nube. | Jairo Gómez (release/infra) |
| EaC-12 | Seguridad — datos de pago | 0 apariciones de PAN/CVV en BD, mensajes o logs | **Cubierto** | SEC-01: `SensitiveDataLeakScanTest` genera actividad real (pago, incidencia, despacho, notificación, fiscal, auditoría y 401), rechaza PAN/CVV con `400` y escanea todas las columnas de texto/JSON del esquema público, los payloads del Outbox, los estados de auditoría y los logs capturados: 0 apariciones. `SecurityAuthorizationTest` cubre la matriz 401/403 y `SensitiveDataSanitizerTest` el enmascarado. Evidencia en `docs/evidence/security/SEC-01-scan-pan-cvv.md`. | Edson Sosa — cerrado en este incremento |

## 3. Resumen

| Estado | Cantidad |
|---|---:|
| Cubierto | 6 (EaC-01, EaC-02, EaC-04, EaC-05, EaC-08, EaC-12) |
| Parcial | 4 (EaC-03, EaC-06, EaC-07, EaC-09) |
| Bloqueado | 0 |
| No evaluado | 2 (EaC-10, EaC-11) |

EaC-05 y EaC-08 permanecen cubiertos. La entrega local de Andy aporta evidencia parcial a EaC-06 y EaC-12, pero no se marcan cubiertos sin infraestructura real, alertas y los módulos de Edson. Esta matriz debe releerse después de revisión e integración.

## 4. Próxima actualización

Actualizar esta matriz en cada daily (sección 11 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`) cuando cualquier integrante reporte una entrega que cambie el estado de una fila. Ninguna tarjeta que afirme cumplir un EaC pasa a `Done` sin que esta tabla lo respalde (regla explícita de la sección 8.3 de responsabilidades).
