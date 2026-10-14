# Puesta en marcha: Google Cloud + GitHub Actions

Guía para dejar funcionando el despliegue automático de ADR-017. Se hace **una
sola vez**; después cada release se despliega solo al empujar su tag
(ver `RELEASES-GITFLOW.md`).

Tiempo estimado: 45–60 minutos.

**Necesitas:**
- Una cuenta de Google Cloud con facturación o crédito de prueba.
- `gcloud` instalado, o usar Cloud Shell desde el navegador.
- Acceso de **administrador** al repositorio de GitHub (dueño: Jairo Gómez), para crear secretos y variables.

---

## 1. Crear la VM (Google Cloud)

En Cloud Shell o en una terminal con `gcloud` autenticado:

```bash
PROJECT_ID="sgo-logic-g5"            # o el id de tu proyecto
REGION="us-central1"; ZONE="us-central1-a"
gcloud config set project "$PROJECT_ID"
gcloud services enable compute.googleapis.com

# IP pública fija (para que no cambie al reiniciar)
gcloud compute addresses create sgo-logic-ip --region="$REGION"
IP=$(gcloud compute addresses describe sgo-logic-ip --region="$REGION" --format='value(address)')
echo "IP pública: $IP"

# VM de 2 vCPU / 8 GB con Ubuntu 24.04 y 30 GB de disco
gcloud compute instances create sgo-logic-vm \
  --zone="$ZONE" --machine-type=e2-standard-2 \
  --image-family=ubuntu-2404-lts-amd64 --image-project=ubuntu-os-cloud \
  --boot-disk-size=30GB --boot-disk-type=pd-balanced \
  --address="$IP" --tags=sgo-logic

# Firewall: web (80) y portales de los sistemas propios
gcloud compute firewall-rules create sgo-logic-web \
  --allow=tcp:80,tcp:8082,tcp:8084,tcp:8085,tcp:8086 --target-tags=sgo-logic
```

> Para que los portales no queden abiertos a todo internet, agrega a la regla
> `--source-ranges=<tu-ip>/32,<ip-del-auxiliar>/32`. El puerto 80 (la tienda)
> puede quedar abierto.

## 2. Preparar la VM (una vez)

```bash
gcloud compute ssh sgo-logic-vm --zone="$ZONE"
# ya dentro de la VM:
curl -fsSL https://raw.githubusercontent.com/JairoGH/AYD2_A_2S2025_PROYECTO_G5/develop/FASE%202/deploy/scripts/vm-bootstrap.sh | sudo bash -s -- deploy
exit
```

El script instala Docker y Compose, crea el usuario `deploy` y la carpeta
`/opt/sgo-logic`, y agrega 4 GB de swap.

> Si el repositorio es privado, el `curl` no funciona. En ese caso copia el
> archivo con `gcloud compute scp "FASE 2/deploy/scripts/vm-bootstrap.sh" sgo-logic-vm:~ --zone="$ZONE"`
> y ejecútalo con `sudo bash ~/vm-bootstrap.sh deploy`.

## 3. Llave SSH exclusiva para el pipeline

En tu equipo (no en la VM):

```bash
ssh-keygen -t ed25519 -f ~/.ssh/sgo_deploy -N "" -C "github-actions-sgo"
gcloud compute scp ~/.ssh/sgo_deploy.pub sgo-logic-vm:/tmp/sgo_deploy.pub --zone="$ZONE"
gcloud compute ssh sgo-logic-vm --zone="$ZONE" --command \
  'sudo install -d -m 700 -o deploy -g deploy /home/deploy/.ssh && sudo sh -c "cat /tmp/sgo_deploy.pub >> /home/deploy/.ssh/authorized_keys" && sudo chown deploy:deploy /home/deploy/.ssh/authorized_keys && sudo chmod 600 /home/deploy/.ssh/authorized_keys'
ssh -i ~/.ssh/sgo_deploy deploy@"$IP" 'docker version --format "Docker {{.Server.Version}} listo"'
```

## 4. Variables de producción

1. Copia `FASE 2/deploy/.env.production.example` a un archivo **fuera del repositorio**.
2. Reemplaza cada `CAMBIAR_*` por un valor aleatorio, por ejemplo `openssl rand -base64 32`. Los secretos JWT deben tener al menos 32 caracteres.
3. Pon `SGO_PUBLIC_SITE_URL=http://<IP>`.
4. Si quieres correo real, llena `SGO_MAIL_USERNAME` y `SGO_MAIL_PASSWORD` (contraseña de aplicación de Gmail). Si los dejas vacíos, el correo usa el simulador.
5. No cambies las contraseñas de las cuentas de demostración de personas: son las que usarán los calificadores (README).

## 5. Secretos y variables en GitHub (administrador del repositorio)

**Settings → Secrets and variables → Actions:**

| Tipo | Nombre | Valor |
|---|---|---|
| Secret | `GCP_VM_SSH_KEY` | Contenido completo de `~/.ssh/sgo_deploy` (la llave **privada**) |
| Secret | `PROD_ENV_FILE` | Contenido completo del archivo del paso 4 |
| Variable | `GCP_VM_HOST` | La IP pública (`$IP`) |
| Variable | `GCP_VM_USER` | `deploy` |

Opcional: en **Settings → Environments → produccion** puedes exigir la
aprobación de una persona antes de cada despliegue.

Después borra el archivo local del paso 4.

## 6. Primer despliegue

Sigue `RELEASES-GITFLOW.md` para publicar `v1`. Al empujar el tag:

1. Corren las pruebas de los 5 proyectos Java y la validación del frontend.
2. Se construyen y publican las 6 imágenes en GHCR (`Packages` del repositorio).
3. `deploy` copia el Compose y los scripts a la VM, escribe el `.env`, despliega, espera los 12 contenedores sanos y corre el smoke dentro de la VM y desde internet.

La primera vez tarda más (unos 15 minutos), porque la VM descarga todas las
imágenes.

## 7. Verificar y entregar el ambiente (KEV3-07)

Desde tu equipo:

```bash
bash "FASE 2/deploy/scripts/smoke.sh" "http://$IP"
```

Debe terminar con `Smoke: todo en verde`.

Luego entrega a Andy, para las pruebas de carga y estrés, y al equipo para
el manual y la demo:

| Qué | URL |
|---|---|
| Tienda web y consola | `http://<IP>/` y `http://<IP>/staff.html` |
| Almacén central | `http://<IP>:8082` |
| Tienda POS-01 | `http://<IP>:8084` |
| LogiExpress | `http://<IP>:8085` |
| PayKlik | `http://<IP>:8086` |
| Cuentas | Las del README de `FASE 2` |

## Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `deploy` falla en "Preparar llave SSH" | Falta la variable `GCP_VM_HOST` o el secreto de la llave | Revisar el paso 5 |
| `Permission denied (publickey)` | La llave pública no quedó en `/home/deploy/.ssh/authorized_keys` | Repetir el paso 3 |
| `denied` al hacer `pull` en la VM | El paquete de GHCR no está vinculado al repositorio | En GitHub, Packages → cada imagen → *Manage Actions access* → agregar el repositorio |
| Contenedores en `unhealthy` | Falta memoria o hay un error de arranque | Ver `RUNBOOK.md` → "Diagnóstico" |
| El smoke externo falla, pero el de la VM pasa | Firewall | Revisar la regla del paso 1 |
