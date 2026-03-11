ALTER TABLE pcs_case
    ADD COLUMN case_flags jsonb;

ALTER TABLE party
    ADD COLUMN flags jsonb;
