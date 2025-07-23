ALTER TABLE public.pcs_case
RENAME COLUMN applicant_forename TO claimant_name;

ALTER TABLE pcs_case
DROP COLUMN applicant_surname;
