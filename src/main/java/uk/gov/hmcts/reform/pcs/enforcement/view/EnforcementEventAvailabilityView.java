package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.event.AddBailiffNote;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementEventAvailability;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffNoteService;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDecisionService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.enforcement.service.RiskLevelService;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class EnforcementEventAvailabilityView {

    private static final Set<State> ENFORCEMENT_STATES = EnumSet.copyOf(List.of(AddBailiffNote.STATES));

    private final CurrentEnforcementService currentEnforcementService;

    public void setEventAvailability(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase, State state) {
        if (pcsCase == null || !ENFORCEMENT_STATES.contains(state)) {
            return;
        }
        EnforcementOrderEntity current = currentEnforcementService.findCurrentEnforcement(pcsCaseEntity)
            .orElse(null);
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        pcsCase.getEnforcementOrder().setEventAvailability(availability(current));
    }

    static EnforcementEventAvailability availability(EnforcementOrderEntity current) {
        return EnforcementEventAvailability.builder()
            .canApplyForEnforcement(YesOrNo.from(EnforcementOrderService.canApplyForEnforcement(current)))
            .canDecideEnforcement(YesOrNo.from(EnforcementDecisionService.canDecideEnforcement(current)))
            .canAllocateBailiff(YesOrNo.from(BailiffAllocationService.canAllocateBailiff(current)))
            .canSetRiskLevel(YesOrNo.from(RiskLevelService.canSetRiskLevel(current)))
            .canAddBailiffNote(YesOrNo.from(BailiffNoteService.canAddBailiffNote(current)))
            .build();
    }
}
