package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class EnterPropertyAddress implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("enterPropertyAddress")
            .pageLabel("What is the address of the property you're claiming possession of?")
            .label("enterPropertyAddress-lineSeparator", "---")
            .mandatory(PCSCase::getPropertyAddress);
    }

}
