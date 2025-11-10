package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementRiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

@AllArgsConstructor
@Component
public class VerbalOrWrittenThreatsRiskPage implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("verbalOrWrittenThreatsRisk", this::midEvent)
                .pageLabel("Their verbal or written threats")
                .showCondition("anyRiskToBailiff=\"YES\" "
                    + " AND enforcementRiskCategoriesCONTAINS\"VERBAL_OR_WRITTEN_THREATS\"")
                .label("verbalOrWrittenThreatsRisk-line-separator", "---")
                .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getRiskDetails)
                .mandatory(EnforcementRiskDetails::getEnforcementVerbalOrWrittenThreatsDetails)
                .done()
                .label("verbalOrWrittenThreatsRisk-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();

        String txt = caseData.getEnforcementOrder().getRiskDetails().getEnforcementVerbalOrWrittenThreatsDetails();

        List<String> validationErrors = getValidationErrors(txt);

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private List<String> getValidationErrors(String txt) {
        return textAreaValidationService.validateSingleTextArea(
            txt,
            RiskCategory.VERBAL_OR_WRITTEN_THREATS.getText(),
            TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
        );
    }
}
