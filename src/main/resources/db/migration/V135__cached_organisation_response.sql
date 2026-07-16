CREATE TABLE public.cached_organisation_response
(
  id UUID PRIMARY KEY,
  idam_id UUID,
  organisation_id varchar(64),
  last_modified_date TIMESTAMP,
  organisation_name varchar(255),
  address_line1 varchar(100),
  address_line2 varchar(100),
  address_line3 varchar(100),
  post_town varchar(100),
  county varchar(100),
  post_code varchar(10),
  country varchar(100)
);

CREATE INDEX idx_cached_organisation_response_idam_id
  ON public.cached_organisation_response(idam_id);
