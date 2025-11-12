CREATE SCHEMA enforcement;

CREATE TABLE enforcement.enf_case (
    enforcement_case_id uuid PRIMARY KEY,
    case_id uuid references public.pcs_case (id),
    submitted_enforcement_data JSONB
);