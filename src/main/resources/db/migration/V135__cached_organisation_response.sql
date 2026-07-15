CREATE TABLE public.cached_organisation_response
(
  id UUID PRIMARY KEY,
  idam_id UUID,
  organisation_id varchar(64),
  last_modified_date TIMESTAMP
);

CREATE INDEX idx_cached_organisation_response_idam_id
  ON public.cached_organisation_response(idam_id);
