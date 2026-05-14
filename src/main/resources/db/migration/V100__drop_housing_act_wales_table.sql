DROP TABLE housing_act_wales;

ALTER TABLE claim
  ADD COLUMN is_exempt_landlord YES_NO;
