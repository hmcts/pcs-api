package uk.gov.hmcts.reform.pcs.ccd.page.updategenapp;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Slf4j
public class SelectAction implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("selectAction", this::midEvent)
            .pageLabel("What would you like to do?")
            .readonly(PCSCase::getGenAppDescriptionMarkdown, ShowConditions.NEVER_SHOW)
            .readonly(PCSCase::getSelectedAction, ShowConditions.NEVER_SHOW)
            .readonly(PCSCase::getGenAppId, ShowConditions.NEVER_SHOW)
            .label("selectAction-info", """
                           ---
                           ${claimDescriptionMarkdown}
                           """)
            .mandatory(PCSCase::getActionList);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        log.info("Handling midEvent for SelectAction page");

        PCSCase caseData = details.getData();
        String selectedActionLabel = caseData.getActionList().getValue().getLabel();
        caseData.setSelectedAction(selectedActionLabel);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
