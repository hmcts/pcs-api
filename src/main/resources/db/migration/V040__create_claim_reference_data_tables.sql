CREATE TABLE public.claimant_type
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE public.claim_type
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  lov_order INTEGER,
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE public.ground_type
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  parent_category VARCHAR(64),
  parent_key VARCHAR(64),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE public.ground_subtype
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  parent_category VARCHAR(64),
  parent_key VARCHAR(64),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE public.tenancy_type
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  lov_order INTEGER,
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE public.entity_role_code
(
  key VARCHAR(64) PRIMARY KEY,
  value_en VARCHAR(128),
  value_cy VARCHAR(128),
  parent_category VARCHAR(64),
  parent_key VARCHAR(64),
  active YES_NO NOT NULL,
  created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_modified_date TIMESTAMP WITHOUT TIME ZONE
);
