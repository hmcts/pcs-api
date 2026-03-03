package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * A generic three-option enum for Yes/No/Prefer not to say questions that can be reused across different contexts.
 */
@AllArgsConstructor
@Getter
public enum YesNoPreferNotToSay implements HasLabel {

    YES("Yes"),
    NO("No"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    private final String label;
}
