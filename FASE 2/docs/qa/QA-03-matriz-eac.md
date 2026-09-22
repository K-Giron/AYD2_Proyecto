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
| EaC-01 | Rendimiento — consulta de orden | 100 % de consultas ≤ 5 s, con p50/p95/p99 | **Parcial** | `GET /api/v1/orders/{orderId}/summary` implementado (QRY-01) y probado funcionalmente: `docker compose up` real, respuesta en un solo salto de base de datos indexada por PK, `HTTP 200` en la prueba de humo del 2026-09-22. Falta el escenario de validación con datos reales de Orders/Payments/Fiscal y medición p50/p95/p99 bajo carga. | Kevin (medición formal) junto con QA-02 (Edson, ambiente de carga) |
| EaC-02 | Rendimiento/Auditabilidad — evidencia de transacción | < 1 s | **Bloqueado** | AUD-01 (registro de auditoría) no existe todavía en el repositorio. | Edson Sosa (AUD-01) |
| EaC-03 | Escalabilidad — Black Friday 8x | Soportar 8x sin degradar EaC-01/EaC-02 | **Bloqueado** | Requiere plataforma estable con todos los módulos P0 y la prueba de carga `T`/`8T` (QA-02). No hay ambiente para ejercitarlo. | Edson Sosa (QA-02), equipo completo |
| EaC-04 | Disponibilidad/Resiliencia — falla de PayKlik | 0 órdenes perdidas; reintento/conciliación ≤ 60 s | **Bloqueado** | PAY-01/PAY-02 (Edson) no existen en el repositorio. | Edson Sosa |
| EaC-05 | Confiabilidad — timeout LogiExpress (2 %) | 0 pedidos bloqueados; hasta 3 reintentos (2/8/32 s) antes de mensajes fallidos | **Cubierto** | `SimulatedLogiExpressAdapter` reproduce ~2 % de timeout; `ShipmentServiceTest.exhaustsThreeRetriesThenSendsToDeadLetterQueue` verifica 1 intento inicial + 3 reintentos exactamente en la secuencia 2/8/32 s y el registro en `shipment_dead_letters`. Prueba en verde dentro de `./mvnw clean verify` (24/24, 2026-09-22). | Kevin — cerrado en este incremento; sustituir el simulador por BB-06 cuando Andy lo entregue |
| EaC-06 | Confiabilidad — entrega eventual a 32 POS | Acuse ≤ 5 min; reintento persistente, alerta antes de 24 h | **Bloqueado** | POS-01 (Andy) no existe en el repositorio. | Andy Sánchez |
| EaC-07 | Auditabilidad — reconstrucción SAT | Reconstruir quién/cuándo/qué; evidencia < 1 s (EaC-02) | **Bloqueado** | Depende de AUD-01 (Edson). `query` guarda `asOf` por campo, pero no es el registro de auditoría formal que exige el DDA. | Edson Sosa (AUD-01) |
| EaC-08 | Disponibilidad parcial — falla del proveedor de notificaciones | 0 operaciones principales bloqueadas | **Cubierto** | `NotificationService.requestNotification` responde `202 Accepted` de inmediato y despacha en un `Executor` propio; `NotificationServiceTest` verifica el reintento único ante `NotificationProviderUnavailableException`. Prueba de humo real: `POST /api/v1/notifications` devolvió `202` sin esperar al proveedor simulado (2026-09-22). | Kevin — cerrado en este incremento |
| EaC-09 | Mantenibilidad — equipo interno | Despliegue con runbook ≤ 2 h; diagnóstico de falla inyectada ≤ 30 min | **Parcial** | `docker compose up --build` reproducible con health checks verdes (Jairo, BB-01/BB-05) y ahora incluye `frontend` (Kevin). No se ha cronometrado un despliegue por una persona no autora ni un diagnóstico de falla inyectada. | Jorge Pumay (EAC-09, tarea explícita en su tabla) |
| EaC-10 | Costo — infraestructura ≤ $1,200/mes | Costo operativo ≤ límite o excepción documentada | **No evaluado** | No existe despliegue en un ambiente medible con costo real; solo Docker Compose local. No se inventa una cifra. | Equipo, al planificar el despliegue real |
| EaC-11 | Portabilidad — on-premises a nube | Mismo artefacto contenedorizado en nube equivalente, sin reescribir el núcleo | **No evaluado** | El backend ya es un artefacto Docker único (BB-05), condición necesaria pero no ejercitada contra un entorno de nube. | Jairo Gómez (release/infra) |
| EaC-12 | Seguridad — datos de pago | 0 apariciones de PAN/CVV en BD, mensajes o logs | **Bloqueado** | BB-07 (autenticación/sanitización) y PAY-01 (Edson) no existen todavía; no hay flujo de pago que verificar. | Edson Sosa (BB-07, SEC-01) |

## 3. Resumen

| Estado | Cantidad |
|---|---:|
| Cubierto | 2 (EaC-05, EaC-08) |
| Parcial | 2 (EaC-01, EaC-09) |
| Bloqueado | 6 (EaC-02, EaC-03, EaC-04, EaC-06, EaC-07, EaC-12) |
| No evaluado | 2 (EaC-10, EaC-11) |

De los doce EaC, los dos que dependen exclusivamente de los módulos de Kevin (EaC-05, EaC-08) están cubiertos con prueba automatizada. Los demás dependen de entregas de Jorge, Edson y Andy que, a la fecha de este corte, no existen en `develop` (ver `BITACORA_FASE_2.md`). Esta matriz debe releerse y actualizarse cada vez que otro integrante entregue su módulo — no se marca `Cubierto` por adelantado.

## 4. Próxima actualización

Actualizar esta matriz en cada daily (sección 11 de `RESPONSABILIDADES_INTEGRANTES_FASE_2.md`) cuando cualquier integrante reporte una entrega que cambie el estado de una fila. Ninguna tarjeta que afirme cumplir un EaC pasa a `Done` sin que esta tabla lo respalde (regla explícita de la sección 8.3 de responsabilidades).
