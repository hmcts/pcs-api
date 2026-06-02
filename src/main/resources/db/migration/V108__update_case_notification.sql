ALTER TABLE case_notification
  RENAME COLUMN notification_id TO id;

ALTER TABLE case_notification
  RENAME CONSTRAINT pk_case_notification TO pk_case_notification_id;

ALTER TABLE case_notification
  ADD COLUMN party_id UUID NOT NULL,
  ADD COLUMN claim_id UUID NOT NULL,
  ADD COLUMN claim_type VARCHAR(255) NOT NULL;

ALTER TABLE case_notification
  ADD CONSTRAINT fk_case_notification_case
    FOREIGN KEY (case_id)
      REFERENCES pcs_case (id),
  ADD CONSTRAINT fk_case_notification_party
    FOREIGN KEY (party_id)
      REFERENCES party (id),
  ADD CONSTRAINT fk_case_notification_claim
    FOREIGN KEY (claim_id)
      REFERENCES claim (id);
