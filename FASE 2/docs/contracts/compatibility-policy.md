# Política propuesta de compatibilidad de contratos

**Estado:** `PROPOSED`

## Objetivo

Evitar que una entrega tardía cambie silenciosamente APIs, eventos o identificadores consumidos por otros módulos.

## Reglas

1. Los identificadores públicos son UUID y mantienen nombre estable en `camelCase`.
2. Los instantes se representan en UTC con ISO 8601.
3. Agregar un campo opcional puede ser compatible; eliminar, renombrar o cambiar el tipo de un campo es incompatible.
4. Un campo obligatorio nuevo exige una versión nueva o un valor predeterminado acordado.
5. Un evento conserva `eventType`; un cambio incompatible incrementa `eventVersion`.
6. Los consumidores deben ignorar campos desconocidos y validar los obligatorios de la versión que soportan.
7. Los códigos de error son estables; el texto de `message` no se usa para lógica.
8. `X-Correlation-Id` viaja durante todo el flujo. `Idempotency-Key` no se reutiliza para operaciones distintas.
9. Ningún módulo lee repositorios, entidades, adaptadores o tablas privadas de otro módulo.
10. Un contrato aceptado solo cambia mediante ADR o nueva versión con análisis de consumidores afectados.

## Flujo de cambio

1. El proponente actualiza el documento y marca el cambio como `PROPOSED`.
2. Enumera productores, consumidores, migración y pruebas afectadas.
3. Los propietarios involucrados revisan el cambio.
4. Se registra la aprobación o rechazo con fecha y evidencia.
5. Solo entonces se actualizan implementaciones y pruebas contractuales.

## Cambios que requieren rechazo inmediato

- Introducir datos sensibles en logs, errores o eventos.
- Usar tablas ajenas como contrato.
- Publicar eventos sin versión.
- Declarar éxito antes de completar una condición fiscal o de pago requerida.
- Reutilizar un evento existente para expresar un hecho semánticamente distinto.
