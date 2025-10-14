package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
                If you want to save your application and return to it later:
                <ol class="govuk-list govuk-list--number">
                    <li>Choose 'Continue'</li>
                    <li>On the next page choose 'Cancel'</li>
                </ol>
                This will save your progress and take you to your case list.
            </div>
        </details>
        """;

}
