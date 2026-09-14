# Orden de integración de Fase 2

**Estado:** guía operativa; debe actualizarse con el estado real del Kanban.

## Reglas de entrada

Una entrega entra a integración únicamente si compila sobre la base oficial, identifica su tarjeta, respeta propiedad modular y adjunta pruebas. No se aceptan backends, bases Compose o árboles de paquetes alternativos.

## Secuencia por dependencias

1. **Backbone:** BB-01, ADR-009/BB-02, BB-05 y QA-01.
2. **Datos y contratos:** BB-03, estados, eventos, APIs, ADR-010 a ADR-013.
3. **Capacidades mínimas:** INV-01, PAY-01, FIS-01 y LOG-01.
4. **Orders:** ORD-01 y ORD-02 cuando sus dependencias estén disponibles.
5. **Camino feliz:** ORD-03 después de integrar los cuatro participantes mínimos.
6. **Resiliencia:** BB-04, INV-02, PAY-02 y luego ORD-04.
7. **E2E:** ORD-05 y QA-04 con todas las líneas P0.
8. **Cierre:** QA restante, DOC-01/DOC-03, demo y REL-01.

## Manejo de entregas tardías

- Integrar una tarjeta por vez y ejecutar verificación después de cada una.
- Si dos entregas modifican un archivo común, resolver primero el contrato y conservar un único responsable del cambio.
- Rechazar accesos directos a datos ajenos aunque aceleren temporalmente la integración.
- Aislar una entrega que no compile; no desactivar pruebas globales para aceptarla.
- Registrar el bloqueo, responsable, efecto y próxima revisión en la bitácora.

## Evidencia mínima por punto de control

| Punto | Evidencia |
|---|---|
| Backbone | Maven verde, Compose válido, tres servicios saludables |
| Cada módulo | Checklist, pruebas y contrato implementado |
| Saga | Secuencia observable con correlation ID común |
| E2E | Datos reproducibles y comando único |
| Release | CI verde, matriz EaC y documentación coherente |
