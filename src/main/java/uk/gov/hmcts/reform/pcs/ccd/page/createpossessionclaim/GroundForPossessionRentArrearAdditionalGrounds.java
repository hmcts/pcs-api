package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

/**
 * Page for selecting additional grounds for possession.
 */
public class GroundForPossessionRentArrearAdditionalGrounds implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundForPossessionRentArrearAdditionalGrounds")
            .pageLabel("What are your grounds for possession?")
            .showCondition("hasOtherGrounds=\"YES\"")
            .label("groundForPossessionRentArrearAdditionalGrounds-info", """
            ---
            <p class="govuk-body">You may have already given the defendants notice of your intention to begin possession proceedings.
            If you have, you should have written the grounds you're making your claim under.
            You should select these grounds here and any extra grounds you'd like to add to your claim, if you need to.</p>
            <p><a href='https://www.gov.uk/evicting-tenants/possession-grounds' target='_blank' rel='noopener noreferrer'>More information about possession grounds (opens in new tab)</a></p>
            """)
            .mandatory(PCSCase::getMandatoryGrounds)
            .mandatory(PCSCase::getDiscretionaryGrounds)
            .done();
    }
}
