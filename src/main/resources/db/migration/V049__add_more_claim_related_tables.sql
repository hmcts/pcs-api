CREATE TABLE rent_arrears (
  id                               UUID PRIMARY KEY,
  version                          INT,
  claim_id                         UUID REFERENCES claim (id),
  total_rent_arrears               DECIMAL(18, 2) NOT NULL,
  third_party_payments_made        YES_NO         NOT NULL,
  arrears_judgment_wanted          YES_NO
);

CREATE TABLE rent_arrears_payment_source (
  id              UUID PRIMARY KEY,
  version         INT,
  rent_arrears_id UUID REFERENCES rent_arrears (id),
  name            VARCHAR(40) NOT NULL,
  description     VARCHAR(60)
);

CREATE TABLE notice_of_possession (
  id               UUID PRIMARY KEY,
  version          INT,
  claim_id         UUID REFERENCES claim (id),
  notice_served    YES_NO NOT NULL,
  notice_type      VARCHAR(60),
  serving_method   VARCHAR(40),
  notice_details   VARCHAR(250),
  notice_date      DATE,
  notice_date_time TIMESTAMP,
  CONSTRAINT chk_notice_date CHECK (notice_date IS NULL OR notice_date_time IS NULL)
);

CREATE TABLE statement_of_truth (
  id            UUID PRIMARY KEY,
  version       INT,
  claim_id      UUID REFERENCES claim (id),
  completed_by  VARCHAR(40) NOT NULL,
  accepted      YES_NO      NOT NULL,
  full_name     VARCHAR(60) NOT NULL,
  firm_name     VARCHAR(60),
  position_held VARCHAR(60)
);

ALTER TABLE pcs_case
  DROP COLUMN tenancy_licence,
  DROP COLUMN statement_of_truth;

