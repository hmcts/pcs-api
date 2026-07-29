package uk.gov.hmcts.reform.pcs.ccd.page.managehearing;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.hearing.ManageHearingOption;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.ccd.service.HearingService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Component
@AllArgsConstructor
public class ManageHearingPage implements CcdPageConfiguration, CcdPage {

    private final HearingService hearingService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey, this::midEvent)
            .pageLabel("Manage hearing")
            .showCondition("showManageHearingPage=\"YES\"")
            .label("manageHearingSeparator", "---")
            .mandatory(PCSCase::getManageHearingOption)
            .readonly(PCSCase::getShowManageHearingPage, NEVER_SHOW)
            .readonly(PCSCase::getSelectedHearingId, NEVER_SHOW, true);
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
            hearingService.prepopulateEditableHearing(details.getId(), caseData);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
