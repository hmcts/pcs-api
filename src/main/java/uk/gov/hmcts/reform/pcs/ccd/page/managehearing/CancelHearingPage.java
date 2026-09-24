package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Component
@RequiredArgsConstructor
public class CancelHearingPage implements CcdPageConfiguration, CcdPage {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey, this::midEvent)
            .showCondition(fieldEquals("manageHearingOption", ManageHearingOption.CANCEL))
            .pageLabel("Cancel a hearing")
                .label(pageKey + "-separator", "---")
            .complex(PCSCase::getHearing)
            .label(pageKey + "-hearingSummary", "${hearing_HearingSummaryMarkdown}")
            .mandatory(Hearing::getCancellationReason)
            .done();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        Hearing hearing = caseData.getHearing();
        List<String> validationErrors = textAreaValidationService.validateSingleTextArea(
            hearing.getCancellationReason(),
            Hearing.CANCELLATION_REASON_LABEL,
            TextAreaValidationService.MEDIUM_TEXT_LIMIT
        );

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

}
