# Guion de presentación — SGO-Logic (45 min + 5 min de preguntas)

**Dueño:** Persona 5 (Rol E) · **Cierre coordinado con:** Persona 1 (Rol A)
**Basado en:** estructura preliminar del DDA §13.6

> Regla del equipo: cada integrante debe poder explicar al menos un driver, una decisión, una vista relacionada y la consecuencia de cambiarla — no solo su propia sección.

---

## 0–3 min · Apertura, objetivo y alcance — Persona 1

- Quiénes somos y qué es SGO-Logic en una frase: *"la capa de orquestación e integración omnicanal que se sienta sobre los sistemas legados de MegaStore sin modificarlos."*
- Una línea del problema: fragmentación entre tiendas, e-commerce, logística y finanzas; costo concreto (USD 150,000 en 45 min de caída, devoluciones de hasta 7 días).
- Alcance en una frase: qué entra (orquestación de orden/pago/despacho/facturación/notificación) y qué NO se toca (ERP-Core, POS, PayKlik, LogiExpress).
- Agenda de los 45 minutos.

**Evidencia a mostrar:** DDA §1 (Resumen Ejecutivo) y §2.2 (alcance).

---

## 3–10 min · Negocio y drivers funcionales — Persona 2

- Los 12 CUN agrupados en macroprocesos (Venta, Postventa, Facturación, Logística, Atención, Gobernanza).
- Recorrer el diagrama de CUN.
- Presentar 1–2 CDU expandidos a profundidad (sugerido: CDU-03 Procesar Pago, por su conexión directa con EaC-04) — flujo principal, alternativo y de excepción.
- Cerrar con la conexión explícita CDU → EaC (ej. "el flujo de excepción de pago existe precisamente por EaC-04").

**Evidencia a mostrar:** DDA §2.4–2.6 (diagrama CUN + descomposición) y §5 (CDU expandidos).

**Pregunta de comité que debe poder responder:** *"¿Por qué el flujo de pago no cancela la orden si PayKlik falla?"*

---

## 10–16 min · Stakeholders, priorización y trazabilidad — Persona 1

- Mapa de stakeholders por poder/interés — destacar los dos vetos explícitos: Ana Lucía Fuentes (trazabilidad/cumplimiento fiscal) y Carlos Méndez (continuidad/escalabilidad).
- Criterio de priorización (valor / riesgo / urgencia / impacto arquitectónico) y por qué EaC-04 quedó primero.
- Mostrar un recorte de la matriz de trazabilidad: elegir una fila (ej. Ana Lucía Fuentes) y seguirla en vivo hasta la vista donde aparece.

**Evidencia a mostrar:** DDA §3 (stakeholders), §8 (priorización), §9 (matriz de trazabilidad).

**Pregunta de comité que debe poder responder:** *"¿Cómo defienden que EaC-04 va antes que la trazabilidad fiscal, si Finanzas tiene veto?"*

---

## 16–24 min · Calidad, restricciones y decisiones — Persona 3

- Presentar EaC-04 completo (fuente/estímulo/artefacto/ambiente/respuesta/medida) como ejemplo de referencia.
- Resumen de los 12 EaC y las 10 restricciones — enfatizar las no negociables (R1: no tocar ERP/POS; R7: Black Friday no se mueve).
- ADR-001: contexto → decisión → alternativas descartadas (monolito sin eventos, microservicios completos) → por qué gana el híbrido.
- Mencionar el estado de los ADR secundarios (002–005) como trabajo en progreso, sin ocultarlo.

**Evidencia a mostrar:** DDA §6 (EaC), §7 (restricciones), §10.2 (ADR-001).

**Pregunta de comité que debe poder responder:** *"¿Por qué no microservicios desde el inicio, si van a migrar a nube eventualmente?"*

---

## 24–34 min · Vistas arquitectónicas — Persona 4

- Contexto → Componentes → Despliegue → Bloques, en ese orden (de frontera a detalle a síntesis ejecutiva).
- En Despliegue, señalar explícitamente dónde vive cada EaC crítico (circuit breaker hacia PayKlik, cola/retry hacia LogiExpress y POS, registro de auditoría) — es el punto que el enunciado exige de forma literal.
- Cerrar con Bloques como resumen para audiencia no técnica.

**Evidencia a mostrar:** DDA §11.1–11.4 y la tabla de correspondencia EaC↔despliegue en `Docs/diagramas/README.md`.

**Pregunta de comité que debe poder responder:** *"Señale en el diagrama de despliegue dónde exactamente se evita perder la orden si PayKlik cae."*

---

## 34–40 min · Prototipo y recorrido omnicanal — Persona 5

- Recorrer las 4 pantallas de `Docs/prototipo/mockups.html` en vivo: carrito móvil → pago web → consulta de estado → devolución.
- En cada pantalla, nombrar el CUN/EaC que ilustra (ya anotado en el mockup).
- Aclarar explícitamente: *"esto es un mockup no funcional — ilustra la experiencia, no es una implementación."*

**Evidencia a mostrar:** `Docs/prototipo/mockups.html`.

---

## 40–45 min · Consecuencias, riesgos y cierre — Persona 5 con cierre de Persona 1

- Trade-offs asumidos conscientemente: consistencia eventual, curva de aprendizaje de mensajería, necesidad de observabilidad operativa.
- Preguntas arquitectónicas abiertas (DDA §2.3) presentadas como transparencia, no como debilidad — con quién se resuelven y cuándo.
- Mensaje de cierre de una frase: qué gana MegaStore con esta arquitectura frente al costo de no cambiar nada.

**Evidencia a mostrar:** DDA §2.3 y §10.2 (consecuencias de ADR-001).

---

## Banco de preguntas simuladas para la ronda de 5 minutos

| Pregunta probable | Quién debe poder responderla primero | Dato de respaldo |
|---|---|---|
| ¿Qué pasa si PayKlik y LogiExpress fallan al mismo tiempo? | Persona 3 o 4 | EaC-04 + EaC-05, circuit breaker y colas independientes por adaptador |
| ¿Por qué no reemplazar el ERP si es de los 90? | Persona 1 | R1, decisión no negociable de Junta; costo/riesgo de reemplazo fuera de alcance |
| ¿Cómo garantizan que no se duplica un cobro en un reintento? | Persona 2 o 3 | Idempotencia mencionada en CDU-03 y ADR-001; detalle final pendiente de ADR-003 |
| ¿Qué pasa si el equipo interno no sabe operar mensajería? | Persona 3 | R5/EaC-09 justifican broker simple, no microservicios completos |
| ¿Cómo cumplen el límite de USD 1,200/mes? | Persona 4 | Nota de validación de costo en el diagrama de despliegue (EaC-10) |
| ¿Este prototipo es la app real? | Persona 5 | No — mockup no funcional, Entregable 4 según enunciado §4.4 |

---

## Pendientes antes del ensayo

- [ ] Confirmar nombres reales en vez de "Persona N".
- [ ] Cronometrar cada bloque en un ensayo real; ajustar si algún rol se pasa de tiempo.
- [ ] Verificar que quien presenta cada sección haya revisado también la sección anterior y la siguiente (para sostener preguntas cruzadas).
- [ ] Confirmar que ADR-001 siga como "propuesto" o actualizar el guion si el equipo lo acepta antes de la entrega.
