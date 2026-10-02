package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppType;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Component
public class ApplicationDetails implements CcdPageConfiguration {

    private static final String SOMETHING_ELSE_DETAILS_LABEL = "Which categories apply";

    private final Clock ukClock;

    private final TextAreaValidationService textAreaValidationService;

    private final FeatureToggleService featureToggleService;

    public ApplicationDetails(@Qualifier("ukClock") Clock ukClock,
                              TextAreaValidationService textAreaValidationService,
                              FeatureToggleService featureToggleService) {
        this.ukClock = ukClock;
        this.textAreaValidationService = textAreaValidationService;
        this.featureToggleService = featureToggleService;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("applicationDetails", this::midEvent)
            .pageLabel("Application details")
            .label("applicationDetails-lineSeparator", "---")
            .mandatory(PCSCase::getPartyRadioList)
            .complex(PCSCase::getEnterGenAppRequest)
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
        LocalDate dateReceived = caseData.getEnterGenAppRequest().getDateReceived();
        LocalDate currentDate = LocalDate.now(ukClock);

        if (dateReceived != null && !dateReceived.isBefore(currentDate)) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errorMessageOverride("Date the application was received must be in the past")
                .build();
        }

        if (isSuspendApplication(caseData) && !featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errorMessageOverride("Suspend applications are not available")
                .build();
        }

        List<String> validationErrors = validateSomethingElseDetails(caseData);

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private boolean isSuspendApplication(PCSCase caseData) {
        EnterGenAppRequest enterGenAppRequest = caseData.getEnterGenAppRequest();
        return enterGenAppRequest != null
            && enterGenAppRequest.getApplicationTypeOption() == EnterGenAppType.SUSPEND;
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
