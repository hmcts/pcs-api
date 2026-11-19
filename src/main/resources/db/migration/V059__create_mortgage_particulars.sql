CREATE TABLE public.mortgage_particulars (
    id integer NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    claim_id uuid NOT NULL UNIQUE REFERENCES public.claim(id) ON DELETE CASCADE,
    mortgage_date date NOT NULL,
    default_notice_date date,
    loan_amount numeric(18, 2) NOT NULL,
    current_repayment_terms text NOT NULL,
    full_repayment_date date NOT NULL,
    full_repayment_amount numeric(18, 2) NOT NULL,
    costs_and_administration numeric(18, 2) NOT NULL,
    outstanding_amount numeric(18, 2),
    start_interest_rate numeric(7, 4) NOT NULL,
    pre_arrears_interest_rate numeric(7, 4) NOT NULL,
    claim_start_interest_rate numeric(7, 4) NOT NULL,
    interested_party text,
    tenancy_date date,
    tenant_notice_served_date date,
    total_arrears numeric(18, 2),
    recovery_steps text,
    money_judgment_wanted public.yes_no,
    original_provider_name text,
    original_provider_bought_date date,
    original_provider_defendant_notified_date date,
    last_modified timestamptz
);

CREATE TABLE public.mortgage_additional_payment (
    id integer NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mortgage_particulars_id integer NOT NULL REFERENCES public.mortgage_particulars(id) ON DELETE CASCADE,
    rank integer NOT NULL CHECK (rank BETWEEN 1 AND 3),
    amount numeric(18, 2) NOT NULL,
    reason text NOT NULL,
    arrears numeric(18, 2),
    CONSTRAINT uq_mortgage_additional_payment_particulars_rank UNIQUE (mortgage_particulars_id, rank)
);

CREATE INDEX idx_mortgage_additional_payment_mortgage_particulars_id
    ON public.mortgage_additional_payment USING btree (mortgage_particulars_id);
