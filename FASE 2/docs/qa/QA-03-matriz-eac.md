# QA-03 — Matriz de Escenarios de Atributos de Calidad (EaC-01 a EaC-12)

| Campo | Valor |
|---|---|
| Responsable | Kevin Girón (PO-QA) |
| Fuente | [`Docs/DDA.md`](../../../Docs/DDA.md), §4.2 |
| Corte | 2026-09-23 |
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
| EaC-01 | Rendimiento — consulta de orden | 100 % de consultas ≤ 5 s, con p50/p95/p99 | **Parcial** | La proyección QRY-01 recibe estados de Orders mediante RabbitMQ e Inbox idempotente, y la UI complementa temporalmente con las demás API. Falta conectar pagos/despacho/entrega/incidencias por eventos y medir p50/p95/p99. | Kevin (medición formal) junto con QA-02 |
| EaC-02 | Rendimiento/Auditabilidad — evidencia de transacción | < 1 s | **Parcial** | `audit_events` conserva actor, instante, cambio, resultado, correlación y hash; `GET /api/v1/audit/events` consulta por orden/transacción y `audit_evidence_access` registra búsqueda/exportación. Falta medición formal menor a un segundo y ampliar productores más allá de las transiciones de orden. | Equipo: ampliar AUD-01 y QA de rendimiento |
| EaC-03 | Escalabilidad — Black Friday 8x | Soportar 8x sin degradar EaC-01/EaC-02 | **Bloqueado** | Requiere plataforma estable con todos los módulos P0 y la prueba de carga `T`/`8T` (QA-02). No hay ambiente para ejercitarlo. | Edson Sosa (QA-02), equipo completo |
| EaC-04 | Disponibilidad/Resiliencia — falla de PayKlik | 0 órdenes perdidas; reintento/conciliación ≤ 60 s | **Parcial** | `PaymentService` persiste el intento antes de invocar PayKlik, conserva `INDETERMINATE`, aplica Circuit Breaker y programa conciliación; `PaymentServiceTest` y `SimulatedPayKlikAdapterTest` están verdes. Falta evidencia temporal de la ventana completa de 60 s bajo falla sostenida. | QA de resiliencia |
| EaC-05 | Confiabilidad — timeout LogiExpress (2 %) | 0 pedidos bloqueados; hasta 3 reintentos (2/8/32 s) antes de mensajes fallidos | **Cubierto** | `SimulatedLogiExpressAdapter` reproduce ~2 % de timeout; `ShipmentServiceTest.exhaustsThreeRetriesThenSendsToDeadLetterQueue` verifica 1 intento inicial + 3 reintentos exactamente en la secuencia 2/8/32 s y el registro en `shipment_dead_letters`. La suite completa terminó con 87 pruebas verdes el 2026-09-23. | Kevin/Andy — revisión cruzada pendiente |
| EaC-06 | Confiabilidad — entrega eventual a 32 POS | Acuse ≤ 5 min; reintento persistente, alerta antes de 24 h | **Parcial** | POS-01 persiste entrega, acuse, intentos y deduplicación para tiendas 1–32; `PosIntegrationServiceTest` verifica éxito y 3 reintentos hasta `DEAD_LETTER`. Falta probar el límite de 5 minutos con infraestructura real, la alerta antes de 24 h y la recuperación durable mediante BB-04. | Andy (infraestructura/alerta) y Edson (BB-04) |
| EaC-07 | Auditabilidad — reconstrucción SAT | Reconstruir quién/cuándo/qué; evidencia < 1 s (EaC-02) | **Parcial** | `order_status_history` y `audit_events` reconstruyen las transiciones de orden con estados previo/nuevo, resultado y hash SHA-256; `audit_evidence_access` registra búsquedas fallidas y exportaciones. Falta incorporar como productores directos pagos, fiscal, despacho e incidencias, y medir latencia. | Equipo: ampliar AUD-01 transversal |
| EaC-08 | Disponibilidad parcial — falla del proveedor de notificaciones | 0 operaciones principales bloqueadas | **Cubierto** | `NotificationService.requestNotification` responde `202 Accepted` de inmediato y despacha en un `Executor` propio; `NotificationServiceTest` verifica el reintento único ante `NotificationProviderUnavailableException`. Prueba de humo real: `POST /api/v1/notifications` devolvió `202` sin esperar al proveedor simulado (2026-09-22). | Kevin — cerrado en este incremento |
| EaC-09 | Mantenibilidad — equipo interno | Despliegue con runbook ≤ 2 h; diagnóstico de falla inyectada ≤ 30 min | **Parcial** | `docker compose up --build` reproducible con health checks verdes (Jairo, BB-01/BB-05) y ahora incluye `frontend` (Kevin). No se ha cronometrado un despliegue por una persona no autora ni un diagnóstico de falla inyectada. | Jorge Pumay (EAC-09, tarea explícita en su tabla) |
| EaC-10 | Costo — infraestructura ≤ $1,200/mes | Costo operativo ≤ límite o excepción documentada | **No evaluado** | No existe despliegue en un ambiente medible con costo real; solo Docker Compose local. No se inventa una cifra. | Equipo, al planificar el despliegue real |
| EaC-11 | Portabilidad — on-premises a nube | Mismo artefacto contenedorizado en nube equivalente, sin reescribir el núcleo | **No evaluado** | El backend ya es un artefacto Docker único (BB-05), condición necesaria pero no ejercitada contra un entorno de nube. | Jairo Gómez (release/infra) |
| EaC-12 | Seguridad — datos de pago | 0 apariciones de PAN/CVV en BD, mensajes o logs | **Parcial** | `payment_attempts` conserva únicamente `payment_reference`; el adaptador rechaza PAN de 13–19 dígitos y texto con CVV mediante pruebas automatizadas. Falta escaneo completo de BD/mensajes/logs y seguridad backend por roles. | Equipo: BB-07 y SEC-01 |

## 3. Resumen

| Estado | Cantidad |
|---|---:|
| Cubierto | 2 (EaC-05, EaC-08) |
| Parcial | 7 (EaC-01, EaC-02, EaC-04, EaC-06, EaC-07, EaC-09, EaC-12) |
| Bloqueado | 1 (EaC-03) |
| No evaluado | 2 (EaC-10, EaC-11) |

EaC-05 y EaC-08 permanecen cubiertos. Órdenes, pagos y el historial auditable ya eliminan bloqueos documentales anteriores, pero no se marcan completamente cubiertos sin mediciones, Outbox, auditoría transversal y seguridad backend. La revisión contra penalizaciones se mantiene en `QA-06-gate-trazabilidad-penalizaciones.md`.

## 4. Próxima actualización

Actualizar esta matriz en cada daily (sección 11 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`) cuando cualquier integrante reporte una entrega que cambie el estado de una fila. Ninguna tarjeta que afirme cumplir un EaC pasa a `Done` sin que esta tabla lo respalde (regla explícita de la sección 8.3 de responsabilidades).
