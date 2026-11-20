DROP INDEX public.idx_bulk_claim_submission_service_request_reference;

CREATE UNIQUE INDEX uq_bulk_claim_submission_service_request_reference
    ON public.bulk_claim_submission USING btree (service_request_reference)
    WHERE service_request_reference IS NOT NULL;
