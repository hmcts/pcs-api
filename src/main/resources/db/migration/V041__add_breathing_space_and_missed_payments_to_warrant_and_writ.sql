-- Add the breathing space and missed payments flags to the warrant and writ records.
ALTER TABLE public.enf_warrant
    ADD COLUMN defendant_in_breathing_space public.yes_no_not_sure,
    ADD COLUMN defendant_missed_payments public.yes_no;

ALTER TABLE public.enf_writ
    ADD COLUMN defendant_in_breathing_space public.yes_no_not_sure,
    ADD COLUMN defendant_missed_payments public.yes_no;
