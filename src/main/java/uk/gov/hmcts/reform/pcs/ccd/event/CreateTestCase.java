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
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.repository.PcsCaseRepository;

@Profile("dev") // Non-prod event
@Component
public class CreateTestCase implements CCDConfig<PcsCase, State, UserRole> {
    @Autowired
    private PcsCaseRepository repository;

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("createTestApplication", this::submit)
            .initialState(State.Open)
            .name("Create test case")
            .aboutToStartCallback(this::start)
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Create test case")
                .mandatory(PcsCase::getCaseDescription)
                .done();
    }

    private AboutToStartOrSubmitResponse<PcsCase, State> start(CaseDetails<PcsCase, State> caseDetails) {
        PcsCase data = caseDetails.getData();
        data.setCaseDescription("Acme Corporation v. Smith");

        return AboutToStartOrSubmitResponse.<PcsCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }

    public void submit(EventPayload<PcsCase, State> p) {
        var c = uk.gov.hmcts.reform.pcs.entity.PcsCase.builder()
            .caseReference(p.caseReference())
            .build();
        repository.save(c);
    }
}
