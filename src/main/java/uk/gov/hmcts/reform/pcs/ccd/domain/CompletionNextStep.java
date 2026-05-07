package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CompletionNextStep implements HasLabel {
    SUBMIT_AND_PAY_NOW("Submit and pay for my claim now"),
    SAVE_IT_FOR_LATER("Save it for later");

    private final String label;
}
