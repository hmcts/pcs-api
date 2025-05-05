package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
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
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestCase;

@Profile("dev") // Non-prod event
@Component
@AllArgsConstructor
public class CreateTestCase implements CCDConfig<PcsCase, State, UserRole> {

    private final PCSCaseRepository pcsCaseRepository;

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(createTestCase.name(), this::submit)
            .initialState(State.Open)
            .name("Create test case")
            .aboutToStartCallback(this::start)
            .grant(Permission.CRUD, UserRole.CIVIL_CASE_WORKER, UserRole.MY_APPLICANT_ROLE)
            .fields()
            .page("Create test case")
                .mandatory(PcsCase::getCaseDescription)
                .mandatory(PcsCase::getApplicantName)
                .mandatory(PcsCase::getRespondentName)
                .done();
    }

    private AboutToStartOrSubmitResponse<PcsCase, State> start(CaseDetails<PcsCase, State> caseDetails) {
        PcsCase pcsCase = caseDetails.getData();
        pcsCase.setCaseDescription("Acme Corporation v. Smith");

        return AboutToStartOrSubmitResponse.<PcsCase, State>builder()
            .data(pcsCase)
            .build();
    }

    public void submit(EventPayload<PcsCase, State> eventPayload) {
        PcsCase pcsCase = eventPayload.caseData();

        var pcsCaseEntity = uk.gov.hmcts.reform.pcs.entity.PcsCase.builder()
            .caseReference(eventPayload.caseReference())
            .description(pcsCase.getCaseDescription())
            .applicantName(pcsCase.getApplicantName())
            .respondentName(pcsCase.getRespondentName())
            .build();

        pcsCaseRepository.save(pcsCaseEntity);
    }
}
