-- Sample orders for testing the employee GHN shipment flow.
-- MySQL 8. Run this section once against the application database.
-- It reuses an existing customer with a valid Vietnamese phone, an in-stock
-- product variant, the NOT_PROCESSED order status, and the COD payment method.
-- It does not create users, products, statuses, or payment methods.
--
-- The first street was provided as "46 l\u00ea l\u00ea"; it is kept literally below.
-- Change it to the intended street name before running if that was a typo.

SET NAMES utf8mb4;

SET @sample_user_id := (
    SELECT u.id
    FROM users u
    JOIN roles r ON r.id = u.role_id
    WHERE UPPER(r.name) = 'CUSTOMER'
      AND u.phone REGEXP '^0[35789][0-9]{8}$'
    ORDER BY u.created_at DESC
    LIMIT 1
);

SET @sample_phone := (
    SELECT u.phone
    FROM users u
    WHERE u.id = @sample_user_id
);

SET @sample_variant_id := (
    SELECT pv.id
    FROM product_variants pv
    WHERE pv.is_selling = 1
      AND pv.is_active = 1
      AND pv.quantity > 0
    ORDER BY pv.created_at DESC
    LIMIT 1
);

SET @sample_unit_price := (
    SELECT COALESCE(NULLIF(pv.promotional_price, 0), pv.price)
    FROM product_variants pv
    WHERE pv.id = @sample_variant_id
);

SET @sample_not_processed_status_id := (
    SELECT id FROM order_status WHERE UPPER(name) = 'NOT_PROCESSED' LIMIT 1
);

SET @sample_cod_payment_id := (
    SELECT id FROM payment_methods WHERE UPPER(name) = 'COD' LIMIT 1
);

-- Check this result before continuing. All selected IDs/price must be non-NULL.
SELECT
    BIN_TO_UUID(@sample_user_id) AS customer_id,
    @sample_phone AS recipient_phone,
    BIN_TO_UUID(@sample_variant_id) AS product_variant_id,
    @sample_unit_price AS item_unit_price,
    @sample_not_processed_status_id AS not_processed_status_id,
    @sample_cod_payment_id AS cod_payment_id;

SET @sample_order_ba_ria := '00000000-0000-4000-8000-000000000101';
SET @sample_order_ngai_giao_1 := '00000000-0000-4000-8000-000000000102';
SET @sample_order_ngai_giao_2 := '00000000-0000-4000-8000-000000000103';

START TRANSACTION;

INSERT INTO orders (
    id, user_id, address, to_province_name, to_district_name, to_ward_name,
    phone, status_id, is_paid_before, amount_from_user, ghn_order_code,
    ghn_status, ghn_shipping_fee, expected_delivery_time, payment_id,
    created_at, updated_at
) VALUES
(
    UUID_TO_BIN(@sample_order_ba_ria), @sample_user_id,
    '46 Lê Lê, Phường Bà Rịa, Hồ Chí Minh',
    'H\u1ed3 Ch\u00ed Minh', NULL, 'Ph\u01b0\u1eddng B\u00e0 R\u1ecba', @sample_phone,
    @sample_not_processed_status_id, b'0', @sample_unit_price,
    NULL, NULL, NULL, NULL, @sample_cod_payment_id, NOW(6), NOW(6)
),
(
    UUID_TO_BIN(@sample_order_ngai_giao_1), @sample_user_id,
    '45 Lê Lợi, Xã Ngãi Giao, Hồ Chí Minh',
    'H\u1ed3 Ch\u00ed Minh', NULL, 'X\u00e3 Ng\u00e3i Giao', @sample_phone,
    @sample_not_processed_status_id, b'0', @sample_unit_price,
    NULL, NULL, NULL, NULL, @sample_cod_payment_id, NOW(6), NOW(6)
),
(
    UUID_TO_BIN(@sample_order_ngai_giao_2), @sample_user_id,
    '45 Lê Lợi, Xã Ngãi Giao, Hồ Chí Minh',
    'H\u1ed3 Ch\u00ed Minh', NULL, 'X\u00e3 Ng\u00e3i Giao', @sample_phone,
    @sample_not_processed_status_id, b'0', @sample_unit_price,
    NULL, NULL, NULL, NULL, @sample_cod_payment_id, NOW(6), NOW(6)
);

INSERT INTO order_items (
    id, order_id, variant_id, count, created_at, updated_at
) VALUES
(
    UUID_TO_BIN(UUID()), UUID_TO_BIN(@sample_order_ba_ria),
    @sample_variant_id, 1, NOW(6), NOW(6)
),
(
    UUID_TO_BIN(UUID()), UUID_TO_BIN(@sample_order_ngai_giao_1),
    @sample_variant_id, 1, NOW(6), NOW(6)
),
(
    UUID_TO_BIN(UUID()), UUID_TO_BIN(@sample_order_ngai_giao_2),
    @sample_variant_id, 1, NOW(6), NOW(6)
);

COMMIT;

-- Show the created IDs so you can open these rows in Employee > Orders.
SELECT
    BIN_TO_UUID(id) AS order_id,
    address,
    to_ward_name,
    to_province_name,
    ghn_order_code,
    UPPER((SELECT name FROM order_status s WHERE s.id = o.status_id)) AS order_status
FROM orders o
WHERE id IN (
    UUID_TO_BIN(@sample_order_ba_ria),
    UUID_TO_BIN(@sample_order_ngai_giao_1),
    UUID_TO_BIN(@sample_order_ngai_giao_2)
);

-- Optional: run this separately only AFTER Employee has created a real GHN
-- shipment for the selected sample order. Replace the UUID below if testing
-- another order. This adds a history row only; it does not simulate the
-- webhook handler or change the order's status.
--
-- SET @sample_order_ba_ria := '00000000-0000-4000-8000-000000000101';
-- INSERT INTO ghn_webhook_events
--     (id, order_code, event_type, event_time, status_value, received_at)
-- SELECT UUID_TO_BIN(UUID()), o.ghn_order_code, 'create', UTC_TIMESTAMP(6),
--        'created', UTC_TIMESTAMP(6)
-- FROM orders o
-- WHERE o.id = UUID_TO_BIN(@sample_order_ba_ria)
--   AND o.ghn_order_code IS NOT NULL;
