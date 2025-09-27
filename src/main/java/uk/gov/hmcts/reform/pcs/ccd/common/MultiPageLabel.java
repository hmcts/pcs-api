package uk.gov.hmcts.reform.pcs.ccd.common;

public class MultiPageLabel {
    public static final String SAVE_AND_RETURN_HTML = """
        <details class="govuk-details">
            <summary class="govuk-details__summary">
                <span class="govuk-details__summary-text">
                    I want to save this application and return to it later
                </span>
            </summary>
            <div class="govuk-details__text">
                If you want to save your application and return to it later:
                <ol class="govuk-list govuk-list--number govuk-!-margin-left-2">
                    <li>Choose 'Continue'.</li>
                    <li>Continue to the next page.</li>
                    <li>Choose 'Cancel'</li>
                </ol>
                <p class="govuk-!-margin-top-2">
                    This will save your progress and take you to your case list.
                </p>
            </div>
        </details>
        """;
}
