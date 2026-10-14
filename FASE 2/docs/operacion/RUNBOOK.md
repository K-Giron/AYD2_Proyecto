# Runbook de operación — SGO-Logic en Google Cloud

Para quien opere el ambiente desplegado (ADR-017). Todos los comandos se
ejecutan en la VM, dentro de la carpeta del sistema:

```bash
ssh -i ~/.ssh/sgo_deploy deploy@<IP>      # o: gcloud compute ssh sgo-logic-vm
cd /opt/sgo-logic
alias dc='docker compose -f docker-compose.yml -f docker-compose.prod.yml'
```

## Estado y salud

| Pregunta | Comando |
|---|---|
| ¿Qué versión corre? | `cat .current_tag` (la anterior: `cat .previous_tag`) |
| ¿Están sanos los 12 contenedores? | `dc ps` (todos deben decir `healthy`) |
| ¿Responde todo de punta a punta? | `./scripts/smoke.sh http://localhost` |
| ¿Cuánta memoria usan? | `docker stats --no-stream` |
| ¿Cuánto disco queda? | `df -h /` y `docker system df` |

## Logs (diagnóstico)

```bash
dc logs --tail=200 backend                 # SGO-Logic
dc logs --tail=200 -f inventory-service    # seguir en vivo el almacén central
dc logs --since=10m | grep -i -E "error|exception"   # errores recientes de todo
```

Todas las peticiones llevan `X-Correlation-Id`. Para seguir una orden por
todos los módulos, busca ese valor en los logs del backend.

## Reiniciar

```bash
dc restart backend          # un servicio
dc up -d --no-build         # vuelve a levantar lo que falte, con la versión actual
```

Un reinicio no pierde datos: las bases y RabbitMQ usan volúmenes.

## Desplegar una versión

Ambiente actual:
- Proyecto `sgo-logic-g5-202010844`, VM `sgo-logic-vm` (`us-central1-a`), IP `136.65.180.39`.
- Acceso: `ssh -i ~/.ssh/sgo_deploy deploy@136.65.180.39`.

Lo normal es **no hacerlo a mano**: al empujar un tag `vN`, el pipeline
despliega solo (ver `RELEASES-GITFLOW.md`). Si hace falta desde la VM, con
una versión ya publicada en GHCR:

```bash
./scripts/deploy.sh v3
```

El script respalda las bases, despliega, espera los 12 contenedores sanos y
corre el smoke. **Si algo falla, regresa solo a la versión anterior** y
termina en error.

Si las imágenes no están en GHCR (por ejemplo, se construyeron en la VM con
`docker compose build` y se etiquetaron como `ghcr.io/jairogh/<imagen>:<tag>`),
se despliegan sin descargar con `SKIP_PULL=1 ./scripts/deploy.sh <tag>`.

## Apagar para no gastar crédito

```bash
gcloud compute instances stop sgo-logic-vm --zone=us-central1-a --project=sgo-logic-g5-202010844    # conserva disco e IP
gcloud compute instances start sgo-logic-vm --zone=us-central1-a --project=sgo-logic-g5-202010844   # los contenedores arrancan solos
```

Para eliminar todo después de la calificación:
`gcloud projects delete sgo-logic-g5-202010844`.

## Rollback

| Situación | Qué hacer |
|---|---|
| El despliegue nuevo no quedó sano | Nada: `deploy.sh` ya volvió a la versión anterior. Revisar los logs del job en GitHub |
| La versión nueva arrancó sana, pero tiene un error funcional | GitHub → Actions → **CI/CD - SGO-Logic** → *Run workflow* → `deploy_tag` = tag anterior (p. ej. `v2`). No recompila; despliega la imagen ya publicada |
| Sin acceso a GitHub | En la VM: `./scripts/deploy.sh $(cat .previous_tag)` |

**Límite:** las migraciones Flyway no se deshacen. Volver a una versión
anterior funciona porque las migraciones del proyecto son aditivas. Si una
release incluyera una migración destructiva, la recuperación es restaurar el
respaldo (siguiente sección).

## Respaldos y restauración

- Se toma un respaldo automático antes de cada despliegue en
  `backups/<fecha>/`: uno por base, comprimido. Se conservan los 7 más
  recientes.
- Respaldo manual: `./scripts/backup.sh`
- Respaldo diario (opcional): `crontab -e` y agregar
  `0 3 * * * cd /opt/sgo-logic && ./scripts/backup.sh >> backups/cron.log 2>&1`

**Restaurar una base**, por ejemplo la de SGO-Logic desde un respaldo concreto:

```bash
set -a; source .env; set +a
dc stop backend
gunzip -c backups/<fecha>/postgres.sql.gz | dc exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
dc start backend
```

Para una restauración completa, primero hay que vaciar el esquema. Coordinarlo
con el equipo.

## Datos de demostración limpios

Para empezar una demo o una prueba de carga desde cero, sin borrar catálogos
semilla, ubicaciones ni cuentas: `./scripts/backup.sh && ./scripts/reset-demo-data.sh --yes`.
El pipeline copia ese script a la VM en cada despliegue.

## Problemas conocidos

| Síntoma | Causa | Acción |
|---|---|---|
| Un servicio Java reinicia en bucle (`OOMKilled` en `docker inspect`) | Memoria insuficiente | Subir su `-Xmx` en `PROD_ENV_FILE` (`BACKEND_JAVA_OPTS`, `SERVICE_JAVA_OPTS`), o la VM a `e2-standard-4` |
| `backend` no sana después de desplegar | Migración fallida o base no disponible | `dc logs backend \| grep -i flyway`; el rollback automático ya restauró la versión anterior |
| Los correos no llegan | Faltan `SGO_MAIL_*` o la contraseña de aplicación es inválida | Revisar `PROD_ENV_FILE`; mientras tanto, los avisos quedan en el simulador |
| El disco se llena | Imágenes viejas o respaldos | `docker image prune -a` (conserva las que están en uso) y revisar `backups/` |

## Contactos

| Frente | Responsable |
|---|---|
| Nube, pipeline, releases | Kevin Girón |
| Carga, estrés y Manual Técnico | Andy Sánchez |
| Seguridad y secretos | Edson Sosa |
| Arquitectura y Saga | Jairo Gómez |
| Inventario y crédito | Jorge Pumay |
