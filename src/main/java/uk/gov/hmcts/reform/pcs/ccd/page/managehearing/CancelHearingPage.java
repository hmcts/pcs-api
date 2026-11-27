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
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.exception.CaseDataValidationException;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsDisabled;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.MAKE_ORDER;

@Component
@RequiredArgsConstructor
public class CancelHearingPage implements CcdPageConfiguration, CcdPage {

    private final TextAreaValidationService textAreaValidationService;
    private final HearingService hearingService;

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
            .mandatory(Hearing::getCancellationReason, featureFlagsDisabled(MAKE_ORDER))
            .mandatory(Hearing::getCancellationReasonSelection, featureFlagsEnabled(MAKE_ORDER))
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
        if (caseData.makeOrderEnabled()) {
            var response = AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(caseData);
            try {
                var reason = hearingService.requireCancellationReason(hearing);
                hearing.getCancellationReasonSelection().setValue(
                    new DynamicStringListElement(reason.getCode(), reason.getLabel()));
                return response.build();
            } catch (CaseDataValidationException e) {
                return response.errors(List.of(e.getMessage())).build();
            }
        }

        List<String> validationErrors = textAreaValidationService.validateSingleTextArea(
            hearing.getCancellationReason(),
            Hearing.CANCELLATION_REASON_LABEL,
            TextAreaValidationService.MEDIUM_TEXT_LIMIT
        );

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }

}
