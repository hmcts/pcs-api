package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CounterClaimStatus implements HasLabel {

    DRAFT("Draft"),
    PENDING_CASE_ISSUED("Pending case issued"),
    CASE_ISSUED("Case issued");

    private String label;
}

