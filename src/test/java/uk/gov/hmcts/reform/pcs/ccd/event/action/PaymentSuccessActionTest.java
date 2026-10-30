package uk.gov.hmcts.reform.pcs.ccd.event.action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSuccessActionTest {

    private static final long CASE_REFERENCE = 2222222222L;
    private static final String PAYMENT_REFERENCE = "RC-2222-4444-1111-4444";

    private PaymentSuccessAction underTest;

    @BeforeEach
    void setUp() {
        underTest = new PaymentSuccessAction() {
            @Override
            public EventId eventId() {
                return EventId.claimIssuePayment;
            }

            @Override
            public SystemEventResult apply(PaymentStateTaskData taskData) {
                return stateTransition(taskData, State.CASE_ISSUED);
            }
        };
    }

    @Test
    void shouldUseTaskDataEventIdAsEventIdAndEventName() {
        // Given
        PaymentStateTaskData taskData = taskData(EventId.enforcementPayment);

        // When
        SystemEventResult result = underTest.stateTransition(taskData, State.CASE_ISSUED);

        // Then
        assertThat(result.eventId()).isEqualTo(EventId.enforcementPayment.name());
        assertThat(result.eventName()).isEqualTo(EventId.enforcementPayment.name());
    }

    @Test
    void shouldNotUseEventIdOfTheAction() {
        // Given
        PaymentStateTaskData taskData = taskData(EventId.enforcementPayment);

        // When
        SystemEventResult result = underTest.stateTransition(taskData, State.CASE_ISSUED);

        // Then
        assertThat(underTest.eventId()).isNotEqualTo(taskData.getEventId());
        assertThat(result.eventId()).isEqualTo(taskData.getEventId().name());
    }

    @Test
    void shouldBuildSummaryFromPaymentAndCaseReferences() {
        // Given
        PaymentStateTaskData taskData = taskData(EventId.claimIssuePayment);

        // When
        SystemEventResult result = underTest.stateTransition(taskData, State.CASE_ISSUED);

        // Then
        assertThat(result.summary()).isPresent();
        String summary = result.summary().get();
        assertThat(summary).isEqualTo("Payment " + PAYMENT_REFERENCE + " updated for " + CASE_REFERENCE);
    }

    private PaymentStateTaskData taskData(EventId eventId) {
        return PaymentStateTaskData.builder().caseReference(CASE_REFERENCE).eventId(eventId)
            .paymentReference(PAYMENT_REFERENCE).idempotencyKey(UUID.randomUUID()).build();
    }

}
