# Vistas arquitectónicas de SGO-Logic

Esta carpeta contiene las cuatro vistas responsabilidad de la Persona 4 (Rol D). Cada vista se conserva en tres formatos:

| Vista | Fuente versionable | Imagen para el DDA | Imagen para presentación |
|---|---|---|---|
| Contexto | `contexto.dot` | `contexto.svg` | `contexto.png` |
| Componentes | `componentes.dot` | `componentes.svg` | `componentes.png` |
| Despliegue | `despliegue.dot` | `despliegue.svg` | `despliegue.png` |
| Bloques | `bloques.dot` | `bloques.svg` | `bloques.png` |

## Notación declarada

- **Contexto:** notación de contexto complementaria con frontera explícita, actores, canales, áreas organizacionales, sistemas externos y relaciones dirigidas.
- **Componentes:** elementos de componente UML 2.5.1 simplificados. Las etiquetas declaran interfaces provistas o requeridas; una línea sólida representa interacción síncrona y una línea discontinua representa mensaje o evento asíncrono.
- **Despliegue:** nodos, entornos de ejecución y artefactos basados en UML 2.5.1. El contorno azul representa el despliegue inicial local; el contorno verde discontinuo representa una ruta futura, no infraestructura existente.
- **Bloques:** notación ejecutiva complementaria. Resume responsabilidades, estilo y valor de negocio sin sustituir las vistas técnicas.

Graphviz se usa únicamente para diagramar y mantener las fuentes como texto revisable. Los estereotipos, fronteras y semántica de las relaciones se declaran en cada vista para evitar que el motor de dibujo se confunda con la notación arquitectónica.

## Regeneración

Desde la raíz del repositorio, con Graphviz instalado y `dot` disponible:

```powershell
Get-ChildItem Docs\diagramas\*.dot | ForEach-Object {
  dot -Tsvg $_.FullName -o ([IO.Path]::ChangeExtension($_.FullName, '.svg'))
  dot -Tpng -Gdpi=150 $_.FullName -o ([IO.Path]::ChangeExtension($_.FullName, '.png'))
}
```

Para regenerar una sola vista:

```powershell
dot -Tsvg Docs\diagramas\despliegue.dot -o Docs\diagramas\despliegue.svg
dot -Tpng -Gdpi=150 Docs\diagramas\despliegue.dot -o Docs\diagramas\despliegue.png
```

## Decisiones abiertas preservadas

Las vistas no inventan cantidades de réplicas, productos, RTO, RPO, disponibilidad ni mecanismos aún no aceptados. En particular:

- ADR-001 continúa propuesto hasta la revisión del equipo y la Persona 3.
- El mecanismo de publicación confiable continúa sujeto a ADR-002; la vista no equipara automáticamente Outbox con Event Store.
- Reintentos, idempotencia y conciliación se muestran como responsabilidades obligatorias, pero sus políticas y umbrales siguen pendientes de ADR.
- La cantidad de instancias de SGO-Logic debe resultar de pruebas contra la demanda de ocho veces el tráfico normal.
- La ruta a nube es una evolución planificada y no se presenta como parte del despliegue inicial.

## Correspondencia con EaC en despliegue

| EaC | Elemento visible |
|---|---|
| EaC-01 | Entrada segura y artefacto SGO-Logic; ruta de consulta con objetivo menor o igual a cinco segundos |
| EaC-02 | Registro y lectura de auditoría con consulta objetivo menor a un segundo |
| EaC-03 | Instancias de SGO-Logic dimensionadas por prueba de carga a ocho veces el tráfico normal y observabilidad |
| EaC-04 | Adaptador PayKlik con timeout y Circuit Breaker, intento persistido, broker y reintento/conciliación |
| EaC-05 | Adaptador LogiExpress con retry/backoff, broker y mensajes fallidos |
| EaC-06 | Trabajadores, broker y entrega eventual a POS |
| EaC-07 | Registro de auditoría de quién, cuándo y qué cambió |
| EaC-08 | Cola y trabajador de notificaciones no bloqueantes |
| EaC-09 | Un desplegable modular operado por el equipo interno con telemetría centralizada |
| EaC-10 | Nota de restricción de costo operativo; la topología se valida contra USD 1,200 mensuales |
| EaC-11 | Contorno discontinuo de ruta futura a nube |
| EaC-12 | Adaptador de pagos e indicación de no persistir PAN completo ni CVV; los controles concretos quedan por decidir |
