# ADR-012 — Contratos y modos de los simuladores externos

- **Fecha:** 2026-09-22
- **Estado:** Proposed
- **Responsable:** Andy Sánchez
- **Revisores:** Edson Sosa, Kevin Girón y Jairo Gómez

## Contexto

PayKlik, SAT, ERP-Core, 32 instalaciones POS, LogiExpress y los proveedores de notificaciones son dependencias externas que no deben modificarse ni ser necesarias para desarrollar el MVP. El equipo necesita resultados reproducibles para caminos felices y fallas, sin confundirlos con contratos reales ya aprobados.

## Drivers relacionados

- BB-06 y DEC-04.
- R1: no modificar ERP-Core ni POS.
- EaC-04, EaC-05 y EaC-08: resiliencia ante proveedores.
- EaC-11: comportamiento configurable por ambiente.
- EaC-12: el sistema no almacena ni registra PAN/CVV.

## Decisión propuesta

Usar puertos de aplicación y adaptadores simulados configurables. Todos comparten `SimulatorMode`:

| Modo | Comportamiento |
|---|---|
| `SUCCESS` | Respuesta exitosa inmediata |
| `REJECTION` | Rechazo funcional, sin tratarlo como caída técnica |
| `LATENCY` | Espera configurada y luego éxito |
| `TIMEOUT` | Excepción de tiempo agotado |
| `OUTAGE` | Excepción de indisponibilidad |
| `RANDOM` | Éxito o falla según `failure-rate` |

Los modos, latencias y tasas se leen de variables `SGO_SIMULATORS_*`. POS permite además `SGO_SIMULATORS_POS_UNAVAILABLE_STORES`, con números entre 1 y 32. ERP permite seleccionar `CSV` o `XML` y ejecuta una ventana cada 30 minutos por defecto.

## Contratos simulados

| Dependencia | Entrada mínima | Salida o error |
|---|---|---|
| PayKlik | orden, importe, moneda, referencia segura, correlación, idempotencia | intento, aprobación/rechazo, referencia de proveedor; PAN/CVV prohibidos |
| SAT | documento fiscal, orden, CUI, serie, XML, correlación, idempotencia | autorización o rechazo; timeout/caída diferenciados |
| ERP-Core | lote, formato y contenido no vacío | acuse o rechazo del archivo; timeout/caída diferenciados |
| POS | tienda 1–32, evento, orden, JSON y correlación | acuse o rechazo; timeout/caída por tienda |
| LogiExpress | despacho, orden, destino y correlación | número de guía; timeout/caída |
| Notificaciones | canal, destinatario y tipo de evento | envío aceptado o proveedor indisponible |

Estos son contratos de desarrollo. No representan una API real de un proveedor ni autorizan cambios dentro de ERP-Core o POS.

## Alternativas evaluadas

1. **Mocks distintos por prueba:** rápidos, pero no permiten demos ni pruebas integradas configurables.
2. **Servicios simulados separados:** mayor fidelidad de red, pero agregan despliegue y mantenimiento innecesarios para esta fase.
3. **Adaptadores dentro del monolito:** simples, configurables y reemplazables mediante los mismos puertos; es la opción propuesta.

## Consecuencias

- Las fallas externas pueden reproducirse sin editar código.
- Los módulos consumidores no dependen de clases concretas del simulador.
- `REJECTION` y `TIMEOUT` conservan semánticas distintas.
- Las respuestas generadas no deben usarse como evidencia de integración real con proveedores.

## Riesgos

- Una prueba en modo `RANDOM` no es determinista; las pruebas automatizadas deben fijar un modo.
- `Thread.sleep` solo modela latencia de desarrollo y no sustituye pruebas de carga.
- La sustitución por adaptadores reales exigirá revisar autenticación, timeouts y esquemas reales.

## Criterios de validación o ratificación

- Cada modo puede seleccionarse por configuración externa.
- Las 32 tiendas POS se validan sin modificar POS.
- PayKlik rechaza referencias que parezcan PAN o incluyan CVV.
- Las pruebas cubren éxito, rechazo y fallas transitorias relevantes.
- Edson, Kevin y Jairo revisan compatibilidad con sus módulos antes de aceptar el ADR.

## Referencias

- `backend/src/main/java/com/megastore/sgologic/shared/simulation/SimulatorMode.java`
- `backend/src/main/resources/application.yml`
- `.env.example`
