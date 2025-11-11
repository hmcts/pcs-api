package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@AllArgsConstructor
@Component
public class ASBQuestionsWales implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("asbQuestionsWales", this::midEvent)
                .pageLabel("Antisocial behaviour and illegal or prohibited conduct")
                .label("asbQuestionsWales-separator", "---")
                .showCondition("showASBQuestionsPageWales=\"Yes\"")
                .readonly(PCSCase::getShowASBQuestionsPageWales, NEVER_SHOW)
                .mandatory(PCSCase::getAntisocialBehaviourWales)
                .mandatory(PCSCase::getAntisocialBehaviourDetailsWales, "antisocialBehaviourWales=\"YES\"")
                .label("asbQuestionsWales-separator-2", "---")
                .mandatory(PCSCase::getIllegalPurposesUseWales)
                .mandatory(PCSCase::getIllegalPurposesUseDetailsWales, "illegalPurposesUseWales=\"YES\"")
                .label("asbQuestionsWales-separator-3", "---")
                .mandatory(PCSCase::getOtherProhibitedConductWales)
                .mandatory(PCSCase::getOtherProhibitedConductDetailsWales, "otherProhibitedConductWales=\"YES\"")
                .label("asbQuestionsWales-end-separator", "---")
                .label(
                        "asbQuestionsWales-info",
                        """
                                  <p class="govuk-body" tabindex="0">
                                  You'll have the option to upload documents that give more details
                                  about the antisocial behaviour or illegal or prohibited conduct you're
                                  describing or evidence of this behaviour later on.
                                  </p>
                                """);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        // Validate text area fields for character limit - ultra simple approach
        List<String> validationErrors = textAreaValidationService.validateMultipleTextAreas(
                TextAreaValidationService.FieldValidation.of(
                        caseData.getAntisocialBehaviourDetailsWales(),
                        "Give details of the actual or threatened antisocial behaviour",
                        TextAreaValidationService.MEDIUM_TEXT_LIMIT),
                TextAreaValidationService.FieldValidation.of(
                        caseData.getIllegalPurposesUseDetailsWales(),
                        "Give details of the actual or threatened use of the premises for illegal purposes",
                        TextAreaValidationService.MEDIUM_TEXT_LIMIT),
                TextAreaValidationService.FieldValidation.of(
                        caseData.getOtherProhibitedConductDetailsWales(),
                        "Give details of the actual or threatened use of the premises for illegal purposes",
                        TextAreaValidationService.MEDIUM_TEXT_LIMIT));

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
