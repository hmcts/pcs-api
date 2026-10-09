-- Legacy IDAM uids are not UUIDs (e.g. 1245).

ALTER TABLE draft.draft_case_data
  ALTER COLUMN idam_user_id TYPE text USING idam_user_id::text;

ALTER TABLE public.party
  ALTER COLUMN idam_id TYPE text USING idam_id::text;

ALTER TABLE public.cached_organisation_response
  ALTER COLUMN idam_id TYPE text USING idam_id::text;

ALTER TABLE public.draft_orders
  ALTER COLUMN author_idam_user_id TYPE text USING author_idam_user_id::text;
