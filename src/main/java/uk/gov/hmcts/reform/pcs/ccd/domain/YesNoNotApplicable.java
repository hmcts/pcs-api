package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * A generic three-option enum for Yes/No/Not applicable questions that can be reused across different contexts.
 */
public enum YesNoNotApplicable implements HasLabel {

    YES("Yes"),
    NO("No"),
    NOT_APPLICABLE("Not applicable");

    private final String label;

    YesNoNotApplicable(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
