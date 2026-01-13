-- Drop FK constraints
ALTER TABLE claim_party
DROP CONSTRAINT IF EXISTS claim_party_claim_id_fkey;

ALTER TABLE claim_ground
DROP CONSTRAINT IF EXISTS claim_ground_claim_id_fkey;

ALTER TABLE enforcement.enf_case
DROP CONSTRAINT IF EXISTS enf_case_claim_id_fkey;

-- Drop old claim table
DROP TABLE IF EXISTS claim;

-- Create YES_NO type
CREATE TYPE YES_NO AS ENUM ('YES', 'NO');

-- Recreate claim table
CREATE TABLE claim (
                     id UUID PRIMARY KEY,
                     version INT,
                     case_id uuid references public.pcs_case (id),
                     claimant_type TEXT,
                     against_trespassers YES_NO,
                     due_to_rent_arrears YES_NO,
                     claim_costs YES_NO,
                     pre_action_protocol_followed YES_NO,
                     mediation_attempted YES_NO,
                     mediation_details VARCHAR(250),
                     settlement_attempted YES_NO,
                     settlement_details VARCHAR(250),
                     claimant_circumstances_provided YES_NO,
                     claimant_circumstances VARCHAR(950),
                     additional_defendants YES_NO,
                     defendant_circumstances_provided YES_NO,
                     defendant_circumstances VARCHAR(950),
                     additional_reasons_provided YES_NO,
                     additional_reasons VARCHAR(6400),
                     underlessee_or_mortgagee YES_NO,
                     additional_underlessees_or_mortgagees YES_NO,
                     additional_docs_provided YES_NO,
                     gen_app_expected YES_NO,
                     language_used TEXT
);

-- Re-add FK constraints
ALTER TABLE claim_party
  ADD CONSTRAINT claim_party_claim_id_fkey
    FOREIGN KEY (claim_id) REFERENCES claim(id);

ALTER TABLE claim_ground
  ADD CONSTRAINT claim_ground_claim_id_fkey
    FOREIGN KEY (claim_id) REFERENCES claim(id);

ALTER TABLE enforcement.enf_case
  ADD CONSTRAINT enf_case_claim_id_fkey
    FOREIGN KEY (claim_id) REFERENCES claim(id);

-- Drop columns from pcs_case
ALTER TABLE pcs_case
    DROP COLUMN mediation_attempted,
    DROP COLUMN mediation_attempted_details,
    DROP COLUMN settlement_attempted,
    DROP COLUMN settlement_attempted_details;
