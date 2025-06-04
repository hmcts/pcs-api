package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.Draft;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.Submitted;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.CITIZEN;
import static uk.gov.hmcts.reform.pcs.ccd.domain.UserRole.CREATOR;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.citizenSubmitApplication;

@Component
@Slf4j
public class CitizenSubmitApplication implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(citizenSubmitApplication.name(), this::submit)
            .forStateTransition(Draft, Submitted)
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Submit case")
            .description("Submit the possession case")
            .grant(Permission.CRU, CREATOR, CITIZEN)
            .grant(Permission.R, UserRole.CIVIL_CASE_WORKER);
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        log.info("Citizen submitted case {}", eventPayload.caseReference());
    }

}
