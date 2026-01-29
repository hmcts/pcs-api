package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;
import uk.gov.hmcts.reform.pcs.ccd.service.FeeValidationService;

import java.math.BigDecimal;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class MoneyOwedPage implements CcdPageConfiguration {

    private final FeeValidationService feeValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("moneyOwed", this::midEvent)
            .pageLabel("The amount the defendants owe you")
            .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW)
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

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = getValidationErrors(caseData);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errors(validationErrors)
            .build();
    }

    private List<String> getValidationErrors(PCSCase caseData) {
        BigDecimal feeAmount = caseData.getEnforcementOrder()
            .getWarrantDetails().getMoneyOwedByDefendants().getAmountOwed();

        return feeValidationService.validateFee(
            feeAmount,
            "Money owed"
        );
    }
}
