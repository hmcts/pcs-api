CREATE TYPE YES_NO_NOT_SURE AS ENUM ('YES', 'NO', 'NOT_SURE');
CREATE TYPE STATEMENT_OF_TRUTH_COMPLETED_BY AS ENUM ('CLAIMANT', 'LEGAL_REPRESENTATIVE');

CREATE TABLE enf_warrant(
  id                                      UUID PRIMARY KEY,
  enf_case_id                             UUID NOT NULL REFERENCES enf_case (id) ON DELETE CASCADE,

  -- UI Control Flags
  show_change_name_address_page           YES_NO,
  show_people_who_will_be_evicted_page    YES_NO,
  show_people_you_want_to_evict_page      YES_NO,

  -- Language & Status
  is_suspended_order                      YES_NO,

  -- Additional Information
  additional_information_select           YES_NO,
  additional_information_details          VARCHAR(6800),

  -- NameAndAddressForEviction
  correct_name_and_address                YES_NO,

  -- PeopleToEvict
  evict_everyone                          YES_NO,

  -- PropertyAccessDetails
  is_difficult_to_access_property         YES_NO,
  clarification_on_access_difficulty_text VARCHAR(6800),

  -- Legal Costs & Finances
  are_legal_costs_to_be_claimed           YES_NO,
  amount_of_legal_costs                   NUMERIC(10, 2),
  amount_owed                             NUMERIC(10, 2),

  -- Land Registry
  have_land_registry_fees_been_paid       YES_NO,
  amount_of_land_registry_fees            NUMERIC(10, 2),

  -- Repayment
  repayment_choice                        VARCHAR(20),
  amount_of_repayment_costs               NUMERIC(10, 2),
  repayment_summary_markdown              TEXT,

  -- Defendants DOB
  defendants_dob_known                    YES_NO,
  defendants_dob_details                  VARCHAR(6800),

  -- Risk Assessment
  enforcement_risk_categories             TEXT,

  -- Statement of Truth
  completed_by                            STATEMENT_OF_TRUTH_COMPLETED_BY,
  certification                           TEXT,
  agreement_claimant                      TEXT,
  full_name_claimant                      VARCHAR(60),
  position_claimant                       VARCHAR(60),
  agreement_legal_rep                     TEXT,
  full_name_legal_rep                     VARCHAR(60),
  firm_name_legal_rep                     VARCHAR(60),
  position_legal_rep                      VARCHAR(60),

  CONSTRAINT unique_warrant_per_enforcement UNIQUE (enf_case_id)
);

CREATE INDEX idx_enf_warrant_case_id ON enf_warrant (enf_case_id);
