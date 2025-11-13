package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;

public class ClaimingCosts implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("claimingCosts")
                .pageLabel("Claiming costs")
                 .label("claimingCosts-info", """
                ---
                """)
                .mandatory(PCSCase::getClaimingCostsWanted)
            .label("claimingCosts-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }
}
