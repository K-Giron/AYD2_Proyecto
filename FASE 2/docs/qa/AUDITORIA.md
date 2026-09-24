# Auditoría de cumplimiento — Hoja de Calificación Fase 2

**Fecha de corte:** 2026-09-23

**Fuentes normativas:** `Docs/DDA.md` y `HC - Fase 2.pdf`

**Evidencia inspeccionada:** código de `develop` en `1294231`, pruebas, migraciones, diagramas, README, bitácora, planificación y exportación local de Trello.
**Alcance:** diagnóstico interno; el punteo definitivo corresponde al catedrático.

## 1. Veredicto ejecutivo

La fase no debe presentarse todavía como cumplimiento total del DDA. El camino
principal carrito → orden → reserva → pago → SAT → consumo de inventario →
despacho está implementado y automatizado, pero cuatro recorridos del DDA no
están cerrados de extremo a extremo:

1. Orden confirmada → registro automático en ERP → lote de 30 minutos.
2. Evento confirmado en Outbox → trabajador → POS destinatario.
3. Devolución → reembolso monetario → reposición → nota de crédito → aviso.
4. Proyección consolidada y auditoría con pago, fiscal, despacho, entrega e
   incidencia como productores automáticos.

Además, varios documentos aún describen Saga, seguridad, CI y pruebas como
pendientes aunque ya existen. Esta incoherencia amenaza directamente los 10
puntos de diagramas/documentación y puede activar el ajuste por trazabilidad.

## 2. Evidencia fuerte ya disponible

- `OrderSagaService` coordina Inventory, Payments, Fiscal y Logistics por
  puertos públicos e idempotencia.
- La suite local terminó con **148 pruebas, 0 fallos, 0 errores y 0 omitidas**.
- GitHub Actions `35944837459`, para `1294231`, terminó verde en pruebas y
  smoke test de Docker Compose.
- Hay cinco recorridos HTTP E2E deterministas en `OrderJourneysE2ETest`.
- Inventory representa 2 centros y 32 tiendas, tiene reserva atómica,
  concurrencia por última unidad y expiración configurable.
- Payments conserva intentos, timeout, circuit breaker, resultado
  indeterminado y conciliación idempotente sin segundo cobro.
- Fiscal genera XML, CUI/serie y conserva resultado SAT.
- Logistics genera guía simulada, tracking, backoff y registro fallido.
- JWT, roles, escaneo de PAN/CVV, Outbox/Inbox y auditoría append-only tienen
  pruebas automatizadas.
- El modelo ER objetivo editable incluye el alcance completo documentado y el
  DDA lo referencia en §6.3.

## 3. Matriz estimada de la hoja de calificación

La columna estimada es conservadora y no constituye una nota oficial.

| Rubro | Máximo | Estimado actual | Estado y riesgo principal |
|---|---:|---:|---|
| Confirmar orden | 5 | 5 | Saga, estados, historial y despacho automático presentes |
| Recibir pago | 3 | 3 | Puerto PayKlik, persistencia y resultado presentes |
| Reintentar/conciliar pago | 2 | 2 | Timeout, circuit breaker, conciliación y Outbox probados |
| Emitir factura | 3 | 3 | XML, CUI/serie y SAT asíncrono presentes |
| Consultar factura | 2 | 2 | Persistencia y consulta por orden presentes |
| Continuar en otro canal | 3 | 3 | Carrito por cliente recuperable desde distintos canales |
| Agregar/quitar productos | 2 | 1 | CRUD presente; no verifica/reserva cada alta ni libera al quitar como exige CDU-01 |
| Consultar inventario | 3 | 3 | Vista de 34 ubicaciones y existencias |
| Reservar producto | 2 | 2 | Operación atómica y prueba concurrente |
| Liberar inventario | 2 | 2 | Expiración configurable y compensación |
| Actualizar inventario | 3 | 1 | Consumo/liberación presentes; faltan entradas y propagación automática a POS |
| Solicitar LogiExpress | 3 | 3 | Adaptador simulado, guía y reintentos |
| Asignar origen/destino | 3 | 2 | Selección determinista por stock; no optimiza distancia al destino |
| Actualizar envío | 2 | 2 | Estados, tracking, persistencia y reintentos |
| Consultar estado <5 s | 2 | 1 | Rendimiento probado; la proyección automática solo consume estado de Orders |
| Registrar atención | 1 | 1 | Incidencias/devoluciones auditables, resolución administrativa |
| Consultar historial | 2 | 1 | Órdenes e incidencias consultables, sin historial consolidado por cliente |
| Sincronizar ERP | 3 | 1.5 | Scheduler/lote/reintento existen; alta de venta aún es manual |
| Informe de ventas | 4 | 0.5 | Hay datos/lotes auditables, pero no un informe conciliado implementado |
| **Funcionalidades** | **50** | **≈39** | Principal deuda: automatización transversal y postventa completa |
| Daily/bitácora | 1 | 0.5 | Evidencia amplia, pero días/horas de varios integrantes siguen pendientes |
| Sprint Planning | 4 | 3 | Plan y backlog existen; exportación local no refleja movimientos reales |
| Retrospectiva y burndown | 4 | 0.5 | Sección individual y evidencia final siguen vacías |
| Release | 1 | 0 | No existe cierre release/main/tag y los commits no siguen todos `#carnet` |
| **Scrum** | **10** | **≈4** | La evidencia administrativa es recuperable antes de entregar |
| Coherencia de diagramas | 10 | 5–7 | ER fuerte; Componentes/Despliegue y documentos no están reconciliados con el código final |
| Feedback Fase 1 | 6 | 4–5 | Hay matrices y evidencia; falta cierre/aprobación inequívoca |
| Modularidad/capas | 2 | 2 | Monolito modular y prueba de arquitectura |
| Frameworks/patrones | 1 | 1 | Spring Modulith, Outbox, Saga, puertos/adaptadores |
| Legibilidad/mantenibilidad | 1 | 1 | Nombres, pruebas y configuración profesionales |
| **Documentación** | **20** | **≈13–16** | Riesgo alto por contradicciones, no por falta total de artefactos |

**Rango base estimado al corte: 56–59 de 80.** Puede aumentar con rapidez al
cerrar Scrum/documentación; para acercarse al máximo funcional sí se requiere
desarrollo adicional. A esto se suman hasta 20 puntos individuales de defensa.

## 4. Brechas exactas frente al DDA

### P0 — necesarias para afirmar cumplimiento funcional integral

1. **ERP automático.** CDU-05 exige seleccionar ventas confirmadas sin
   sincronizar. Actualmente `POST /integrations/erp/sales` registra la venta de
   forma manual. Crear consumidor idempotente del evento confirmado que invoque
   `registerConfirmedSale`; conservar el scheduler de 30 minutos.
2. **POS desde Outbox.** CDU-06 establece como precondición un evento confirmado
   en Outbox y un trabajador que determine la tienda. Actualmente el endpoint
   `/integrations/pos/events` inicia el flujo manualmente. Crear consumidor
   RabbitMQ/Inbox y demostrar ACK, duplicado, reintento y DLQ.
3. **Devolución completa.** CDU-10 requiere elegibilidad, cálculo,
   reembolso/cambio, actualización de inventario, documento fiscal relacionado
   y notificación. `IncidentService.resolve` solo cambia el estado y guarda un
   monto administrativo.
4. **Consulta consolidada real.** CDU-09 exige compra, pago, despacho, entrega e
   incidencia. Solo `ORDER_STATUS_CHANGED` alimenta automáticamente la
   proyección; las otras fuentes dependen de una ingesta temporal manual.
5. **Auditoría transversal completa.** CDU-12 exige reconstruir orden, pago,
   fiscal, despacho y postventa. Orders, Payments e Incidents aportan evidencia;
   Fiscal y Logistics aún deben escribirla directamente.
6. **Notificaciones por el ciclo completo.** CDU-11 exige confirmación,
   preparación, tránsito, entrega e incidencia. El consumidor actual solo emite
   `ORDER_CONFIRMED`.
7. **Informe de ventas conciliado.** Implementar consulta/reporte con ventas,
   pago, factura, sincronización ERP y estado de conciliación; probar y trazar al
   modelo ER y despliegue.

### P1 — necesarias para proteger documentación y trazabilidad

1. Reconciliar `README.md`: elimina afirmaciones obsoletas de que Saga,
   despacho, compensaciones, CI o consumidores no existen.
2. Actualizar `release-readiness.md`; actualmente niega seguridad, carga y CI ya
   implementados, y a la vez marca QA-04 completo sin ERP/POS en el mismo flujo.
3. Cambiar contratos/propuestas de Orders de “propuesto/no conectado” al estado
   realmente implementado y registrar aprobaciones.
4. Actualizar `trazabilidad-cun-mvp.md`, la matriz EaC y conteos de pruebas con
   el resultado de 148.
5. Regenerar o anotar Componentes y Despliegue para que representen la topología
   ejecutable real: frontend nginx, JAR modular, PostgreSQL, RabbitMQ,
   consumidores actuales, simuladores y límites aceptados.
6. Ejecutar el gate por cada rubro: **CDU expandido → ER → Despliegue → código →
   prueba**. Si una capacidad no usa persistencia propia, justificarlo
   expresamente para evitar una penalización artificial.

### P1 — Scrum y evidencia de entrega

1. Completar retrospectiva individual de los cinco integrantes.
2. Adjuntar captura inicial y final reales del Kanban y generar el burndown con
   datos reales, sin reconstruir horas inventadas.
3. Actualizar Trello: el JSON versionado coloca todas las tarjetas en `To Do` y
   no sirve como evidencia del estado actual.
4. Conseguir las aprobaciones requeridas de ADR-009 a ADR-014 y la validación de
   despliegue por una persona no autora.
5. Cerrar REL-01 solo cuando el equipo autorice release/main/tag. No reescribir
   historial para fingir el formato `#carnet`; documentar la excepción y aplicar
   la convención a los commits finales.

### P2 — robustez y consistencia

1. Corregir el manejador HTTP global para que headers/parámetros requeridos
   ausentes respondan 400 y no 500.
2. Alinear CDU-01: verificar disponibilidad al agregar y definir reserva/liberación
   vinculada al carrito, o documentar y aprobar explícitamente el punto de reserva
   en checkout sin cambiar el alcance silenciosamente.
3. Mejorar el algoritmo de origen con distancia/costo respecto del destino, o
   justificar que la heurística actual es el criterio óptimo aceptado.
4. Ejecutar EaC-03 sobre el camino transaccional de ventas, no solo Query/Audit;
   documentar que una prueba en una instancia no demuestra balanceo horizontal.
5. Validar EaC-10 (costo) y EaC-11 (portabilidad) con evidencia reproducible o
   conservarlos honestamente como no demostrados.

## 5. Cinco recorridos E2E que debe mostrar la entrega

1. Compra feliz completa: catálogo → login → carrito → reserva → PayKlik → SAT →
   despacho/guía → notificación → POS → ERP → consulta consolidada → auditoría.
2. PayKlik tarda/falla: orden preservada → pago indeterminado → conciliación con
   la misma clave → continuación sin doble cobro.
3. Dos clientes compiten por la última unidad: una reserva gana, la otra recibe
   indisponibilidad; no hay sobreventa.
4. LogiExpress falla: preparación continúa, reintentos 2/8/32, éxito posterior o
   DLQ visible y auditable.
5. Devolución: orden entregada → incidencia/devolución parcial o total →
   reembolso → reposición → nota de crédito → notificación → consulta/auditoría.

Los cinco E2E actuales son valiosos, pero no sustituyen el recorrido de diez
pasos de QA-04 mientras ERP y POS estén fuera de la misma ejecución observable.

## 6. Orden recomendado de cierre

1. Integrar ERP y POS al evento confirmado.
2. Completar Query/Audit/Notifications con productores de Fiscal, Logistics e
   Incidents.
3. Implementar informe de ventas.
4. Completar devolución/reembolso o declarar con honestidad su impacto de nota.
5. Automatizar un E2E de diez pasos y conservar los otros cinco como regresión.
6. Reconciliar documentación y diagramas contra el código final.
7. Completar retrospectiva, Kanban final, burndown, aprobaciones y revisión por
   no autor.
8. Ejecutar `clean verify`, smoke Compose y demo desde ambiente limpio; después
   realizar REL-01.
