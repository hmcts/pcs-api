package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class MakeAClaim implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Make a claim")
            .pageLabel("Claimant name")
            .label("lineSeparator", "---")
            .label("makeAClaim-organisationQuestion", "Your claimant name registered with MyHMCTS is:")
            .readonly(PCSCase::getOrganisationName)
            .mandatory(PCSCase::getCorrectOrganisation);
    }
}
