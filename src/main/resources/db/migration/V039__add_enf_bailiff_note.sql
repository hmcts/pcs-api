-- Notes that bailiff staff add about an enforcement (warrant / writ). A note belongs to the enforcement,
-- not to the possession case, so a claim with several enforcements keeps each one's notes apart.
-- Notes are never edited, and are removed with their enforcement.
CREATE TABLE public.enf_bailiff_note (
    id uuid NOT NULL,
    enf_case_id uuid NOT NULL,
    note varchar(6800) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    author_first_name text,
    author_last_name text,
    CONSTRAINT enf_bailiff_note_pkey PRIMARY KEY (id),
    CONSTRAINT enf_bailiff_note_enf_case_id_fkey FOREIGN KEY (enf_case_id)
        REFERENCES public.enf_case(id) ON DELETE CASCADE
);

CREATE INDEX idx_enf_bailiff_note_enf_case_id ON public.enf_bailiff_note USING btree (enf_case_id);
