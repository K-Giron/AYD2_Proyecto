# ADR-016 — Crédito propio y Saga financiera dual

- **Fecha:** 2026-10-12
- **Estado:** Proposed
- **Responsable del borrador:** Jairo Gómez
- **Revisores:** Jorge Pumay, Edson Sosa, Andy Sánchez y Kevin Girón
- **Resuelve:** incorporación de “Compra ahora, paga en 30 días” sin convertir el crédito en un pago PayKlik ficticio

## Contexto

La Fase 3 exige ofrecer crédito propio únicamente a clientes elegibles, decidir
con rapidez a partir del historial disponible, conservar el pago tradicional y
notificar el resultado. La orden ya se coordina mediante una Saga orquestada y
la facturación de Fase 2 estaba vinculada semánticamente a
`paymentAttemptId`. Reutilizar ese identificador para crédito afirmaría que
existió un cobro externo que nunca ocurrió y contaminaría auditoría,
conciliación y evidencia fiscal.

La implementación continúa siendo un monolito modular Spring Modulith. Crédito
no necesita un proceso, base de datos o despliegue independiente para satisfacer
el requisito; sí necesita propiedad clara de sus reglas, decisiones y
obligaciones.

## Drivers relacionados

- Campaña “Compra ahora, paga en 30 días” y objetivo de negocio de aumentar el
  ticket promedio en 40 %; el porcentaje es un objetivo, no un resultado medido.
- Decisión certera y rápida, sustentada en historial y versión de reglas.
- PayKlik debe continuar disponible si crédito rechaza o no puede evaluarse.
- Una repetición no puede duplicar orden, reserva, crédito, factura ni despacho.
- Finanzas debe reconstruir la decisión y la operación concedida o rechazada.
- Las notificaciones no deben bloquear ni revertir la orden.
- Compatibilidad con los contratos y datos creados durante Fase 2.

## Decisión

1. Se incorpora un módulo `credit` dentro de `sgo-logic`, dueño de
   `credit_customer_profiles`, `credit_decisions` y `credit_operations`.
   `orders` solo lo consume mediante la interfaz pública `credit :: api`.
2. La evaluación es síncrona porque el checkout necesita saber qué alternativa
   mostrar. Es *fail closed*: sin perfil o con historia insuficiente no se ofrece
   crédito. Una falla técnica al consultar opciones se degrada a PayKlik.
3. La política inicial es configurable y versionada (`credit-rules-v1`): mínimo
   de compras completadas, puntualidad mínima, devolución máxima, incidentes
   críticos máximos, ausencia de mora y límite disponible suficiente. Cada
   evaluación persiste entradas, resultado, razón pública, límite y versión.
4. La concesión es idempotente por orden y por `Idempotency-Key`. Una operación
   `GRANTED` crea una obligación con vencimiento calculado a 30 días; una
   operación `REJECTED` no crea deuda ni intento PayKlik.
5. `orders` conserva dos rutas explícitas:
   `PAYMENT_PENDING/APPROVED/REJECTED/INDETERMINATE` para PayKlik y
   `CREDIT_PENDING/APPROVED/REJECTED` para crédito. Ambas convergen en
   `FISCAL_PENDING` únicamente después de una autorización financiera válida.
6. Tras `CREDIT_REJECTED`, la reserva y la orden permanecen recuperables para
   que el cliente elija PayKlik. Después de una concesión no se permite cambiar
   la ruta financiera.
7. Facturación referencia el par neutral
   `financialOperationId + financialOperationType`. Para compatibilidad, el
   contrato legado `paymentAttemptId` se acepta y normaliza a `PAYKLIK`, y la
   columna anterior se conserva nullable durante la transición.
8. Crédito publica mediante Outbox `CREDIT_DECISION_RECORDED`,
   `CREDIT_GRANTED` y `CREDIT_REJECTED`. Los consumidores usan Inbox; la
   notificación es asíncrona y no modifica el resultado financiero.
9. El endpoint de opciones obtiene monto y cliente del carrito persistido y no
   recibe esos datos del navegador. La UI oculta crédito cuando no está
   habilitado, pero la Saga vuelve a validar y no confía en la UI.
10. El módulo no lee tablas privadas de ERP, pagos u órdenes. El perfil local es
    una proyección; su alimentación productiva desde fuentes autorizadas queda
    como trabajo de integración y gobierno de datos.
11. La identidad canónica de la cuenta `CLIENT` demostrativa se incluye como
    claim firmado `customer_id`. Un guard del backend deriva de ese claim el
    filtro de consultas y rechaza el acceso a carritos u órdenes ajenos; el
    navegador no decide la identidad efectiva.
12. La evaluación de opciones tiene un límite temporal configurable (2 s por
    defecto). Una demora, excepción o timeout cancela la tarea y devuelve solo
    PayKlik, sin convertir una indisponibilidad de crédito en error del checkout.
13. La concesión serializa transacciones concurrentes mediante advisory locks
    PostgreSQL sobre `customerId`, `orderId` e `idempotencyKey`, además de las
    restricciones `UNIQUE`. El lock del cliente evita que dos órdenes distintas
    excedan juntas su línea, y la repetición relee la operación ya confirmada.
14. `available_credit` representa el saldo utilizable acumulado. Una concesión
    lo descuenta en la misma transacción que persiste la obligación y los
    eventos; las compras siguientes se evalúan contra el remanente.
15. La venta confirmada conserva `PAYKLIK|OWN_CREDIT|STORE` en ERP, proyección
    de consulta y reporte conciliado. Una venta a crédito se muestra como
    `CREDIT_GRANTED`, no como `SIN_PAGO`.

## Alternativas evaluadas

1. **Crear un intento PayKlik ficticio para crédito:** descartada; falsea la
   evidencia, mezcla conciliación externa con cartera propia y aumenta el
   acoplamiento.
2. **Reutilizar únicamente estados `PAYMENT_*`:** descartada; reportes y UI no
   podrían distinguir cobro externo de concesión propia con claridad.
3. **Evaluación completamente asíncrona:** descartada para el checkout; no puede
   decidir a tiempo qué alternativa mostrar. Los efectos secundarios sí son
   asíncronos.
4. **Microservicio de crédito con base independiente:** pospuesta; agrega falla
   de red y operación distribuida sin una necesidad demostrada. El límite
   modular y el puerto permiten extraerlo más adelante.
5. **Crédito visible para todos y rechazo al final:** descartada; degrada la
   experiencia y contradice que la alternativa aparezca solo a ciertos clientes.

## Consecuencias

### Positivas

- PayKlik y crédito conservan semántica, datos y evidencia independientes.
- La Saga existente se extiende sin crear un segundo orquestador.
- Reintentos son recuperables y no duplican la obligación.
- El contrato fiscal puede representar cualquier operación financiera válida.
- Las reglas cambian por configuración y cada decisión conserva su versión.

### Costos y límites

- Se agregan estados, migraciones, eventos y casos de regresión.
- El perfil demostrativo actual es una proyección sembrada; aún falta acordar
  propietario, frescura y reconciliación del historial productivo.
- El ciclo de cobranza, abonos, mora, notas de crédito y restitución de línea no
  forma parte de esta decisión. Requiere reglas y contrato de cartera del dueño
  financiero antes de considerarlo apto para operación productiva completa.
- La cuenta `CLIENT` demostrativa continúa siendo una sola identidad canónica,
  ahora ligada al claim JWT `customer_id`. Antes de producción se necesita un
  almacén de credenciales o proveedor de identidad que emita un cliente
  distinto por persona; las cuentas locales del navegador no constituyen ese
  directorio productivo.
- La política configura 30 días; cambios regulatorios, mora, pagos de cuota y
  cobranza requieren decisiones y alcance adicionales.

## Riesgos

- Los advisory locks usados para la primera concesión dependen de PostgreSQL;
  una extracción futura del módulo debe sustituirlos por una garantía
  equivalente y conservar las restricciones `UNIQUE`.
- Un perfil obsoleto puede producir una decisión incorrecta; se necesita SLO de
  frescura, sello temporal y reconciliación con la fuente autorizada.
- Exponer razones demasiado detalladas permitiría inferir información sensible;
  la API pública entrega solo códigos controlados.
- La reserva retenida después de rechazo debe respetar su TTL y liberación
  existente para no inmovilizar inventario indefinidamente.

## Criterios de validación o ratificación

- Cliente elegible: PayKlik y crédito aparecen; la concesión vence en 30 días.
- Cliente no elegible o evaluación fallida: PayKlik permanece y crédito se omite.
- Crédito aprobado: no existe `payment_attempt` para la orden y sí una sola
  `credit_operation`, factura y transición confirmada.
- Crédito rechazado: no existe deuda y la misma orden puede continuar por
  PayKlik dentro de las transiciones permitidas.
- Repetición con la misma llave devuelve la misma operación.
- `ApplicationModules.verify()` no detecta acceso a internals de `credit`.
- Los eventos contienen identificadores, versión, resultado y razón pública,
  pero no historial completo, PAN, CVV ni secretos.
- La regresión de PayKlik, conciliación y seguridad permanece verde.

## Referencias

- `Docs/planificacion/PLANIFICACION_FASE_3.md`
- `Docs/planificacion/PLAN_EJECUCION_JAIRO_FASE_3.md`
- `Proyecto Fase 3_convertido.md`
- ADR-002, ADR-003, ADR-005, ADR-007, ADR-008 y ADR-015
- Migraciones `V4004__fiscal_generalize_financial_operation.sql` y
  `V4005__credit_and_order_financial_route.sql` y
  `V4006__credit_sale_classification.sql`
