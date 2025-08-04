package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Component
public class ClaimantInformation implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Organisation Details")
            .pageLabel("Organisation Details")
            .label("OrganisationQuestionTest", "Your claimant name registered with My HMCTS is:")
            .readonly(PCSCase::getShortenedName)
            .label("OrganisationQuestionTest3", "After shortened Name")
            .mandatory(PCSCase::getYesOrNo)
            .done();
        // .complex(PCSCase::getOrganisationPolicy)
        //  .complex(OrganisationPolicy::getOrganisation)
        //  .optional(Organisation::getOrganisationName)
        //      .done()
        //  .done()
        //  .mandatory(PCSCase::getYesOrNo);


        //readonly , reference field, show condititon
    }
}
