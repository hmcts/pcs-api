-- The returns a bailiff records against a warrant (HDPI-7347, HDPI-8424). A return belongs to the
-- enforcement, not to the possession case, and an enforcement keeps every return it has had: interim
-- returns, its final return, and any return recorded after it is reopened. Returns are never edited, and
-- are removed with their enforcement.
-- The allowed values match the enums in uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.
CREATE TABLE public.enf_warrant_return (
    id uuid NOT NULL,
    enf_case_id uuid NOT NULL,
    return_type text NOT NULL CHECK (return_type IN ('FINAL', 'INTERIM')),
    outcome text CHECK (outcome IN ('EVICTION_SUCCESSFUL', 'EVICTION_UNSUCCESSFUL', 'EVICTION_CANCELLED')),
    return_name text NOT NULL CHECK (return_name IN (
        'EVICTION_SUCCESSFUL',
        'WARRANT_WITHDRAWN',
        'WARRANT_SUSPENDED_BY_ORDER',
        'POSSESSION_ORDER_SET_ASIDE',
        'UNABLE_TO_GAIN_ACCESS',
        'CLAIMANT_DID_NOT_CONFIRM_EVICTION',
        'NO_LOCKSMITH_CLAIMANT_FAILED_TO_ARRANGE',
        'NO_ANIMAL_HANDLER_CLAIMANT_FAILED_TO_ARRANGE',
        'NO_CLAIMANT_OR_AGENT_IN_ATTENDANCE',
        'NO_REPLY_TO_REQUEST_FOR_INFORMATION',
        'EVICTION_UNSUCCESSFUL_OTHER',
        'WARRANT_EXPIRED',
        'WARRANT_ISSUED_IN_ERROR',
        'APPLICATION_UNSUCCESSFUL_WARRANT_RETURNED',
        'REQUEST_FOR_ADDITIONAL_INFORMATION',
        'NO_ANIMAL_HANDLER_PARTIES_UNAWARE',
        'NO_POLICE_PRESENCE',
        'CHILDREN_LEFT_UNATTENDED',
        'CROWDS_OR_PROTESTS',
        'OBSTRUCTION_BY_DEFENDANTS',
        'SAFEGUARDING_OR_VULNERABILITY_CONCERNS',
        'DEFENDANT_STATES_BREATHING_SPACE',
        'CLAIMANT_ADVISED_BREATHING_SPACE',
        'OTHER_INTERIM_RETURN',
        'REOPEN_WARRANT'
    )),
    eviction_at timestamp with time zone,
    additional_details varchar(6800),
    recorded_at timestamp with time zone NOT NULL,
    recorded_by text,
    CONSTRAINT enf_warrant_return_pkey PRIMARY KEY (id),
    CONSTRAINT enf_warrant_return_enf_case_id_fkey FOREIGN KEY (enf_case_id)
        REFERENCES public.enf_case(id) ON DELETE CASCADE,
    -- Only a final return has an outcome, and every final return has one.
    CONSTRAINT enf_warrant_return_outcome_final_only CHECK ((return_type = 'FINAL') = (outcome IS NOT NULL)),
    -- Only a successful eviction has an eviction date and time.
    CONSTRAINT enf_warrant_return_eviction_at_successful_only
        CHECK (eviction_at IS NULL OR outcome = 'EVICTION_SUCCESSFUL')
);

CREATE INDEX idx_enf_warrant_return_enf_case_id ON public.enf_warrant_return USING btree (enf_case_id);
