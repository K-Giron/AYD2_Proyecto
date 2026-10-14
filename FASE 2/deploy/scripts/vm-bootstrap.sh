#!/usr/bin/env bash
# Preparación única de la VM de Google Cloud (Ubuntu 24.04). Ejecutar una vez:
#   curl -fsSL <url-raw-de-este-archivo> | sudo bash -s -- <usuario-de-despliegue>
# Instala Docker Engine + Compose, crea /opt/sgo-logic y deja al usuario en el
# grupo docker. No guarda secretos: el .env lo escribe el pipeline.
set -Eeuo pipefail
DEPLOY_USER="${1:-deploy}"
apt-get update -y
apt-get install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
id "${DEPLOY_USER}" >/dev/null 2>&1 || useradd -m -s /bin/bash "${DEPLOY_USER}"
usermod -aG docker "${DEPLOY_USER}"
mkdir -p /opt/sgo-logic/scripts /opt/sgo-logic/backups
chown -R "${DEPLOY_USER}:${DEPLOY_USER}" /opt/sgo-logic
# Swap de 4 GB como colchón ante picos (pruebas de estrés).
if ! swapon --show | grep -q swapfile; then
  fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
echo "Listo. Docker $(docker --version | cut -d' ' -f3) y /opt/sgo-logic para ${DEPLOY_USER}."
