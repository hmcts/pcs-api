package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.updateApplicantName;

@Profile("dev") // Non-prod event
@Component
@AllArgsConstructor
public class UpdateApplicantName implements CCDConfig<PcsCase, State, UserRole> {

    private final PCSCaseRepository pcsCaseRepository;

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(updateApplicantName.name(), this::submit)
            .forState(State.Open)
            .name("Update Applicant Details")
            .grant(Permission.CRUD, UserRole.MY_APPLICANT_CASE_ROLE, UserRole.MY_JUDGE_ROLE)
            .fields()
            .page("Applicant Details")
                .mandatory(PcsCase::getApplicantName)
                .done();
    }

    public void submit(EventPayload<PcsCase, State> eventPayload) {
        PcsCase pcsCase = eventPayload.caseData();
        long caseReference = eventPayload.caseReference();

        uk.gov.hmcts.reform.pcs.entity.PcsCase pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException("No case data found for reference " + caseReference)
        );

        pcsCaseEntity.setApplicantName(pcsCase.getApplicantName());

        pcsCaseRepository.save(pcsCaseEntity);
    }

}
