package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantsDOB;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@AllArgsConstructor
@Component
public class DefendantsDOBPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantsDOB", this::midEvent)
            .pageLabel("The defendants' dates of birth")
            .label("defendantDOBLabel-lineSeparator", "---")
            .label("defendantDOBLabel", "<h2> Do you know the defendants' dates of birth? </h2>")
            .mandatory(PCSCase::getIsDefendantsDOBKnown)
        .list(PCSCase::getDobDefendants, "isDefendantsDOBKnown=\"YES\"")
            .readonly(DefendantsDOB::getDefendantName)
            .optional(DefendantsDOB::getDob)
            .done()
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                    CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().build();
    }

}
