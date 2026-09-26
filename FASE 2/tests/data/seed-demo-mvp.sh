#!/usr/bin/env bash
set -euo pipefail

# Crea datos de demostración usando exclusivamente contratos públicos. Así
# cada orden conserva reservas, pago, factura, despacho, Outbox y auditoría
# coherentes. Se puede ejecutar después de levantar Docker Compose.

API_BASE="${API_BASE:-http://localhost:8080}"
CUSTOMER_ID="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
RUN_ID="${DEMO_RUN_ID:-$(date -u +%Y%m%d%H%M%S)}"
CORRELATION_ID="$(cat /proc/sys/kernel/random/uuid)"

for command_name in curl jq; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Falta el comando requerido: $command_name" >&2
        exit 1
    fi
done

request() {
    local method="$1"
    local path="$2"
    local token="$3"
    local body="${4:-}"
    local idempotency_key="${5:-}"
    local headers=(
        --header "Authorization: Bearer $token"
        --header "X-Correlation-Id: $CORRELATION_ID"
    )
    if [[ -n "$body" ]]; then
        headers+=(--header "Content-Type: application/json" --data "$body")
    fi
    if [[ -n "$idempotency_key" ]]; then
        headers+=(--header "Idempotency-Key: $idempotency_key")
    fi
    curl --fail-with-body --silent --show-error \
        --request "$method" "${API_BASE}${path}" "${headers[@]}"
}

login() {
    local username="$1"
    local password="$2"
    curl --fail-with-body --silent --show-error \
        --header "Content-Type: application/json" \
        --data "$(jq -cn --arg username "$username" --arg password "$password" \
            '{username:$username,password:$password}')" \
        "${API_BASE}/api/v1/auth/login" | jq -er '.accessToken'
}

echo "Verificando SGO-Logic en ${API_BASE}..." >&2
curl --fail --silent --show-error "${API_BASE}/actuator/health/readiness" >/dev/null
CLIENT_TOKEN="$(login 'cliente@megastore.com' 'Cliente123')"
STAFF_TOKEN="$(login 'staff@megastore.com' 'Staff123')"

create_complete_order() {
    local scenario="$1"
    local product_id="$2"
    local product_name="$3"
    local unit_price="$4"
    local destination="$5"
    local key="demo-${RUN_ID}-${scenario}"

    echo "Creando escenario ${scenario}..." >&2
    local cart
    cart="$(request POST '/api/v1/carts' "$CLIENT_TOKEN" \
        "$(jq -cn --arg customerId "$CUSTOMER_ID" '{customerId:$customerId,channel:"WEB"}')" \
        "${key}-cart")"
    local cart_id
    cart_id="$(jq -er '.id' <<<"$cart")"

    request PUT "/api/v1/carts/${cart_id}/items/${product_id}" "$CLIENT_TOKEN" \
        "$(jq -cn --arg name "$product_name" --argjson price "$unit_price" \
            '{productName:$name,unitPrice:$price,quantity:1}')" >/dev/null

    local order
    order="$(request POST "/api/v1/carts/${cart_id}/checkout" "$CLIENT_TOKEN" '' "${key}-checkout")"
    local order_id
    order_id="$(jq -er '.id' <<<"$order")"

    local saga_body
    saga_body="$(jq -cn --arg destination "$destination" \
        '{paymentMethod:"CARD",currency:"GTQ",paymentReference:"tok_demo_4242",taxIdentifier:"CF",customerName:"Cliente Demo MVP",destination:$destination}')"
    local saga=''
    for attempt in $(seq 1 15); do
        saga="$(request POST "/api/v1/orders/${order_id}/saga" "$CLIENT_TOKEN" \
            "$saga_body" "${key}-saga")"
        if [[ "$(jq -r '.sagaStatus' <<<"$saga")" != 'WAITING_FISCAL' ]]; then
            break
        fi
        sleep 1
    done
    if [[ "$(jq -r '.sagaStatus' <<<"$saga")" != 'COMPLETED' ]]; then
        echo "La Saga de ${scenario} no se completó: $(jq -c . <<<"$saga")" >&2
        exit 1
    fi

    jq -cn \
        --arg scenario "$scenario" \
        --arg orderId "$order_id" \
        --arg shipmentId "$(jq -er '.shipmentId' <<<"$saga")" \
        --arg fiscalCui "$(jq -er '.fiscalCui' <<<"$saga")" \
        '{scenario:$scenario,orderId:$orderId,shipmentId:$shipmentId,fiscalCui:$fiscalCui}'
}

wait_for_tracking_number() {
    local shipment_id="$1"
    local shipment=''
    for attempt in $(seq 1 15); do
        shipment="$(request GET "/api/v1/shipments/${shipment_id}" "$STAFF_TOKEN")"
        if [[ "$(jq -r '.trackingNumber // empty' <<<"$shipment")" != '' ]]; then
            return 0
        fi
        sleep 1
    done
    echo "LogiExpress no emitió guía para el despacho ${shipment_id}." >&2
    return 1
}

PREPARING="$(create_complete_order 'preparacion' \
    '11111111-1111-1111-1111-111111111101' 'Camisa Oxford azul' 249.00 \
    'Recoger en Tienda 01, Ciudad de Guatemala')"

IN_TRANSIT="$(create_complete_order 'en-transito' \
    '11111111-1111-1111-1111-111111111106' 'Cafetera de goteo 12 tazas' 389.00 \
    'Zona 10, Ciudad de Guatemala')"
wait_for_tracking_number "$(jq -r '.shipmentId' <<<"$IN_TRANSIT")"
request POST "/api/v1/shipments/$(jq -r '.shipmentId' <<<"$IN_TRANSIT")/tracking-events" \
    "$STAFF_TOKEN" '{"status":"IN_TRANSIT"}' >/dev/null

POST_SALE="$(create_complete_order 'postventa' \
    '11111111-1111-1111-1111-111111111110' 'Contenedores de vidrio x3' 229.00 \
    'Zona 4, Mixco, Guatemala')"
wait_for_tracking_number "$(jq -r '.shipmentId' <<<"$POST_SALE")"
request POST "/api/v1/shipments/$(jq -r '.shipmentId' <<<"$POST_SALE")/tracking-events" \
    "$STAFF_TOKEN" '{"status":"DELIVERED"}' >/dev/null

INCIDENT="$(request POST '/api/v1/incidents' "$CLIENT_TOKEN" \
    "$(jq -cn --arg orderId "$(jq -r '.orderId' <<<"$POST_SALE")" --arg customerId "$CUSTOMER_ID" \
        '{orderId:$orderId,customerId:$customerId,type:"RETURN",reason:"Una tapa llegó dañada",partial:true,items:[{sku:"MS-HOG-110",quantity:1}]}')")"
request PATCH "/api/v1/incidents/$(jq -r '.id' <<<"$INCIDENT")/resolution" \
    "$STAFF_TOKEN" '{"resolution":"APPROVED","refundAmount":229.00}' >/dev/null

jq -n \
    --arg runId "$RUN_ID" \
    --arg correlationId "$CORRELATION_ID" \
    --argjson preparing "$PREPARING" \
    --argjson inTransit "$IN_TRANSIT" \
    --argjson postSale "$POST_SALE" \
    --arg incidentId "$(jq -r '.id' <<<"$INCIDENT")" \
    '{runId:$runId,correlationId:$correlationId,orders:[$preparing,$inTransit,$postSale],incidentId:$incidentId}'

echo "Demo cargada. Copia los orderId anteriores para Cliente, Staff y Finanzas." >&2
