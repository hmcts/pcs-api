package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementStateEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Component
@RequiredArgsConstructor
public class EnforcementOrderView {

    private static final Set<EnforcementState> RECORD_WARRANT_RETURN_STATES = EnumSet.of(
        EnforcementState.CASE_PROGRESSION,
        EnforcementState.BREATHING_SPACE,
        EnforcementState.AWAITING_ENFORCEMENT_OUTCOME,
        EnforcementState.AWAITING_PROPERTY_VISIT,
        EnforcementState.AWAITING_SCHEDULING,
        EnforcementState.AWAITING_BAILIFF_ALLOCATION,
        EnforcementState.JUDICIAL_REFERRAL,
        EnforcementState.HEARING_READINESS,
        EnforcementState.PREPARE_FOR_HEARING_CONDUCT_HEARING,
        EnforcementState.CLOSED,
        EnforcementState.CASE_STAYED
    );

    private final DraftCaseDataService draftCaseDataService;

    public void setCaseFields(PCSCase pcsCase, long caseReference, State state, PcsCaseEntity pcsCaseEntity) {
        setHasEnforcementDraftData(caseReference, state, pcsCase);
        setActiveEnforcementState(pcsCaseEntity, pcsCase);
    }

    private void setHasEnforcementDraftData(long caseReference, State state, PCSCase pcsCase) {
        if (State.CASE_ISSUED == state) {
            boolean hasUnsubmittedCaseData = draftCaseDataService
                .hasUnsubmittedCaseData(caseReference, enforceTheOrder);
            if (hasUnsubmittedCaseData) {
                Optional<PCSCase> unsubmittedCaseData = draftCaseDataService
                    .getUnsubmittedCaseData(caseReference, enforceTheOrder);
                unsubmittedCaseData.ifPresent(inflated ->
                                                  pcsCase.setEnforcementOrder(inflated.getEnforcementOrder()));
                pcsCase.setHasUnsubmittedEnforcementData(YesOrNo.YES);
            } else {
                pcsCase.setHasUnsubmittedEnforcementData(YesOrNo.NO);
            }
        }
    }

    private void setActiveEnforcementState(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        EnforcementStateEntity enforcementStateEntity = pcsCaseEntity.getEnforcementState();

        if (enforcementStateEntity == null) {
            pcsCase.setCanRecordWarrantReturn(YesOrNo.NO);
            return;
        }

        EnforcementState activeState = enforcementStateEntity.getState();
        pcsCase.setActiveEnforcementState(activeState);
        pcsCase.setCanRecordWarrantReturn(
            RECORD_WARRANT_RETURN_STATES.contains(activeState) ? YesOrNo.YES : YesOrNo.NO
        );
    }
}
