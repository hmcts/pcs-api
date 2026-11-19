CREATE TABLE public.claim_payment_history_line (
    id integer NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    claim_id uuid NOT NULL REFERENCES public.claim(id) ON DELETE CASCADE,
    rank integer NOT NULL,
    payment_due_date date NOT NULL,
    amount_due numeric(18, 2) NOT NULL,
    amount_paid numeric(18, 2) NOT NULL,
    cumulative_arrears numeric(18, 2) NOT NULL,
    last_modified timestamptz,
    CONSTRAINT uq_claim_payment_history_line_claim_rank UNIQUE (claim_id, rank)
);

CREATE INDEX idx_claim_payment_history_line_claim_id
    ON public.claim_payment_history_line USING btree (claim_id);
