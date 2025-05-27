package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

//@Profile("dev") // Non-prod event
//@Component
public class CreateTestCase implements CCDConfig<PCSCase, State, UserRole> {
    @Autowired
    private PCSCaseRepository repository;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("createTestApplication", this::submit)
            .initialState(State.Open)
            .name("Create test case")
            .aboutToStartCallback(this::start)
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Create test case")
                .mandatory(PCSCase::getCaseDescription)
                .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> start(CaseDetails<PCSCase, State> caseDetails) {
        PCSCase data = caseDetails.getData();
        data.setCaseDescription("Acme Corporation v. Smith");

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }

    public void submit(EventPayload<PCSCase, State> p) {
        var c = PcsCase.builder()
            .reference(p.caseReference())
            .caseDescription(p.caseData().getCaseDescription())
            .build();
        repository.save(c);
    }
}
