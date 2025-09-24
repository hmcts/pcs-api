package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CREATOR;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.citizenUpdateApplication;

@Component
@Slf4j
@AllArgsConstructor
public class CitizenUpdateApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(citizenUpdateApplication.name(), this::submit)
            .forStates(AWAITING_SUBMISSION_TO_HMCTS)
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Patch case")
            .description("Patch a possession case")
            .grant(Permission.CRU, CREATOR)
            .grant(Permission.R, UserRole.PCS_CASE_WORKER);
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Citizen updated case {}", caseReference);

        pcsCaseService.patchCase(caseReference, pcsCase);
    }
}
