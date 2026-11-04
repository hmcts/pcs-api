package uk.gov.hmcts.reform.pcs.ccd.page;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonPageContent {

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
                    <li class="govuk-!-static-margin-0">Choose ‘Continue’.</li>
                    <li>On the next page choose ‘Cancel’.</li>
                </ol>
                <p>This will save your progress and take you to the case overview.</p>
            </div>
        </details>
        """;
}
