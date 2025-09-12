package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class IntroductoryDemotedOrOtherGroundsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("introductoryDemotedOrOtherGroundsForPossession")
            .pageLabel("Grounds for possession")
            .showCondition(
                "typeOfTenancyLicence=\"INTRODUCTORY_TENANCY\" "
                  + "OR typeOfTenancyLicence=\"DEMOTED_TENANCY\" "
                  + "OR typeOfTenancyLicence=\"OTHER\"")
            .label(
                "introductoryDemotedOrOtherGroundsForPossession-info",
                  """
                   ---
                   <p class="govuk-body">In some cases, a claimant can make for possession of a property
                   without having to rely on a specific ground. If your claim meets these
                   requirements, you can select that you have no grounds for possession.
                   
                   You may have already given the defendants notice of your intention to begin
                    possession proceedings.
                    If you have, you should have written the grounds you're making your claim under. You should select
                    these grounds here and any extra ground you'd like to add to your claim, if you need to.
                   </p>
                   """)
            .mandatory(PCSCase::getIntroductoryDemotedOtherGroundsForPossession)
            .mandatory(PCSCase::getIntroductoryDemotedOrOtherGrounds,
                    "introductoryDemotedOtherGroundsForPossession=\"YES\"")
            .mandatory(PCSCase::getOtherGroundsOfPossession,
                    "introductoryDemotedOrOtherGroundsCONTAINS "
                    + "\"OTHER\"");
    }
}
