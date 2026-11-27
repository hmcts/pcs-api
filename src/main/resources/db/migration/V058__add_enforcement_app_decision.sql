CREATE TABLE enf_application_decision (
  id                                  INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  decision                            VARCHAR(10) NOT NULL,
  rejection_reason                    VARCHAR(40),
  rejection_reason_additional_details TEXT,
  created_at                          TIMESTAMP WITHOUT TIME ZONE DEFAULT now() NOT NULL,
  last_modified_at                    TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE enf_warrant
  ADD COLUMN enf_application_decision_id INTEGER REFERENCES enf_application_decision (id);

ALTER TABLE enf_writ
  ADD COLUMN enf_application_decision_id INTEGER REFERENCES enf_application_decision (id);
