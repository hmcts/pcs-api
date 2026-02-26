package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.ShowConditionsEnforcementType.WARRANT_OF_RESTITUTION_FLOW;


public class ShareEvidenceWithJudgePage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("shareEvidenceWithJudge")
            .pageLabel("On the next few questions, we will ask you to share evidence with a judge")
            .showCondition(WARRANT_OF_RESTITUTION_FLOW)
            .label("shareEvidenceWithJudge-line-separator", "---")
            .label("shareEvidenceWithJudge-content",
                   """
                       <p class="govuk-body">
                       The judge will read this evidence and either approve
                       or refuse your request for a warrant of restitution.
                       </p>
                       """
            );
    }
}
