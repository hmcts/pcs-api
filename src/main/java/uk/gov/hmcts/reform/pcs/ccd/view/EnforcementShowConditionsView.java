package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.EnforcementShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class EnforcementShowConditionsView {

    private final EnforcementOrderMediator enforcementOrderMediator;
    private final FeatureToggleService featureToggleService;

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
        setEnforcementShowConditions(pcsCaseEntity, pcsCase);
    }

    public Optional<EnforcementOrderEntity> getRiskAssessmentApplication(PcsCaseEntity pcsCaseEntity) {
        if (!featureToggleService.isEnabled(FeatureFlag.ASSESS_RISK_LEVEL)) {
            return Optional.empty();
        }
        return enforcementOrderMediator.getRiskAssessmentApplication(pcsCaseEntity);
    }

    private void setEnforcementShowConditions(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        Optional<EnforcementState> activeState = enforcementOrderMediator.getEnforcementOrder(pcsCaseEntity)
            .map(EnforcementOrderEntity::getState);
        boolean canRecordWarrantReturn = activeState.filter(RECORD_WARRANT_RETURN_STATES::contains).isPresent();

        pcsCase.setEnfShowConditions(EnforcementShowConditions.builder()
            .recordWarrantReturn(YesOrNo.from(canRecordWarrantReturn))
            .canViewRiskAssessment(YesOrNo.from(getRiskAssessmentApplication(pcsCaseEntity).isPresent()))
            .build());
    }
}
