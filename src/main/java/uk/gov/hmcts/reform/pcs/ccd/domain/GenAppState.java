package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum GenAppState implements HasLabel {

    AWAITING_SUBMISSION_TO_HMCTS("Awaiting submission"),
    ISSUED("Application issued"),
    ACCEPTED("Application accepted"),
    REJECTED("Application rejected");

    private final String label;

}

