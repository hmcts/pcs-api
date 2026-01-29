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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.LEGAL_COSTS_HELP;
import uk.gov.hmcts.reform.pcs.ccd.service.FeeValidationService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class LegalCostsPage implements CcdPageConfiguration {

    private final FeeValidationService feeValidationService;

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
        return List.of();
    }
}
