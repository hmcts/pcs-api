package uk.gov.hmcts.reform.pcs.ccd.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum VerticalChoice implements HasLabel {

    YES("Yes"),
    NO("No"),
    NOT_SURE("I'm not sure");

    private final String label;
}
