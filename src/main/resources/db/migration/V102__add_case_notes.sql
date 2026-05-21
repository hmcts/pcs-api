CREATE TABLE public.case_note
(
    id UUID PRIMARY KEY,
    claim_id UUID NOT NULL REFERENCES public.claim (id),
    created_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    note VARCHAR(500) NOT NULL
);

CREATE INDEX idx_case_note_claim_id ON public.case_note(claim_id);