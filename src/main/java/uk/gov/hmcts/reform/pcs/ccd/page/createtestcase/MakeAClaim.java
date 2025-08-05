package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Organisation;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public class MakeAClaim implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Make a claim", this::midEvent)
            .pageLabel("What is the address of the property you're claiming possession of?")
            .label("lineSeparator", "---")
            .mandatory(PCSCase::getPropertyAddress)
            .complex(PCSCase::getOrganisationPolicy)
                .complex(OrganisationPolicy::getOrganisation)
                .readonly(Organisation::getOrganisationName, ShowConditions.NEVER_SHOW)
                .done()
            .done();

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        details.getData().setShortenedName(details.getData().getOrganisationPolicy()
            .getOrganisation().getOrganisationName());

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().build();
    }
}
