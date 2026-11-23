-- If interrupted, inspect and drop indexes created here with DROP INDEX CONCURRENTLY,
-- repair failed Flyway history if needed, then retry.

CREATE INDEX CONCURRENTLY idx_claim_party_organisation_party_id
    ON public.claim_party_organisation (party_id);

CREATE INDEX CONCURRENTLY idx_claim_party_contact_details_case_id
    ON public.claim_party_contact_details (case_id);
