ALTER TABLE claim
  ADD COLUMN pre_action_protocol_incomplete_explanation VARCHAR(250),
  DROP COLUMN mediation_details,
  DROP COLUMN settlement_details;
