package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.StartEventHandler;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.SubmitEventHandler;
import uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page.RespondToPossessionDraftSavePage;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_3;

@Component
@Slf4j
public class RespondPossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final StartEventHandler startEventHandler;
    private final SubmitEventHandler submitEventHandler;
    private final RespondToPossessionDraftSavePage respondToPossessionDraftSavePage;
    private final FeatureToggleService featureToggleService;

    public RespondPossessionClaim(@Qualifier("respondToClaimStartEventHandler") StartEventHandler startEventHandler,
                                  @Qualifier("respondToClaimSubmitEventHandler") SubmitEventHandler submitEventHandler,
                                  RespondToPossessionDraftSavePage respondToPossessionDraftSavePage,
                                  FeatureToggleService featureToggleService) {

        this.startEventHandler = startEventHandler;
        this.submitEventHandler = submitEventHandler;
        this.respondToPossessionDraftSavePage = respondToPossessionDraftSavePage;
        this.featureToggleService = featureToggleService;
    }

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        var eventTypeBuilder = configBuilder
            .decentralisedEvent(respondPossessionClaim.name(), submitEventHandler, startEventHandler);

        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = featureToggleService.isEnabled(RELEASE_1_DOT_3)
            ? eventTypeBuilder.forStates(EventStates.respondPossessionClaim())
            : eventTypeBuilder.forAllStates();

        eventBuilder = eventBuilder
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Defendant Response Submission")
            .description("Save defendants response as draft or to a case based on flag")
            .grant(Permission.CRU, UserRole.DEFENDANT)
            .grant(Permission.CRU, UserRole.DEFENDANT_SOLICITOR)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);
        new PageBuilder(eventBuilder)
            .add(respondToPossessionDraftSavePage);
    }
}
