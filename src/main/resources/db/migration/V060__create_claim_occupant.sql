CREATE TABLE public.claim_occupant (
    id integer NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    claim_id uuid NOT NULL REFERENCES public.claim(id) ON DELETE CASCADE,
    rank integer NOT NULL,
    title text,
    first_name text NOT NULL,
    last_name text NOT NULL,
    other_names text,
    initials text,
    last_modified timestamptz,
    CONSTRAINT uq_claim_occupant_claim_rank UNIQUE (claim_id, rank)
);

CREATE INDEX idx_claim_occupant_claim_id ON public.claim_occupant USING btree (claim_id);
