CREATE TABLE public.enf_state (
    pcs_case_id UUID NOT NULL,
    state VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT enf_state_pkey PRIMARY KEY (pcs_case_id),
    CONSTRAINT enf_state_pcs_case_id_fkey FOREIGN KEY (pcs_case_id) REFERENCES public.pcs_case (id)
);

ALTER TABLE public.enf_case RENAME TO enf_application;

ALTER TABLE public.enf_application
    RENAME CONSTRAINT enf_case_pkey TO enf_application_pkey;

ALTER TABLE public.enf_application
    RENAME CONSTRAINT enf_case_sot_id_fkey TO enf_application_sot_id_fkey;

ALTER INDEX public.idx_enf_case_sot_id RENAME TO idx_enf_application_sot_id;

ALTER TABLE public.enf_application
    DROP COLUMN claim_id,
    ADD COLUMN pcs_case_id UUID NOT NULL,
    ADD COLUMN created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ADD COLUMN last_modified_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;

ALTER TABLE public.enf_warrant
    DROP COLUMN enf_case_id,
    ADD COLUMN enf_application_id UUID NOT NULL;

ALTER TABLE public.enf_writ
    DROP COLUMN enf_case_id,
    ADD COLUMN enf_application_id UUID NOT NULL;

ALTER TABLE public.enf_warrant_of_restitution
    DROP COLUMN enf_case_id,
    ADD COLUMN enf_application_id UUID NOT NULL;

ALTER TABLE public.enf_writ_of_restitution
    DROP COLUMN enf_case_id,
    ADD COLUMN enf_application_id UUID NOT NULL;

ALTER TABLE public.enf_selected_defendants
    DROP COLUMN enf_case_id,
    ADD COLUMN enf_application_id UUID NOT NULL;

ALTER TABLE public.enf_risk_profile
    DROP COLUMN enf_case_id,
    ADD COLUMN enf_application_id UUID NOT NULL;

ALTER TABLE public.enf_application
    ADD CONSTRAINT enf_application_pcs_case_fk FOREIGN KEY (pcs_case_id) REFERENCES public.pcs_case (id);

ALTER TABLE public.enf_warrant
    ADD CONSTRAINT enf_warrant_enf_application_id_fk FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id);

ALTER TABLE public.enf_writ
    ADD CONSTRAINT enf_writ_enf_application_id_fk FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id);

ALTER TABLE public.enf_warrant_of_restitution
    ADD CONSTRAINT enf_warrant_of_restitution_enf_application_id_fk FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id);

ALTER TABLE public.enf_writ_of_restitution
    ADD CONSTRAINT enf_writ_of_restitution_enf_application_id_fk FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id);

ALTER TABLE public.enf_selected_defendants
    ADD CONSTRAINT enf_selected_defendants_enf_application_id_fk FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id);

ALTER TABLE public.enf_risk_profile
    ADD CONSTRAINT enf_risk_profile_enf_application_id_fk FOREIGN KEY (enf_application_id) REFERENCES public.enf_application (id);

ALTER TABLE public.document
    RENAME COLUMN enf_case_id TO enf_application_id;

ALTER TABLE public.document
    RENAME CONSTRAINT document_enf_case_id_fkey TO document_enf_application_id_fkey;

ALTER INDEX public.idx_document_enf_case_id RENAME TO idx_document_enf_application_id;