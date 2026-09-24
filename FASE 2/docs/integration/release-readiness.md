# Checklist de preparación de release

**Tarjeta relacionada:** REL-01

**Estado actual:** `NOT READY`

## Gobierno

- [ ] Gate 0 cerrado con evidencia real.
- [ ] ADR-009 cuenta con al menos tres aprobaciones.
- [ ] ADR-010 a ADR-013 reflejan las decisiones implementadas.
- [ ] Kanban, dailies, bloqueos y burndown están actualizados.

## Construcción e integración

- [ ] `./mvnw clean verify` verde en CI.
- [ ] Workflow QA-01 ejecutado con secretos configurados.
- [ ] Smoke test desde un entorno limpio realizado por una persona no autora.
- [ ] No existen dependencias modulares no aprobadas.
- [x] Las migraciones parten de una base vacía y preservan una existente.

## Funcionalidad y calidad

- [ ] Todas las líneas P0 están integradas.
- [x] ORD-05 y QA-04 ejecutan el camino feliz completo.
- [ ] Fallas externas tienen evidencia versionada y no causan pérdida silenciosa; las pruebas automatizadas existen, falta consolidar artefactos E2E.
- [x] Matriz EaC → prueba → resultado completa y gate CUN → ER → despliegue → código → prueba creado en QA-06.
- [x] No se presentan métricas, disponibilidad o integraciones reales no demostradas.

## Operación y documentación

- [ ] README probado por una persona no autora.
- [ ] `.env.example` contiene todas las variables y ningún secreto real.
- [x] Compose termina con servicios saludables.
- [ ] Se registra el ensayo de apagado sin `-v` y reinicio conservando datos.
- [ ] DDA, diagramas, contratos y código coinciden.
- [ ] Capturas inicial y final del Kanban disponibles.

## Autorización

- Responsable de integración: Jairo Gómez.
- Revisor: pendiente.
- Fecha y decisión: pendiente.

## Bloqueos reales al corte 2026-09-23

- Outbox e Inbox implementados; Query y Notifications ya consumen eventos de estado de orden. Faltan consumidores RabbitMQ hacia ERP/POS y productores para despacho, incidencias y las demás fuentes de la proyección.
- Auditoría append-only, consulta y registro de accesos implementados para transiciones de orden; falta cobertura directa de pagos, fiscal, logística e incidencias y medición < 1 s.
- Seguridad backend y autorización por rol no implementadas.
- Evidencia de carga 8x, costo y portabilidad a nube no ejecutada.
- Compensación completa de devoluciones pendiente.

El release permanece `NOT READY` hasta cerrar o aceptar explícitamente estos puntos. El modelo ER no oculta los faltantes: los muestra en una página separada como estructuras objetivo pendientes.
