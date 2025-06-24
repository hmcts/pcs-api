package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PCS;
import uk.gov.hmcts.reform.pcs.ccd.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PCSCaseService;

@Component
public class CreateTestCase implements CCDConfig<PCSCase, State, UserRole> {


    private final PCSCaseRepository repository;
    private final PCSCaseService pcsCaseService;

    public CreateTestCase(PCSCaseRepository repository, PCSCaseService pcsCaseService) {
        this.repository = repository;
        this.pcsCaseService = pcsCaseService;
    }

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder.decentralisedEvent("createTestApplication", this::submit)
            .initialState(State.Open)
            .name("Make a claim")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Make a claim")
            .pageLabel("What is the address of the property you're claiming possession of?")
            .label("lineSeparator", "---")
            .mandatory(PCSCase::getPropertyAddress)
            .page("claimant information")
            .pageLabel("Please enter applicant's name")
            .mandatory(PCSCase::getApplicantForename)
                .done();
    }

    /**
     private AboutToStartOrSubmitResponse PCSCase, State  start(CaseDetails PCSCase, State caseDetails) {
        PCSCase data = caseDetails.getData();
        data.setApplicantForename("Preset value");

     return AboutToStartOrSubmitResponse.PCSCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }
     */
    private void submit(EventPayload<PCSCase, State> eventPayload) {
        var pcsCase = PCS.builder()
            .ccdCaseReference(eventPayload.caseReference())
            .build();


        PCS saved = repository.save(pcsCase);
    }

}
