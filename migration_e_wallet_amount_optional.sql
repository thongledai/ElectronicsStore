-- Allow wallet top-ups to accept any positive amount received for the reference.
-- Run once on databases where wallet_top_up_requests already exists.
ALTER TABLE wallet_top_up_requests
  MODIFY COLUMN requested_amount DECIMAL(18,2) NULL;
