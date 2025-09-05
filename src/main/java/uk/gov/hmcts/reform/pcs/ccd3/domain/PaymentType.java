package uk.gov.hmcts.reform.pcs.ccd3.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum PaymentType implements HasLabel {

    PBA("Pay fee using Payment by Account (PBA)"),
    CARD("Pay by credit or debit card");

    private final String label;

}
