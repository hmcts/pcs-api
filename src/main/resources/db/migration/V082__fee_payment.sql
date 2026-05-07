CREATE TABLE fee_payment (
  id                            UUID NOT NULL,
  claim_id                      UUID NOT NULL,
  party_id                      UUID,
  request_date                  TIMESTAMP NOT NULL,
  request_reference             VARCHAR(255),
  external_reference            VARCHAR(255),
  amount                        NUMERIC(19, 2),
  payment_status                VARCHAR(50),

  CONSTRAINT pk_fee_payment PRIMARY KEY (id),
  CONSTRAINT fk_fee_payment_claim  FOREIGN KEY (claim_id)  REFERENCES claim (id)
);

CREATE INDEX idx_fee_payment_request_reference ON fee_payment (request_reference);
