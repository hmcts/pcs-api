ALTER TABLE fee_payment
  RENAME COLUMN request_reference to service_request_reference;

ALTER TABLE fee_payment
  ADD COLUMN related_entity_id UUID;
