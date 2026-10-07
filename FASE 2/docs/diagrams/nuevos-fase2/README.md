# NUEVO · Diagramas de Fase 2 (código real)

> Agregados por Kevin Girón el 2026-10-07. **No sustituyen** ningún diagrama
> existente:
> - `Docs/diagramas/*` es la vista objetivo de Fase 1.
> - `../modelo-datos.md` y `../SGO-Logic-Modelo-ER.*` siguen como estaban.
>
> Estos muestran lo que el código de Fase 2 hace hoy, para la trazabilidad que
> pide la hoja: CDU expandido, modelo de datos, despliegue y código.

| Archivo | Vista | Qué muestra |
|---|---|---|
| `NUEVO-despliegue-fase2.*` | Despliegue | Los 12 contenedores de `docker-compose.yml` con sus puertos y bases, el volumen `erp-outbox`, los sistemas externos y cada conexión con su protocolo |
| `NUEVO-componentes-fase2.*` | Componentes | Los módulos del monolito (los del equipo y los NUEVOS), los adaptadores reales y los sistemas propios |
| `NUEVO-bloques-fase2.*` | Bloques / contexto | Quién centraliza qué: central = inventario; SGO-Logic = ventas y conciliación; ERP = solo recibe |
| `NUEVO-modelo-datos-central.*` | Modelo de datos | 14 tablas de `inventory-service` |
| `NUEVO-modelo-datos-tienda-pos01.*` | Modelo de datos | 9 tablas de `store-service` |
| `NUEVO-modelo-datos-payklik.*` | Modelo de datos | 2 tablas de `payklik-service` |
| `NUEVO-modelo-datos-logiexpress.*` | Modelo de datos | 3 tablas de `logiexpress-service` |
| `NUEVO-modelo-datos-sgo-logic-nuevas.*` | Modelo de datos | 12 tablas nuevas del monolito (onlinestore, inventorysync, customers) y columnas nuevas de `shipments` y `erp_sales` |
| `NUEVO-cdu-expandidos-fase2.md` | CDU expandidos | Ampliaciones de CDU-03, 05, 06, 07/08 y 09, y los casos nuevos CDU-13 a CDU-16 |

## Cómo regenerarlos

```sh
python3 "FASE 2/docs/diagrams/generar-er-fase2.py"   # modelo de datos, leído de las migraciones Flyway
sh "FASE 2/docs/diagrams/render.sh"                  # PNG y SVG de todos los .dot (usa Docker)
```

Los ER se generan desde las migraciones reales: si alguien agrega una
migración, se vuelven a generar y siguen coincidiendo con el código.

## Pendiente para DOC-01 (Jairo)

- Enlazar estas figuras desde `Docs/DDA.md` (§3 CDU, §6 vistas) y desde
  `modelo-datos.md`.
- Si el equipo prefiere drawio, copiarlas a
  `vistas-arquitectonicas-sgo-logic.drawio` y a `SGO-Logic-Modelo-ER.drawio`.
