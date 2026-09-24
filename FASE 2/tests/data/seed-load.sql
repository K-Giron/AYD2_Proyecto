-- QA-02 / DEC-02: conjunto de datos declarado para la linea base T y la
-- carga 8T. Es idempotente: puede ejecutarse varias veces sin duplicar.
--
-- 500 proyecciones de orden (00000000-0000-0000-0000-000000000001 a ...0500)
-- y 10 000 eventos de auditoria (20 por orden). Datos sinteticos.

INSERT INTO order_summary_projections
    (order_id, order_status, order_status_as_of, updated_at)
SELECT ('00000000-0000-0000-0000-' || lpad(i::text, 12, '0'))::uuid,
       'CONFIRMED',
       now(),
       now()
FROM generate_series(1, 500) AS i
ON CONFLICT (order_id) DO NOTHING;

INSERT INTO audit_events
    (id, event_id, aggregate_type, aggregate_id, order_id, actor, action, result,
     before_state, after_state, correlation_id, causation_id, occurred_at)
SELECT gen_random_uuid(),
       gen_random_uuid(),
       'Order',
       ('00000000-0000-0000-0000-' || lpad(i::text, 12, '0'))::uuid,
       ('00000000-0000-0000-0000-' || lpad(i::text, 12, '0'))::uuid,
       'load-seed',
       'ORDER_STATE_CHANGED',
       'OK',
       NULL,
       NULL,
       gen_random_uuid(),
       NULL,
       now() - (j || ' seconds')::interval
FROM generate_series(1, 500) AS i,
     generate_series(1, 20) AS j
ON CONFLICT (event_id) DO NOTHING;
