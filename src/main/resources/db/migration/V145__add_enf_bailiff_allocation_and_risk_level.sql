-- The bailiffs a warrant has been allocated to (HDPI-7356, HDPI-8189). An allocation belongs to the
-- enforcement, not to the possession case. Reallocating ends the active row and adds a new one, so the
-- history is kept and at most one allocation per enforcement is active (ended_at is null).
--
-- This stands in for the allocated-bailiff CASE role assignment in AM, which pcs-api cannot create yet:
-- see BailiffAllocationService. Rows are removed with their enforcement.
CREATE TABLE public.enf_bailiff_allocation (
    id uuid NOT NULL,
    enf_case_id uuid NOT NULL,
    bailiff_user_id text NOT NULL,
    bailiff_name text,
    bailiff_email text,
    allocated_by_user_id text,
    started_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone,
    CONSTRAINT enf_bailiff_allocation_pkey PRIMARY KEY (id),
    CONSTRAINT enf_bailiff_allocation_enf_case_id_fkey FOREIGN KEY (enf_case_id)
        REFERENCES public.enf_case(id) ON DELETE CASCADE,
    CONSTRAINT enf_bailiff_allocation_ends_after_start CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE INDEX idx_enf_bailiff_allocation_enf_case_id ON public.enf_bailiff_allocation USING btree (enf_case_id);

CREATE UNIQUE INDEX uq_enf_bailiff_allocation_active
    ON public.enf_bailiff_allocation USING btree (enf_case_id) WHERE ended_at IS NULL;

-- The eviction risk level the bailiff sets from the claimant's risk assessment (HDPI-7343). A high
-- level needs a reason. The allowed values match uk.gov.hmcts.reform.pcs.enforcement.domain.RiskLevel.
ALTER TABLE public.enf_case
    ADD COLUMN risk_level TEXT CHECK (risk_level IN ('HIGH', 'MEDIUM')),
    ADD COLUMN risk_level_reason varchar(6800),
    ADD COLUMN risk_level_set_at TIMESTAMP WITH TIME ZONE,
    ADD CONSTRAINT enf_case_high_risk_level_has_reason
        CHECK (risk_level IS DISTINCT FROM 'HIGH' OR risk_level_reason IS NOT NULL);
