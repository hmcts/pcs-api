package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page.RespondToPossessionDraftSavePage;

@Component
@Slf4j
public class ExtRespondPossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final RespondToPossessionDraftSavePage respondToPossessionDraftSavePage;

    public ExtRespondPossessionClaim(
        RespondToPossessionDraftSavePage respondToPossessionDraftSavePage
    ) {

        this.respondToPossessionDraftSavePage = respondToPossessionDraftSavePage;
    }

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent("ext:respondPossessionClaim", new NoopSubmitHandler(), new NoopStartEventHandler())
            .forAllStates()
            .showCondition("legalRepUpdatedDetails=\"Yes\"")
            .name("Respond to claim")
            .description("Save defendants response as draft or to a case based on flag")
            .grant(Permission.CRU, UserRole.DEFENDANT_SOLICITOR);
        new PageBuilder(eventBuilder)
            .add(respondToPossessionDraftSavePage);
    }

    private static class NoopSubmitHandler implements Submit<PCSCase, State>  {

        @Override
        public SubmitResponse<State> submit(EventPayload<PCSCase, State> payload) {
            return null;
        }
    }

    private static class NoopStartEventHandler implements Start<PCSCase, State> {

        @Override
        public PCSCase start(EventPayload<PCSCase, State> payload) {
            return null;
        }
    }
}
