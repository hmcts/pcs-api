CREATE TABLE enf_confirm_eviction(
  id                          UUID PRIMARY KEY,
  enf_case_id                 UUID NOT NULL REFERENCES enf_case (id) ON DELETE CASCADE,

  -- The eviction date
  eviction_date_confirmed     YES_NO NOT NULL,

  -- Dates when you can not attend an eviction
  has_unavaliable_dates       YES_NO NOT NULL,





  language_used     VARCHAR(30),
  created           TIMESTAMP WITHOUT TIME ZONE NOT NULL,

  CONSTRAINT unique_confirm_eviction_per_enforcement UNIQUE(enf_case_id)
);

