package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class MoneyOwedPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("moneyOwed")
            .pageLabel("The amount the defendants owe you")
            .showCondition("selectEnforcementType=\"WARRANT\"")
            .label("moneyOwed-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .complex(WarrantDetails::getMoneyOwedByDefendants)
            .label("moneyOwed-amount-label",
                """
                    <p class="govuk-body govuk-!-margin-bottom-0">
                        You can include:
                        <ul class="govuk-list govuk-list--bullet">
                            <li class="govuk-!-font-size-19">
                            rent or mortgage arrears</li>
                            <li class="govuk-!-font-size-19">
                            the fee you paid to make a possession claim</li>
                        </ul>
                    </p>
                    <p class="govuk-body">
                        If you do not know the fee you paid to make your possession claim,
                        <a href="/cases/case-details/${[CASE_REFERENCE]}#Service%20Request" target="_blank">
                            check the service request tab (opens in a new tab)</a>.
                        This shows all of the fees you have paid when you made a claim
                    </p>
                """
                )
            .mandatory(MoneyOwedByDefendants::getAmountOwed)
            .label("moneyOwed-save-and-return", SAVE_AND_RETURN);
    }
}
