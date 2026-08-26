package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventMetadata;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action;
import uk.gov.hmcts.reform.pcs.ccd.service.order.MakeOrderService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.makeOrder;

@Component
@AllArgsConstructor
public class MakeOrder implements CCDConfig<PCSCase, State, UserRole> {

    private final MakeOrderService makeOrderService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(makeOrder.name(), this::submit, this::start)
            .forStates(
                State.CASE_ISSUED,
                State.CASE_PROGRESSION,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME
            )
            .name("Make an order")
            .grant(Permission.CRUD,
                UserRole.JUDGE,
                UserRole.FEE_PAID_JUDGE,
                UserRole.CIRCUIT_JUDGE,
                UserRole.LEADERSHIP_JUDGE);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setMakeOrderPayload(makeOrderService.start(eventPayload.caseReference(), pcsCase));
        return pcsCase;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        Action action = makeOrderService.submit(
            eventPayload.caseReference(),
            eventPayload.caseData().getMakeOrderPayload());
        return SubmitResponse.<State>builder()
            .eventMetadata(eventMetadata(action))
            .build();
    }

    private EventMetadata eventMetadata(Action action) {
        return switch (action) {
            case START_DRAFT -> EventMetadata.builder()
                .summary("Order draft started")
                .description("Started drafting an order")
                .build();
            case SAVE_DRAFT -> EventMetadata.builder()
                .summary("Order draft saved")
                .description("Saved an order as a draft")
                .build();
            case SUBMIT_FOR_REVIEW -> EventMetadata.builder()
                .summary("Order submitted for review")
                .description("Submitted an order for caseworker review")
                .build();
        };
    }
}
