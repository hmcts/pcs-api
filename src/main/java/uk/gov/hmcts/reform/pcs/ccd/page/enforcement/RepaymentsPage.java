package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RepaymentCosts;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

public class RepaymentsPage implements CcdPageConfiguration {
    public static final String REPAYMENT_TABLE = """
      <table class="govuk-table">
          <caption class="govuk-table__caption govuk-table__caption--m">Total amount that can be repaid</caption>
          <thead class="govuk-table__head">
            <tr class="govuk-table__row">
             <th scope="col" class="govuk-table__header">Repayment for</th>
             <th scope="col" class="govuk-table__header">Amount</th>
            </tr>
          </thead>
          <tbody class="govuk-table__body">
            <tr class="govuk-table__row">
             <th scope="row" class="govuk-table__header govuk-body govuk-!-font-weight-regular">Arrears and other costs
             </th>
              <td class="govuk-table__cell">${formattedAmountOfTotalArrears}</td>
            </tr>
            <tr class="govuk-table__row">
             <th scope="row" class="govuk-table__header govuk-!-font-weight-regular">Legal costs</th>
             <td class="govuk-table__cell">${formattedAmountOfLegalFees}</td>
            </tr>
            <tr class="govuk-table__row">
             <th scope="row" class="govuk-table__header govuk-body govuk-!-font-weight-regular">Land Registry fees</th>
             <td class="govuk-table__cell">${formattedAmountOfLandRegistryFees}</td>
            </tr>
            <tr class="govuk-table__row">
             <th scope="row" class="govuk-table__header govuk-body govuk-!-font-weight-regular">Warrant of possesion fee
             </th>
             <td class="govuk-table__cell">${warrantFeeAmount}</td>
            </tr>
            <tr class="govuk-table__row">
             <th scope="row" class="govuk-table__header govuk-body govuk-!-font-weight-regular">Total</th>
             <td class="govuk-table__cell">${formattedAmountOfTotalFees}</td>
            </tr>
          </tbody>
      </table>""";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("repaymentsPage")
            .pageLabel("Repayments Page")
            .label("repaymentsPage-content", "---")
            .complex(PCSCase::getEnforcementOrder)
            .readonly(EnforcementOrder::getWarrantFeeAmount, NEVER_SHOW, true)
            .complex(EnforcementOrder::getRepaymentCosts)
            .readonly(RepaymentCosts::getFormattedAmountOfLegalFees, NEVER_SHOW, true)
            .readonly(RepaymentCosts::getFormattedAmountOfLandRegistryFees, NEVER_SHOW, true)
            .readonly(RepaymentCosts::getFormattedAmountOfTotalArrears, NEVER_SHOW, true)
            .readonly(RepaymentCosts::getFormattedAmountOfTotalFees, NEVER_SHOW, true)
            .label("repayments-table-content", REPAYMENT_TABLE)
            .mandatory(RepaymentCosts::getRepaymentChoice)
            .mandatory(RepaymentCosts::getAmountOfRepaymentCosts, "repaymentChoice=\"SOME\"")
            .done()
            .label("repaymentsPage-save-and-return", SAVE_AND_RETURN);
    }
}
