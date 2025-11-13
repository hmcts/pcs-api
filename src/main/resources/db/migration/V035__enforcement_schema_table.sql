CREATE SCHEMA enforcement;

CREATE TABLE enforcement.enf_case (
    id uuid PRIMARY KEY,
    claim_id uuid references public.claim (id),
    enforcement_order JSONB
);