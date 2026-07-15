package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.StartEventHandler;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.SubmitEventHandler;
import uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page.RespondToPossessionDraftSavePage;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@Component
@Slf4j
public class ExtRespondPossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final StartEventHandler startEventHandler;
    private final SubmitEventHandler submitEventHandler;
    private final RespondToPossessionDraftSavePage respondToPossessionDraftSavePage;

    public ExtRespondPossessionClaim(@Qualifier("respondToClaimStartEventHandler") StartEventHandler startEventHandler,
                                     @Qualifier("respondToClaimSubmitEventHandler") SubmitEventHandler submitEventHandler,
                                     RespondToPossessionDraftSavePage respondToPossessionDraftSavePage) {

        this.startEventHandler = startEventHandler;
        this.submitEventHandler = submitEventHandler;
        this.respondToPossessionDraftSavePage = respondToPossessionDraftSavePage;
    }

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent("ext:respondPossessionClaim", submitEventHandler, startEventHandler)
            .forState(State.CASE_ISSUED)
            .name("Respond to claim")
            .description("Save defendants response as draft or to a case based on flag")
            .grant(Permission.CRU, UserRole.DEFENDANT)
            .grant(Permission.CRU, UserRole.DEFENDANT_SOLICITOR)
            .grant(Permission.CRU, UserRole.CREATOR)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);
        new PageBuilder(eventBuilder)
            .add(respondToPossessionDraftSavePage);
    }
}
