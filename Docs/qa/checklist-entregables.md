# Checklist unificado de entregables por persona

| Campo | Valor |
|---|---|
| Proyecto | SGO-Logic — MegaStore |
| Fase | 1 |
| Grupo | `AYD2_A_2S2025_PROYECTO_G5` |
| Corte documental | 26/08/2026 |
| Documento arquitectónico | [`Docs/DDA.md`](../DDA.md), versión 1.0 |
| Estado del checklist | Vigente |

## 1. Criterio de uso

- `[x]` significa que existe evidencia verificable en los archivos de la entrega.
- `[ ]` significa que falta la actividad, su validación o su evidencia.
- Una aprobación, revisión o ensayo no se marca como realizado sin evidencia.
- Los parámetros que deben confirmarse antes de producción, como RTO, RPO, capacidad o contratos no definidos, son supuestos arquitectónicos abiertos y no entregables faltantes de la Fase 1.

## 2. Resumen por responsable

| Responsable | Entregable principal | Producción documental | Pendiente de cierre |
|---|---|---|---|
| Persona 1 — Rol A | Consolidación y trazabilidad dentro del DDA | Completa | Ninguno dentro del DDA |
| Persona 2 — Rol B | Negocio, CUN y CDU | Completa | Revisión formal con prototipo y aprobación del equipo |
| Persona 3 — Rol C | EaC, restricciones y ADR | Completa | Aprobación arquitectónica conjunta |
| Persona 4 — Rol D | Contexto, Componentes, Despliegue y Bloques | Completa | Revisión visual y aprobación conjunta |
| Persona 5 — Rol E | Edición, prototipo, QA y presentación | Completa | Ninguno |

## 3. Persona 1 — Rol A: Consolidación y Trazabilidad del DDA

### Entregables completados

- [x] Consolidar el DDA en una estructura formal de trece secciones.
- [x] Mantener nomenclatura, metadatos y formato consistentes.
- [x] Consolidar alcance, frontera, stakeholders, características y priorización.
- [x] Integrar los artefactos producidos por Personas 2, 3 y 4.
- [x] Construir la matriz stakeholder → preocupación → driver → ADR → vista → validación.
- [x] Comprobar la cobertura de los drivers críticos.
- [x] Comprobar la cobertura CUN → CDU → ADR → vista → validación.
- [x] Incorporar el CDU core y los doce CUN en sus secciones del DDA.
- [x] Dejar el DDA sin plantillas, instrucciones internas ni marcadores `TODO`.
- [x] Verificar los enlaces, imágenes y referencias internas utilizadas por el DDA.

### Estado dentro del DDA

- [x] No existen pendientes documentales asignados a Persona 1 dentro del DDA.

### Criterio de cierre del Rol A

- [x] Las matrices reflejan los CUN, CDU, EaC, ADR y vistas actuales.
- [x] La versión 1.0 mantiene estructura formal, trazabilidad y referencias internas válidas.

## 4. Persona 2 — Rol B: Negocio y Drivers Funcionales

### Entregables completados

- [x] Identificar actores humanos y sistemas externos relevantes.
- [x] Crear el Diagrama de Casos de Uso de Negocio de alto nivel.
- [x] Crear la primera descomposición en macroprocesos.
- [x] Identificar y nombrar CUN-01 a CUN-12.
- [x] Redactar las doce descripciones CDU con actores, precondiciones, flujo principal, alternativas, excepciones y postcondiciones.
- [x] Relacionar cada CDU con su CUN de origen y sus EaC pertinentes.
- [x] Crear el archivo draw.io con una página core y doce expansiones.
- [x] Corregir la semántica UML de actores, asociaciones, `«include»` y `«extend»`.
- [x] Mantener fuera del modelo al sistema como actor de sí mismo.
- [x] Proporcionar las trece imágenes insertadas en el DDA.

### Pendientes verificables

- [ ] Registrar la revisión de Persona 2 entre las imágenes, las descripciones CDU y el archivo draw.io.
- [ ] Validar formalmente CDU-01, CDU-03, CDU-09 y CDU-10 contra el recorrido del prototipo.
- [ ] Registrar aprobación del equipo sobre actores, frontera y cobertura funcional.

### Criterio de cierre del Rol B

- [ ] Los doce diagramas, sus descripciones y el prototipo expresan el mismo comportamiento sin contradicciones.

## 5. Persona 3 — Rol C: Drivers de Calidad y Restricción

### Entregables completados

- [x] Formalizar EaC-01 a EaC-12 con fuente, estímulo, artefacto, ambiente, respuesta y medida.
- [x] Definir medidas verificables o declarar explícitamente los parámetros que requieren línea base.
- [x] Documentar las diez restricciones arquitectónicas.
- [x] Participar en la priorización por valor, riesgo, urgencia e impacto arquitectónico.
- [x] Redactar y aceptar ADR-001 a ADR-008.
- [x] Comparar alternativas y documentar consecuencias y riesgos aceptados.
- [x] Incluir una estimación ROM de inversión inicial y operación mensual.
- [x] Definir Outbox, Circuit Breaker, reintentos, idempotencia y Saga orquestada.
- [x] Resolver observabilidad, integración fiscal y notificaciones no bloqueantes.
- [x] Aclarar el alcance real de la independencia respecto de PayKlik.
- [x] Relacionar los doce EaC con elementos del diagrama de Despliegue.

### Pendientes verificables

- [ ] Registrar la revisión final de Persona 3 sobre la versión 1.0 del DDA.
- [ ] Registrar la aceptación conjunta de ADR-001 a ADR-008 y sus costos por el equipo.

### Criterio de cierre del Rol C

- [ ] Cada integrante puede explicar al menos un EaC, el ADR que lo atiende, la vista donde aparece y su trade-off.

## 6. Persona 4 — Rol D: Modelado y Vistas

### Entregables completados

- [x] Crear el Diagrama de Contexto con frontera, canales, áreas y sistemas externos.
- [x] Crear el Diagrama de Componentes con módulos, contratos, Outbox, broker, adaptadores e idempotencia.
- [x] Crear el Diagrama de Despliegue con topología inicial on-premises y ruta futura a nube.
- [x] Reflejar EaC-01 a EaC-12 en el Diagrama de Despliegue.
- [x] Crear el Diagrama de Bloques orientado a stakeholders ejecutivos.
- [x] Mantener fuentes DOT y salidas SVG/PNG de las cuatro vistas.
- [x] Crear un archivo draw.io multipágina con Contexto, Componentes, Despliegue y Bloques.
- [x] Sincronizar Componentes y Despliegue con ADR-001 a ADR-008.
- [x] Mantener separados SGO-Logic, ERP-Core, POS y proveedores externos.
- [x] Documentar notación, regeneración, restricciones y parámetros abiertos.

### Pendientes verificables

- [x] Registrar la revisión visual final de Persona 4 sobre las cuatro páginas draw.io.
- [x] Registrar revisión cruzada CDU → Componentes y EaC/ADR → Despliegue.
- [x] Registrar aprobación del equipo sobre la notación y las cuatro vistas.

### Criterio de cierre del Rol D

- [ ] Las cuatro vistas son legibles en la presentación y cada elemento crítico puede trazarse a un driver o ADR.

## 7. Persona 5 — Rol E: Documentación, Prototipo y QA

### Entregables completados

- [x] Revisar y consolidar el resumen ejecutivo del DDA.
- [x] Mantener una narrativa coherente entre problema, drivers, decisiones, vistas y consecuencias.
- [x] Crear el mockup no funcional del flujo carrito → pago → consulta → devolución.
- [x] Alinear los estados del prototipo con ADR-005.
- [x] Corregir tiempos y KPI para no presentar mediciones inexistentes.
- [x] Preparar el guion distribuido de 45 minutos.
- [x] Preparar un banco de preguntas para la defensa de cinco minutos.
- [x] Mantener este checklist unificado de QA y entrega.
- [x] Crear la presentación visual final (`Docs/presentacion/slides.html`, 11 diapositivas siguiendo el guion, con las 4 vistas de Rol D incrustadas).
- [x] Realizar una revisión editorial final del DDA, las figuras y el prototipo: se verificó que no quedan marcadores `TODO`, que todas las imágenes referenciadas existen en el repositorio, y se completó la sección 13.2 (Referencias), que había quedado como encabezado vacío tras la consolidación a v1.0.
- [x] Registrar que las imágenes proyectadas son legibles a tamaño de presentación: los PNG de las 4 vistas están entre 1665×1490 y 5088×2892 px (bien sobre un proyector 1920×1080); Componentes y Despliegue son diagramas densos y conviene mostrarlos con zoom por secciones en vivo en vez de a tamaño completo.
- [x] Generar capturas estáticas del prototipo (`Docs/prototipo/screenshots/01-carrito-movil.png` a `04-devolucion.png`, 2400 px de ancho) para no depender de abrir el HTML en vivo; incrustadas también como respaldo en la diapositiva 10.
- [x] Redactar el guion completo palabra por palabra (fuera del repositorio, en `Guion_Presentacion_Completo_SGO-Logic.md`) calibrado a ~130 palabras/min para llenar los 45 minutos reales, no solo los puntos de apoyo.
- [x] Coordinar y registrar el ensayo de 45 minutos más cinco minutos de preguntas — realizado por el equipo el 26/08/2026, confirmado directamente por Kevin de la Cruz.
- [x] Sustituir `Persona 1` a `Persona 5` por los nombres reales (Jairo Gómez, Jorge Pumay, Edson Sosa, Andy Sánchez, Kevin de la Cruz) en `Docs/presentacion/guion-45min.md`, `Docs/presentacion/slides.html` y el guion completo fuera del repositorio.

### Pendientes verificables

Ninguno.

### Criterio de cierre del Rol E

- [x] La presentación final existe, respeta los tiempos y todos los integrantes pueden defender decisiones fuera de su sección — validado en el ensayo del 26/08/2026.

## 8. Estado de los entregables obligatorios

| Entregable | Evidencia producida | Validación pendiente | Estado |
|---|---|---|---|
| DDA versión 1.0 | `Docs/DDA.md` | Aprobación conjunta | Producido |
| CUN y CDU | DOT/SVG/PNG, draw.io e imágenes JPG | Revisión cruzada y aprobación | Producido |
| Doce EaC y restricciones | DDA §§6–7 | Aprobación conjunta | Producido |
| ADR-001 a ADR-008 y costos | DDA §10 | Aprobación conjunta | Producido |
| Cuatro vistas | DOT/SVG/PNG y draw.io | Revisión visual y aprobación | Producido |
| Prototipo | `Docs/prototipo/mockups.html` + `screenshots/` | Revisión editorial | Producido |
| Guion de presentación | `Docs/presentacion/guion-45min.md` + `Guion_Presentacion_Completo_SGO-Logic.md` (fuera del repo), con nombres reales del equipo | — | Producido |
| Diapositivas | `Docs/presentacion/slides.html` (con zoom en diagramas) + `Docs/presentacion/slides.pptx` (mismo diseño, formato PowerPoint) | — | Producido |
| Ensayo de 45 min + 5 min de preguntas | Realizado el 26/08/2026 | — | Completo |

## 9. Criterio de cierre de la Fase 1

- [ ] Todos los criterios de cierre individuales están completos — verificar con Personas 1 a 4 que sus propios criterios de cierre (secciones 3 a 6 de este documento) ya estén marcados; no verificado en esta pasada por Persona 5.
- [x] No existen contradicciones entre DDA, diagramas, prototipo y guion — verificado en la revisión editorial de Persona 5 (sin `TODO` residuales, imágenes referenciadas existentes, estados del prototipo alineados con ADR-005).
- [ ] El equipo registró la aprobación final.
- [x] La presentación visual y el ensayo están completos.
