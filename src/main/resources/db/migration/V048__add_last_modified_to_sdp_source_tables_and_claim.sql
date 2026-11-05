ALTER TABLE public.fee_payment
    ADD COLUMN last_modified TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE public.defendant_response
    ADD COLUMN last_modified TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE public.claim_party
    ADD COLUMN last_modified TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE public.claim_party_organisation
    ADD COLUMN last_modified TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE public.notice_of_possession
    ADD COLUMN last_modified TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE public.tenancy_licence
    ADD COLUMN last_modified TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE public.claim_ground
    ADD COLUMN last_modified TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE public.claim
    ADD COLUMN last_modified TIMESTAMP WITHOUT TIME ZONE;
