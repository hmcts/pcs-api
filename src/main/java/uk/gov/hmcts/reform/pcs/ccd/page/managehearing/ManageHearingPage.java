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
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.Hearing;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingSummaryRenderer;
import uk.gov.hmcts.reform.pcs.exception.HearingNotFoundException;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.MAKE_ORDER;

@Component
@AllArgsConstructor
public class ManageHearingPage implements CcdPageConfiguration, CcdPage {

    private final HearingService hearingService;
    private final HearingSummaryRenderer hearingSummaryRenderer;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey, this::midEvent)
            .pageLabel("Manage hearing")
            .readonly(PCSCase::getShowManageHearingPage, NEVER_SHOW)
            .showCondition("showManageHearingPage=\"YES\"")
            .label("manageHearingSeparator", "---")
            .mandatory(PCSCase::getManageHearingOption)
            .readonly(PCSCase::getFeatureFlags, NEVER_SHOW, true)
            .mandatory(PCSCase::getHearingSelection, ShowConditions.and(ShowConditions.featureFlagsEnabled(MAKE_ORDER),
                "(manageHearingOption=\"EDIT\" OR manageHearingOption=\"CANCEL\")"), true)
            .readonly(PCSCase::getHearingSummaryLocation, NEVER_SHOW, true)
            .readonly(PCSCase::getSelectedHearingId, NEVER_SHOW, true)
            .readonly(PCSCase::getMhDraftPartyList, NEVER_SHOW, true)
            .complex(PCSCase::getHearing)
                .readonly(Hearing::getHearingId, ShowConditions.NEVER_SHOW, true)
                .readonly(Hearing::getHearingSummaryMarkdown, ShowConditions.NEVER_SHOW, true)
            .done()
            .complex(PCSCase::getManageHearingDraft)
                .readonly(Hearing::getHearingId, NEVER_SHOW, true)
                .readonly(Hearing::getHearingSummaryMarkdown, NEVER_SHOW, true)
                .readonly(Hearing::getType, NEVER_SHOW, true)
                .readonly(Hearing::getOtherHearingType, NEVER_SHOW, true)
                .readonly(Hearing::getNoticeWording, NEVER_SHOW, true)
                .readonly(Hearing::getDate, NEVER_SHOW, true)
                .readonly(Hearing::getDurationDays, NEVER_SHOW, true)
                .readonly(Hearing::getDurationHours, NEVER_SHOW, true)
                .readonly(Hearing::getDurationMinutes, NEVER_SHOW, true)
                .readonly(Hearing::getNotes, NEVER_SHOW, true)
                .readonly(Hearing::getIssueNotice, NEVER_SHOW, true)
                .readonly(Hearing::getIsWithoutNotice, NEVER_SHOW, true)
                .readonly(Hearing::getAdditionalInformation, NEVER_SHOW, true)
                .readonly(Hearing::getCancellationReason, NEVER_SHOW, true)
            .done();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        if (caseData.getManageHearingOption() == ManageHearingOption.ADD) {
            hearingService.clearHearingForm(caseData);
            caseData.setSelectedHearingId(null);
        } else {
            try {
                var selected = hearingService.resolveHearingSelection(details.getId(), caseData);
                String previousId = caseData.getSelectedHearingId();
                caseData.setSelectedHearingId(selected.getId().toString());
                hearingService.initialiseEditableHearing(details.getId(), caseData, previousId);
                caseData.getHearing().setHearingSummaryMarkdown(
                    hearingSummaryRenderer.renderMarkdown(selected, caseData.getHearingSummaryLocation()));
            } catch (HearingNotFoundException e) {
                return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .data(caseData).errors(List.of(e.getMessage())).build();
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
