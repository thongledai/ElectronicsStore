-- Migration: thanh toán chuyển khoản VietQR (MySQL 8)
-- Có thể bỏ qua file này nếu chạy app với DDL_AUTO=update (mặc định) - Hibernate sẽ tự tạo cột/bảng tương ứng.
-- Chạy file này TRƯỚC khi khởi động app nếu môi trường dùng DDL_AUTO=validate/none.

ALTER TABLE `orders`
  ADD COLUMN `payment_status`     varchar(30) NULL COMMENT 'NULL = COD; UNPAID/PAID/EXPIRED/CANCELLED/NEEDS_RECONCILIATION',
  ADD COLUMN `payment_reference`  varchar(30) NULL COMMENT 'Mã tham chiếu duy nhất trong nội dung chuyển khoản',
  ADD COLUMN `payment_expires_at` datetime(6) NULL,
  ADD COLUMN `paid_at`            datetime(6) NULL,
  ADD UNIQUE KEY `uk_orders_payment_reference` (`payment_reference`);

CREATE TABLE IF NOT EXISTS `bank_transfer_events` (
  `id`                      binary(16)    NOT NULL,
  `provider`                varchar(30)   NOT NULL,
  `provider_transaction_id` varchar(100)  NOT NULL,
  `bank_reference_code`     varchar(100)  DEFAULT NULL,
  `order_id`                binary(16)    DEFAULT NULL,
  `payment_reference`       varchar(30)   DEFAULT NULL,
  `account_number`          varchar(30)   DEFAULT NULL,
  `amount`                  decimal(18,2) DEFAULT NULL,
  `transfer_type`           varchar(10)   DEFAULT NULL,
  `content`                 varchar(500) CHARACTER SET utf8mb4 DEFAULT NULL,
  `result`                  varchar(30)   NOT NULL,
  `created_at`              datetime(6)   NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bank_event_provider_tx` (`provider`, `provider_transaction_id`),
  KEY `idx_bank_event_order` (`order_id`),
  KEY `idx_bank_event_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Phương thức thanh toán mới (app cũng tự thêm nếu thiếu khi khởi động)
INSERT INTO `payment_methods` (`name`)
SELECT 'BANK_QR' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `payment_methods` WHERE `name` = 'BANK_QR');

-- Truy vấn đối soát: các giao dịch tiền vào chưa được ghép tự động
-- SELECT * FROM bank_transfer_events WHERE result <> 'PAID' ORDER BY created_at DESC;
-- SELECT HEX(id), payment_reference, total FROM orders WHERE payment_status = 'NEEDS_RECONCILIATION';
