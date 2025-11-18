package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.warrant.EnforcementOrder;

public class EnforcementApplicationPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("enforcementApplication")
                .pageLabel("Your application")
                .label("enforcementApplication-content", "---")
                .complex(PCSCase::getEnforcementOrder)
                .mandatory(EnforcementOrder::getSelectEnforcementType)
                .label("enforcementApplication-clarification",
                    """
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
                    """);
    }

}
