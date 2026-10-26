
CREATE TABLE breathing_space (
    id UUID PRIMARY KEY,
    claim_id uuid NOT NULL,
    reference_number VARCHAR(14),
    start_date DATE,
    type TEXT NOT NULL CHECK (type IN ('MENTAL_HEALTH_CRISIS_MORATORIUM', 'STANDARD')),
    expected_end_date DATE
);

CREATE INDEX idx_breathing_space_claim_id ON public.breathing_space USING btree (claim_id);
