package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@AllArgsConstructor
@Component
public class AddHearingPage implements CcdPageConfiguration, CcdPage {

    private static final String PARTYLIST_SHOW_CONDITION = ShowConditions.and(
            fieldEquals("hearing_IssueNotice", VerticalYesNo.YES),
            fieldEquals("hearing_IsWithoutNotice", VerticalYesNo.YES)
    );

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey, this::midEvent)
            .showCondition(fieldEquals("manageHearingOption", ManageHearingOption.ADD))
            .pageLabel("Add a hearing")
            .readonly(PCSCase::getHearingLocation, NEVER_SHOW)
            .label(pageKey + "-separator", "---")
            .label(
                pageKey + "-hearingLocationHeading",
                "<p class=\"govuk-body govuk-!-font-weight-bold\">Hearing location:</p>"
            )
            .label("-hearingLocationbody", "${hearingLocation}")
            .complex(PCSCase::getHearing)
            .mandatory(Hearing::getType)
            .mandatory(Hearing::getOtherHearingType, fieldEquals("hearing_Type", HearingType.OTHER))
            .mandatory(Hearing::getNoticeWording)
            .mandatory(Hearing::getDate)
            .label(pageKey + "-hearingDurationLabel",
                """
                    <span class="form-label ng-star-inserted">How long will the hearing be?</span>
                    <span class="form-hint ng-star-inserted">Enter duration</span>
                """
            )
            .mandatory(Hearing::getDurationHours)
            .mandatory(Hearing::getDurationMinutes)
            .optional(Hearing::getNotes)
            .mandatory(Hearing::getIssueNotice)
            .mandatory(Hearing::getIsWithoutNotice, fieldEquals("hearing_IssueNotice", VerticalYesNo.YES))
            .done()
            .mandatory(PCSCase::getPartyMultiSelectionList, PARTYLIST_SHOW_CONDITION, null,
                       "Who should receive the hearing notice?", "Select all that apply")
            .complex(PCSCase::getHearing)
            .optional(Hearing::getAdditionalInformation)
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
        List<String> validationErrors = textAreaValidationService.validateMultipleTextAreas(
                TextAreaValidationService.FieldValidation.of(
                    hearing.getNotes(),
                    Hearing.NOTES_LABEL,
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                ),
                TextAreaValidationService.FieldValidation.of(
                    hearing.getAdditionalInformation(),
                    Hearing.ADDITIONAL_INFORMATION_LABEL,
                    TextAreaValidationService.MEDIUM_TEXT_LIMIT
                )
        );
        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
