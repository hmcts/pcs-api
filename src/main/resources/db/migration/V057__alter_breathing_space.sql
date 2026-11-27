
ALTER TABLE breathing_space ADD COLUMN case_id uuid;
UPDATE breathing_space bs SET case_id = c.case_id FROM claim c WHERE c.id = bs.claim_id;
ALTER TABLE breathing_space ALTER COLUMN claim_id DROP NOT NULL;
ALTER TABLE breathing_space ALTER COLUMN case_id SET NOT NULL;
ALTER TABLE breathing_space ADD COLUMN created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
DROP INDEX idx_breathing_space_claim_id;
CREATE INDEX idx_breathing_space_case_id ON public.breathing_space USING btree (case_id);
ALTER TABLE breathing_space ADD CONSTRAINT fk_breathing_space_case_id FOREIGN KEY (case_id) REFERENCES pcs_case(id);
