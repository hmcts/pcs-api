package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CounterClaimState implements HasLabel {

    AWAITING_SUBMISSION_TO_HMCTS("Awaiting submission"),
    CLAIM_ISSUED("Claim issued"),
    BREATHING_SPACE("Breathing space"),
    CLAIM_WITHDRAWN("Claim withdrawn"),
    CLAIM_RESOLVED("Claim resolved"),
    CLAIM_REJECTED("Claim rejected");

    private final String label;

}

