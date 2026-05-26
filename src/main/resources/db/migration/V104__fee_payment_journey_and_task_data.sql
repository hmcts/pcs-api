ALTER TABLE fee_payment ADD COLUMN payment_callback_handler_type VARCHAR(30) NOT NULL;
ALTER TABLE fee_payment ADD COLUMN task_data JSONB;
