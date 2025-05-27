package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

//@Component
public class CreateTestCaseDecentralised implements CCDConfig<PCSCase, State, UserRole> {
    @Autowired
    private PCSCaseRepository repository;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("createTestApplicationDecentralised", this::submit, this::start)
            .initialState(State.Open)
            .name("Create test case decentralised")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Create test case")
                .mandatory(PCSCase::getCaseDescription)
                .done();
    }

    private PCSCase start(EventPayload<PCSCase, State> p) {
        p.caseData().setCaseDescription("Acme Corporation v. Smith");

        return p.caseData();
    }

    public void submit(EventPayload<PCSCase, State> p) {
        var c = PcsCase.builder()
            .reference(p.caseReference())
            .caseDescription(p.caseData().getCaseDescription())
            .build();
        repository.save(c);
    }
}
