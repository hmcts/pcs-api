ALTER TABLE fee_payment ADD COLUMN journey_id VARCHAR(30) NOT NULL;
ALTER TABLE fee_payment ADD COLUMN task_data JSONB;
