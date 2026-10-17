package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.LatestWarrantReturn;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantReturnEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * Shows the current enforcement's most recent warrant return on the case, for the "Enforcement status" tab
 * (HDPI-8424). The tab itself is configured elsewhere.
 */
@Component
@RequiredArgsConstructor
public class WarrantReturnView {

    private final WarrantReturnService warrantReturnService;

    /**
     * Leaves the field empty when the current enforcement has no return, or the claim has no enforcement. It
     * is cleared rather than left alone, because a draft "Enforce the order" may have replaced the enforcement
     * order on the case view with one carrying an old copy.
     */
    public void setLatestWarrantReturn(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity == null || pcsCase == null) {
            return;
        }
        Optional<WarrantReturnEntity> latest = warrantReturnService.findLatestReturn(pcsCaseEntity);
        if (latest.isEmpty()) {
            if (pcsCase.getEnforcementOrder() != null) {
                pcsCase.getEnforcementOrder().setLatestWarrantReturn(null);
            }
            return;
        }
        enforcementOrderOf(pcsCase).setLatestWarrantReturn(toLatestWarrantReturn(latest.get()));
    }

    static LatestWarrantReturn toLatestWarrantReturn(WarrantReturnEntity entity) {
        return LatestWarrantReturn.builder()
            .type(entity.getReturnType())
            .name(entity.getReturnName().getLabel())
            .outcome(entity.getOutcome())
            .evictionDateTime(entity.getEvictionAt() == null
                                  ? null
                                  : entity.getEvictionAt().atZone(UK_ZONE_ID).toLocalDateTime())
            .additionalDetails(entity.getAdditionalDetails())
            .build();
    }

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }
}
