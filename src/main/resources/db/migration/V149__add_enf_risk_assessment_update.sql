-- The risk assessment answers a claimant gives again while confirming the eviction (HDPI-7552). The answers
-- from the application stay on enf_case.enforcement_order; each row here is the full set of answers after
-- one change, so the latest row is the current risk assessment and earlier rows are its history. A row is
-- only written when an answer changed. Rows are never edited, and are removed with their enforcement.
CREATE TABLE public.enf_risk_assessment_update (
    id uuid NOT NULL,
    enf_case_id uuid NOT NULL,
    answers jsonb NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT enf_risk_assessment_update_pkey PRIMARY KEY (id),
    CONSTRAINT enf_risk_assessment_update_enf_case_id_fkey FOREIGN KEY (enf_case_id)
        REFERENCES public.enf_case(id) ON DELETE CASCADE
);

CREATE INDEX idx_enf_risk_assessment_update_enf_case_id
    ON public.enf_risk_assessment_update USING btree (enf_case_id);
