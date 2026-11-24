CREATE TABLE public.bulk_claim_submission (
    id uuid PRIMARY KEY,
    version integer NOT NULL DEFAULT 0,
    organisation_id text NOT NULL,
    customer_unique_identifier text NOT NULL,
    claim_kind text NOT NULL CHECK (claim_kind IN ('RENTAL', 'MORTGAGE')),
    schema_version text,
    request_hash text NOT NULL,
    status text NOT NULL CHECK (status IN ('PENDING', 'CASE_CREATED', 'FAILED_VALIDATION', 'FAILED')),
    status_code integer,
    fail_reason_code integer,
    fail_reason text,
    validation_errors jsonb,
    staged_case_data jsonb,
    staged_extras jsonb,
    organisation_snapshot jsonb,
    pba_account_number text,
    payment_customer_reference text,
    case_id uuid REFERENCES public.pcs_case(id),
    case_reference bigint,
    service_request_reference text,
    payment_status text NOT NULL DEFAULT 'NOT_STARTED'
        CHECK (payment_status IN ('NOT_STARTED', 'PENDING', 'PAID', 'FAILED')),
    payment_failure_reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    last_modified timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_bulk_claim_submission_org_customer_id UNIQUE (organisation_id, customer_unique_identifier)
);

CREATE INDEX idx_bulk_claim_submission_case_id ON public.bulk_claim_submission USING btree (case_id);
CREATE INDEX idx_bulk_claim_submission_case_reference ON public.bulk_claim_submission USING btree (case_reference);
CREATE INDEX idx_bulk_claim_submission_service_request_reference
    ON public.bulk_claim_submission USING btree (service_request_reference);
