package uk.gov.hmcts.reform.pcs.ccd.page.addReviewDate;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReviewDate;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

@AllArgsConstructor
@Component
public class AddCaseReviewDatePage implements CcdPageConfiguration, CcdPage {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey, this::midEvent)
            .pageLabel("Review dates")
            .label(pageKey + "-line-separator", "---")
            .list(PCSCase::getReviewDates)
                .mandatory(ReviewDate::getDate)
                .mandatory(ReviewDate::getReason)
                .mandatory(ReviewDate::getDescription);
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        List<ListValue<ReviewDate>> reviewDates = caseData.getReviewDates();
        TextAreaValidationService.FieldValidation[] fieldValidations =
            reviewDates.stream()
                .map(this::buildTextAreaValidations)
                .toArray(TextAreaValidationService.FieldValidation[]::new);
        List<String> validationErrors = textAreaValidationService.validateMultipleTextAreas(fieldValidations);
        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

    private TextAreaValidationService.FieldValidation buildTextAreaValidations(ListValue<ReviewDate> listValue) {
        ReviewDate reviewDate = listValue.getValue();
        return TextAreaValidationService.FieldValidation.of(
            reviewDate.getDescription(),
            ReviewDate.DESCRIPTION_LABEL,
            TextAreaValidationService.MEDIUM_TEXT_LIMIT
        );
    }
}
