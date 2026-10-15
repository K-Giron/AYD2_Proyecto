# Guion de presentación y demostración — Fase 3

## MegaStore / SGO-Logic — crédito propio, calidad y despliegue

| Campo | Valor |
|---|---|
| Duración objetivo | 15–18 minutos más preguntas |
| Ambiente | `http://136.65.180.39` |
| Versión demostrada | Último SHA validado de `develop` |
| Evidencia principal | DDA, ADR-016/017, matriz de Fase 3, reportes, bitácora y manuales |
| Regla de seguridad | No mostrar contraseñas, JWT, PAN, CVV/CVC ni secretos |

---

## 1. Preparación previa

1. Abrir la tienda, consola interna y cuatro portales conectados.
2. Confirmar que el estado público de la plataforma responde `UP`.
3. Utilizar dos cuentas de cliente distintas para demostrar aislamiento.
4. Tener un producto publicado con inventario suficiente.
5. Mantener disponible una orden confirmada para reporte y auditoría.
6. Conservar abiertos los enlaces del CI, despliegue, reporte de rendimiento y
   matriz requisito–evidencia.
7. No alterar el estado de PayKlik o LogiExpress sin restablecerlo al terminar.

### Enlaces de la demostración

| Servicio | Dirección |
|---|---|
| Tienda web | `http://136.65.180.39` |
| Consola interna | `http://136.65.180.39/staff.html` |
| Almacén Central | `http://136.65.180.39:8082` |
| POS-01 | `http://136.65.180.39:8084` |
| LogiExpress | `http://136.65.180.39:8085` |
| PayKlik | `http://136.65.180.39:8086` |

---

## 2. Apertura — Jairo Gómez — 1 minuto

> “En Fase 3 incorporamos crédito propio sin reemplazar PayKlik ni crear una
> segunda arquitectura. Extendimos el monolito modular y la Saga existente,
> añadimos una decisión financiera auditable y desplegamos la solución completa
> mediante un pipeline con pruebas obligatorias.”

Resultados que se deben presentar:

- cada cliente registrado recibe una línea individual inicial de Q2,500;
- el saldo se consume entre compras y nunca se comparte entre clientes;
- PayKlik permanece disponible como ruta independiente;
- una compra a crédito no crea un intento PayKlik ficticio;
- decisión, factura, ERP, notificación y despacho conservan trazabilidad;
- la plataforma está desplegada en Google Cloud.

Evidencia: planificación de Fase 3, ADR-016, ADR-017 y DDA.

---

## 3. Recorrido del cliente — Jairo Gómez — 5 minutos

### 3.1 Identidad y separación entre clientes

1. Registrar o iniciar sesión con el primer cliente.
2. Agregar un producto al carrito y abrir **Mis pedidos**.
3. Cerrar sesión e ingresar con un segundo cliente.
4. Comprobar que el segundo cliente no ve el carrito ni los pedidos del primero.
5. Explicar que el backend obtiene `customer_id` del JWT y rechaza recursos
   ajenos con 403.

Mensaje clave: carrito, pedidos y crédito pertenecen a la identidad autenticada;
no se almacenan como información global compartida.

### 3.2 Crédito inicial y consumo acumulado

1. Preparar un carrito cuyo total sea menor o igual a Q2,500.
2. Mostrar **Compra ahora, paga en 30 días** y el saldo disponible.
3. Confirmar la compra mediante `OWN_CREDIT`.
4. Mostrar número de orden, operación de crédito, vencimiento, factura y avance
   hacia preparación.
5. Crear un segundo carrito con el mismo cliente.
6. Confirmar que el saldo disponible es Q2,500 menos las compras financiadas
   anteriores; abrir otro carrito no reinicia la línea.
7. Ingresar con el segundo cliente y mostrar que conserva sus propios Q2,500.

Mensaje clave: el límite pertenece al cliente, no al carrito ni a la sesión del
navegador.

### 3.3 Rechazo y continuidad por PayKlik

1. Preparar una compra superior al saldo restante.
2. Explicar que crédito se oculta cuando la evaluación previa determina que el
   monto no cabe en la línea.
3. Si se demuestra la reevaluación durante la Saga, mostrar `CREDIT_REJECTED`,
   `dueDate = null` y ausencia de deuda concedida.
4. Continuar mediante PayKlik sin crear otra intención comercial.
5. Mostrar que PayKlik conserva una sola autorización, factura y orden.

Mensaje clave: un rechazo de crédito no genera deuda, no cobra automáticamente
por PayKlik y no deja al cliente sin alternativa de pago.

---

## 4. Crédito, seguridad e integración — Jorge y Edson — 3 minutos

### Jorge Pumay

- Explicar perfil, historial ERP, versión de reglas, saldo disponible y estados
  `GRANTED`, `OVERDUE`, `SETTLED` y `CANCELLED`.
- Mostrar que bloqueos transaccionales y restricciones únicas evitan que dos
  compras simultáneas gasten el mismo saldo.
- Distinguir la línea inicial de Q2,500 del límite histórico máximo de Q5,000.

### Edson Sosa

- Mostrar el endpoint de explicación financiera para Finanzas.
- Explicar Outbox, RabbitMQ e Inbox para notificaciones no bloqueantes.
- Resumir los E2E de crédito lento o caído, reintento sin duplicados y falla de
  notificaciones sin reversión de la venta.
- Señalar las pruebas 401/403/404 y el escaneo contra exposición de datos
  sensibles.

Evidencia: commit `224a73c`, reporte funcional y matriz de Fase 3.

---

## 5. Operación financiera y logística — Jairo Gómez — 2 minutos

1. Abrir la orden desde la consola interna.
2. Mostrar ruta `OWN_CREDIT`, estado financiero, referencia, factura y despacho.
3. Abrir el informe conciliado y localizar la misma venta.
4. Confirmar que ERP conserva `OWN_CREDIT` y no la clasifica como “Sin pago”.
5. Revisar evidencia de auditoría y correlación.
6. Mostrar la notificación `CREDIT_GRANTED` y la guía de LogiExpress, cuando
   corresponda.

Mensaje clave: la misma identidad financiera acompaña orden, crédito, factura,
reporte, ERP, auditoría y notificación.

---

## 6. Arquitectura — Jairo Gómez — 2 minutos

Mostrar las cuatro vistas de `docs/diagrams/fase3`:

1. **Componentes:** `orders` consume únicamente `credit :: api`.
2. **Secuencia:** decisión síncrona, concesión dentro de la Saga y notificación
   asíncrona.
3. **Datos:** perfil, decisión y operación pertenecen al módulo `credit`.
4. **Despliegue:** crédito permanece dentro del backend modular; no se presenta
   como un microservicio inexistente.

Defensa resumida:

- sincronía para decidir antes de ofrecer la alternativa;
- Saga para coordinar efectos y compensaciones;
- Outbox/Inbox para propagar sin bloquear;
- idempotencia y locks para impedir dobles efectos y sobregiro.

---

## 7. Rendimiento y operación — Andy y Kevin — 3 minutos

### Andy Sánchez

- Presentar los cinco escenarios de carga y cinco de estrés.
- Indicar que los cinco de carga cumplieron sus umbrales.
- Mostrar la repetición de STRESS-03 con 0 % de errores.
- Declarar el cuello de botella de notificaciones encontrado por STRESS-04 y la
  ausencia de telemetría de recursos durante el pico.
- Mostrar el smoke independiente de nube: 11/11 verificaciones aprobadas.

### Kevin Girón

- Mostrar pipeline `build → test → images → deploy` con gate efectivo.
- Explicar secretos de GitHub, imágenes inmutables, health checks, respaldo y
  rollback automático.
- Mostrar el ambiente GCP y el smoke externo de 17 verificaciones.
- Distinguir el despliegue técnico aprobado del cierre administrativo de los
  releases `v2`, `v3` y `v4`.

---

## 8. Cierre — Jairo Gómez — 1 minuto

> “La Fase 3 deja una ruta de crédito real dentro del alcance académico: cada
> cliente tiene una línea independiente, las compras consumen el saldo de forma
> segura y toda la operación es auditable. PayKlik sigue funcionando de manera
> aislada. La solución fue probada funcionalmente, sometida a carga y estrés y
> desplegada en nube con un pipeline que impide promover una versión fallida.”

Cerrar mostrando:

1. Manual de Usuario con capturas.
2. Manual Técnico.
3. Bitácora de contribuciones.
4. Matriz requisito–implementación–prueba–evidencia.
5. Run final de CI sobre `develop`.
6. Releases publicados cuando el equipo complete la promoción final.

---

## 9. Preguntas frecuentes para la defensa

### ¿Por qué todos los clientes reciben crédito?

Es la política acordada para la tienda: cada identidad registrada inicia con
Q2,500. El historial nunca reduce ese valor base, aunque mora, incidentes
críticos, devoluciones o saldo insuficiente todavía pueden bloquear una compra.

### ¿Por qué PayKlik no cae cuando crédito falla?

Son puertos y rutas financieras separadas. El endpoint construye PayKlik como
alternativa base y degrada de forma segura si crédito excede el timeout o falla.

### ¿Cómo evitan compartir crédito entre clientes?

El `customer_id` firmado en el JWT identifica al dueño. El perfil, las
decisiones y las operaciones se consultan y bloquean por ese UUID.

### ¿Cómo evitan doble deuda o sobregiro?

Con locks transaccionales por cliente, orden y llave idempotente, restricciones
únicas, descuento atómico del saldo y recuperación de la operación existente.

### ¿Por qué una compra a crédito no genera un pago PayKlik?

La factura utiliza una referencia financiera neutral que distingue `PAYKLIK`
de `OWN_CREDIT`; no se inventa un intento de pago externo.

### ¿Qué sucede si se repite la Saga?

Se recuperan la misma operación financiera, factura y despacho. Las pruebas
comprueban que no se duplican reservas, cobros, créditos ni documentos.

### ¿Qué limitaciones se reconocen?

- El ambiente es académico y no declara capacidad productiva.
- STRESS-04 identificó un cuello de botella de notificaciones bajo el pico
  documentado.
- Pago de cuotas, abonos y gestión completa de cartera no forman parte del
  alcance funcional de esta entrega.
- La publicación de `v2`, `v3` y `v4` es el cierre operativo del equipo.

---

## 10. Checklist del ensayo

- [ ] Dos clientes muestran carritos, pedidos y crédito independientes.
- [ ] Una compra a crédito consume únicamente el saldo de su cliente.
- [ ] La venta aparece como `OWN_CREDIT` en factura, reporte y ERP.
- [ ] PayKlik continúa disponible ante rechazo o falla del crédito.
- [ ] La repetición conserva identificadores y no duplica efectos.
- [ ] Se muestran DDA, ADR-016, diagramas y matriz de evidencia.
- [ ] Se presentan resultados de carga, estrés y sus limitaciones.
- [ ] Se muestra CI, despliegue, health, smoke y rollback.
- [ ] Cada integrante responde por su frente y reconoce los límites reales.
- [ ] La demostración completa respeta el tiempo asignado.
