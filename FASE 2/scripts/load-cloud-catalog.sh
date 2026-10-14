#!/usr/bin/env bash
set -Eeuo pipefail

BASE_URL="${BASE_URL:-http://136.65.180.39}"
INVENTORY_URL="${INVENTORY_URL:-${BASE_URL}:8082}"
POS01_URL="${POS01_URL:-${BASE_URL}:8084}"
INVENTORY_USER="${INVENTORY_USER:-almacen@almacencentral.local}"
INVENTORY_PASSWORD="${INVENTORY_PASSWORD:-Almacen123}"
LOGISTICS_USER="${LOGISTICS_USER:-logistica@almacencentral.local}"
LOGISTICS_PASSWORD="${LOGISTICS_PASSWORD:-Logistica123}"
ONLINE_USER="${ONLINE_USER:-tienda-online@megastore.com}"
ONLINE_PASSWORD="${ONLINE_PASSWORD:-TiendaOnline123}"
LOAD_ID="${LOAD_ID:-CATALOG-20261014}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_DIR="${IMAGE_DIR:-${SCRIPT_DIR}/../docs/evidence/catalogo-demo/imagenes}"

command -v curl >/dev/null || { echo "Falta curl." >&2; exit 1; }
command -v jq >/dev/null || { echo "Falta jq." >&2; exit 1; }
[[ -d "$IMAGE_DIR" ]] || { echo "No existe el directorio de imágenes: $IMAGE_DIR" >&2; exit 1; }

login() {
    local url="$1" username="$2" password="$3"
    curl --fail-with-body --silent --show-error \
        -H 'Content-Type: application/json' \
        -d "$(jq -nc --arg username "$username" --arg password "$password" '{username:$username,password:$password}')" \
        "$url/api/v1/auth/login" | jq -er '.accessToken'
}

inventory_token="$(login "$INVENTORY_URL" "$INVENTORY_USER" "$INVENTORY_PASSWORD")"
logistics_token="$(login "$INVENTORY_URL" "$LOGISTICS_USER" "$LOGISTICS_PASSWORD")"
online_token="$(login "$BASE_URL" "$ONLINE_USER" "$ONLINE_PASSWORD")"

inventory_api() {
    local method="$1" path="$2"
    shift 2
    curl --fail-with-body --silent --show-error -X "$method" \
        -H "Authorization: Bearer $inventory_token" "$@" "$INVENTORY_URL$path"
}

online_api() {
    local method="$1" path="$2"
    shift 2
    curl --fail-with-body --silent --show-error -X "$method" \
        -H "Authorization: Bearer $online_token" "$@" "$BASE_URL$path"
}

logistics_api() {
    local method="$1" path="$2"
    shift 2
    curl --fail-with-body --silent --show-error -X "$method" \
        -H "Authorization: Bearer $logistics_token" "$@" "$INVENTORY_URL$path"
}

# sku|nombre|descripción|categoría|precio|archivo
PRODUCTS=(
  'MS-HOG-201|Set de ropa de cama terracota|Juego de cama suave con acabado contemporáneo para dormitorio, ideal para renovar el ambiente con tonos cálidos.|Hogar|699.00|MS-HOG-201-ropa-cama.webp'
  'MS-HOG-202|Cafetera programable 12 tazas|Cafetera programable con jarra de vidrio, temporizador y capacidad para 12 tazas.|Hogar|549.00|MS-HOG-202-cafetera.webp'
  'MS-HOG-203|Juego de toallas premium|Set de toallas absorbentes de textura suave para baño, en tonos naturales.|Hogar|299.00|MS-HOG-203-toallas.webp'
  'MS-HOG-204|Set de recipientes de vidrio|Recipientes de vidrio con tapas herméticas para conservar y organizar alimentos.|Hogar|279.00|MS-HOG-204-recipientes.webp'
  'MS-HOG-205|Lámpara de mesa de cerámica|Lámpara decorativa de cerámica con pantalla textil, perfecta para mesa de noche o sala.|Hogar|349.00|MS-HOG-205-lampara.webp'
  'MS-TEC-301|Bocina Bluetooth compacta|Bocina inalámbrica portátil con sonido claro y diseño compacto para acompañarte en cualquier espacio.|Tecnología|389.00|MS-TEC-301-bocina.webp'
  'MS-TEC-302|Laptop ultraligera de 14 pulgadas|Computadora portátil ligera con pantalla de 14 pulgadas, pensada para estudio, trabajo y movilidad.|Tecnología|5499.00|MS-TEC-302-laptop.webp'
  'MS-TEC-303|Audífonos inalámbricos|Audífonos Bluetooth de diadema con diseño cómodo y sonido envolvente.|Tecnología|649.00|MS-TEC-303-audifonos.webp'
  'MS-TEC-304|Reloj inteligente Active|Reloj inteligente con pantalla táctil y seguimiento diario de actividad.|Tecnología|799.00|MS-TEC-304-smartwatch.webp'
  'MS-MOD-401|Bolso ejecutivo camel|Bolso estructurado de estilo ejecutivo con asas resistentes y amplio espacio interior.|Moda|749.00|MS-MOD-401-bolso.webp'
  'MS-MOD-402|Chaqueta ligera beige|Chaqueta versátil de corte moderno para combinar en clima fresco y ocasiones casuales.|Moda|599.00|MS-MOD-402-chaqueta.webp'
  'MS-CAM-101|Camisa manga larga azul|Camisa azul de manga larga con corte clásico, cómoda para oficina o uso casual.|Moda|249.00|MS-CAM-101-camisa.webp'
  'MS-PAN-102|Pantalón de lona negro|Pantalón negro de lona con corte recto y estilo versátil para uso diario.|Moda|329.00|MS-PAN-102-pantalon.webp'
  'MS-ZAP-103|Tenis urbanos blancos|Calzado deportivo blanco de diseño limpio, cómodo para recorridos y actividades cotidianas.|Moda|599.00|MS-ZAP-103-tenis.webp'
)

for row in "${PRODUCTS[@]}"; do
    IFS='|' read -r sku name description category price image_file <<<"$row"
    [[ -f "$IMAGE_DIR/$image_file" ]] || { echo "Falta la imagen de $sku: $image_file" >&2; exit 1; }
done

articles="$(inventory_api GET /api/v1/articles)"
points="$(inventory_api GET /api/v1/points-of-sale)"
online_pos_id="$(jq -er '.[] | select(.code == "POS-ONLINE") | .id' <<<"$points")"
pos01_id="$(jq -er '.[] | select(.code == "POS-01") | .id' <<<"$points")"

echo "1/5 Creando artículos faltantes en almacén central..."
for row in "${PRODUCTS[@]}"; do
    IFS='|' read -r sku name description category price image_file <<<"$row"
    article_id="$(jq -r --arg sku "$sku" '.[] | select(.sku == $sku) | .id' <<<"$articles")"
    if [[ -z "$article_id" ]]; then
        payload="$(jq -nc --arg sku "$sku" --arg name "$name" --arg description "$description" --argjson price "$price" \
            '{sku:$sku,name:$name,description:$description,price:$price}')"
        created="$(inventory_api POST /api/v1/articles -H 'Content-Type: application/json' -d "$payload")"
        articles="$(jq -c --argjson item "$created" '. + [$item]' <<<"$articles")"
        echo "  creado $sku"
    else
        echo "  existente $sku"
    fi
done

echo "2/5 Cargando existencia y emitiendo despachos..."
movements="$(inventory_api GET /api/v1/stock-movements)"
stock="$(inventory_api GET /api/v1/stock)"
online_folios=()
pos01_folios=()

register_movement() {
    local article_id="$1" type="$2" quantity="$3" reason="$4" pos_id="$5" reference="$6"
    local payload
    payload="$(jq -nc --arg articleId "$article_id" --arg type "$type" --argjson quantity "$quantity" \
        --arg reason "$reason" --arg pointOfSaleId "$pos_id" --arg externalReference "$reference" \
        '{articleId:$articleId,type:$type,quantity:$quantity,reason:$reason,pointOfSaleId:(if $pointOfSaleId == "" then null else $pointOfSaleId end),externalReference:$externalReference}')"
    inventory_api POST /api/v1/stock-movements -H 'Content-Type: application/json' -d "$payload"
}

for row in "${PRODUCTS[@]}"; do
    IFS='|' read -r sku name description category price image_file <<<"$row"
    article_id="$(jq -er --arg sku "$sku" '.[] | select(.sku == $sku) | .id' <<<"$articles")"
    entry_ref="$LOAD_ID-ENTRY-$sku"
    online_ref="$LOAD_ID-ONLINE-$sku"
    pos01_ref="$LOAD_ID-POS01-$sku"

    if ! jq -e --arg ref "$entry_ref" '.[] | select(.externalReference == $ref)' <<<"$movements" >/dev/null; then
        current="$(jq -r --arg sku "$sku" '[.[] | select(.sku == $sku) | .quantityOnHand][0] // 0' <<<"$stock")"
        entry_quantity=$((60 - current))
        if (( entry_quantity > 0 )); then
            movement="$(register_movement "$article_id" ENTRADA "$entry_quantity" 'Carga inicial de catálogo demostrativo' '' "$entry_ref")"
            movements="$(jq -c --argjson item "$movement" '. + [$item]' <<<"$movements")"
            echo "  $sku: entrada de $entry_quantity"
        fi
    fi

    existing="$(jq -c --arg ref "$online_ref" '[.[] | select(.externalReference == $ref)][0] // null' <<<"$movements")"
    if [[ "$existing" == "null" ]]; then
        movement="$(register_movement "$article_id" SALIDA 18 'Abastecimiento inicial de Tienda Online' "$online_pos_id" "$online_ref")"
        movements="$(jq -c --argjson item "$movement" '. + [$item]' <<<"$movements")"
    else
        movement="$existing"
    fi
    online_folios+=("$(jq -er '.folio' <<<"$movement")")

    existing="$(jq -c --arg ref "$pos01_ref" '[.[] | select(.externalReference == $ref)][0] // null' <<<"$movements")"
    if [[ "$existing" == "null" ]]; then
        movement="$(register_movement "$article_id" SALIDA 15 'Abastecimiento inicial de Tienda Centro' "$pos01_id" "$pos01_ref")"
        movements="$(jq -c --argjson item "$movement" '. + [$item]' <<<"$movements")"
    else
        movement="$existing"
    fi
    pos01_folios+=("$(jq -er '.folio' <<<"$movement")")
    echo "  $sku: despachos listos para POS-ONLINE y POS-01"
done

echo "3/5 Sincronizando y confirmando recepciones..."
advance_shipment_to() {
    local folio="$1" target="$2"
    logistics_api PATCH "/api/v1/shipments/$folio/status" \
        -H 'Content-Type: application/json' -d "$(jq -nc --arg status "$target" '{status:$status}')" >/dev/null
}

# El almacén registra que los folios salieron antes de que las tiendas los reciban.
for folio in "${online_folios[@]}" "${pos01_folios[@]}"; do
    status="$(inventory_api GET "/api/v1/shipments/$folio" | jq -r '.status')"
    if [[ "$status" == "PENDIENTE" ]]; then
        advance_shipment_to "$folio" EN_TRANSITO
    fi
done

online_api POST /api/v1/online-store/sync >/dev/null
curl --fail-with-body --silent --show-error -X POST "$POS01_URL/api/store/sync" >/dev/null

for folio in "${online_folios[@]}"; do
    shipment="$(online_api GET "/api/v1/online-store/receiving/$folio")"
    if [[ "$(jq -r '.status' <<<"$shipment")" == "EN_TRANSITO" ]]; then
        online_api POST "/api/v1/online-store/receiving/$folio/confirm" >/dev/null
    fi
done
for folio in "${pos01_folios[@]}"; do
    shipment="$(curl --fail-with-body --silent --show-error "$POS01_URL/api/store/receiving/$folio")"
    if [[ "$(jq -r '.status' <<<"$shipment")" == "EN_TRANSITO" ]]; then
        curl --fail-with-body --silent --show-error -X POST \
            -H 'Content-Type: application/json' -d '{"seller":"Carga inicial de catálogo"}' \
            "$POS01_URL/api/store/receiving/$folio/confirm" >/dev/null
    fi
done

# Ambas tiendas ya confirmaron la recepción; se cierra el mismo folio en el central.
for folio in "${online_folios[@]}" "${pos01_folios[@]}"; do
    status="$(inventory_api GET "/api/v1/shipments/$folio" | jq -r '.status')"
    if [[ "$status" == "PENDIENTE" ]]; then
        advance_shipment_to "$folio" EN_TRANSITO
        status="EN_TRANSITO"
    fi
    if [[ "$status" == "EN_TRANSITO" ]]; then
        advance_shipment_to "$folio" ENTREGADO
    fi
done

echo "4/5 Actualizando fichas, imágenes y publicación web..."
products="$(online_api GET /api/v1/online-store/products)"
for row in "${PRODUCTS[@]}"; do
    IFS='|' read -r sku name description category price image_file <<<"$row"
    product_id="$(jq -er --arg sku "$sku" '.[] | select(.sku == $sku) | .id' <<<"$products")"
    payload="$(jq -nc --arg name "$name" --arg description "$description" --arg category "$category" --argjson price "$price" \
        '{name:$name,description:$description,category:$category,price:$price}')"
    online_api PUT "/api/v1/online-store/products/$product_id" -H 'Content-Type: application/json' -d "$payload" >/dev/null
    online_api PUT "/api/v1/online-store/products/$product_id/image" -H 'Content-Type: image/webp' --data-binary "@$IMAGE_DIR/$image_file" >/dev/null
    online_api POST "/api/v1/online-store/products/$product_id/publish" >/dev/null
    echo "  publicado $sku ($category)"
done

echo "5/5 Verificando resultado final..."
central_stock="$(inventory_api GET /api/v1/stock)"
online_inventory="$(online_api GET /api/v1/online-store/inventory)"
pos01_stock="$(curl --fail-with-body --silent --show-error "$POS01_URL/api/store/stock")"
public_catalog="$(curl --fail-with-body --silent --show-error "$BASE_URL/api/v1/online-catalog")"

jq -e --argjson expected "$(printf '%s\n' "${PRODUCTS[@]}" | wc -l)" \
    '[.[] | select(.available >= 18 and (.image != null))] | length == $expected' \
    <<<"$public_catalog" >/dev/null
jq -e --argjson expected "$(printf '%s\n' "${PRODUCTS[@]}" | wc -l)" \
    '[.[] | select(.onHand >= 18 and .available >= 18 and .published == true)] | length == $expected' \
    <<<"$online_inventory" >/dev/null
jq -e --argjson expected "$(printf '%s\n' "${PRODUCTS[@]}" | wc -l)" \
    '[.[] | select(.quantity >= 15)] | length == $expected' <<<"$pos01_stock" >/dev/null

echo "Carga completada:"
jq -r 'group_by(.category)[] | "  \(.[0].category): \(length) producto(s) publicado(s)"' <<<"$public_catalog"
echo "  Tienda Online: $(jq 'length' <<<"$online_inventory") productos con existencia"
echo "  POS-01: $(jq 'length' <<<"$pos01_stock") productos con existencia"
echo "  Almacén central: $(jq 'length' <<<"$central_stock") artículos controlados"
