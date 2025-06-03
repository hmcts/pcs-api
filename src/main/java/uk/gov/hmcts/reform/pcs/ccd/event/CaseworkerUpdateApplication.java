package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.Submitted;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.CIVIL_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.caseworkerUpdateApplication;

@Component
@Slf4j
@AllArgsConstructor
public class CaseworkerUpdateApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(caseworkerUpdateApplication.name(), this::submit)
            .forStates(Submitted)
            .name("Update case")
            .description("Update a possession case")
            .grant(Permission.CRU, CIVIL_CASE_WORKER)
            .fields()
            .page("page-1")
                .mandatory(PCSCase::getApplicantForename)
                .mandatory(PCSCase::getApplicantSurname)
            .done();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Caseworked updated case {}", caseReference);

        pcsCaseService.patchCase(caseReference, pcsCase);
    }
}
