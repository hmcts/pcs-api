package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EnforcementFlagGuard {

    static final String NOT_AVAILABLE = "This action is not available";

    private final FeatureToggleService featureToggleService;

    public boolean isOff() {
        return !featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT);
    }

    public SubmitResponse<State> refused() {
        return SubmitResponse.<State>builder()
            .errors(List.of(NOT_AVAILABLE))
            .build();
    }

    public void requireOnStart() {
        if (isOff()) {
            throw new CaseAccessException(NOT_AVAILABLE);
        }
    }
}
