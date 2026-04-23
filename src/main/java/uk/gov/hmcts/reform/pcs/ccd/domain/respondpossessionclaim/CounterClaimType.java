package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CounterClaimType implements HasLabel {

    PAYMENT_OR_COMPENSATION("a sum of money or compensation "),
    SOMETHING_ELSE("Something else"),
    BOTH("Both");


    private String label;
}
