package uk.gov.hmcts.reform.pcs.ccd.event;

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
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseEntityRepository;

@Component
public class CreateTestCase implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseEntityRepository pcsCaseEntityRepository;

    public CreateTestCase(PcsCaseEntityRepository pcsCaseEntityRepository) {
        this.pcsCaseEntityRepository = pcsCaseEntityRepository;
    }

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
                .mandatory(PCSCase::getApplicantForename)
                .done();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        String applicantForename = eventPayload.caseData().getApplicantForename();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setApplicantForename(applicantForename);

        pcsCaseEntityRepository.save(pcsCaseEntity);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> start(CaseDetails<PCSCase, State> caseDetails) {
        PCSCase data = caseDetails.getData();
        data.setApplicantForename("Preset value");

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseDetails.getData())
            .build();
    }

}
