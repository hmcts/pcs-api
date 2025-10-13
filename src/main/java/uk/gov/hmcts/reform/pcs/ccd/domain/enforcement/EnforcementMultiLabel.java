package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

public class EnforcementMultiLabel {

    public static final String WRIT_OR_WARRENT_CLARIFICATION = """
        <details class="govuk-details">
            <summary class="govuk-details__summary">
                <span class="govuk-details__summary-text">
                    I do not know if I need a writ or a warrant
                </span>
            </summary>
            <div class="govuk-details__text">
                ...
            </div>
        </details>
        """;

    public static final String SAVE_AND_RETURN = """
        <details class="govuk-details">
            <summary class="govuk-details__summary">
                <span class="govuk-details__summary-text">
                    I want to save this application and return to it later
                </span>
            </summary>
            <div class="govuk-details__text">
                ...
            </div>
        </details>
        """;

    private EnforcementMultiLabel() {}
}
