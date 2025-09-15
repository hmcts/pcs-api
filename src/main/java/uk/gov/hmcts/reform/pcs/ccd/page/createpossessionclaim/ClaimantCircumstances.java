package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class ClaimantCircumstances implements CcdPageConfiguration {

    public static final String YOU_CAN_ENTER_UP_TO_950_CHARACTERS = "You can enter up to 950 characters";
    public static final String SHOW_CONDITION = "claimantCircumstancesSelect=\"YES\"";
    public static final String CLAIMANT_CIRCUMSTANCES_INFO = "claimantCircumstances-Info";
    public static final String CLAIMANT_CIRCUMSTANCES = "claimantCircumstances";
    public static final String CLAIMANT_CIRCUMSTANCES_LABEL = "Claimant circumstances";
    public static final String GIVE_DETAILS_ABOUT_THE_CLAIMANT_NAME_CIRCUMSTANCES = "Give details about the ${claimantName} circumstances";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(CLAIMANT_CIRCUMSTANCES)
            .pageLabel(CLAIMANT_CIRCUMSTANCES_LABEL)
            .label(
                CLAIMANT_CIRCUMSTANCES_INFO, """
                ---
                <b>Is there any information you'd like to provide about the ${claimantName} circumstances?</b>
                """)
            .mandatoryWithLabel(PCSCase::getClaimantCircumstancesSelect,
                       "This can be any information about your financial or general situation that " +
                           "you'd like the court to consider when making its decision whether or not to grant a possession order"
                       )
            .mandatory(PCSCase::getClaimantCircumstancesDetails, SHOW_CONDITION,
                       "",
                       GIVE_DETAILS_ABOUT_THE_CLAIMANT_NAME_CIRCUMSTANCES,
                       YOU_CAN_ENTER_UP_TO_950_CHARACTERS,
                       false
            );
    }

}
