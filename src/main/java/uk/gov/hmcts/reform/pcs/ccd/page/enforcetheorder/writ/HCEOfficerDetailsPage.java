package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

@AllArgsConstructor
@Component
public class HCEOfficerDetailsPage implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("HCEOfficerDetails", this::midEvent)
            .pageLabel("Your High Court enforcement officer")
            .showCondition("selectEnforcementType=\"WRIT\" AND writHasHiredHighCourtEnforcementOfficer=\"YES\"")
            .label("hCEOfficerDetails-line-separator", "---")
            .label(
                "hCEOfficerDetails-information-text", """
                    <p class="govuk-body govuk-!-font-weight-bold">Who have you hired to carry out your eviction?</p>
                    """
            )
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWritDetails)
            .mandatory(WritDetails::getHighCourtEnforcementOfficerDetails)
            .done()
            .done()
            .label("hCEOfficerDetails-saveAndReturn", SAVE_AND_RETURN);
        ;
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = getValidationErrors(caseData);

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private List<String> getValidationErrors(PCSCase caseData) {
        String txt = caseData.getEnforcementOrder().getWritDetails().getHighCourtEnforcementOfficerDetails();

        return textAreaValidationService.validateSingleTextArea(
            txt,
            "Name of your High Court enforcement officer?",
            TextAreaValidationService.BYTE_TEXT_LIMIT
        );
    }
}
