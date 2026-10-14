#!/usr/bin/env bash
# Deja los tres sistemas sin datos de operación para probar desde cero
# (folios, envíos, facturación, ventas web y de tienda), sin tocar la
# configuración: usuarios, almacenes, puntos de venta, catálogo web e
# inventario de la web (Jorge) se conservan.
#
#   ./scripts/reset-demo-data.sh          pide confirmación
#   ./scripts/reset-demo-data.sh --yes    sin preguntar
#
# Requiere el stack levantado con `docker compose up -d` desde FASE 2.
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ "${1:-}" != "--yes" ]]; then
    echo "Esto BORRA los datos de operación de:"
    echo "  - Almacén central (inventory-db): artículos, existencias, folios, envíos, cuentas,"
    echo "    facturas y traslados de tiendas, stock por tienda y bitácora."
    echo "  - Tienda web (postgres): órdenes, carritos, pagos, fiscal, envíos, incidencias,"
    echo "    notificaciones, outbox/inbox, auditoría, ERP/POS, clientes (salvo el demo) y la"
    echo "    tienda online (recepciones, catálogo publicado y su inventario en CD-WEB)."
    echo "  - Tienda física POS-01 (store-db): todo."
    read -r -p "¿Continuar? escribe 'si': " answer
    [[ "$answer" == "si" ]] || { echo "Cancelado."; exit 1; }
fi

# Lee una variable de .env sin ejecutarlo (hay valores con espacios).
env_get() {
    local value=""
    [[ -f .env ]] && value="$(grep -E "^$1=" .env | tail -n1 | cut -d= -f2-)"
    echo "${value:-$2}"
}

psql_in() {
    local service="$1" user="$2" db="$3"
    docker compose exec -T "$service" psql -v ON_ERROR_STOP=1 -q -U "$user" -d "$db"
}

echo "→ Almacén central (inventory-db)"
psql_in inventory-db "$(env_get INVENTORY_POSTGRES_USER inventory_local)" "$(env_get INVENTORY_POSTGRES_DB inventory_service_local)" <<'SQL'
BEGIN;
TRUNCATE store_invoice_lines, store_invoices, store_transfers,
         pos_stock_events, pos_stock_movements, pos_stock,
         shipments, invoices, stock_movements, stock, articles,
         audit_log_entries
    CASCADE;
ALTER SEQUENCE seq_folio_entrada     RESTART WITH 1;
ALTER SEQUENCE seq_folio_salida      RESTART WITH 1;
ALTER SEQUENCE seq_pos_folio_entrada RESTART WITH 1;
ALTER SEQUENCE seq_pos_folio_venta   RESTART WITH 1;
ALTER SEQUENCE seq_store_invoice     RESTART WITH 1;
ALTER SEQUENCE seq_store_transfer    RESTART WITH 1;
ALTER SEQUENCE seq_folio_ajuste      RESTART WITH 1;
COMMIT;
SQL

echo "→ Tienda web (postgres)"
psql_in postgres "$(env_get POSTGRES_USER sgo_local)" "$(env_get POSTGRES_DB sgo_logic_local)" <<'SQL'
BEGIN;
TRUNCATE cart_items, carts,
         customer_order_items, customer_orders, order_status_history, order_customer_links,
         order_summary_projections,
         payment_attempts, payment_reconciliations,
         fiscal_documents, erp_sales, erp_batches, pos_deliveries,
         shipment_tracking_events, shipment_dead_letters, shipments,
         incident_items, incidents,
         notification_requests, notification_dead_letters,
         inventory_reservations,
         inventorysync_order_origins, inventory_bridge_dead_letters,
         outbox_event, inbox_event, inbox_messages,
         audit_events,
         online_store_receipts, online_catalog_images, online_catalog_products,
         online_store_transfers, online_store_sync_log, online_store_stock_events
    CASCADE;
-- Crédito propio: se borran decisiones, obligaciones e historial cargado, y el
-- cliente demo (V4005) recupera su perfil y su saldo de origen.
TRUNCATE credit_operations, credit_decisions, credit_history_records, credit_customer_profiles CASCADE;
INSERT INTO credit_customer_profiles (
    customer_id, completed_purchases, on_time_payment_rate, return_rate,
    critical_incidents, has_overdue_credit, available_credit, observed_at
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 8, 1.0000, 0.1250,
    0, FALSE, 2500.00, CURRENT_TIMESTAMP
);
ALTER SEQUENCE seq_online_transfer RESTART WITH 1;
-- Lo recibido por la tienda online (ubicación CD-WEB) es operación, no semilla.
DELETE FROM inventory_stock_movements
WHERE location_id IN (SELECT id FROM inventory_locations WHERE code = 'CD-WEB');
DELETE FROM inventory_items
WHERE location_id IN (SELECT id FROM inventory_locations WHERE code = 'CD-WEB');
-- Solo queda el cliente demo (sembrado por V1008) y su contacto (V4003).
DELETE FROM customer_contacts WHERE customer_id <> 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
DELETE FROM customers WHERE id <> 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
-- Sin órdenes no puede haber reservas; la existencia física de la web se conserva.
UPDATE inventory_items SET quantity_reserved = 0;
COMMIT;
SQL

echo "→ Tienda física POS-01 (store-db)"
psql_in store-db "$(env_get STORE_POSTGRES_USER store_local)" "$(env_get STORE_POSTGRES_DB store_pos01_local)" <<'SQL'
BEGIN;
TRUNCATE sale_lines, sales, transfers, store_receipts, store_stock, store_articles, sync_log, pos_events, stock_events CASCADE;
ALTER SEQUENCE seq_ticket   RESTART WITH 1;
ALTER SEQUENCE seq_transfer RESTART WITH 1;
COMMIT;
SQL

echo "→ LogiExpress (logiexpress-db): guías y rastreo"
psql_in logiexpress-db "$(env_get LOGIEXPRESS_POSTGRES_USER logiexpress_local)" "$(env_get LOGIEXPRESS_POSTGRES_DB logiexpress_local)" <<'SQL'
BEGIN;
TRUNCATE guide_events, guides;
ALTER SEQUENCE seq_tracking_number RESTART WITH 1;
UPDATE operation_settings SET mode = 'NORMAL', updated_by = 'reset', updated_at = now();
COMMIT;
SQL

echo "→ PayKlik (payklik-db): transacciones"
psql_in payklik-db "$(env_get PAYKLIK_POSTGRES_USER payklik_local)" "$(env_get PAYKLIK_POSTGRES_DB payklik_local)" <<'SQL'
BEGIN;
TRUNCATE transactions;
ALTER SEQUENCE seq_authorization_code RESTART WITH 1;
UPDATE operation_settings SET mode = 'NORMAL', updated_by = 'reset', updated_at = now();
COMMIT;
SQL

echo "→ Archivos de lotes ERP (carpeta del AS/400)"
docker compose exec -T backend sh -c 'rm -f /var/sgo/erp-outbox/ventas_*' >/dev/null 2>&1 || true

# Cachés en memoria (catálogo del central en la web, estado de sincronización
# de la tienda) se vacían reiniciando los servicios.
echo "→ Reiniciando servicios para vaciar cachés en memoria"
docker compose restart backend inventory-service store-service >/dev/null 2>&1

echo "Listo. Siguiente paso: crear artículos en el almacén central (http://localhost:$(env_get INVENTORY_SERVICE_PORT 8082))"
echo "y surtir POS-01 y POS-ONLINE con folios de salida."
