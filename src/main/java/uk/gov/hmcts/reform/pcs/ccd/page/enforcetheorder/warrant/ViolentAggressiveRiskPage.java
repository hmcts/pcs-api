package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

@AllArgsConstructor
@Component
public class ViolentAggressiveRiskPage implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;


    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("violentAggressiveRisk", this::midEvent)
            .pageLabel("Their violent or aggressive behaviour")
            .showCondition("warrantAnyRiskToBailiff=\"YES\""
                + " AND warrantEnforcementRiskCategoriesCONTAINS\"VIOLENT_OR_AGGRESSIVE\""
                + " AND selectEnforcementType=\"WARRANT\"")
            .label("violentAggressiveRisk-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .complex(WarrantDetails::getRiskDetails)
            .mandatory(EnforcementRiskDetails::getEnforcementViolentDetails)
            .done()
            .label("violentAggressiveRisk-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = getValidationErrors(caseData);

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private List<String> getValidationErrors(PCSCase caseData) {
        String txt = caseData.getEnforcementOrder()
                .getWarrantDetails().getRiskDetails().getEnforcementViolentDetails();

        return textAreaValidationService.validateSingleTextArea(
            txt,
            RiskCategory.VIOLENT_OR_AGGRESSIVE.getText(),
            TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
        );
    }

}


