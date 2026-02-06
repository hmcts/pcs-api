CREATE TYPE YES_NO_NOT_SURE AS ENUM ('YES', 'NO', 'NOT_SURE');
CREATE TYPE STATEMENT_OF_TRUTH_COMPLETED_BY AS ENUM ('CLAIMANT', 'LEGAL_REPRESENTATIVE');

CREATE TABLE enf_warrant(
  id                                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  enf_case_id                             UUID NOT NULL REFERENCES enf_case (id) ON DELETE CASCADE,

  -- UI Control Flags
  show_change_name_address_page           YES_NO,
  show_people_who_will_be_evicted_page    YES_NO,
  show_people_you_want_to_evict_page      YES_NO,

  -- Language & Status
  is_suspended_order                      YES_NO,

  -- Additional Information
  additional_information_select           YES_NO,
  additional_information_details          TEXT,

  -- NameAndAddressForEviction
  correct_name_and_address                YES_NO,

  -- PeopleToEvict
  evict_everyone                          YES_NO,

  -- PropertyAccessDetails
  is_difficult_to_access_property         YES_NO,
  clarification_on_access_difficulty_text TEXT,

  -- Legal Costs & Finances
  are_legal_costs_to_be_claimed           YES_NO,
  amount_of_legal_costs                   NUMERIC(10, 2),
  amount_owed                             NUMERIC(10, 2),

  -- Land Registry
  have_land_registry_fees_been_paid       YES_NO,
  amount_of_land_registry_fees            NUMERIC(10, 2),

  -- Repayment
  repayment_choice                        TEXT,
  amount_of_repayment_costs               NUMERIC(10, 2),
  repayment_summary_markdown              TEXT,

  -- Defendants DOB
  defendants_dob_known                    YES_NO,
  defendants_dob_details                  TEXT,

  -- Risk Assessment
  any_risk_to_bailiff                     YES_NO_NOT_SURE,
  enforcement_risk_categories             TEXT,

  -- Vulnerable People (from RawWarrantDetails)
  vulnerable_people_present               YES_NO_NOT_SURE,

  -- Statement of Truth
  statement_of_truth_completed_by         STATEMENT_OF_TRUTH_COMPLETED_BY,
  statement_of_truth_certification        TEXT,
  statement_of_truth_agreement_claimant   TEXT,
  statement_of_truth_full_name_claimant   TEXT,
  statement_of_truth_position_claimant    TEXT,
  statement_of_truth_agreement_legal_rep  TEXT,
  statement_of_truth_full_name_legal_rep  TEXT,
  statement_of_truth_firm_name_legal_rep  TEXT,
  statement_of_truth_position_legal_rep   TEXT,

  CONSTRAINT unique_warrant_per_enforcement UNIQUE (enf_case_id)
);

CREATE INDEX idx_enf_warrant_case_id ON enf_warrant (enf_case_id);
