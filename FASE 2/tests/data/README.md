# Datos reproducibles de pruebas

Este directorio contiene los datos versionados y no sensibles de ORD-05.

- `ord-05-deterministic-fixtures.json`: usuarios demo, productos semilla y valores estables de checkout usados por los cinco recorridos E2E.

Cada conjunto futuro deberá indicar:

- versión del contrato;
- propietario;
- propósito y prueba consumidora;
- IDs ficticios y deterministas;
- precondiciones;
- resultado esperado;
- mecanismo de restablecimiento;
- confirmación de que no contiene información real o sensible.

No se deben crear tablas ni datos artificiales únicamente para ocultar una dependencia pendiente.

## Datos funcionales para la demostración

Con la plataforma levantada y saludable, ejecute desde `FASE 2`:

```bash
./tests/data/seed-demo-mvp.sh | tee /tmp/sgo-demo.json
```

El script requiere `curl` y `jq`, autentica las cuentas oficiales de demo y
crea tres recorridos mediante la API: una orden confirmada/en preparación,
una orden con despacho en tránsito y una orden entregada con devolución
aprobada. Imprime los UUID que se deben usar en las pantallas de Cliente,
Staff y Finanzas. No inserta órdenes directamente en la base, por lo que
reservas, pago PayKlik, documento SAT, despacho, eventos, notificaciones y
auditoría permanecen consistentes.

Puede definir `API_BASE` si el backend no está publicado en el puerto 8080 y
`DEMO_RUN_ID` si necesita claves deterministas para diagnosticar una corrida.
