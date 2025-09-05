package uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd3.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd3.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;

public class DefendantsDetails implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantsDetails")
            .pageLabel("Defendant 1 details")
            .mandatory(PCSCase::getDefendant1);
    }
}
