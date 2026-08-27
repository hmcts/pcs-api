package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;

@Component
@Slf4j
public class ExtRespondPossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("ext:respondPossessionClaim", new NoopSubmitHandler())
            .forState(State.CASE_ISSUED)
            .showCondition(ShowConditions.and(
                "legalRepUpdatedDetails=\"Yes\"",
                ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_2)))
            .name("Respond to claim")
            .description("Respond to claim")
            .grant(Permission.CRU, UserRole.DEFENDANT_SOLICITOR);
    }

    private static class NoopSubmitHandler implements Submit<PCSCase, State> {

        @Override
        public SubmitResponse<State> submit(EventPayload<PCSCase, State> payload) {
            return SubmitResponse.defaultResponse();
        }
    }

}
