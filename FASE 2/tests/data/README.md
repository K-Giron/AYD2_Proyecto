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
