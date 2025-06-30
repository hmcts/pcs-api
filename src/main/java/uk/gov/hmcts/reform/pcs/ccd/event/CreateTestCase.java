package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
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

@Profile("dev") // Non-prod event
@Component
public class CreateTestCase implements CCDConfig<PCSCase, State, UserRole> {
    @Autowired
    private PCSCaseRepository repository;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("createTestApplication", this::submit, this::start)
            .initialState(State.Open)
            .name("Create test case")
            .grant(Permission.CRUD, UserRole.CASE_WORKER)
            .fields()
            .page("Create test case")
                .mandatory(MyDTO::getCaseDescription)
                .done();
    }

    private MyDTO start(EventPayload<MyDTO, State> p) {
        var result = p.caseData();
        result.setCaseDescription("Acme Corporation v. Smith");
        return result;
    }

    public void submit(EventPayload<MyDTO, State> p) {
        var c = PcsCase.builder()
            .reference(p.caseReference())
            .caseDescription(p.caseData().getCaseDescription())
            .build();
        repository.save(c);
    }

    @Data
    public static class MyDTO {
        private String caseDescription;
    }
}
