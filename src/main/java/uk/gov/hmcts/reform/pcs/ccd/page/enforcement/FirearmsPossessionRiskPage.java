package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.service.TextValidationService;

import java.util.List;

@AllArgsConstructor
@Component
public class FirearmsPossessionRiskPage implements CcdPageConfiguration {

    private final TextValidationService textValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("firearmsPossessionRisk", this::midEvent)
            .pageLabel("Their history of firearm possession")
            .showCondition("anyRiskToBailiff=\"YES\" AND enforcementRiskCategoriesCONTAINS\"FIREARMS_POSSESSION\"")
            .label("firearmsPossessionRisk-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getRiskDetails)
            .mandatory(EnforcementRiskDetails::getEnforcementFirearmsDetails)
            .done()
            .label("firearmsPossessionRisk-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = getValidationErrors(caseData);

        return textValidationService.createValidationResponse(caseData, validationErrors);
    }

    private List<String> getValidationErrors(PCSCase caseData) {
        String txt = caseData.getEnforcementOrder().getRiskDetails().getEnforcementFirearmsDetails();

        return textValidationService.validateSingleTextArea(
            txt,
            RiskCategory.FIREARMS_POSSESSION.getText(),
            TextValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
        );
    }

}


