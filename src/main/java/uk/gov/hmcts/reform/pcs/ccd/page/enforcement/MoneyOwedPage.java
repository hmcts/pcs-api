package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.MoneyOwedByDefendants;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class MoneyOwedPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("moneyOwed")
            .pageLabel("The amount the defendants owe you")
            .label("moneyOwed-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getMoneyOwedByDefendants)
            .label("moneyOwed-amount-label-hint",
                """
                    <h3 class="govuk-heading-s"
                    govuk-!-margin-bottom-0>What is the total amount that the defendants owe you</h3>
                    <p class="govuk-hint govuk-!-font-size-16 govuk-!-margin-bottom-0">
                        You can include:
                        <ul class="govuk-hint govuk-!-font-size-16 govuk-!-margin-top-1">
                            <li class="govuk-hint govuk-!-font-size-16 govuk-!-margin-top-1">
                            rent or mortgage arrears</li>
                            <li class="govuk-hint govuk-!-font-size-16 govuk-!-margin-top-1">
                            the fee you paid to make a possession claim</li>
                        </ul>
                    </p>
                    <p class="govuk-hint govuk-!-font-size-16 govuk-!-margin-bottom-0">
                        If you do not know the fee you paid to make your possession claim,
                        <a href="/cases/case-details/${[CASE_REFERENCE]}#Service%20Request" target="_blank">
                            check the service request tab (opens in a new tab)
                        </a>.
                        This shows all of the fees you have paid when you made a claim
                    </p>
                """
                )
            .mandatory(MoneyOwedByDefendants::getAmountOwed)
            .label("moneyOwed-save-and-return", SAVE_AND_RETURN);
    }
}
