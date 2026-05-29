ALTER TABLE fee_payment ADD COLUMN IF NOT EXISTS payment_callback_handler_type VARCHAR(30);
ALTER TABLE fee_payment ADD COLUMN IF NOT EXISTS task_data JSONB;

UPDATE fee_payment
SET payment_callback_handler_type = 'CLAIM'
WHERE payment_callback_handler_type IS NULL;

ALTER TABLE fee_payment ALTER COLUMN payment_callback_handler_type SET NOT NULL;
