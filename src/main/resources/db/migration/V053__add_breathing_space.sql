
CREATE TABLE breathing_space (
    id UUID PRIMARY KEY,
    pcs_case_id uuid NOT NULL,
    reference_number VARCHAR(14),
    start_date DATE,
    type VARCHAR(50),
    expected_end_date DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE
       DEFAULT CURRENT_TIMESTAMP NOT NULL,

        CONSTRAINT breathing_space_pcs_case_fk
               FOREIGN KEY (pcs_case_id)
               REFERENCES public.pcs_case (id)
);

CREATE INDEX idx_breathing_space_pcs_case_id ON public.breathing_space USING btree (pcs_case_id);
