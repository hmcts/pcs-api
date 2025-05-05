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
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.updateRespondentName;

@Profile("dev") // Non-prod event
@Component
@AllArgsConstructor
public class UpdateRespondentName implements CCDConfig<PcsCase, State, UserRole> {

    private final PCSCaseRepository pcsCaseRepository;

    @Override
    public void configure(ConfigBuilder<PcsCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(updateRespondentName.name(), this::submit)
            .forState(State.Open)
            .name("Update Respondent Details")
            .grant(Permission.CRUD, UserRole.MY_RESPONDENT_CASE_ROLE, UserRole.MY_JUDGE_ROLE)
            .fields()
            .page("Respondent Details")
                .mandatory(PcsCase::getRespondentName)
                .done();
    }

    public void submit(EventPayload<PcsCase, State> eventPayload) {
        PcsCase pcsCase = eventPayload.caseData();
        long caseReference = eventPayload.caseReference();

        uk.gov.hmcts.reform.pcs.entity.PcsCase pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException("No case data found for reference " + caseReference)
        );

        pcsCaseEntity.setRespondentName(pcsCase.getRespondentName());

        pcsCaseRepository.save(pcsCaseEntity);
    }

}
