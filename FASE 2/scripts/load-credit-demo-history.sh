#!/usr/bin/env bash
# Carga el historial ficticio de crédito (tests/data/credit-history-demo.csv)
# mediante el contrato credit-history-v1, como lo haría el ERP.
#
#   ./scripts/load-credit-demo-history.sh            carga contra http://localhost:8080
#   ./scripts/load-credit-demo-history.sh --print    solo imprime el JSON del lote
#
# Es idempotente: cada fila tiene un sourceReference fijo, así que repetirlo no
# duplica nada. Las fechas se calculan hacia atrás desde hoy para que el
# historial siempre caiga dentro de la ventana de 12 meses.
#
# Clientes de demostración (todos ficticios):
#   ...0001  elegible              ...0004  incidentes críticos (rechazado)
#   ...0002  poca historia         ...0005  pagos tardíos (rechazado)
#   ...0003  devoluciones altas
set -euo pipefail

cd "$(dirname "$0")/.."

API_URL="${SGO_API_URL:-http://localhost:8080}"
FINANCE_USER="${SGO_SECURITY_FINANCE_USER:-finanzas@megastore.com}"
FINANCE_PASSWORD="${SGO_SECURITY_FINANCE_PASSWORD:-Finanzas123}"
DATA_FILE="tests/data/credit-history-demo.csv"

entries=""
while IFS=, read -r customer reference type days amount on_time critical; do
    [[ "$customer" == "customerId" ]] && continue
    at="$(date -u -d "$days days ago" +%Y-%m-%dT%H:%M:%SZ)"
    on_time_json="null"
    [[ -n "$on_time" ]] && on_time_json="$on_time"
    entries+="${entries:+,}{\"customerId\":\"$customer\",\"sourceReference\":\"$reference\",\"type\":\"$type\",\"occurredAt\":\"$at\",\"amount\":$amount,\"onTime\":$on_time_json,\"critical\":$critical}"
done < <(tr -d '\r' < "$DATA_FILE")

correlation="$(cat /proc/sys/kernel/random/uuid 2>/dev/null || printf 'dddddddd-0000-4000-8000-%012d' "$RANDOM")"
batch="{\"contractVersion\":\"credit-history-v1\",\"correlationId\":\"$correlation\",\"entries\":[$entries]}"

if [[ "${1:-}" == "--print" ]]; then
    echo "$batch"
    exit 0
fi

token="$(curl --fail --silent --show-error -X POST "$API_URL/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$FINANCE_USER\",\"password\":\"$FINANCE_PASSWORD\"}" \
    | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"
[[ -n "$token" ]] || { echo "No se obtuvo sesión de finanzas." >&2; exit 1; }

echo "→ Enviando historial de demostración a $API_URL"
curl --fail --silent --show-error -X POST "$API_URL/api/v1/credit/history" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$batch"
echo
