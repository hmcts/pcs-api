ALTER TABLE counter_claim
  ADD COLUMN court_permission_granted YES_NO,
  ADD COLUMN permission_order_date DATE,
  ADD COLUMN claim_received_date DATE;
