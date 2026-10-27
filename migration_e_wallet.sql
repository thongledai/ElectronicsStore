-- Migration ví điện tử cho nhánh e-wallet (MySQL 8).
-- Cột users.e_wallet đã tồn tại trong schema hiện tại của ElectronicsStore.
-- Chạy migration này một lần trước khi bật luồng ví trên môi trường DDL_AUTO=validate/none.

CREATE TABLE IF NOT EXISTS wallet_top_up_requests (
  id                      binary(16)    NOT NULL,
  user_id                 binary(16)    NOT NULL,
  payment_reference       varchar(30)   NOT NULL,
  requested_amount        decimal(18,2) DEFAULT NULL,
  received_amount         decimal(18,2) DEFAULT NULL,
  status                  varchar(30)   NOT NULL,
  provider_transaction_id varchar(100)  DEFAULT NULL,
  expires_at              datetime(6)   DEFAULT NULL,
  paid_at                 datetime(6)   DEFAULT NULL,
  created_at              datetime(6)   NOT NULL,
  updated_at              datetime(6)   NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wallet_top_up_reference (payment_reference),
  UNIQUE KEY uk_wallet_top_up_provider_tx (provider_transaction_id),
  KEY idx_wallet_top_up_user_status (user_id, status),
  CONSTRAINT fk_wallet_top_up_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE transactions
  ADD COLUMN transaction_type varchar(30) DEFAULT NULL,
  ADD COLUMN reference varchar(50) DEFAULT NULL,
  ADD COLUMN order_id binary(16) DEFAULT NULL,
  ADD KEY idx_transactions_user_created (user_id, created_at);

ALTER TABLE bank_transfer_events
  ADD COLUMN event_type varchar(30) DEFAULT NULL,
  ADD COLUMN wallet_top_up_request_id binary(16) DEFAULT NULL,
  ADD KEY idx_bank_event_wallet_top_up (wallet_top_up_request_id);

INSERT INTO payment_methods (name)
SELECT 'E_WALLET' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM payment_methods WHERE UPPER(name) = 'E_WALLET');

-- Kiểm tra giao dịch nạp ví và thanh toán đơn hàng:
-- SELECT * FROM wallet_top_up_requests ORDER BY created_at DESC;
-- SELECT * FROM transactions WHERE transaction_type IN ('WALLET_TOP_UP', 'ORDER_PAYMENT') ORDER BY created_at DESC;
-- SELECT * FROM bank_transfer_events WHERE event_type = 'WALLET_TOP_UP' ORDER BY created_at DESC;
