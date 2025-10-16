package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnforcementMultiLabel {

    public static final String WRIT_OR_WARRANT_CLARIFICATION = """
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

}
