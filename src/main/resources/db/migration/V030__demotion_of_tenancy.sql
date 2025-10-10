ALTER TABLE claim
  ADD COLUMN demotion_of_tenancy_housing_act VARCHAR(30),
  ADD COLUMN demotion_of_tenancy_reason VARCHAR(250),
  ADD COLUMN statement_of_express_terms_details VARCHAR(950);