package uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd3.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd3.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;

@Slf4j
public class ClaimantInformation implements CcdPageConfiguration {

    private static final String UPDATED_CLAIMANT_NAME_HINT = """
        Changing your claimant name here only updates it for this claim.
        It does not change your registered claimant name on My HMCTS.
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("claimantInformation")
            .pageLabel("Claimant name")
            .label("claimantInformation-separator", "---")
            .readonlyWithLabel(PCSCase::getClaimantName, "Your claimant name registered with My HMCTS is:")
            .mandatoryWithLabel(PCSCase::getIsClaimantNameCorrect,"Is this the correct claimant name?")
            .mandatory(PCSCase::getOverriddenClaimantName,
                    "isClaimantNameCorrect=\"NO\"",
                    null,
                    "What is the correct claimant name?",
                    UPDATED_CLAIMANT_NAME_HINT,
                    false);

    }

}
