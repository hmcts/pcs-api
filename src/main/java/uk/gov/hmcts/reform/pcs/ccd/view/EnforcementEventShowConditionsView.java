package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.EnforcementEventShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class EnforcementEventShowConditionsView {

    private final EnforcementOrderMediator enforcementOrderMediator;

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

    public void setCaseFields(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        setEnforcementEventShowConditions(pcsCaseEntity, pcsCase);
    }

    private void setEnforcementEventShowConditions(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        Optional<EnforcementOrderEntity>  enforcementOrderEntity =
            enforcementOrderMediator.getEnforcementOrder(pcsCaseEntity);
        Optional<EnforcementState> enforcementStateOptional = enforcementOrderEntity.map(
            EnforcementOrderEntity::getState
        );

        if (enforcementStateOptional.isEmpty()) {
            EnforcementEventShowConditions enforcementEventShowConditions = EnforcementEventShowConditions.builder()
                .recordWarrantReturn(YesOrNo.NO)
                .scheduleEviction(YesOrNo.NO)
                .build();
            pcsCase.setEnfShowConditions(enforcementEventShowConditions);
            return;
        }

        EnforcementState activeState = enforcementStateOptional.get();
        EnforcementEventShowConditions enforcementEventShowConditions = EnforcementEventShowConditions.builder()
            .recordWarrantReturn(RECORD_WARRANT_RETURN_STATES.contains(activeState) ? YesOrNo.YES : YesOrNo.NO)
            .scheduleEviction(canScheduleEviction(enforcementOrderEntity.get()) ? YesOrNo.YES : YesOrNo.NO)
            .build();

        pcsCase.setEnfShowConditions(enforcementEventShowConditions);
    }

    private boolean canScheduleEviction(EnforcementOrderEntity activeEnforcementOrder) {
        return activeEnforcementOrder.getState() == EnforcementState.AWAITING_SCHEDULING
            && activeEnforcementOrder.getWarrantDetails() != null;
    }
}
