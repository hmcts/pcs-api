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
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingService;
import uk.gov.hmcts.reform.pcs.ccd.service.hearing.HearingSummaryRenderer;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

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
        } else if (caseData.getManageHearingOption() == ManageHearingOption.EDIT) {
            String previouslySelectedHearingId = caseData.getSelectedHearingId();
            hearingService.initialiseEditableHearing(details.getId(), caseData, previouslySelectedHearingId);
        } else if (caseData.getManageHearingOption() == ManageHearingOption.CANCEL) {
            initialiseCancellableHearing(details.getId(), caseData);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private void initialiseCancellableHearing(long caseReference, PCSCase caseData) {
        hearingService.findEditableHearing(caseReference).ifPresent(hearingEntity -> {
            Hearing hearing = caseData.getHearing() == null ? Hearing.builder().build() : caseData.getHearing();
            hearing.setHearingId(hearingEntity.getId());
            hearing.setHearingSummaryMarkdown(renderSummary(hearingEntity, caseData));
            caseData.setHearing(hearing);
        });
    }

    private String renderSummary(HearingEntity hearingEntity, PCSCase caseData) {
        return hearingSummaryRenderer.renderMarkdown(hearingEntity, caseData.getHearingLocation());
    }
}
