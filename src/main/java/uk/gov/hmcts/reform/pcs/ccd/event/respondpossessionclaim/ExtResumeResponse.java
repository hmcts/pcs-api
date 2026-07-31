package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.springframework.stereotype.Component;

import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;

@Component
public class ExtResumeResponse implements CCDConfig<PCSCase, State, UserRole> {

    private static final String EVENT_ID = "ext:resumeResponse";

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(EVENT_ID, new NoopSubmitHandler())
            .forState(State.CASE_ISSUED)
            .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_2))
            .name("Resume response")
            .description("Exposes event to redirect to pcs-frontend")
            .grant(Permission.CRU, UserRole.DEFENDANT_SOLICITOR);
    }

}
