ALTER TABLE public.party
    ADD COLUMN title text,
    ADD COLUMN initials text,
    ADD COLUMN other_names text,
    ADD COLUMN dx_number text,
    ADD COLUMN fax_number text,
    ADD COLUMN is_occupant public.yes_no;
