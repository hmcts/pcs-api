package uk.gov.hmcts.reform.pcs.ccd3.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum PaymentStatus implements HasLabel  {

    PAID("Paid"),
    UNPAID("Unpaid");

    private final String label;

}
