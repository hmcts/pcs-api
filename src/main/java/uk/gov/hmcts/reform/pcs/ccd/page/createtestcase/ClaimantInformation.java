package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class ClaimantInformation implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Makes a claim")
            .pageLabel("Claimant name")
            .readonlyWithLabel(PCSCase::getClaimantName, "Your claimant name registered with My HMCTS is:")
            .label("preset-claimant-label", "### Is this the correct claimant name?")
            .mandatory(PCSCase::getIsClaimantName)
            .label("new-claimant-name-Label", "### What is the correct claimant name?", "isClaimantName=\"No\"")
            .mandatory(PCSCase::getCorrectClaimantName, "isClaimantName=\"No\"", false);

    }
}
