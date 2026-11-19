package uk.gov.hmcts.reform.pcs.ccd.event.breathingspace;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.liftBreathingSpace;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Component
public class LiftBreathingSpace implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(liftBreathingSpace.name(), this::submit)
            .forState(State.BREATHING_SPACE)
            .name("Lift breathing space")
            .grant(Permission.CRUD, CASEWORKER_ROLES)
            .showCondition(ShowConditions.featureFlagsEnabled(ENFORCEMENT));
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        // Guard against accidentally treating an unfinished event as a lift.
        throw new IllegalStateException(
            "Lift Breathing Space submission is not yet implemented"
        );
    }
}
