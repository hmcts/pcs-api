package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class ClaimantCircumstances implements CcdPageConfiguration {

    private static final String YOU_CAN_ENTER_UP_TO_950_CHARACTERS = "You can enter up to 950 characters";
    private static final String SHOW_CONDITION = "claimantCircumstancesSelect=\"YES\"";
    private static final String CLAIMANT_CIRCUMSTANCES_INFO = "claimantCircumstances-Info";
    private static final String CLAIMANT_CIRCUMSTANCES = "claimantCircumstances";
    private static final String CLAIMANT_CIRCUMSTANCES_LABEL = "Claimant circumstances";
    private static final String GIVE_DETAILS_ABOUT_THE_CLAIMANT_NAME_CIRCUMSTANCES
        = "Give details about ${displayedClaimantName} circumstances";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(CLAIMANT_CIRCUMSTANCES)
            .pageLabel(CLAIMANT_CIRCUMSTANCES_LABEL)
            .readonly(PCSCase::getDisplayedClaimantName, NEVER_SHOW)
            .label(
                CLAIMANT_CIRCUMSTANCES_INFO, """
                ---
                """)
            .mandatory(PCSCase::getClaimantCircumstancesSelect)
            .mandatory(PCSCase::getClaimantCircumstancesDetails, SHOW_CONDITION,
                       "",
                       GIVE_DETAILS_ABOUT_THE_CLAIMANT_NAME_CIRCUMSTANCES,
                       YOU_CAN_ENTER_UP_TO_950_CHARACTERS,
                       false
            );
    }

}
