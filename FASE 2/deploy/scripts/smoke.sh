#!/usr/bin/env bash
# Prueba de humo de la plataforma desplegada. Uso: ./scripts/smoke.sh http://IP
# Verifica más que puertos abiertos: salud de cada sistema, login real con JWT,
# catálogo, ruta autorizada y ruta prohibida (403), y los portales.
set -Euo pipefail
BASE="${1:?Uso: smoke.sh <url-base, p. ej. http://34.1.2.3>}"
HOST="$(echo "$BASE" | sed -E 's#^https?://##; s#[:/].*$##')"
SCHEME="$(echo "$BASE" | grep -oE '^https?')"
FAILS=0

check() {   # check <descripción> <código esperado> <curl args...>
  local name="$1" expected="$2"; shift 2
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "$@" || echo 000)
  if [[ "$code" == "$expected" ]]; then echo "  OK   $name ($code)"; else echo "  FALLA $name (esperado $expected, obtuvo $code)"; FAILS=$((FAILS+1)); fi
}

echo "Smoke de ${BASE}"
check "Tienda web" 200 "${BASE}/"
check "API SGO-Logic (vía proxy)" 200 "${BASE}/api/v1/platform/status"
check "Catálogo publicado" 200 "${BASE}/api/v1/online-catalog"
TOKEN=$(curl -s --max-time 15 -H 'Content-Type: application/json' \
  -d '{"username":"finanzas@megastore.com","password":"Finanzas123"}' "${BASE}/api/v1/auth/login" \
  | grep -oE '"accessToken" *: *"[^"]+"' | sed -E 's/.*"([^"]+)"$/\1/')
if [[ -n "${TOKEN}" ]]; then echo "  OK   Login JWT (Finanzas)"; else echo "  FALLA Login JWT (Finanzas)"; FAILS=$((FAILS+1)); fi
check "Informe de ventas con rol FINANCE" 200 -H "Authorization: Bearer ${TOKEN}" "${BASE}/api/v1/reports/sales"
check "Ruta protegida sin token" 401 "${BASE}/api/v1/reports/sales"
for port in 8082 8084 8085 8086; do
  check "Salud :${port}" 200 "${SCHEME}://${HOST}:${port}/actuator/health"
done

if (( FAILS > 0 )); then echo "Smoke: ${FAILS} verificación(es) fallida(s)"; exit 1; fi
echo "Smoke: todo en verde"
