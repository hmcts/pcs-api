CREATE TABLE public.citizen_claim_submission (
    id uuid NOT NULL,
    claim_id uuid NOT NULL,
    submitted_claim jsonb NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT citizen_claim_submission_pkey PRIMARY KEY (id),
    CONSTRAINT citizen_claim_submission_claim_id_key UNIQUE (claim_id),
    CONSTRAINT citizen_claim_submission_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim (id)
);

CREATE TABLE public.rent_history_ledger_entry (
    id uuid NOT NULL,
    claim_id uuid NOT NULL,
    rank integer NOT NULL,
    transaction_date date,
    rent_charge boolean NOT NULL,
    tenant_payment boolean NOT NULL,
    description varchar(500),
    amount_charged numeric(18,2),
    amount_paid numeric(18,2),
    CONSTRAINT rent_history_ledger_entry_pkey PRIMARY KEY (id),
    CONSTRAINT rent_history_ledger_entry_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim (id)
);

CREATE INDEX idx_rent_history_ledger_entry_claim_id ON public.rent_history_ledger_entry USING btree (claim_id);

CREATE TABLE public.rent_history_period (
    id uuid NOT NULL,
    claim_id uuid NOT NULL,
    rank integer NOT NULL,
    period_start date,
    date_due date,
    amount_due numeric(18,2),
    date_paid date,
    amount_paid numeric(18,2),
    CONSTRAINT rent_history_period_pkey PRIMARY KEY (id),
    CONSTRAINT rent_history_period_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES public.claim (id)
);

CREATE INDEX idx_rent_history_period_claim_id ON public.rent_history_period USING btree (claim_id);
