package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;

import java.util.ArrayList;
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
            .complex(PCSCase::getAsbQuestionsWales)
            .mandatory(ASBQuestionsDetailsWales::getAntisocialBehaviour)
            .mandatory(ASBQuestionsDetailsWales::getAntisocialBehaviourDetails,
                    "walesAntisocialBehaviour=\"YES\"")
            .label("asbQuestionsWales-separator-2", "---")
            .mandatory(ASBQuestionsDetailsWales::getIllegalPurposesUse)
            .mandatory(ASBQuestionsDetailsWales::getIllegalPurposesUseDetails,
                    "walesIllegalPurposesUse=\"YES\"")
            .label("asbQuestionsWales-separator-3", "---")
            .mandatory(ASBQuestionsDetailsWales::getOtherProhibitedConduct)
            .mandatory(ASBQuestionsDetailsWales::getOtherProhibitedConductDetails,
                    "walesOtherProhibitedConduct=\"YES\"")
            .done()
            .label("asbQuestionsWales-end-separator", "---")
            .label(
                    "asbQuestionsWales-info",
                    """
                              <p class="govuk-body" tabindex="0">
                              You’ll have the option to upload documents that give more details
                              about the antisocial behaviour or illegal or prohibited conduct you’re
                              describing or evidence of this behaviour later on.
                              </p>
                            """)
            .label("asbQuestionsWales-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> validationErrors = new ArrayList<>();

        ASBQuestionsDetailsWales asbQuestions = caseData.getAsbQuestionsWales();

        if (asbQuestions != null) {
            if (asbQuestions.getAntisocialBehaviour() == VerticalYesNo.YES) {
                textAreaValidationService.validateTextArea(
                        asbQuestions.getAntisocialBehaviourDetails(),
                        "Give details of the actual or threatened antisocial behaviour",
                        TextAreaValidationService.MEDIUM_TEXT_LIMIT,
                        validationErrors);
            }

            if (asbQuestions.getIllegalPurposesUse() == VerticalYesNo.YES) {
                textAreaValidationService.validateTextArea(
                        asbQuestions.getIllegalPurposesUseDetails(),
                        "Give details of the actual or threatened use of the premises for illegal purposes",
                        TextAreaValidationService.MEDIUM_TEXT_LIMIT,
                        validationErrors);
            }

            if (asbQuestions.getOtherProhibitedConduct() == VerticalYesNo.YES) {
                textAreaValidationService.validateTextArea(
                        asbQuestions.getOtherProhibitedConductDetails(),
                        "Give details of other prohibited conduct",
                        TextAreaValidationService.MEDIUM_TEXT_LIMIT,
                        validationErrors);
            }
        }

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
