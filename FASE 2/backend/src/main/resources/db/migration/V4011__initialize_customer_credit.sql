-- Línea inicial individual para todo cliente registrado antes de esta versión.
-- Las cuentas creadas posteriormente se inicializan idempotentemente al
-- consultar sus opciones de pago, desde CreditService.
INSERT INTO credit_customer_profiles (
    customer_id, completed_purchases, on_time_payment_rate, return_rate,
    critical_incidents, has_overdue_credit, available_credit, observed_at, version
)
SELECT c.id, 0, 1.0000, 0.0000, 0, FALSE, 2500.00, CURRENT_TIMESTAMP, 0
FROM customers c
ON CONFLICT (customer_id) DO NOTHING;
