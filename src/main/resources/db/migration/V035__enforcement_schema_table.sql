CREATE SCHEMA enforcement;

CREATE TABLE enforcement.enf_case (
    id uuid PRIMARY KEY,
    enf_case_id uuid references public.claim (id),
    submitted_enf_order JSONB
);