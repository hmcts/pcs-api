package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Component
public class ClaimantInformation implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Organisation Details", this::midEvent)
            .pageLabel("Organisation Details")
            .label("OrganisationQuestionTest", "Your claimant name registered with My HMCTS is:")
            .readonly(PCSCase::getShortenedName)
            .label("OrganisationQuestionTest3", "After shortened Name")
            .mandatory(PCSCase::getYesOrNo);
        // .complex(PCSCase::getOrganisationPolicy)
        //  .complex(OrganisationPolicy::getOrganisation)
        //  .optional(Organisation::getOrganisationName)
        //      .done()
        //  .done()
        //  .mandatory(PCSCase::getYesOrNo);


        //readonly , reference field, show condititon
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        System.out.println("HERE");
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(details.getData())
            .build();
    }
}
