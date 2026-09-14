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
- [ ] Las migraciones parten de una base vacía y preservan una existente.

## Funcionalidad y calidad

- [ ] Todas las líneas P0 están integradas.
- [ ] ORD-05 y QA-04 ejecutan el camino feliz completo.
- [ ] Fallas externas tienen evidencia y no causan pérdida silenciosa.
- [ ] Matriz EaC → prueba → resultado completa.
- [ ] No se presentan métricas, disponibilidad o integraciones reales no demostradas.

## Operación y documentación

- [ ] README probado por una persona no autora.
- [ ] `.env.example` contiene todas las variables y ningún secreto real.
- [ ] Compose termina con servicios saludables y se apaga sin `-v`.
- [ ] DDA, diagramas, contratos y código coinciden.
- [ ] Capturas inicial y final del Kanban disponibles.

## Autorización

- Responsable de integración: Jairo Gómez.
- Revisor: pendiente.
- Fecha y decisión: pendiente.
