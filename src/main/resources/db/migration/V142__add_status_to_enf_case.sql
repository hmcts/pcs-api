-- Each enforcement (warrant / writ) follows its own lifecycle, independent of the CCD case state.
-- The allowed values match uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.
ALTER TABLE public.enf_case
    ADD COLUMN status TEXT CHECK (status IN (
        'AWAITING_PAYMENT',
        'AWAITING_REVIEW',
        'REJECTED',
        'AWAITING_BAILIFF_ALLOCATION',
        'AWAITING_SCHEDULING',
        'AWAITING_CLAIMANT_CONFIRMATION',
        'CLAIMANT_CONFIRMED',
        'CLAIMANT_UNABLE_TO_ATTEND',
        'AWAITING_PROPERTY_VISIT',
        'BREATHING_SPACE',
        'TRANSFERRED_TO_HIGH_COURT',
        'CLOSED'
    )),
    ADD COLUMN status_updated_at TIMESTAMP WITH TIME ZONE;

-- Every enf_case row is a submitted application: drafts live in draft.draft_case_data and are only
-- copied here on submit. No enforcement fee has ever been taken through pcs-api (no fee_payment row
-- points at an enforcement), so every existing application is still waiting for its fee.
-- status_updated_at stays null because the time these rows were submitted is not recorded.
UPDATE public.enf_case SET status = 'AWAITING_PAYMENT' WHERE status IS NULL;
