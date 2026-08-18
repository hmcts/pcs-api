package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CounterClaimState implements HasLabel {

    PENDING_COUNTER_CLAIM_ISSUED("Pending counter claim issued"),
    COUNTER_CLAIM_ISSUED("Counter claim issued");

    private String label;
}
