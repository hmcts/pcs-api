package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class ClaimantInformation implements CcdPageConfiguration {

    private static final String UPDATED_CLAIMANT_NAME_HINT = """
        Changing your claimant name here only updates it for this claim.
        It does not change your registered claimant name on My HMCTS.
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Makes a claim")
            .pageLabel("Claimant name")
            .readonlyWithLabel(PCSCase::getClaimantName, "Your claimant name registered with My HMCTS is:")
            .mandatoryWithLabel(PCSCase::getIsClaimantNameCorrect,"Is this the correct claimant name?")
            .mandatory(PCSCase::getUpdatedClaimantName,
                    "isClaimantNameCorrect=\"No\"",
                    null,
                    "What is the correct claimant name?",
                    UPDATED_CLAIMANT_NAME_HINT,
                    false);

    }
}
