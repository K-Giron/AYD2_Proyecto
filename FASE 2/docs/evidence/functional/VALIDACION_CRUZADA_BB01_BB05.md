# Validación cruzada de BB-01 y BB-05

**Estado:** pendiente de ejecución por una persona distinta de Jairo Gómez.

## Identificación

| Campo | Valor |
|---|---|
| Revisor | Pendiente |
| Fecha y zona horaria | Pendiente |
| Commit o PR probado | Pendiente |
| Sistema operativo | Pendiente |
| Docker / Compose / Java | Pendiente |

## Procedimiento desde una copia limpia

1. Crear localmente `FASE 2/.env` a partir de `.env.example` usando credenciales ficticias.
2. Ejecutar:

   ```bash
   cd "FASE 2"
   ./tests/smoke/verify-platform.sh
   ```

3. Confirmar que el comando reporta 13 pruebas verdes, tres servicios saludables, Actuator `UP` y correlation ID correcto.
4. Confirmar que al finalizar no quedan contenedores de Compose ejecutándose.
5. No usar `docker compose down -v` durante esta validación.

## Resultado observado por el revisor

| Comprobación | Resultado | Evidencia |
|---|---|---|
| Construcción mediante Maven Wrapper | Pendiente | Pendiente |
| Pruebas automatizadas | Pendiente | Pendiente |
| PostgreSQL saludable | Pendiente | Pendiente |
| RabbitMQ saludable | Pendiente | Pendiente |
| Backend saludable | Pendiente | Pendiente |
| Actuator HTTP 200 / `UP` | Pendiente | Pendiente |
| Status HTTP 200 y correlation ID | Pendiente | Pendiente |
| Limpieza sin eliminar volúmenes | Pendiente | Pendiente |

## Captura

> **INSERTAR AQUÍ LA CAPTURA DE LA VALIDACIÓN CRUZADA**
>
> Ruta sugerida: `FASE 2/docs/evidence/functional/validacion-cruzada-bb01-bb05.png`

## Decisión

- BB-01: pendiente (`Aprobada`, `Requiere corrección`).
- BB-05: pendiente (`Aprobada`, `Requiere corrección`).
- Observaciones: pendiente.
- Firma o enlace de aprobación: pendiente.
