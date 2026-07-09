package uk.gov.hmcts.reform.pcs.ccd.page.entergenapp;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.EnterGenAppType;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@AllArgsConstructor
@Component
public class ApplicationDetails implements CcdPageConfiguration {

    private static final String SOMETHING_ELSE_DETAILS_LABEL = "Which categories apply";

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("applicationDetails", this::midEvent)
            .pageLabel("Application details")
            .label("applicationDetails-lineSeparator", "---")
            .complex(PCSCase::getEnterGenAppRequest)
            .mandatory(EnterGenAppRequest::getApplicantParty)
            .mandatory(EnterGenAppRequest::getDateReceived)
            .mandatory(EnterGenAppRequest::getApplicationTypeOption)
            .mandatory(
                EnterGenAppRequest::getSomethingElseDetails,
                fieldEquals("enter_genapp_ApplicationTypeOption", EnterGenAppType.SOMETHING_ELSE)
            )
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = validateSomethingElseDetails(caseData);

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private List<String> validateSomethingElseDetails(PCSCase caseData) {
        EnterGenAppRequest enterGenAppRequest = caseData.getEnterGenAppRequest();
        if (enterGenAppRequest == null
            || enterGenAppRequest.getApplicationTypeOption() != EnterGenAppType.SOMETHING_ELSE) {
            return List.of();
        }

        return textAreaValidationService.validateSingleTextArea(
            enterGenAppRequest.getSomethingElseDetails(),
            SOMETHING_ELSE_DETAILS_LABEL,
            TextAreaValidationService.MEDIUM_TEXT_LIMIT
        );
    }

}
