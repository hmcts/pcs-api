package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.order.MakeOrderService;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakeOrderTest extends BaseEventTest {

    @Mock
    private MakeOrderService makeOrderService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new MakeOrder(makeOrderService));
    }

    @Test
    void shouldConfigureTheJudicialMakeOrderJourney() {
        assertThat(configuredEvent.getName()).isEqualTo("Make an order");
        assertConfiguredForStates(
            State.CASE_ISSUED,
            State.CASE_PROGRESSION,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME
        );
        assertGrants(UserRole.JUDGE, Permission.CRUD);
        assertGrants(UserRole.FEE_PAID_JUDGE, Permission.CRUD);
        assertGrants(UserRole.CIRCUIT_JUDGE, Permission.CRUD);
        assertGrants(UserRole.LEADERSHIP_JUDGE, Permission.CRUD);
    }

    @Test
    void shouldGiveTheFrontendTheEnvelopeFromTheService() {
        PCSCase caseData = PCSCase.builder().build();
        MakeOrderEnvelope envelope = new MakeOrderEnvelope(null, null, null);
        when(makeOrderService.start(TEST_CASE_REFERENCE, caseData)).thenReturn(envelope);

        MakeOrderEnvelope result = callPayloadStartHandler(caseData);

        assertThat(result).isSameAs(envelope);
    }

    @ParameterizedTest
    @MethodSource("actionsAndMetadata")
    void shouldRecordMeaningfulAuditMetadataForEachAction(
        Action action,
        String expectedSummary,
        String expectedDescription
    ) {
        PCSCase caseData = PCSCase.builder().build();
        MakeOrderEnvelope envelope = new MakeOrderEnvelope(action, null, null);
        when(makeOrderService.submit(TEST_CASE_REFERENCE, envelope)).thenReturn(action);

        SubmitResponse<State> response = callPayloadSubmitHandler(caseData, envelope);

        assertThat(response.getEventMetadata().getSummary()).isEqualTo(expectedSummary);
        assertThat(response.getEventMetadata().getDescription()).isEqualTo(expectedDescription);
    }

    private static Stream<Arguments> actionsAndMetadata() {
        return Stream.of(
            Arguments.of(Action.START_DRAFT, "Order draft started", "Started drafting an order"),
            Arguments.of(Action.SAVE_DRAFT, "Order draft saved", "Saved an order as a draft"),
            Arguments.of(
                Action.SUBMIT_FOR_REVIEW,
                "Order submitted for review",
                "Submitted an order for caseworker review"
            )
        );
    }
}
