#!/usr/bin/env bash
# Despliega una versión (tag de imagen) en la VM y la revierte sola si no queda sana.
# Uso: ./scripts/deploy.sh <tag-de-imagen>      (p. ej. v3 o sha-1a2b3c4)
# Lo invoca el pipeline por SSH desde /opt/sgo-logic (ADR-017).
set -Eeuo pipefail

cd "$(dirname "$0")/.."
NEW_TAG="${1:?Uso: deploy.sh <tag-de-imagen>}"
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.prod.yml)
PREVIOUS_TAG="$(cat .current_tag 2>/dev/null || true)"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-420}"
EXPECTED_SERVICES="${EXPECTED_SERVICES:-12}"
# SKIP_PULL=1: las imágenes ya están en la VM (construidas o cargadas a mano), no se descargan.
SKIP_PULL="${SKIP_PULL:-0}"
SMOKE_CMD="${SMOKE_CMD:-./scripts/smoke.sh http://localhost:${PUBLIC_HTTP_PORT:-80}}"

log() { printf '[deploy %s] %s\n' "$(date -u +%H:%M:%S)" "$*"; }

set_tag() { sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$1/" .env; }

wait_healthy() {
  local deadline=$((SECONDS + HEALTH_TIMEOUT))
  while (( SECONDS < deadline )); do
    local total healthy
    total=$("${COMPOSE[@]}" ps --format '{{.Service}}' | wc -l)
    healthy=$("${COMPOSE[@]}" ps --format '{{.Status}}' | grep -c '(healthy)' || true)
    log "contenedores sanos: ${healthy}/${total}"
    if (( total >= EXPECTED_SERVICES && healthy == total )); then return 0; fi
    sleep 10
  done
  return 1
}

rollback() {
  if [[ -z "${PREVIOUS_TAG}" || "${PREVIOUS_TAG}" == "${NEW_TAG}" ]]; then
    log "No hay versión anterior a la cual volver; el servicio queda como está."
    return 1
  fi
  log "ROLLBACK a ${PREVIOUS_TAG}"
  set_tag "${PREVIOUS_TAG}"
  [[ "${SKIP_PULL}" == 1 ]] || "${COMPOSE[@]}" pull --quiet
  "${COMPOSE[@]}" up -d --no-build --remove-orphans
  wait_healthy && ${SMOKE_CMD} || true
  log "Servicio restaurado con ${PREVIOUS_TAG}. Revise los logs de ${NEW_TAG}."
}

log "Versión actual: ${PREVIOUS_TAG:-ninguna}; nueva: ${NEW_TAG}"
./scripts/backup.sh || log "Advertencia: el respaldo previo falló; se continúa."

set_tag "${NEW_TAG}"
[[ "${SKIP_PULL}" == 1 ]] || "${COMPOSE[@]}" pull --quiet
"${COMPOSE[@]}" up -d --no-build --remove-orphans

if wait_healthy && ${SMOKE_CMD}; then
  [[ -n "${PREVIOUS_TAG}" && "${PREVIOUS_TAG}" != "${NEW_TAG}" ]] && echo "${PREVIOUS_TAG}" > .previous_tag
  echo "${NEW_TAG}" > .current_tag
  docker image prune -f >/dev/null || true
  log "Despliegue de ${NEW_TAG} sano."
  exit 0
fi

log "La versión ${NEW_TAG} no quedó sana. Estado:"
"${COMPOSE[@]}" ps || true
"${COMPOSE[@]}" logs --no-color --tail=80 backend frontend || true
rollback || true
exit 1
