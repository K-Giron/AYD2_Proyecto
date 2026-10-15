# Manual de usuario

## MegaStore / SGO-Logic — operación integral de la plataforma

| Campo | Información |
|---|---|
| Sistema | MegaStore / SGO-Logic |
| Documento | Manual de usuario |
| Versión funcional | Fase 3 |
| Versión documental | 1.0 |
| Fecha de actualización | 13 de octubre de 2026 |
| Audiencia | Clientes, soporte, logística, finanzas y administración de la tienda online |
| Estado | Borrador funcional pendiente de incorporar capturas definitivas |

---

## Control de cambios

| Versión | Fecha | Descripción | Responsable |
|---|---|---|---|
| 1.0 | 13/10/2026 | Operación integral de la plataforma: tienda, pedidos, inventario, logística, integraciones, postventa y crédito | Equipo SGO-Logic |

---

## Índice

1. [Propósito del manual](#1-propósito-del-manual)
2. [Alcance funcional](#2-alcance-funcional)
3. [Requisitos para utilizar el sistema](#3-requisitos-para-utilizar-el-sistema)
4. [Acceso e inicio de sesión](#4-acceso-e-inicio-de-sesión)
5. [Navegación de la tienda](#5-navegación-de-la-tienda)
6. [Catálogo, búsqueda y favoritos](#6-catálogo-búsqueda-y-favoritos)
7. [Administración del carrito](#7-administración-del-carrito)
8. [Preparación del checkout](#8-preparación-del-checkout)
9. [Compra mediante PayKlik](#9-compra-mediante-payklik)
10. [Compra mediante crédito propio](#10-compra-mediante-crédito-propio)
11. [Crédito no disponible o rechazado](#11-crédito-no-disponible-o-rechazado)
12. [Confirmación y factura](#12-confirmación-y-factura)
13. [Seguimiento de pedidos](#13-seguimiento-de-pedidos)
14. [Devoluciones e incidencias](#14-devoluciones-e-incidencias)
15. [Centro de operaciones](#15-centro-de-operaciones)
16. [Operación y seguimiento de órdenes](#16-operación-y-seguimiento-de-órdenes)
17. [Inventario y kárdex](#17-inventario-y-kárdex)
18. [Atención omnicanal](#18-atención-omnicanal)
19. [ERP, POS y sistemas conectados](#19-erp-pos-y-sistemas-conectados)
20. [Informe de ventas y auditoría](#20-informe-de-ventas-y-auditoría)
21. [Administración de la tienda online](#21-administración-de-la-tienda-online)
22. [Portales externos de demostración](#22-portales-externos-de-demostración)
23. [Estados mostrados por el sistema](#23-estados-mostrados-por-el-sistema)
24. [Solución de problemas](#24-solución-de-problemas)
25. [Recomendaciones de seguridad](#25-recomendaciones-de-seguridad)
26. [Cierre de sesión](#26-cierre-de-sesión)
27. [Lista de capturas pendientes](#27-lista-de-capturas-pendientes)

---

# 1. Propósito del manual

Este documento explica cómo utilizar la plataforma completa de MegaStore:
portal público, registro, tienda del cliente, compra y postventa, centro de
operaciones, inventario, logística, atención omnicanal, integraciones ERP/POS,
auditoría, reportes y administración de la tienda online.

El crédito propio es una alternativa financiera dentro del checkout; no es el
único propósito del manual. Se documenta junto con PayKlik y con el resto del
ciclo comercial de la plataforma.

Las instrucciones están orientadas al ambiente académico de demostración. Los
datos, usuarios y direcciones pueden variar en un despliegue posterior.

---

# 2. Alcance funcional

El manual cubre las siguientes funciones:

- autenticación mediante una cuenta de cliente;
- registro persistente de una cuenta de cliente y acceso según rol;
- consulta y búsqueda de productos publicados;
- administración de favoritos y carrito;
- selección entre entrega a domicilio y retiro en tienda;
- ingreso de datos fiscales;
- compra con tarjeta o transferencia mediante PayKlik;
- compra mediante la línea de crédito propia de MegaStore;
- generación y consulta de factura;
- seguimiento consolidado de pedidos;
- registro y consulta de devoluciones e incidencias;
- consulta de órdenes, despacho, notificaciones y evidencia de auditoría;
- recepción, inventario, catálogo, ventas, traslados y sincronización de la
  tienda online;
- generación de lotes ERP y seguimiento de eventos enviados a tiendas físicas;
- conciliación financiera de ventas PayKlik, crédito propio y tienda física;
- uso demostrativo de Almacén Central, POS-01, LogiExpress y PayKlik.

No forma parte de esta versión el pago de cuotas, los abonos a una deuda, la
gestión de mora ni la restitución automática de la línea después de pagar. La
Fase 3 registra la concesión, su vencimiento y el consumo de crédito disponible.

## 2.1 Roles considerados

| Rol | Funciones principales |
|---|---|
| Cliente | Catálogo, favoritos, carrito, checkout, factura, pedidos y devoluciones |
| Soporte | Consulta operativa de órdenes e incidencias |
| Finanzas | Informe de ventas, conciliación, auditoría y archivos del ERP |
| Logística | Despachos, seguimiento y eventos enviados a puntos de venta |
| Tienda online | Recepción, inventario, catálogo, ventas, traslados y sincronización |

---

# 3. Requisitos para utilizar el sistema

Antes de comenzar, confirme lo siguiente:

- la plataforma se encuentra levantada y sus servicios están saludables;
- dispone de un navegador web actualizado;
- JavaScript y almacenamiento del navegador están habilitados;
- conoce las credenciales asignadas a su rol;
- el equipo tiene acceso a la dirección donde se publicó MegaStore.

En el ambiente local, la dirección principal es:

```text
http://localhost:8081
```

## 3.1 Cuentas del ambiente de demostración

| Perfil | Usuario | Contraseña | Uso |
|---|---|---|---|
| Cliente | `cliente@megastore.com` | `Cliente123` | Compra y seguimiento |
| Soporte | `staff@megastore.com` | `Staff123` | Centro de operaciones |
| Finanzas | `finanzas@megastore.com` | `Finanzas123` | Reportes, ERP y auditoría |
| Logística | `logistica@megastore.com` | `Logistica123` | Despacho y POS |
| Tienda online | `tienda-online@megastore.com` | `TiendaOnline123` | Administración completa de la web |

> **Importante:** estas credenciales pertenecen únicamente al ambiente
> académico. En un ambiente compartido o productivo deben reemplazarse por
> cuentas individuales.

## 3.2 Portal público

La página principal permite consultar productos publicados sin iniciar sesión.

1. Abra `http://localhost:8081`.
2. Recorra el catálogo o utilice la navegación hacia **Productos disponibles**.
3. Abra un producto para consultar su detalle y disponibilidad.
4. Si desea comprar, seleccione **Iniciar sesión** o **Crear cuenta** cuando el
   sistema lo solicite.

El stock público es informativo; la reserva definitiva ocurre durante el
checkout después de autenticar al cliente.

![Portal público](Docs/capturas/manual-usuario/MU-25-portal-publico.png)
---

# 4. Acceso e inicio de sesión

1. Abra `http://localhost:8081`.
2. Seleccione la opción para iniciar sesión.
3. En la pantalla de acceso, seleccione **Cliente** o **Personal interno**.
4. Ingrese el correo electrónico y la contraseña correspondientes.
5. Si desea conservar la sesión al cerrar el navegador, marque
   **Recordarme en este navegador**.
6. Presione **Iniciar sesión**.

El sistema redirige al cliente hacia la tienda y al personal autorizado hacia
la consola correspondiente a sus permisos.

## 4.1 Resultado esperado

- El cliente visualiza el catálogo y su carrito asociado.
- El personal interno visualiza la consola operativa.
- Si las credenciales son inválidas, el sistema muestra un mensaje y no inicia
  la sesión.

![InicioSesion](Docs/capturas/manual-usuario/MU-01-inicio-sesion.png)

## 4.2 Registro de un cliente de demostración

1. Desde la pantalla pública, seleccione **Crear cuenta**.
2. Ingrese nombre completo, correo electrónico y una contraseña de al menos
   ocho caracteres.
3. Acepte el uso de los datos para operar la compra.
4. Presione **Crear cuenta**.
5. Confirme que el sistema inicia la sesión y abre la tienda.

El registro crea una identidad individual en el servidor. La contraseña se
almacena codificada y el cliente recibe un identificador propio, por lo que su
carrito, sus pedidos y su crédito no se comparten con otras cuentas.

> **CAPTURA PENDIENTE MU-14 — Registro de cliente**  
> Insertar aquí una captura de `registro.html` con los campos del formulario y
> el consentimiento visibles.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-14-registro-cliente.png`

---

# 5. Navegación de la tienda

Después de iniciar sesión como cliente, la barra principal presenta estas
opciones:

| Opción | Descripción |
|---|---|
| Departamentos | Regresa al catálogo de productos |
| Favoritos | Muestra productos guardados en el navegador |
| Mi carrito | Abre el resumen y proceso de compra |
| Mis pedidos | Consulta órdenes y seguimiento |
| Devoluciones | Registra o consulta solicitudes de postventa |
| Cerrar sesión | Finaliza la sesión actual |

La cabecera también contiene un buscador y un contador con la cantidad de
productos agregados al carrito.

> **CAPTURA PENDIENTE MU-02 — Pantalla principal de la tienda**  
> Insertar aquí una captura completa de `cliente.html` donde se distingan la
> navegación, el buscador, el catálogo y el contador del carrito.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-02-tienda-cliente.png`

---

# 6. Catálogo, búsqueda y favoritos

## 6.1 Buscar un producto

1. Ingrese un nombre, categoría o SKU en el buscador superior.
2. Presione **Buscar** o espere a que el catálogo aplique el filtro.
3. Revise los productos coincidentes y su disponibilidad.
4. Abra el producto si necesita consultar su información detallada.

## 6.2 Agregar un producto al carrito

1. Localice un producto disponible.
2. Presione el control para agregarlo.
3. Verifique que el contador de **Mi carrito** aumente.
4. Si necesita más unidades, utilice los controles de cantidad.

El sistema no debe permitir una cantidad superior al inventario disponible.

## 6.3 Guardar un favorito

1. Presione el icono de favorito del producto.
2. Abra la sección **Favoritos**.
3. Confirme que el producto aparece en la lista.

Los favoritos se almacenan en el dispositivo actual; no sustituyen el carrito
persistido del cliente.

> **CAPTURA PENDIENTE MU-03 — Catálogo y producto agregado**  
> Insertar aquí una captura con un producto disponible, su precio y el contador
> del carrito actualizado.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-03-catalogo-producto.png`

---

![alt text](image.png)
# 7. Administración del carrito

1. Seleccione **Mi carrito**.
2. Revise nombre, precio unitario, cantidad y subtotal de cada producto.
3. Utilice los controles **+** y **−** para modificar cantidades.
4. Reduzca la cantidad a cero si desea retirar un producto.
5. Verifique el total actualizado antes de continuar.

No cierre la página mientras se está actualizando una cantidad. Si el producto
dejó de estar disponible, actualice el carrito y seleccione otra cantidad.

## 7.1 Resultado esperado

El resumen debe mostrar exclusivamente los productos y cantidades vigentes. El
total utilizado por PayKlik o crédito propio se obtiene del carrito persistido,
no de un valor escrito manualmente en el navegador.

> **CAPTURA PENDIENTE MU-04 — Carrito de compra**  
> Insertar aquí una captura con productos, cantidades, subtotal y total.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-04-carrito.png`

---

# 8. Preparación del checkout

El checkout está dividido en entrega, datos fiscales, forma de pago y resumen.

## 8.1 Seleccionar la entrega

Elija una de estas alternativas:

- **Retiro en tienda:** seleccione un punto de retiro disponible.
- **Entrega a domicilio:** escriba calle, número, zona y municipio.

## 8.2 Completar los datos fiscales

1. Ingrese el nombre que debe aparecer en la factura.
2. Ingrese el NIT correspondiente.
3. Si la compra es para consumidor final, utilice los datos permitidos por el
   ambiente de demostración.

## 8.3 Seleccionar la forma de pago

PayKlik debe aparecer siempre. La alternativa **Compra ahora, paga en 30 días**
solo aparece cuando el cliente y el total actual cumplen las reglas de crédito.

> **CAPTURA PENDIENTE MU-05 — Checkout y opciones financieras**  
> Insertar aquí una captura que muestre entrega, datos fiscales, PayKlik y, para
> el cliente elegible, la opción de crédito propio.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-05-checkout-opciones.png`

---

# 9. Compra mediante PayKlik

## 9.1 Pago con tarjeta

1. Seleccione **PayKlik** como forma de pago.
2. Seleccione **Tarjeta**.
3. Presione **Reservar y pagar**.
4. En la ventana de PayKlik, revise el total.
5. Utilice únicamente la referencia tokenizada solicitada por el ambiente.
6. Presione **Pagar con esta tarjeta**.
7. Espere la confirmación sin recargar ni duplicar la operación.

## 9.2 Pago por transferencia

1. Seleccione **PayKlik** y luego **Transferencia**.
2. Presione **Reservar y pagar**.
3. Revise banco, cuenta y referencia mostrados.
4. Realice la transferencia en el entorno correspondiente.
5. Presione **Ya realicé la transferencia**.
6. Espere el resultado de la orden.

## 9.3 Resultado esperado

Una respuesta aprobada continúa con facturación y preparación. Si el resultado
queda pendiente, el sistema puede reconciliarlo sin volver a cobrar. No cree
otra compra ni cambie la referencia mientras la primera esté procesándose.

> **CAPTURA PENDIENTE MU-06 — Ventana de PayKlik**  
> Insertar aquí una captura del modal de PayKlik con el total y las alternativas
> de tarjeta o transferencia. No mostrar datos reales de tarjeta.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-06-payklik.png`

---

# 10. Compra mediante crédito propio

La alternativa de crédito permite comprar ahora y pagar en un plazo de 30 días.
Su disponibilidad depende del historial del cliente, ausencia de mora, reglas
vigentes y saldo suficiente.

## 10.1 Interpretar el límite disponible

El límite mostrado es una línea acumulada, no un límite nuevo para cada carrito.

Ejemplo:

```text
Línea disponible inicial:   Q2,500.00
Compra concedida:             Q399.00
Nuevo saldo disponible:     Q2,101.00
```

Si el mismo cliente abre otro carrito, conserva Q2,101.00. Una compra superior
a ese saldo no puede concederse mediante crédito propio.

## 10.2 Realizar la compra

1. Prepare entrega y datos fiscales.
2. Seleccione **Compra ahora, paga en 30 días**.
3. Revise el total, el crédito disponible y la fecha estimada de vencimiento.
4. Presione **Reservar y pagar**.
5. Espere mientras el sistema reserva inventario y vuelve a validar el crédito.
6. Revise el identificador de operación y la fecha de vencimiento mostrados al
   finalizar.

En esta ruta no se solicita tarjeta, transferencia ni referencia de PayKlik.
La compra crea una obligación de crédito propia y consume inmediatamente el
saldo concedido.

## 10.3 Resultado esperado

- La orden queda asociada a la ruta **Crédito propio**.
- Se presenta una fecha de vencimiento a 30 días.
- No se genera un cobro ni un intento de pago PayKlik.
- La facturación referencia la operación de crédito.
- La venta continúa hacia preparación y despacho.

> **CAPTURA PENDIENTE MU-07 — Selección de crédito propio**  
> Insertar aquí una captura del checkout con **Compra ahora, paga en 30 días**
> seleccionada, mostrando límite y vencimiento preliminar.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-07-credito-seleccionado.png`

> **CAPTURA PENDIENTE MU-08 — Crédito concedido**  
> Insertar aquí una captura del resultado exitoso con número de orden,
> identificador de operación de crédito y fecha de vencimiento.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-08-credito-concedido.png`

---

# 11. Crédito no disponible o rechazado

## 11.1 La alternativa no aparece

Esto puede ocurrir cuando:

- no existe historial suficiente;
- el cliente tiene una condición incompatible con la política vigente;
- el total excede el saldo disponible;
- la evaluación tardó demasiado o el servicio no estuvo disponible.

Por privacidad, la tienda no muestra todas las métricas internas del cliente.
PayKlik debe permanecer disponible para continuar la compra.

## 11.2 La concesión es rechazada al confirmar

La elegibilidad se valida nuevamente al finalizar. Si el saldo cambió o la
compra ya no cumple las reglas:

1. el sistema muestra que el crédito fue rechazado;
2. no se crea una deuda con vencimiento;
3. no se realiza un cobro PayKlik automático;
4. la orden permanece recuperable;
5. el cliente puede seleccionar PayKlik y continuar sobre la misma orden.

No repita la compra creando órdenes nuevas. Utilice la alternativa disponible
en la pantalla o contacte a soporte con el identificador de orden.

> **CAPTURA PENDIENTE MU-09 — Rechazo y recuperación por PayKlik**  
> Insertar aquí una captura del mensaje de rechazo y otra donde PayKlik siga
> disponible para continuar.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-09-credito-rechazado.png`

---

# 12. Confirmación y factura

Cuando la operación financiera y la certificación fiscal concluyen, el sistema
muestra la confirmación de compra.

## 12.1 Consultar la factura

1. Abra la factura desde el resultado de compra o desde **Mis pedidos**.
2. Revise los datos del cliente, productos, totales y referencia financiera.
3. Utilice **Descargar XML** para guardar el documento electrónico.
4. Utilice **Imprimir factura** si necesita una copia impresa o PDF.
5. Presione **Cerrar** para regresar a la tienda.

En una compra financiada, la referencia corresponde a la operación de crédito y
no a un intento de PayKlik.

> **CAPTURA PENDIENTE MU-10 — Factura de compra a crédito**  
> Insertar aquí una captura de la factura donde pueda identificarse la operación
> financiera sin exponer información sensible.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-10-factura-credito.png`

---

# 13. Seguimiento de pedidos

1. Seleccione **Mis pedidos**.
2. Elija una orden reciente o ingrese su UUID en **Número de orden**.
3. Presione **Consultar**.
4. Revise el estado de la orden, pago o crédito, factura, envío y
   notificaciones.

## 13.1 Resultado esperado

La vista consolidada permite distinguir el avance de la compra sin consultar
cada sistema por separado. Si algún dato todavía no ha sido actualizado, espere
unos segundos y vuelva a consultar la misma orden.

> **CAPTURA PENDIENTE MU-11 — Seguimiento consolidado**  
> Insertar aquí una captura de **Mis pedidos** con una orden a crédito y su
> progreso de preparación o despacho.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-11-seguimiento.png`

---

# 14. Devoluciones e incidencias

## 14.1 Registrar una solicitud

1. Abra **Devoluciones**.
2. Seleccione la compra relacionada.
3. Elija **Devolución** o **Incidencia**.
4. Describa el motivo de forma clara.
5. Seleccione producto y cantidad.
6. Marque si se trata de una devolución parcial.
7. Presione **Enviar solicitud**.

## 14.2 Consultar solicitudes

1. Seleccione una compra en el panel de consulta.
2. Presione **Buscar por orden**.
3. Para revisar todas las solicitudes del cliente, presione
   **Ver todo mi historial**.

La aprobación de una devolución no implica que la línea de crédito se restaure
automáticamente en esta versión. Si la compra fue financiada, soporte y finanzas
deben aplicar el procedimiento definido para la cartera.

> **CAPTURA PENDIENTE MU-12 — Devoluciones e incidencias**  
> Insertar aquí una captura del formulario y del historial de solicitudes.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-12-devoluciones.png`

---

# 15. Centro de operaciones

El Centro de operaciones se encuentra en:

```text
http://localhost:8081/staff.html
```

El acceso y las acciones disponibles dependen de los roles asignados. La
pantalla reúne indicadores, órdenes recientes, inventario, atención al cliente,
integraciones, reportes y el detalle operativo de cada orden.

## 15.1 Consultar los indicadores generales

1. Inicie sesión con una cuenta interna.
2. Revise **Órdenes registradas**, **Confirmadas**, **Requieren atención** y
   **Valor procesado**.
3. Presione **Actualizar operación** para solicitar datos recientes.

Los indicadores ayudan a priorizar el trabajo, pero no sustituyen la revisión
individual de una orden con rechazo, pago indeterminado o discrepancia.

## 15.2 Seleccionar una orden

Puede abrir una orden de dos maneras:

1. seleccionándola en **Órdenes recientes**; o
2. pegando su UUID completo en **Consultar una orden por identificador** y
   presionando **Cargar**.

Al cargarla se habilita el tablero con resumen, despacho, notificaciones,
incidencias, ERP/POS, auditoría y cadena de integración.

> **CAPTURA PENDIENTE MU-15 — Centro de operaciones**  
> Insertar aquí una captura con indicadores, órdenes recientes y selector por
> identificador.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-15-centro-operaciones.png`

---

# 16. Operación y seguimiento de órdenes

## 16.1 Resumen consolidado

Después de cargar una orden, revise en **Resumen consolidado**:

- estado comercial de la orden;
- ruta financiera y resultado del pago o crédito;
- documento fiscal;
- despacho y entrega;
- incidencias asociadas;
- fecha de la última actualización.

Si el resumen indica que un dato está desactualizado, presione **Actualizar** y
espere a que los consumidores procesen los eventos pendientes.

## 16.2 Crear o revisar un despacho

1. Abra el panel **Despacho** de la orden.
2. Revise los envíos existentes y su línea de tiempo.
3. Para crear uno manualmente, expanda **Nuevo despacho**.
4. Seleccione el origen: centro de distribución, tienda o recogida por cliente.
5. Ingrese la dirección o tienda de destino.
6. Presione **Crear despacho**.

Los cambios posteriores pueden provenir de LogiExpress o del punto de venta que
entrega el pedido. No cree un segundo despacho cuando ya exista uno activo.

## 16.3 Notificaciones

El panel muestra mensajes enviados por correo, SMS o push. Para registrar una
notificación manual de demostración:

1. expanda **Nueva notificación**;
2. seleccione el evento;
3. seleccione el canal;
4. ingrese el destinatario;
5. presione **Enviar**.

Una falla de notificación no debe revertir una venta confirmada. El evento
queda disponible para revisión o reintento.

## 16.4 Gestionar devoluciones e incidencias

1. Abra **Devoluciones e incidencias** en la orden seleccionada.
2. Revise las solicitudes existentes.
3. El personal autorizado puede registrar una nueva solicitud indicando tipo,
   motivo, SKU, cantidad y si es parcial.
4. Cuando corresponda, resuelva la solicitud como reembolsada o rechazada.

Antes de resolver, valide productos, cantidades, entrega y ruta financiera.

> **CAPTURA PENDIENTE MU-16 — Operación consolidada de una orden**  
> Insertar aquí una captura del tablero con resumen, despacho, notificaciones e
> incidencias de la misma orden.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-16-orden-consolidada.png`

---

# 17. Inventario y kárdex

El panel **Inventario de la tienda online — existencias y kárdex** muestra el
stock de `CD-WEB` y sus movimientos.

## 17.1 Interpretar las existencias

| Concepto | Significado |
|---|---|
| En mano | Unidades físicamente recibidas por la tienda online |
| Reservado | Unidades apartadas por compras en curso |
| Disponible | En mano menos reservado |
| En tránsito | Enviadas por el almacén central pero aún no recibidas |

Solo las unidades disponibles pueden venderse o trasladarse.

## 17.2 Consultar movimientos

1. Utilice el filtro para mostrar entradas, salidas o reservas.
2. Escriba un SKU, nombre de artículo u orden en el buscador.
3. Revise fecha, tipo, cantidad, referencia y saldo del movimiento.
4. Presione **Actualizar** si necesita consultar el estado más reciente.

Las reservas vencidas o canceladas deben aparecer como liberadas; una venta
confirmada consolida la salida correspondiente.

> **CAPTURA PENDIENTE MU-17 — Inventario y kárdex**  
> Insertar aquí una captura con el resumen de stock y movimientos filtrados.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-17-inventario-kardex.png`

---

# 18. Atención omnicanal

El bloque **Atención al cliente — call center** permite recuperar el mismo
carrito utilizado por el cliente en la web.

1. Ingrese el correo electrónico del cliente.
2. Presione **Buscar cliente**.
3. Verifique nombre e identidad antes de modificar información.
4. Revise el carrito activo, órdenes e incidencias.
5. Agregue, aumente, disminuya o retire productos según la solicitud.
6. Confirme verbalmente el resultado con el cliente.

El call center no debe crear un cliente nuevo cuando ya existe uno con el mismo
correo. Tampoco debe solicitar contraseñas, datos completos de tarjeta ni CVV.

> **CAPTURA PENDIENTE MU-18 — Atención omnicanal**  
> Insertar aquí una captura de la búsqueda por correo y el carrito recuperado.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-18-call-center.png`

---

# 19. ERP, POS y sistemas conectados

## 19.1 Lotes para el ERP

El personal de finanzas puede consultar lotes de ventas y generar un lote bajo
demanda:

1. abra **ERP AS/400 y POS de tiendas**;
2. revise los lotes existentes y su estado;
3. presione **Generar lote ERP ahora** si corresponde;
4. descargue el archivo disponible;
5. si un lote falló, utilice la opción de reintento autorizada.

Los archivos pueden generarse en CSV o XML según la configuración. No modifique
manualmente los totales, referencias financieras ni controles del archivo.

## 19.2 Eventos enviados a POS

El panel permite comprobar qué cambios de orden fueron distribuidos a las 32
tiendas y si POS-01 los recibió. Utilice **Actualizar** para renovar la vista.

## 19.3 Cadena de integración

La sección resume la participación de:

- PayKlik para autorización de pagos;
- SAT para facturación y CUI;
- LogiExpress para guía y seguimiento;
- mensajería para correo, SMS y push;
- ERP y POS para consolidación comercial.

Una indisponibilidad temporal puede generar reintentos. Antes de repetir una
acción manual, revise el estado y la evidencia de la orden.

> **CAPTURA PENDIENTE MU-19 — ERP y puntos de venta**  
> Insertar aquí una captura de lotes ERP y eventos POS con sus estados.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-19-erp-pos.png`

---

# 20. Informe de ventas y auditoría

## 20.1 Generar el informe conciliado

Esta función está disponible para el personal con permisos de finanzas.

1. Seleccione las fechas **Desde** y **Hasta**.
2. Presione **Generar**.
3. Revise cantidad de ventas, total, base, IVA, entregado, pendiente y número
   de discrepancias.
4. Revise el detalle por canal y punto de venta.
5. Presione **Exportar CSV** si necesita conservar la evidencia.

El informe compara orden, autorización financiera, factura y ERP. Las filas con
discrepancias requieren revisión antes del cierre financiero.

## 20.2 Consulta financiera de una venta a crédito

Esta sección está dirigida al personal con permisos de finanzas.

1. Inicie sesión como personal interno autorizado.
2. Abra el módulo **Informe de ventas**.
3. Seleccione un rango que incluya la fecha de la compra.
4. Genere el informe.
5. Localice la orden por su identificador.
6. Confirme los siguientes datos:

| Campo | Valor esperado para crédito propio |
|---|---|
| Ruta financiera | Crédito propio / `OWN_CREDIT` |
| Estado financiero | Crédito concedido / `CREDIT_GRANTED` |
| Referencia | Identificador de la operación de crédito |
| Estado fiscal | Factura aprobada o estado fiscal correspondiente |
| ERP | Venta registrada con el mismo total y ruta financiera |

La venta a crédito no debe aparecer como **Sin pago** ni como un cobro PayKlik.
Si la ruta del ERP y la orden son diferentes, el reporte debe señalar una
discrepancia para revisión.

> **CAPTURA PENDIENTE MU-13 — Informe de venta a crédito**  
> Insertar aquí una captura del informe con ruta **Crédito propio**, estado
> **CREDIT_GRANTED** y referencia de la operación.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-13-informe-credito.png`

## 20.3 Evidencia de auditoría

1. Cargue una orden en el Centro de operaciones.
2. Abra **Evidencia de auditoría**.
3. Revise actor, acción, resultado, fecha e identificadores de correlación.
4. Presione **Exportar JSON** si cuenta con permisos de Finanzas o Auditor SAT.

La exportación también queda auditada. El archivo no debe alterarse antes de
ser entregado como evidencia.

> **CAPTURA PENDIENTE MU-20 — Informe y auditoría**  
> Insertar aquí una captura del resumen financiero y otra del historial de
> auditoría de una orden.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-20-informe-auditoria.png`

---

# 21. Administración de la tienda online

La administración de la tienda se encuentra en:

```text
http://localhost:8081/tienda-online.html
```

Utilice la cuenta de **Tienda online**. La cabecera muestra el estado de
conexión con el Almacén Central y permite solicitar una sincronización manual.

## 21.1 Recibir mercadería por folio

1. Abra **Recepción**.
2. Ingrese el folio `SAL-...` emitido por el Almacén Central.
3. Presione **Buscar** o salga del campo con Tab.
4. Compare productos y cantidades contra el envío físico.
5. Confirme la recepción.
6. Verifique que el folio pase al historial y que aumente el inventario.

La mercadería **En tránsito** no está disponible para venta hasta confirmar la
recepción. No confirme un folio si existen diferencias físicas.

> **CAPTURA PENDIENTE MU-21 — Recepción por folio**  
> Insertar aquí una captura con el detalle de un folio y la lista de envíos en
> tránsito.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-21-recepcion-folio.png`

## 21.2 Consultar inventario

1. Abra **Inventario**.
2. Revise en mano, reservado, disponible y en tránsito.
3. Utilice el filtro por código o nombre.
4. Investigue cualquier saldo negativo o diferencia antes de publicar o
   trasladar productos.

## 21.3 Administrar el catálogo publicado

1. Abra **Catálogo**.
2. Busque el producto y seleccione la opción para editarlo.
3. Complete nombre comercial, categoría, precio y descripción.
4. Cargue una imagen JPG, PNG o WebP de hasta 2 MB.
5. Presione **Guardar cambios**.
6. Presione **Publicar** cuando la ficha tenga imagen, precio y stock válido.

Para retirar temporalmente un producto de la web, utilice **Despublicar**. Esta
acción no elimina el producto ni sus movimientos históricos.

> **CAPTURA PENDIENTE MU-22 — Inventario y catálogo online**  
> Insertar aquí una captura del inventario y otra del editor de producto.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-22-catalogo-online.png`

## 21.4 Ventas y facturas

1. Abra **Ventas y facturas**.
2. Seleccione el rango de fechas.
3. Presione **Filtrar**.
4. Revise estado de orden, Saga y factura.
5. Abra una factura para imprimirla o descargar su XML.

## 21.5 Emitir un traslado

1. Abra **Traslados**.
2. Seleccione producto, cantidad y destino.
3. Escriba el motivo.
4. Presione **Emitir folio de traslado**.
5. Revise el estado en **Traslados emitidos**.

Solo se traslada inventario disponible; las unidades reservadas por una compra
no pueden moverse. Si el destino rechaza el traslado, el inventario debe volver
a estar disponible. Si no hay conexión, el traslado queda pendiente y se
reintenta.

## 21.6 Revisar la bitácora de sincronización

1. Abra **Bitácora**.
2. Filtre por bajadas, subidas o errores.
3. Revise folio, operación, fecha, resultado y mensaje.
4. Utilice **Sincronizar ahora** solo cuando necesite adelantar el ciclo normal.

> **CAPTURA PENDIENTE MU-23 — Ventas, traslados y bitácora**  
> Insertar aquí capturas de una venta facturada, un traslado y el registro de
> sincronización relacionado.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-23-operacion-online.png`

---

# 22. Portales externos de demostración

MegaStore se integra con sistemas que poseen su propia interfaz local.

| Sistema | URL local | Acceso | Uso principal |
|---|---|---|---|
| Almacén Central | `http://localhost:8082` | `almacen@almacencentral.local` / `Almacen123` | Artículos, existencias, folios y stock por tienda |
| POS-01 | `http://localhost:8084` | Nombre del vendedor | Venta física, recepción, pedidos web y entrega |
| LogiExpress | `http://localhost:8085` | Nombre del mensajero | Guías, recolección, entrega y rastreo |
| PayKlik | `http://localhost:8086` | Nombre del operador | Transacciones y estado del servicio |

## 22.1 Almacén Central

1. Inicie sesión con la cuenta del almacén.
2. Consulte o registre el artículo que se distribuirá.
3. Registre la existencia correspondiente.
4. Emita un folio de salida hacia `POS-ONLINE` o una tienda física.
5. Revise el stock por ubicación y el estado del folio.

El folio representa mercadería en tránsito. No aumente manualmente la
existencia del destino: esta se incorpora cuando el receptor confirma el folio.

## 22.2 Tienda física POS-01

1. Ingrese el nombre del vendedor.
2. Reciba los folios destinados a la tienda y valide sus cantidades.
3. Utilice el módulo de caja para ventas físicas con CF o NIT.
4. Consulte pedidos web destinados a retiro en esa tienda.
5. Entregue al cliente únicamente cuando el pedido esté disponible y se haya
   validado su identidad.
6. Revise la bitácora ante diferencias de inventario o sincronización.

## 22.3 LogiExpress

1. Ingrese el nombre del mensajero.
2. Localice la guía asociada a la orden.
3. Registre la recolección.
4. Actualice el avance del envío.
5. Marque la entrega a domicilio o en tienda según corresponda.
6. Confirme que el nuevo estado aparezca en MegaStore.

El portal permite simular estados del servicio. No marque una entrega si el
paquete no completó el recorrido previsto en la demostración.

## 22.4 PayKlik

1. Ingrese el nombre del operador.
2. Consulte transacciones por referencia u orden.
3. Revise el estado de autorización y los reintentos.
4. Utilice **Estado del servicio** para demostrar operación normal, rechazo,
   lentitud o indisponibilidad.
5. Restablezca el servicio a normal después de la prueba.

Cambiar el estado del proveedor no transforma una compra de crédito propio en
un pago PayKlik; ambas rutas financieras permanecen separadas.

## 22.5 Flujo integral sugerido

1. En Almacén Central, cree o seleccione un artículo y asigne existencia.
2. Emita folios hacia `POS-ONLINE` y `POS-01`.
3. Reciba el folio en la Tienda online y publique el artículo.
4. Realice una compra web con entrega o retiro en tienda.
5. Revise la autorización en PayKlik cuando la ruta corresponda.
6. En LogiExpress, registre recolección y avance de entrega.
7. Si el destino es tienda, finalice la entrega desde POS-01.
8. Compruebe la orden en el Centro de operaciones, ERP, informe y auditoría.

Los portales de PayKlik y LogiExpress permiten cambiar el estado del proveedor
para demostrar lentitud, rechazo o indisponibilidad. Utilice esas opciones solo
en ambientes de prueba y restablezca el modo normal al finalizar.

> **CAPTURA PENDIENTE MU-24 — Sistemas conectados**  
> Insertar aquí un mosaico o capturas separadas de Almacén Central, POS-01,
> LogiExpress y PayKlik.  
> Archivo sugerido: `Docs/capturas/manual-usuario/MU-24-sistemas-conectados.png`

---

# 23. Estados mostrados por el sistema

| Estado | Interpretación |
|---|---|
| `CREATED` | La orden fue creada |
| `INVENTORY_RESERVED` | Los productos fueron reservados |
| `PAYMENT_PENDING` | PayKlik está procesando la solicitud |
| `PAYMENT_APPROVED` | PayKlik aprobó el pago |
| `PAYMENT_REJECTED` | PayKlik rechazó el pago |
| `PAYMENT_INDETERMINATE` | El resultado debe reconciliarse antes de reintentar |
| `CREDIT_PENDING` | La concesión de crédito está siendo evaluada |
| `CREDIT_APPROVED` | El crédito fue concedido |
| `CREDIT_REJECTED` | No se concedió crédito ni se creó deuda |
| `FISCAL_PENDING` | SAT está procesando la factura |
| `FISCAL_APPROVED` | La factura fue aprobada |
| `CONFIRMED` | La venta fue confirmada |
| `PREPARING` | El pedido está en preparación |
| `IN_TRANSIT` | El pedido está en camino |
| `DELIVERED` | El pedido fue entregado |
| `CANCELLED` | La orden fue cancelada |

---

# 24. Solución de problemas

| Situación | Acción recomendada |
|---|---|
| No puedo iniciar sesión | Verifique correo, contraseña y que el backend esté disponible |
| El catálogo no carga | Actualice una vez y confirme que la plataforma esté saludable |
| El carrito aparece vacío | Confirme que inició sesión con la misma cuenta y vuelva a abrir **Mi carrito** |
| Crédito no aparece | El cliente, el total o la disponibilidad del servicio no cumplen las condiciones; utilice PayKlik |
| Crédito fue rechazado | No cree otra orden; seleccione PayKlik en la orden recuperable |
| PayKlik queda pendiente | Espere la reconciliación y consulte **Mis pedidos**; no vuelva a pagar |
| La factura aún no aparece | Espere el procesamiento fiscal y consulte nuevamente la misma orden |
| No encuentro una orden | Copie el UUID completo desde la confirmación o solicite apoyo a soporte |
| El reporte marca discrepancia | Finanzas debe comparar orden, operación financiera, factura y ERP |
| Un folio no aparece para recepción | Confirme destino, estado de envío y sincronización con Almacén Central |
| No puedo publicar un producto | Verifique imagen, precio y existencia recibida |
| Un traslado queda pendiente | Revise la conexión con el destino; no emita otro folio mientras se reintenta |
| LogiExpress o PayKlik no responden | Compruebe el estado del proveedor y restablézcalo en su portal de pruebas |
| No veo una función interna | Inicie sesión con el rol autorizado para Finanzas, Logística o Tienda online |

## 24.1 Información para solicitar soporte

Comparta únicamente:

- identificador completo de la orden;
- fecha y hora aproximada;
- pantalla y acción que estaba realizando;
- estado o mensaje mostrado;
- identificador de correlación, si la interfaz lo presenta.

No comparta contraseñas, JWT, números completos de tarjeta, CVV/CVC ni
referencias privadas de una cuenta bancaria.

---

# 25. Recomendaciones de seguridad

- Utilice únicamente la dirección oficial del ambiente.
- No comparta su contraseña.
- No ingrese PAN, CVV o CVC reales en el ambiente académico.
- Revise total, entrega y datos fiscales antes de confirmar.
- No duplique una compra cuando el sistema indique que está pendiente.
- Cierre sesión en equipos compartidos.
- No publique capturas que contengan tokens o información sensible.

---

# 26. Cierre de sesión

1. Presione **Cerrar sesión** en la cabecera.
2. Confirme que el sistema regresa a la pantalla pública o de acceso.
3. Si utilizó un equipo compartido, cierre también la ventana del navegador.

La opción **Recordarme en este navegador** conserva la sesión en el dispositivo;
por ello no debe utilizarse en equipos públicos.

---

# 27. Lista de capturas pendientes

Antes de entregar el manual, reemplace cada marcador por una captura tomada en
el mismo ambiente y con datos coherentes entre sí.

| Código | Captura | Archivo sugerido | Estado |
|---|---|---|---|
| MU-01 | Inicio de sesión | `MU-01-inicio-sesion.png` | Pendiente |
| MU-02 | Pantalla principal del cliente | `MU-02-tienda-cliente.png` | Pendiente |
| MU-03 | Catálogo y producto agregado | `MU-03-catalogo-producto.png` | Pendiente |
| MU-04 | Carrito de compra | `MU-04-carrito.png` | Pendiente |
| MU-05 | Checkout y opciones financieras | `MU-05-checkout-opciones.png` | Pendiente |
| MU-06 | Ventana PayKlik | `MU-06-payklik.png` | Pendiente |
| MU-07 | Crédito seleccionado | `MU-07-credito-seleccionado.png` | Pendiente |
| MU-08 | Crédito concedido | `MU-08-credito-concedido.png` | Pendiente |
| MU-09 | Crédito rechazado y recuperación | `MU-09-credito-rechazado.png` | Pendiente |
| MU-10 | Factura de crédito | `MU-10-factura-credito.png` | Pendiente |
| MU-11 | Seguimiento consolidado | `MU-11-seguimiento.png` | Pendiente |
| MU-12 | Devoluciones e incidencias | `MU-12-devoluciones.png` | Pendiente |
| MU-13 | Informe financiero de crédito | `MU-13-informe-credito.png` | Pendiente |
| MU-14 | Registro de cliente | `MU-14-registro-cliente.png` | Pendiente |
| MU-15 | Centro de operaciones | `MU-15-centro-operaciones.png` | Pendiente |
| MU-16 | Operación consolidada de orden | `MU-16-orden-consolidada.png` | Pendiente |
| MU-17 | Inventario y kárdex | `MU-17-inventario-kardex.png` | Pendiente |
| MU-18 | Atención omnicanal | `MU-18-call-center.png` | Pendiente |
| MU-19 | ERP y puntos de venta | `MU-19-erp-pos.png` | Pendiente |
| MU-20 | Informe y auditoría | `MU-20-informe-auditoria.png` | Pendiente |
| MU-21 | Recepción por folio | `MU-21-recepcion-folio.png` | Pendiente |
| MU-22 | Inventario y catálogo online | `MU-22-catalogo-online.png` | Pendiente |
| MU-23 | Ventas, traslados y bitácora | `MU-23-operacion-online.png` | Pendiente |
| MU-24 | Sistemas conectados | `MU-24-sistemas-conectados.png` | Pendiente |
| MU-25 | Portal público | `MU-25-portal-publico.png` | Pendiente |

## 27.1 Criterios para las capturas

- Utilizar resolución legible y conservar la interfaz completa cuando aporte
  contexto.
- Mantener el mismo cliente y, cuando sea posible, la misma orden en todo el
  recorrido.
- Ocultar contraseñas, JWT, PAN, CVV/CVC y otros datos sensibles.
- No recortar mensajes de estado relevantes.
- Agregar una descripción breve debajo de cada imagen.
- Confirmar que el nombre del archivo coincida con el código del manual.

---

## Validación final del documento

- [ ] Todas las capturas MU-01 a MU-25 fueron incorporadas.
- [ ] Las imágenes corresponden a la versión entregada del sistema.
- [ ] Se verificaron ortografía, numeración e índice.
- [ ] Las credenciales de demostración siguen vigentes o fueron actualizadas.
- [ ] Se comprobó una compra PayKlik.
- [ ] Se comprobó una compra mediante crédito propio.
- [ ] Se comprobó el consumo acumulado de la línea.
- [ ] Se comprobó una factura y el seguimiento de la orden.
- [ ] Finanzas comprobó la ruta `OWN_CREDIT` en el informe y el ERP.
- [ ] Soporte comprobó la recuperación del carrito omnicanal.
- [ ] Logística comprobó despacho y seguimiento en LogiExpress.
- [ ] Tienda online comprobó recepción, publicación y traslado.
- [ ] Se comprobó la comunicación con Almacén Central y POS-01.
- [ ] Se revisaron notificaciones, incidencias, auditoría y exportaciones.
- [ ] El documento fue revisado por una persona distinta de quien tomó las
      capturas.
