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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.DefendantsDOB;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsWarrantOrWrit;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

@AllArgsConstructor
@Component
public class DefendantsDOBPage implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("knownDefendantsDOBInformation", this::midEvent)
            .pageLabel("Enter the defendants’ dates of birth")
            .showCondition(ShowConditionsWarrantOrWrit.WARRANT_FLOW
                + " AND warrantDefendantsDOBKnown=\"YES\"")
            .label("knownDefendantsDOBInformation-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantDetails)
            .complex(WarrantDetails::getDefendantsDOB)
            .mandatory(DefendantsDOB::getDefendantsDOBDetails)
            .done()
            .done()
            .done()
            .label("knownDefendantsDOBInformation-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = getValidationErrors(caseData);

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private List<String> getValidationErrors(PCSCase caseData) {
        String txt = caseData.getEnforcementOrder().getWarrantDetails().getDefendantsDOB().getDefendantsDOBDetails();

        return textAreaValidationService.validateSingleTextArea(
            txt,
            "What are the defendants’ dates of birth?",
            TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT
        );
    }
}
