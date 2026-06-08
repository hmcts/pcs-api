ALTER TABLE fee_payment
  ADD CONSTRAINT unique_service_request_ref UNIQUE(service_request_reference)
