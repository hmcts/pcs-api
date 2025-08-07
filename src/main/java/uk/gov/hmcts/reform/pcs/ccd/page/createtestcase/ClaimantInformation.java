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
            .mandatory(PCSCase::getYesOrNo)
            .done();
    }
}
