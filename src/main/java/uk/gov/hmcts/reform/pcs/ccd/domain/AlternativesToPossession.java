package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum AlternativesToPossession implements HasLabel {

    SUSPENSION_OF_RIGHT_TO_BUY("Suspension of right to buy"),
    DEMOTION_OF_TENANCY("Demotion of tenancy");

    private final String label;
}
