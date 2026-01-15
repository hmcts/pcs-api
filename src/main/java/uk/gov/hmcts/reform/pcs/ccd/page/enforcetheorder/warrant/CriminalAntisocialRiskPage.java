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
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

@AllArgsConstructor
@Component
public class CriminalAntisocialRiskPage implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("criminalAntisocialRisk", this::midEvent)
            .pageLabel("Their history of criminal or antisocial behaviour")
            .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW
                + " AND warrantEnforcementRiskCategoriesCONTAINS\"CRIMINAL_OR_ANTISOCIAL\""
                + " AND warrantAnyRiskToBailiff=\"YES\"")
            .label("criminalAntisocialRisk-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .complex(WarrantDetails::getRiskDetails)
            .mandatory(EnforcementRiskDetails::getEnforcementCriminalDetails)
            .done()
            .label("criminalAntisocialRisk-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = getValidationErrors(caseData);

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private List<String> getValidationErrors(PCSCase caseData) {
        String txt = caseData.getEnforcementOrder()
                .getWarrantDetails().getRiskDetails().getEnforcementCriminalDetails();

        return textAreaValidationService.validateSingleTextArea(
            txt,
            RiskCategory.CRIMINAL_OR_ANTISOCIAL.getText(),
            TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
        );
    }
}


