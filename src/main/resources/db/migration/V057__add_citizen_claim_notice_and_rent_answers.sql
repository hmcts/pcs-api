-- Every way a notice was served, with the date and details of each. notice_of_possession keeps the first
-- of them in serving_method, notice_date and notice_details.
CREATE TABLE public.notice_service_method (
    id uuid NOT NULL,
    notice_of_possession_id integer NOT NULL,
    method varchar(40) NOT NULL,
    served_date date,
    details text,
    display_order integer NOT NULL,
    CONSTRAINT notice_service_method_pkey PRIMARY KEY (id),
    CONSTRAINT notice_service_method_notice_of_possession_id_fkey
        FOREIGN KEY (notice_of_possession_id) REFERENCES public.notice_of_possession (id) ON DELETE CASCADE
);

CREATE INDEX idx_notice_service_method_notice_of_possession_id
    ON public.notice_service_method USING btree (notice_of_possession_id);

-- The arrears on the day notice was served, the day total_rent_arrears was worked out, and how the arrears
-- are proved: UPLOAD_STATEMENT, LEDGER or TABLE.
ALTER TABLE public.rent_arrears
    ADD COLUMN arrears_at_notice_date numeric(18,2),
    ADD COLUMN total_rent_arrears_date date,
    ADD COLUMN proof_type varchar(20);

-- The day of the month monthly rent is due.
ALTER TABLE public.tenancy_licence
    ADD COLUMN rent_due_day smallint;
