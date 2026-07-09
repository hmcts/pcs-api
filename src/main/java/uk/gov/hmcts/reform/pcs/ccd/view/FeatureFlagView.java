package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureFlags;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

@Component
@RequiredArgsConstructor
public class FeatureFlagView {

    private final FeatureToggleService featureToggleService;

    public void setCaseFields(PCSCase pcsCase) {
        pcsCase.setFeatureFlags(FeatureFlags.builder()
                                    .release1dot2Enabled(getFlag(FeatureFlag.RELEASE_1_DOT_2))
                                    .caseWorkerEventsEnabled(getFlag(FeatureFlag.CASEWORKER_EVENTS))
                                    .build());
    }

    private VerticalYesNo getFlag(FeatureFlag featureFlag) {
        return VerticalYesNo.from(featureToggleService.isEnabled(featureFlag));
    }

}
