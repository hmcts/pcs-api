CREATE TABLE enf_writ(
  id          UUID PRIMARY KEY,
  enf_case_id UUID NOT NULL REFERENCES enf_case (id) ON DELETE CASCADE,

  -- NameAndAddressForEviction (JsonUnwrapped)
  correct_name_and_address                   YES_NO,

  -- Direct fields
  show_change_name_address_page              YES_NO,
  show_people_who_will_be_evicted_page       YES_NO,
  has_hired_high_court_enforcement_officer   YES_NO,
  hceo_details                               VARCHAR(120),
  has_claim_transferred_to_high_court        YES_NO,

  -- LandRegistryFees
  have_land_registry_fees_been_paid          YES_NO,
  amount_of_land_registry_fees               NUMERIC(10, 2),

  -- LegalCosts
  are_legal_costs_to_be_claimed              YES_NO,
  amount_of_legal_costs                      NUMERIC(10, 2),

  -- MoneyOwedByDefendants
  amount_owed                                NUMERIC(10, 2),

  CONSTRAINT unique_writ_per_enforcement UNIQUE(enf_case_id)
);

CREATE INDEX idx_enf_writ_case_id ON enf_writ (enf_case_id);
