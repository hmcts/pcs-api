package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CitizenClaimEqualityChoice implements HasLabel {

    CONTINUE("Continue to the questions"),
    SKIP("I do not want to answer these questions");

    private final String label;
}
