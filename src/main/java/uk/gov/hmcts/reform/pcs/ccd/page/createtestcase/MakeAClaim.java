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
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

public class MakeAClaim implements CcdPageConfiguration {

    PcsCaseService pcsCaseService;
    long caseReference;

    public MakeAClaim(PcsCaseService pcsCaseService, long caseReference) {
        this.pcsCaseService = pcsCaseService;
        this.caseReference = caseReference;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Make a claim", this::midEvent)
            .pageLabel("What is the address of the property you're claiming possession of?")
            .label("lineSeparator", "---")
            .mandatory(PCSCase::getPropertyAddress)
            .complex(PCSCase::getOrganisationPolicy)
                .complex(OrganisationPolicy::getOrganisation)
                .readonly(Organisation::getOrganisationName)

                .done()
            .done();

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        details.getData().setShortenedName("testshortenedanme");
        PCSCase casePatch = details.getData();
        pcsCaseService.patchCase(caseReference, casePatch);

//        details.getData().setShortenedName("testshortenedname");

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().build();
    }
}
