-- Keep the legacy enforcement table and foreign-key columns during the rollout so base-branch
-- instances can continue loading cases. Enforcement is feature-flagged off, so the new model starts
-- empty; the legacy schema can be removed in a later migration after all old instances are gone.
CREATE TABLE public.enf_application (
    id UUID PRIMARY KEY,
    pcs_case_id UUID NOT NULL REFERENCES public.pcs_case (id),
    state VARCHAR(30) NOT NULL,
    enforcement_order JSONB NOT NULL,
    bailiff_date TIMESTAMP WITHOUT TIME ZONE,
    sot_id INTEGER REFERENCES public.statement_of_truth (id),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_enf_application_pcs_case_id ON public.enf_application (pcs_case_id);
CREATE INDEX idx_enf_application_sot_id ON public.enf_application (sot_id);

ALTER TABLE public.enf_risk_profile
    ALTER COLUMN enf_case_id DROP NOT NULL,
    ADD COLUMN enf_application_id UUID,
    ADD CONSTRAINT enf_risk_profile_enf_application_id_fk
        FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id) ON DELETE CASCADE;

ALTER TABLE public.enf_warrant
    ALTER COLUMN enf_case_id DROP NOT NULL,
    ADD COLUMN enf_application_id UUID,
    ADD CONSTRAINT enf_warrant_enf_application_id_fk
        FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id) ON DELETE CASCADE;

ALTER TABLE public.enf_writ
    ALTER COLUMN enf_case_id DROP NOT NULL,
    ADD COLUMN enf_application_id UUID,
    ADD CONSTRAINT enf_writ_enf_application_id_fk
        FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id) ON DELETE CASCADE;

ALTER TABLE public.enf_warrant_of_restitution
    ALTER COLUMN enf_case_id DROP NOT NULL,
    ADD COLUMN enf_application_id UUID,
    ADD CONSTRAINT enf_warrant_of_restitution_enf_application_id_fk
        FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id) ON DELETE CASCADE;

ALTER TABLE public.enf_writ_of_restitution
    ALTER COLUMN enf_case_id DROP NOT NULL,
    ADD COLUMN enf_application_id UUID,
    ADD CONSTRAINT enf_writ_of_restitution_enf_application_id_fk
        FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id) ON DELETE CASCADE;

ALTER TABLE public.enf_selected_defendants
    ALTER COLUMN enf_case_id DROP NOT NULL,
    ADD COLUMN enf_application_id UUID,
    ADD CONSTRAINT enf_selected_defendants_enf_application_id_fk
        FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id) ON DELETE CASCADE;

ALTER TABLE public.document
    ADD COLUMN enf_application_id UUID,
    ADD CONSTRAINT document_enf_application_id_fkey
        FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id);

CREATE INDEX idx_enf_risk_profile_enf_application_id ON public.enf_risk_profile (enf_application_id);
CREATE INDEX idx_enf_warrant_enf_application_id ON public.enf_warrant (enf_application_id);
CREATE INDEX idx_enf_writ_enf_application_id ON public.enf_writ (enf_application_id);
CREATE INDEX idx_enf_warrant_of_restitution_enf_application_id
    ON public.enf_warrant_of_restitution (enf_application_id);
CREATE INDEX idx_enf_writ_of_restitution_enf_application_id
    ON public.enf_writ_of_restitution (enf_application_id);
CREATE INDEX idx_enf_selected_defendants_enf_application_id
    ON public.enf_selected_defendants (enf_application_id);
CREATE INDEX idx_document_enf_application_id ON public.document (enf_application_id);
