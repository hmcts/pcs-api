package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.LegalCosts;

public class LegalCostsPage implements CcdPageConfiguration {

    public static final String LEGAL_COSTS_HELP = """
        <details class="govuk-details">
            <summary class="govuk-details__summary">
                <span class="govuk-details__summary-text">
                    I do not know if I need to reclaim any legal costs
                </span>
            </summary>
            <div class="govuk-details__text">
                Legal costs are the costs you incur when a lawyer, legal representative, or
                someone working in a legal department applies for a writ or warrant on
                your behalf.
        
                They will invoice these costs to you, and you can reclaim them from the
                defendant.
            </div>
            <div class="govuk-details__text govuk-!-font-weight-bold">
                If you are not sure how much you can reclaim
            </div>
            <div  class="govuk-details__text">
                The amount you can reclaim from the defendant is usually fixed.
        
                You can either:
                <ul>
                    <li>ask your lawyer or legal representative how much you can reclaim, or</li>
                    <li>refer to the Ministry of Justice's published legal costs guidance for
                    enforcement proceedings:
                    <a href="https://www.gov.uk/guidance/civil-legal-aid-costs-guidelines-for-court-proceedings"
                    target="_blank">
                    check the Civil Procedure Rules (Justice.gov website, opens in a new tab)
                    </a></li>
                </ul>
            </div>
        </details>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("legalCosts")
                .pageLabel("Legal costs")
                .label("legalCosts-line-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getLegalCosts)
                .mandatory(LegalCosts::getAreLegalCostsToBeClaimed)
                .mandatory(LegalCosts::getAmountOfLegalCosts,
                        "areLegalCostsToBeClaimed=\"YES\"")
                .done()
                .label("legalCosts-help", LEGAL_COSTS_HELP);
    }
}
