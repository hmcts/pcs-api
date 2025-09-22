package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Page for selecting additional grounds for possession.
 */
public class GroundForPossessionAdditionalGrounds implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundForPossessionAdditionalGrounds")
            .pageLabel("What are your grounds for possession?")
            .showCondition("hasOtherAdditionalGrounds=\"Yes\""
                           + " AND typeOfTenancyLicence=\"ASSURED_TENANCY\""
                               + " AND groundsForPossession=\"Yes\""
            )
            .label("groundForPossessionAdditionalGrounds-info", """
            ---
            <p class="govuk-body">You may have already given the defendants notice of your intention to begin
                possession proceedings. If you have, you should have written the grounds you're making your
                claim under. You should select these grounds here and any extra grounds you'd like to add to
                your claim, if you need to.</p>
            """)
            .mandatory(PCSCase::getMandatoryGrounds)
            .mandatory(PCSCase::getDiscretionaryGrounds)
            .done();
    }
}
