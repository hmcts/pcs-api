package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction.ConfirmEvictionConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.service.ClaimantEvictionConfirmationService;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.confirmEviction;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Component
@AllArgsConstructor
@Slf4j
public class ConfirmEviction implements CCDConfig<PCSCase, State, UserRole> {

    /**
     * EnforcementOrderMediator sets this for the case view only while the claim's current enforcement is
     * awaiting the claimant's confirmation, which is the only time ClaimantEvictionConfirmationService records
     * an answer. Without it the journey is offered on every issued case and silently records nothing.
     */
    static final String SHOW_CONFIRM_EVICTION_JOURNEY = "showConfirmEvictionJourney=\"Yes\"";

    private final ConfirmEvictionConfigurer confirmEvictionConfigurer;
    private final ClaimantEvictionConfirmationService claimantEvictionConfirmationService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(confirmEviction.name(), this::submit)
                .forState(State.CASE_ISSUED)
                .name("Confirm the eviction details")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .grant(Permission.CRUD, UserRole.GA_CLAIMANT_SOLICITOR)
                .grant(Permission.CRUD, UserRole.CLAIMANT)
                .showCondition(ShowConditions.and(ShowConditions.featureFlagsEnabled(ENFORCEMENT),
                                                 SHOW_CONFIRM_EVICTION_JOURNEY))
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary();
        confirmEvictionConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        claimantEvictionConfirmationService.recordAttendance(
            eventPayload.caseReference(),
            Optional.ofNullable(eventPayload.caseData().getEnforcementOrder())
                .map(EnforcementOrder::getClaimantCanAttendEviction)
                .orElse(null));
        return SubmitResponse.defaultResponse();
    }

}
