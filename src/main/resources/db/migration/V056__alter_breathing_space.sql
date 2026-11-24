
ALTER TABLE breathing_space ADD COLUMN pcs_case_id uuid;
UPDATE breathing_space bs SET pcs_case_id = c.case_id FROM claim c WHERE c.id = bs.claim_id;
ALTER TABLE breathing_space ALTER COLUMN pcs_case_id SET NOT NULL;
ALTER TABLE breathing_space ADD COLUMN created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
DROP INDEX idx_breathing_space_claim_id;
CREATE INDEX idx_breathing_space_pcs_case_id ON public.breathing_space USING btree (pcs_case_id);
ALTER TABLE breathing_space ADD CONSTRAINT fk_breathing_space_pcs_case_id FOREIGN KEY (pcs_case_id) REFERENCES pcs_case(id);
ALTER TABLE breathing_space DROP COLUMN claim_id;
