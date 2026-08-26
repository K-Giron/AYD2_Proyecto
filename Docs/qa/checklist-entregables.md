# Checklist de QA final — SGO-Logic Fase 1

**Dueño:** Persona 5 (Rol E) · **Fuente:** enunciado §4.4 (Entregables) y estado real del repo al momento de esta revisión.

> Usar antes de la entrega: marcar solo lo que está verificado en el DDA/repo, no lo que "se supone que ya está".

## Entregable 1 — Documento de Definición de Arquitectura (DDA)

| Apartado exigido | Estado verificado en `Docs/DDA.md` | Acción de QA pendiente |
|---|---|---|
| Identificación del caso de negocio (CUN + primera descomposición) | ⏳ Lista textual de 12 CUN y macroprocesos (§2.5–2.6); **falta el diagrama real** (§2.4 sigue `[TODO]`) | Confirmar con Persona 2 si el diagrama ya existe en otra rama antes de dar por cerrada esta fila |
| Listado de stakeholders y preocupaciones | ✅ Completo (§3), con entidades externas separadas (§3.1) | Validar que el equipo no tenga altas/bajas pendientes |
| Características del sistema priorizadas | ✅ Completo (§4) | — |
| Drivers Requisitos Funcionales (CDU expandidos) | ⏳ Solo 1 de varios CDU expandido (CDU-03, §5.2); el resto en `[TODO]` (§5.3) | Verificar con Persona 2 cuántos CDU adicionales alcanzan a cerrarse antes de la entrega |
| Drivers de Calidad (EaC reflejados en despliegue) | ✅ 12 EaC resumidos (§6.3) + 1 desarrollado a profundidad (§6.2); **sí están reflejados** en el diagrama de despliegue (§11.3, confirmado por Persona 4) | Verificar si el resto de EaC necesita el formato completo de 6.1 o si el resumen de 6.3 se acepta como suficiente |
| Drivers de Restricción | ✅ Completo (§7) | — |
| Matrices de Trazabilidad | ✅ Precargada con datos reales (§9), con pendientes explícitos en §9.2 | Cerrar cuando ADR-002 a 005 tengan contenido definitivo |

## Entregable 2 — Diagramas Arquitectónicos

| Diagrama | Estado | Acción de QA pendiente |
|---|---|---|
| Contexto | ✅ Entregado (`Docs/diagramas/contexto.svg`) | Confirmar que actores coinciden con los que use Persona 2 en el diagrama de CUN cuando exista |
| Componentes | ✅ Entregado (`componentes.svg`) | Confirmar que los módulos coinciden con los nombrados en ADR-001 |
| Despliegue | ✅ Entregado (`despliegue.svg`), con tabla EaC↔elemento visible | Verificar en vivo, señalando cada EaC crítico, antes del ensayo |
| Bloques | ✅ Entregado (`bloques.svg`) | Confirmar que es coherente con Contexto/Componentes/Despliegue (mismo estilo, misma frontera) |
| Justificación de decisiones (ADR) | ⏳ ADR-001 completo y "propuesto"; ADR-002 a 005 sin redactar (§10.3) | Decidir con Persona 3 si se redactan versiones cortas de 002–005 antes de la entrega o si se declara explícitamente como trabajo en progreso |

## Entregable 3 — Presentación Oral y Defensa

| Requisito | Estado | Acción de QA pendiente |
|---|---|---|
| 45 minutos, todos exponen | ✅ Guion armado (`Docs/presentacion/guion-45min.md`) | Ensayar y cronometrar; confirmar nombres reales |
| Justifica decisiones del DDA | ✅ Cada bloque del guion cita la sección del DDA que respalda | — |
| Ronda de preguntas de 5 min | ✅ Banco de preguntas simuladas incluido en el guion | Ampliar el banco si el ensayo revela puntos débiles |

## Entregable 4 — Prototipo arquitectónico

| Requisito | Estado | Acción de QA pendiente |
|---|---|---|
| Mockups no funcionales | ✅ Entregado (`Docs/prototipo/mockups.html`) | Contrastar contra CDU expandidos de Persona 2 cuando existan, para evitar contradicciones |
| Flujo principal | ✅ Cubre carrito → pago → consulta → devolución en un solo recorrido | — |

## Tablero Kanban y repositorio

| Requisito | Estado | Acción de QA pendiente |
|---|---|---|
| 6 columnas obligatorias | ✅ `Docs/tablero-kanban-megastore-fase-1.json` ya existe en el repo | Confirmar que refleja el estado real (no solo lo planificado) antes de la entrega |
| Nomenclatura oficial del repo | ✅ `AYD2_A_2S2025_PROYECTO_G5` | — |

## Consistencia cruzada (lo que específicamente le toca a Persona 5 revisar)

- [ ] Ningún CDU de §5 contradice las pantallas del prototipo (`Docs/prototipo/mockups.html`).
- [ ] Los actores del futuro diagrama de CUN coinciden con los del diagrama de Contexto (`Docs/diagramas/contexto.svg`).
- [ ] Todo EaC marcado como crítico en §8.2 aparece efectivamente en el diagrama de despliegue (`Docs/diagramas/README.md`, tabla de correspondencia).
- [ ] El estado de ADR-001 ("propuesto") es el mismo en §10.2, en la matriz de §9 y en el guion de presentación — si el equipo lo acepta antes de entregar, actualizar los tres lugares.
- [ ] El Resumen Ejecutivo (§1) sigue siendo cierto después de cualquier cambio de última hora en alcance o arquitectura.
