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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;
import uk.gov.hmcts.reform.pcs.ccd.service.FeeValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class LegalCostsPage implements CcdPageConfiguration {

    private FeeValidationService feeValidationService;

    @SuppressWarnings("checkstyle:LineLength")
    static final String LEGAL_COSTS_HELP = """
        <details class="govuk-details">
            <summary class="govuk-details__summary">
                <span class="govuk-details__summary-text">
                    I do not know if I need to reclaim any legal costs
                </span>
            </summary>
            <div class="govuk-details__text">
                <p>
                    Legal costs are the costs you incur when a lawyer, legal representative, or
                    someone working in a legal department applies for a writ or warrant on your behalf.
                </p>
                <p>
                    They will invoice these costs to you, and you can reclaim them from the defendant.
                </p>
                <p>
                    <div class="govuk-!-font-weight-bold">
                        If you are not sure how much you can reclaim
                    </div>
                </p>
                <p>
                    The amount you can reclaim from the defendant is usually fixed.
                </p>
                <p>
                    You can either:
                </p>
                <p>
                    <ul>
                        <li class="govuk-list govuk-!-font-size-19">ask your lawyer or legal representative how much you can reclaim, or</li>
                        <li class="govuk-list govuk-!-font-size-19">
                            <a href="https://www.justice.gov.uk/courts/procedure-rules/civil/rules/part45-fixed-costs/practice-direction-45-fixed-costs"
                                target="_blank">
                                check the Civil Procedure Rules (Justice.gov website, opens in a new tab)
                            </a>
                        </li>
                    </ul>
                </p>
            </div>
        </details>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("legalCosts", this::midEvent)
                .pageLabel("Legal costs")
                .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW)
                .label("legalCosts-line-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getWarrantDetails)
                .complex(WarrantDetails::getLegalCosts)
                .mandatory(LegalCosts::getAreLegalCostsToBeClaimed)
                .mandatory(LegalCosts::getAmountOfLegalCosts,
                        "warrantAreLegalCostsToBeClaimed=\"YES\"")
                .done()
                .label("legalCosts-help", LEGAL_COSTS_HELP)
                .label("legalCosts-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
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
        List<String> errors = new ArrayList<>();
        LegalCosts legalCosts = caseData.getEnforcementOrder()
            .getWarrantDetails().getLegalCosts();

        if (legalCosts.getAreLegalCostsToBeClaimed().toBoolean()) {
            errors.addAll(feeValidationService.validateFee(
                legalCosts.getAmountOfLegalCosts(),
                "Legal cost"
            ));
        }
        return errors;
    }
}
