# Bitácora de trabajo — Fase 3

## 1. Información general

| Campo | Valor |
|---|---|
| Sistema | SGO-Logic |
| Equipo | Grupo 5 |
| Integrantes | Jairo Gómez, Jorge Pumay, Edson Sosa, Andy Sánchez y Kevin Girón |
| Periodo | 24 de septiembre al 14 de octubre de 2026 (cierre intensivo del 10 al 14 de octubre) |
| Objetivo | Crédito propio "Compra ahora, paga en 30 días" sin degradar PayKlik, más CI/CD con despliegue en la nube, 25 pruebas, cuatro releases y manuales |
| Planificación | `Docs/planificacion/PLANIFICACION_FASE_3.md` |

## 2. Daily Standup

Un bloque por fecha e integrante, sin completar información retroactiva.

### Fecha: 2026-10-13 — Integrante: Kevin Girón

- Trabajo realizado (frente DevOps, KEV3-01 a KEV3-08):
  - **KEV3-01:** ADR-017. Google Compute Engine `e2-standard-2` (8 GB) con Docker Compose, imágenes en GHCR, costo aproximado USD 50/mes y alternativas evaluadas (Cloud Run, GKE, Azure, AWS).
  - **KEV3-03:** `FASE 2/deploy/docker-compose.prod.yml`. Usa imágenes inmutables, solo publica la web (80) y los portales, deja ocultas las bases, RabbitMQ y la API, pone topes de memoria por JVM y `restart: always`. Incluye la plantilla `deploy/.env.production.example` sin secretos.
  - **KEV3-04:** `.github/workflows/ci-cd.yml`.
    - Corre las pruebas de los 5 proyectos Java (matriz) y del frontend.
    - Luego construye las 6 imágenes Docker y, solo con un tag `vN`, despliega a GCP por SSH.
    - `images` depende de todas las pruebas y `deploy` de `images`: **si una prueba falla, no se despliega**.
    - Se puede pedir un despliegue manual de un tag ya publicado (rollback).
  - **KEV3-05:** scripts de operación.
    - `deploy/scripts/deploy.sh`: respaldo previo, espera de los 12 contenedores sanos, smoke y **rollback automático**.
    - `smoke.sh`: 10 verificaciones (salud, login JWT, 200/401, catálogo y portales).
    - `backup.sh`: `pg_dump` de las 5 bases.
    - `vm-bootstrap.sh`: Docker y swap en la VM.
  - **KEV3-06 y KEV3-08:** `docs/operacion/RELEASES-GITFLOW.md` (flujo, comandos y contenido de `v1` a `v4`), `RUNBOOK.md` y `GUIA-GCP-Y-PIPELINE.md`.
- Resultado funcional:
  - El pipeline pasa `actionlint`, incluido `shellcheck`.
  - El Compose de producción combinado con el base es válido.
  - `smoke.sh`: 10 de 10 en verde contra el sistema completo local.
  - `deploy.sh` probado con imágenes reales en un registro local: una versión sana se despliega; una versión que no queda sana se **revierte sola** a la anterior y el script termina en error.
  - Backend: 228 pruebas en verde.
- **Actualización 2026-10-14 (madrugada), KEV3-02 y KEV3-07:**
  - Infraestructura creada en GCP: proyecto `sgo-logic-g5-202010844`, VM `e2-standard-2` en `us-central1-a`, IP fija `136.65.180.39` y firewall de los puertos 80, 8082, 8084, 8085 y 8086.
  - En la VM: Docker 29.9, usuario `deploy` con llave exclusiva y 4 GB de swap.
  - Primer despliegue (`v0-manual`): 12 de 12 contenedores sanos y smoke 10 de 10 desde internet.
  - Backend con el crédito de Jairo: 260 pruebas en verde antes de desplegar.
- **Actualización 2026-10-14 (mañana):**
  - Revisión de lo subido por Jorge (historial ERP del crédito, ciclo de la obligación, matriz 401/403, corrección de tracking concurrente, `.gitattributes` LF): backend **306 pruebas** en verde.
  - Nube actualizada a `v0-manual-2` y cargado el historial de crédito de demostración con `scripts/load-credit-demo-history.sh` (29 registros, 5 perfiles).
  - Corregidos dos defectos de operación encontrados al desplegar:
    - `backup.sh` fallaba con valores de `.env` con espacios; ahora lee solo las claves que necesita.
    - Un corte de SSH podía dejar el despliegue a medias; ahora el pipeline lo ejecuta en la VM con `nohup` y solo sigue su log (probado: el código de salida se propaga).
- **Actualización 2026-10-14 (tarde):**
  - Probando la nube se encontró que la tienda del cliente no cargaba sus botones (ni cerrar sesión): `crypto.randomUUID` solo existe en HTTPS o `localhost` y la nube se sirve por `http://<IP>`. Se agregó un respaldo en `session.js`.
  - nginx ahora comprime HTML, CSS, JS y JSON y guarda en caché las imágenes un día (la portada pesa 1.8 MB).
  - `deploy.sh`: si un servicio Java tardaba más que su healthcheck al arrancar, `docker compose up` fallaba y el script terminaba sin rollback. Ahora sigue esperando la salud, reintenta `up` y después decide.
  - Nube actualizada a `v0-manual-3`: 12 de 12 contenedores sanos y smoke 10 de 10.
- Impedimentos, responsable e impacto:
  - **KEV3-02:** la infraestructura en GCP está lista. Falta cargar en GitHub los secretos (`GCP_VM_SSH_KEY`, `PROD_ENV_FILE`) y las variables (`GCP_VM_HOST`, `GCP_VM_USER`); requiere el rol de administrador del repositorio.
  - **KEV3-07:** el despliegue automático por el pipeline (release `v1`) depende de lo anterior. El despliegue manual y el smoke externo ya están hechos.
  - Los releases `v2` a `v4` dependen de las entregas de Jorge, Jairo, Edson y Andy.
- Horas reales y estimadas restantes: pendiente de registrar por Kevin.
- Persona que quedaría bloqueada y acción temporal: ninguna; Andy ya puede usar la nube (`http://136.65.180.39`) para carga y estrés.
