# Vistas arquitectónicas de SGO-Logic

Esta carpeta contiene las cuatro vistas responsabilidad de la Persona 4 (Rol D). Las versiones originales se conservan en Graphviz y la edición visual consolidada se entrega en un único archivo multipágina de draw.io:

- [`vistas-arquitectonicas-sgo-logic.drawio`](./vistas-arquitectonicas-sgo-logic.drawio): páginas `01 - Contexto`, `02 - Componentes`, `03 - Despliegue` y `04 - Bloques`.
- `generar_vistas_arquitectonicas_drawio.py`: fuente reproducible del archivo multipágina.

Los formatos originales permanecen disponibles como respaldo y para las imágenes insertadas en el DDA:

| Vista | Fuente versionable | Imagen para el DDA | Imagen para presentación |
|---|---|---|---|
| Contexto | `contexto.dot` | `contexto.svg` | `contexto.png` |
| Componentes | `componentes.dot` | `componentes.svg` | `componentes.png` |
| Despliegue | `despliegue.dot` | `despliegue.svg` | `despliegue.png` |
| Bloques | `bloques.dot` | `bloques.svg` | `bloques.png` |

También contiene [`cdu-negocio-y-expandidos.drawio`](./cdu-negocio-y-expandidos.drawio), archivo UML editable de 13 páginas generado por `generar_cdu_drawio.py`: una página core y una expansión por cada CUN.

Las trece exportaciones JPG ubicadas en [`img`](./img/) están insertadas como Figuras 3–15 del DDA: una imagen core y una por cada CUN expandido.

## Notación declarada

- **Contexto:** notación de contexto complementaria con frontera explícita, actores, canales, áreas organizacionales, sistemas externos y relaciones dirigidas.
- **Componentes:** elementos de componente UML 2.5.1 simplificados. Las etiquetas declaran interfaces provistas o requeridas; una línea sólida representa interacción síncrona y una línea discontinua representa mensaje o evento asíncrono.
- **Despliegue:** nodos, entornos de ejecución y artefactos basados en UML 2.5.1. El contorno continuo representa el despliegue inicial local; el contorno discontinuo representa una ruta futura, no infraestructura existente. Las imágenes Graphviz diferencian esos contornos adicionalmente con azul y verde.
- **Bloques:** notación ejecutiva complementaria. Resume responsabilidades, estilo y valor de negocio sin sustituir las vistas técnicas.

Graphviz se usa únicamente para diagramar y mantener las fuentes como texto revisable. Los estereotipos, fronteras y semántica de las relaciones se declaran en cada vista para evitar que el motor de dibujo se confunda con la notación arquitectónica.

La versión draw.io usa formas nativas y editables, fondo blanco, jerarquía tipográfica y paleta monocromática. No incrusta las imágenes Graphviz: reconstruye sus elementos y relaciones para facilitar revisión académica y edición manual sin perder la trazabilidad ADR/EaC.

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

Para regenerar el archivo UML multipágina de CDU desde la raíz del repositorio:

```powershell
python Docs\diagramas\generar_cdu_drawio.py
python C:\ruta\al\skill\scripts\validate_drawio.py Docs\diagramas\cdu-negocio-y-expandidos.drawio
```

La segunda ruta depende de dónde esté instalado el skill `uml-casos-uso-drawio`; el archivo generado también puede abrirse directamente en diagrams.net para revisión visual.

Para regenerar las cuatro vistas arquitectónicas editables:

```powershell
python Docs\diagramas\generar_vistas_arquitectonicas_drawio.py
python C:\ruta\al\skill\scripts\validate_drawio.py Docs\diagramas\vistas-arquitectonicas-sgo-logic.drawio
```

El validador especializado en CDU puede advertir que estas páginas no contienen actores u óvalos de casos de uso. Esas advertencias son esperables en Componentes, Despliegue y Bloques; la validación estructural del XML, las referencias y las auto-conexiones sí aplica.

## Decisiones aceptadas y parámetros abiertos

Las vistas reflejan ADR-001 a ADR-008 aceptados. Conservan abiertos únicamente los parámetros que el caso no proporciona:

- ADR-001 fija un monolito modular con puertos, adaptadores y mensajería.
- ADR-002 fija Outbox; no se equipara con Event Store.
- ADR-004 ubica idempotencia en los adaptadores para no modificar POS ni ERP.
- ADR-005 fija una Saga orquestada para el ciclo de la orden.
- ADR-006 a ADR-008 fijan observabilidad centralizada, recuperación fiscal y notificaciones no bloqueantes.
- La cantidad de instancias de SGO-Logic debe resultar de pruebas contra la demanda de ocho veces el tráfico normal.
- Productos, SLO, RTO, RPO, retención y residencia de datos requieren evidencia o confirmación del stakeholder correspondiente.
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
| EaC-12 | Adaptador de pagos con TLS, tokenización e indicación de no persistir PAN completo ni CVV |
