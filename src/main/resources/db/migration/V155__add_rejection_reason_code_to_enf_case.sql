-- The reason the caseworker picked from the fixed list when rejecting an enforcement application (HDPI-8656).
-- rejection_reason keeps its meaning: the free text, which only the four reasons below ask for in addition to
-- the option. The allowed values match uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementRejectionReason.
--
-- Applications rejected before the list existed have a reason in free text and no code, so the code is
-- nullable and the paired constraint ignores a null code.
ALTER TABLE public.enf_case
    ADD COLUMN rejection_reason_code TEXT CHECK (rejection_reason_code IN (
        'CHANGE_IN_PARTY_CIRCUMSTANCES',
        'APPLYING_OUT_OF_TIME',
        'ORDER_NOT_ENFORCEABLE',
        'ORDER_SET_ASIDE_OR_SUSPENDED',
        'APPLYING_BEFORE_POSSESSION_DATE',
        'NOT_TRANSFERRED_TO_HIGH_COURT',
        'ANOTHER_ENFORCEMENT_JOURNEY_ONGOING',
        'DEFENDANTS_IN_BREATHING_SPACE',
        'OTHER')),
    ADD CONSTRAINT enf_case_rejection_reason_code_has_reason
        CHECK (rejection_reason_code NOT IN (
                   'CHANGE_IN_PARTY_CIRCUMSTANCES',
                   'ORDER_NOT_ENFORCEABLE',
                   'ORDER_SET_ASIDE_OR_SUSPENDED',
                   'OTHER')
               OR rejection_reason IS NOT NULL);
