#!/usr/bin/env bash
# Respaldo lógico de las 5 bases (pg_dump comprimido). Se ejecuta antes de cada
# despliegue y puede programarse en cron. Conserva los últimos 7 respaldos.
set -Eeuo pipefail
cd "$(dirname "$0")/.."
# Se leen solo las claves necesarias: hacer `source .env` falla con valores que
# llevan espacios (p. ej. STORE_NAME=Tienda Centro).
env_get() { grep -E "^$1=" .env | tail -1 | cut -d= -f2-; }
STAMP="$(date -u +%Y%m%d-%H%M%S)"
DIR="backups/${STAMP}"
mkdir -p "${DIR}"
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.prod.yml)
dump() {  # dump <servicio> <usuario> <base>
  if "${COMPOSE[@]}" ps --status running --format '{{.Service}}' | grep -qx "$1"; then
    "${COMPOSE[@]}" exec -T "$1" pg_dump -U "$2" -d "$3" | gzip > "${DIR}/$1.sql.gz"
    echo "  respaldo $1 → ${DIR}/$1.sql.gz"
  fi
}
dump postgres         "$(env_get POSTGRES_USER)" "$(env_get POSTGRES_DB)"
dump inventory-db     "$(env_get INVENTORY_POSTGRES_USER)" "$(env_get INVENTORY_POSTGRES_DB)"
dump store-db         "$(env_get STORE_POSTGRES_USER)" "$(env_get STORE_POSTGRES_DB)"
dump payklik-db       "$(env_get PAYKLIK_POSTGRES_USER)" "$(env_get PAYKLIK_POSTGRES_DB)"
dump logiexpress-db   "$(env_get LOGIEXPRESS_POSTGRES_USER)" "$(env_get LOGIEXPRESS_POSTGRES_DB)"
ls -1dt backups/*/ 2>/dev/null | tail -n +8 | xargs -r rm -rf
