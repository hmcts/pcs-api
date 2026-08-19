package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.IntegerValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing.HOUR_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing.MINUTE_LABEL;

@AllArgsConstructor
@Component
public class HearingDetailsPage implements CcdPageConfiguration, CcdPage {

    private final TextAreaValidationService textAreaValidationService;
    private final HearingService hearingService;
    private final IntegerValidationService integerValidationService;
    private static final String ZERO_DURATION_ERROR = "At least one of Days, Hours or Minutes must be larger than zero";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        configureHearingDetailsPage(
            pageBuilder
                .page(pageKey, this::midEvent)
                .showCondition("manageHearingOption=\"ADD\" OR manageHearingOption=\"EDIT\"")
                .pageLabel("Manage hearing")
        );
    }

    static void configureHearingDetailsPage(
        FieldCollectionBuilder<PCSCase, State, EventBuilder<PCSCase, UserRole, State>> page
    ) {
        page
            .readonly(PCSCase::getHearingLocation, NEVER_SHOW)
            .label("separator", "---")
            .label(
                "hearingLocationHeading",
                "<span class=\"form-label ng-star-inserted "
                    + "govuk-!-font-weight-bold govuk-!-margin-bottom-1\">Hearing location:</span>"
                    + "<span class=\"form-label ng-star-inserted govuk-!-font-weight-regular "
                    + "govuk-!-margin-top-0\">${hearingLocation}</span>"
            )
            .complex(PCSCase::getHearing)
            .mandatory(Hearing::getType)
            .mandatory(Hearing::getOtherHearingType, "hearing_Type=\"OTHER\"")
            .mandatory(Hearing::getNoticeWording)
            .mandatory(Hearing::getDate)
            .label("hearingDurationLabel",
                """
                    <span class="form-label ng-star-inserted">How long will the hearing be?</span>
                    <span class="form-hint ng-star-inserted">Enter duration</span>
                """
            )
            .mandatory(Hearing::getDurationDays)
            .mandatory(Hearing::getDurationHours)
            .mandatory(Hearing::getDurationMinutes)
            .optional(Hearing::getNotes)
            .mandatory(Hearing::getIssueNotice)
            .mandatory(Hearing::getIsWithoutNotice, "hearing_IssueNotice=\"YES\"")
            .done()
            .mandatory(PCSCase::getPartyMultiSelectionList,
                       "hearing_IssueNotice=\"YES\" AND hearing_IsWithoutNotice=\"YES\"", null,
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

        Integer durationDays = hearing.getDurationDays();
        Float durationHours = hearing.getDurationHours();
        Float durationMinutes = hearing.getDurationMinutes();

        if (Integer.valueOf(0).equals(durationDays)
            && Float.valueOf(0).equals(durationHours)
            && Float.valueOf(0).equals(durationMinutes)) {
            validationErrors.add(ZERO_DURATION_ERROR);
        } else {
            integerValidationService
                .validateFloatIsInteger(hearing.getDurationHours(), HOUR_LABEL, validationErrors);
            integerValidationService
                .validateFloatIsInteger(hearing.getDurationMinutes(), MINUTE_LABEL, validationErrors);
        }

        hearingService.storeDraftHearingForm(caseData);
        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
