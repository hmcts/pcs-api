package uk.gov.hmcts.reform.pcs.ccd.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum YesNoDateOfEviction implements HasLabel {

    NO("No, I am available for any date in the next 3 months"),
    YES("Yes, there are some dates that I cannot attend");

    private final String label;
}
