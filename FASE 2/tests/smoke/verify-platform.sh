#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PHASE_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKEND_DIR="${PHASE_DIR}/backend"
RUN_MAVEN=true
COMPOSE_STARTED=false
TEMP_DIR="$(mktemp -d)"

usage() {
  echo "Uso: $0 [--skip-maven]"
}

if [[ ${1:-} == "--skip-maven" ]]; then
  RUN_MAVEN=false
  shift
fi

if [[ $# -ne 0 ]]; then
  usage >&2
  exit 2
fi

for command_name in docker curl awk grep; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Falta el comando requerido: ${command_name}" >&2
    exit 1
  fi
done

cleanup() {
  local exit_code=$?
  trap - EXIT INT TERM

  if [[ ${COMPOSE_STARTED} == true ]]; then
    if [[ ${exit_code} -ne 0 ]]; then
      echo "La verificación falló. Estado y logs finales:" >&2
      (cd "${PHASE_DIR}" && docker compose ps --all) >&2 || true
      (cd "${PHASE_DIR}" && docker compose logs --no-color --tail=200) >&2 || true
    fi
    (cd "${PHASE_DIR}" && docker compose down) || true
  fi

  rm -rf "${TEMP_DIR}"
  exit "${exit_code}"
}
trap cleanup EXIT INT TERM

if [[ ${RUN_MAVEN} == true ]]; then
  echo "[1/6] Ejecutando pruebas Maven..."
  (cd "${BACKEND_DIR}" && ./mvnw --batch-mode clean verify)
else
  echo "[1/6] Pruebas Maven omitidas porque ya fueron ejecutadas por el invocador."
fi

echo "[2/6] Validando Docker Compose sin imprimir secretos..."
(cd "${PHASE_DIR}" && docker compose config --quiet)

echo "[3/6] Construyendo y levantando la plataforma..."
COMPOSE_STARTED=true
(cd "${PHASE_DIR}" && docker compose up --build --detach)

echo "[4/6] Esperando health checks..."
for service in postgres rabbitmq backend; do
  healthy=false
  for _ in {1..60}; do
    container_id="$(cd "${PHASE_DIR}" && docker compose ps --quiet "${service}")"
    if [[ -n ${container_id} ]]; then
      health_status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_id}")"
      if [[ ${health_status} == "healthy" ]]; then
        healthy=true
        break
      fi
      if [[ ${health_status} == "exited" || ${health_status} == "dead" || ${health_status} == "unhealthy" ]]; then
        break
      fi
    fi
    sleep 2
  done

  if [[ ${healthy} != true ]]; then
    echo "El servicio ${service} no alcanzó el estado healthy." >&2
    exit 1
  fi
  echo "  - ${service}: healthy"
done

backend_binding="$(cd "${PHASE_DIR}" && docker compose port backend 8080 | head -n 1)"
backend_port="${backend_binding##*:}"
base_url="http://127.0.0.1:${backend_port}"

echo "[5/6] Verificando Actuator..."
curl --fail --silent --show-error \
  --dump-header "${TEMP_DIR}/health.headers" \
  --output "${TEMP_DIR}/health.json" \
  "${base_url}/actuator/health"
grep --fixed-strings --quiet '"status":"UP"' "${TEMP_DIR}/health.json"

echo "[6/6] Verificando status y correlación..."
expected_correlation_id="123e4567-e89b-12d3-a456-426614174000"
curl --fail --silent --show-error \
  --header "X-Correlation-Id: ${expected_correlation_id}" \
  --dump-header "${TEMP_DIR}/status.headers" \
  --output "${TEMP_DIR}/status.json" \
  "${base_url}/api/v1/platform/status"

actual_correlation_id="$(awk -F ': ' 'tolower($1) == "x-correlation-id" {gsub("\r", "", $2); print $2}' "${TEMP_DIR}/status.headers")"
if [[ ${actual_correlation_id} != "${expected_correlation_id}" ]]; then
  echo "El encabezado X-Correlation-Id no coincide con el valor enviado." >&2
  exit 1
fi

grep --fixed-strings --quiet '"applicationName":"sgo-logic"' "${TEMP_DIR}/status.json"
grep --fixed-strings --quiet '"status":"UP"' "${TEMP_DIR}/status.json"
grep --fixed-strings --quiet "\"correlationId\":\"${expected_correlation_id}\"" "${TEMP_DIR}/status.json"

if grep --extended-regexp --ignore-case --quiet 'password|secret|datasource|stackTrace|exception' "${TEMP_DIR}/status.json"; then
  echo "El endpoint técnico expuso un campo sensible o interno." >&2
  exit 1
fi

echo "Verificación completa: Maven, Compose, health checks, Actuator y correlation ID correctos."
