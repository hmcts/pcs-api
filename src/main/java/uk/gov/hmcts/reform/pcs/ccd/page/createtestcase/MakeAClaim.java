package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Organisation;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

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
            .optional(PCSCase::getPropertyAddress)
            .complex(PCSCase::getOrganisationPolicy, NEVER_SHOW)
                .complex(OrganisationPolicy::getOrganisation)
                .readonly(Organisation::getOrganisationName)
                .done();

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase mycase =  details.getData();
        mycase.setShortenedName(mycase.getOrganisationPolicy().getOrganisation().getOrganisationName());
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(mycase).build();
    }
}
