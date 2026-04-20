CREATE TABLE enf_eviction(
  id                                                      UUID PRIMARY KEY,
  enf_case_id                                             UUID NOT NULL REFERENCES enf_case (id) ON DELETE CASCADE,

  -- The eviction date
  eviction_date_confirmed                                 YES_NO NOT NULL,

  -- Dates when you can not attend an eviction
  has_unavailable_dates                                    YES_NO NOT NULL,

  -- separate table for dates unavailable table

  -- Contact for Bailiff
  before_eviction_name_or_department                        VARCHAR(100),
  before_eviction_telephone_number                          VARCHAR(20),
  before_eviction_email_address                             VARCHAR(60),

  after_eviction_name_or_department                         VARCHAR(100),
  after_eviction_telephone_number                           VARCHAR(20),
  after_eviction_email_address                              VARCHAR(60),

  -- access to the property
  difficult_to_access                                       YES_NO,
  difficult_to_access_details                               VARCHAR(6800),

  -- Anything else that could help with the eviction
  anything_else                                              YES_NO,
  anything_else_details                                      VARCHAR(6800),

  -- Gaining access to and securing the property after the eviction
  confirm_arrangement_for_access                              YES_NO,

  language_used                                               VARCHAR(30),
  created                                                     TIMESTAMP WITHOUT TIME ZONE NOT NULL,

  CONSTRAINT unique_confirm_eviction_per_enforcement UNIQUE(enf_case_id)
);

ALTER TABLE enf_risk_profile
  ADD COLUMN physical_description_of_risk_defendant YES_NO;

ALTER TABLE enf_risk_profile
  ADD COLUMN physical_description_of_risk_defendant_details VARCHAR(6800);

CREATE TABLE enf_unavailable_date (
  id                                                   UUID PRIMARY KEY,
  enf_eviction_id                                      UUID NOT NULL REFERENCES enf_eviction (id) ON DELETE CASCADE,
  unavailable_date                                     DATE NOT NULL
);
