CREATE TYPE YES_NO_NA AS ENUM ('YES', 'NO', 'NOT_APPLICABLE');

CREATE TABLE housing_act_wales (
  id                     UUID PRIMARY KEY,
  version                INT,
  claim_id               UUID REFERENCES claim (id),
  registered             YES_NO_NA NOT NULL,
  registration_number    VARCHAR(60),
  licensed               YES_NO_NA NOT NULL,
  licence_number         VARCHAR(60),
  agent_appointed        YES_NO_NA NOT NULL,
  agent_first_name       VARCHAR(60),
  agent_last_name        VARCHAR(60),
  agent_licence_number   VARCHAR(60),
  agent_appointment_date DATE
);

/*
sortb = suspension of right to buy
dot = demotion of tenancy
*/
CREATE TABLE possession_alternatives (
  id                        UUID PRIMARY KEY,
  version                   INT,
  claim_id                  UUID REFERENCES claim (id),
  sortb_requested           YES_NO NOT NULL,
  sortb_housing_act_section VARCHAR(20),
  sortb_reason              VARCHAR(250),
  dot_requested             YES_NO NOT NULL,
  dot_housing_act_section   VARCHAR(20),
  dot_statement_served      YES_NO,
  dot_statement_details     VARCHAR(950),
  dot_reason                VARCHAR(250)
);

CREATE TABLE tenancy_licence (
  id                   UUID PRIMARY KEY,
  version              INT,
  case_id              UUID REFERENCES pcs_case (id),
  type                 VARCHAR(40) NOT NULL,
  other_type_details   VARCHAR(500),
  start_date           DATE,
  rent_amount          DECIMAL(18, 2),
  rent_frequency       VARCHAR(20),
  other_rent_frequency VARCHAR(60),
  rent_per_day         DECIMAL(18, 2)
);
