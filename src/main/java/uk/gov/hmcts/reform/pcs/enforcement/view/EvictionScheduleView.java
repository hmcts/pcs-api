package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionScheduleService;

/**
 * Sets the fields that gate "Schedule eviction", "Reschedule eviction" and "Mark notice of eviction as served" on
 * the claim's current enforcement, and whether defendants may see it. Leaves them unset when the claim has no
 * enforcement, which hides the events.
 */
@Component
@RequiredArgsConstructor
public class EvictionScheduleView {

    private final EvictionScheduleService evictionScheduleService;

    public void setEvictionScheduleFields(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity == null || pcsCase == null) {
            return;
        }
        evictionScheduleService.findCurrentEnforcement(pcsCaseEntity)
            .ifPresent(enforcement -> setFields(enforcement, pcsCase));
    }

    private static void setFields(EnforcementOrderEntity enforcement, PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
        enforcementOrder.setEvictionSchedule(EvictionSchedule.builder()
            .canScheduleEviction(YesOrNo.from(EvictionScheduleService.canSchedule(enforcement)))
            .canRescheduleEviction(YesOrNo.from(EvictionScheduleService.canReschedule(enforcement)))
            .canMarkNoticeOfEvictionServed(YesOrNo.from(EvictionScheduleService.canMarkNoticeServed(enforcement)))
            .build());
        enforcementOrder.setDefendantsCanSeeEnforcement(YesOrNo.from(enforcement.isDefendantsCanSeeEnforcement()));
    }
}
