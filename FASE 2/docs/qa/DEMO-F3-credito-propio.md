# Guion de demostración — Fase 3, crédito propio

**Duración objetivo:** 8–10 minutos  
**Base:** ambiente local limpio o despliegue de Fase 3 validado  
**Cuenta cliente canónica:** `cliente@megastore.com`

## Preparación

1. Levantar la plataforma y verificar health checks.
2. Confirmar que el cliente canónico conserva el perfil elegible sembrado.
3. Tener disponibles identificadores de un producto con stock y una cantidad
   alta que exceda el límite para el escenario rechazado.
4. No mostrar contraseñas, JWT, PAN, CVV ni variables secretas en pantalla.

## Recorrido 1 — opción rápida y segura

1. Iniciar sesión y abrir un carrito propio.
2. Mostrar que el backend firma `customer_id` y que otro UUID recibe 403.
3. Consultar opciones: PayKlik aparece siempre; crédito aparece solo si el
   perfil y el monto son elegibles.
4. Explicar que una evaluación lenta vence a los 2 s y conserva PayKlik.

## Recorrido 2 — crédito aprobado e idempotente

1. Elegir **Compra ahora, paga en 30 días** y confirmar.
2. Mostrar `creditOperationId`, vencimiento, documento fiscal y despacho.
3. Verificar que no existe `payment_attempt` PayKlik para esa orden.
4. Repetir la Saga con la misma llave: operación, factura y despacho conservan
   sus identificadores y existe una sola fila de cada efecto.
5. Mostrar que el límite inicial de Q2,500 disminuyó por el total concedido;
   abrir otro carrito no restablece el saldo.

## Recorrido 3 — rechazo y recuperación

1. Usar una cantidad que exceda el saldo remanente y forzar la ruta
   `OWN_CREDIT`.
2. Mostrar `CREDIT_REJECTED`, `dueDate = null`, cero deuda concedida y cero
   intentos PayKlik.
3. Ejecutar la misma orden por PayKlik y mostrar que alcanza confirmación con un
   solo intento, una factura y un despacho.

## Recorrido 4 — arquitectura y operación

1. Abrir ADR-016 y las cuatro vistas en `docs/diagrams/fase3`.
2. Señalar que `credit` es un módulo del backend, no un microservicio inventado.
3. Explicar sincronía para decidir, Saga para coordinar y Outbox/Inbox para
   notificar sin bloquear.
4. Cerrar con la matriz requisito–evidencia y el reporte de pruebas.

## Respuestas breves para la defensa

- **¿Por qué PayKlik no cae con crédito?** Son rutas separadas; opciones degrada
  por timeout/excepción y siempre construye PayKlik primero.
- **¿Cómo se evita doble deuda o sobregiro?** Locks transaccionales por cliente,
  orden y llave, restricciones únicas, descuento atómico del saldo y relectura
  de la operación existente.
- **¿Por qué no hay pago ficticio?** Fiscal usa una referencia financiera
  neutral que distingue `PAYKLIK` de `OWN_CREDIT`.
- **¿Qué queda pendiente?** Fuente productiva del perfil, contrato de cartera
  para abonos/mora/restitución de línea, identidad multiusuario, carga/estrés,
  nube, CI/CD, revisión del ADR y releases del equipo.
