package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class DefendantList implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantList", this::midEvent)
            .pageLabel("Defendant List")
            .readonly(PCSCase::getDefendantsTableHtml, NEVER_SHOW)
            .label("defendantList-line", """
                ---
                """)
            .label("defendantList-content", "${defendantsTableHtml}")
            .mandatory(PCSCase::getAddAnotherDefendant);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        // Handle "add another defendant" flow
        if (caseData.getAddAnotherDefendant() == YesOrNo.YES) {
            // Increment defendant number for next defendant
            int nextDefendantNumber = (caseData.getCurrentDefendantNumber() != null ? caseData.getCurrentDefendantNumber() : 0) + 1;
            caseData.setCurrentDefendantNumber(nextDefendantNumber);
            
            // Reset defendant1 for new entry
            caseData.setDefendant1(new DefendantDetails());
            
            // Keep addAnotherDefendant as Yes to trigger the show condition
            caseData.setAddAnotherDefendant(YesOrNo.YES);
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}