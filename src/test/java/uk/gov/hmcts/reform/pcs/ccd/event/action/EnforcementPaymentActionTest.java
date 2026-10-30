package uk.gov.hmcts.reform.pcs.ccd.event.action;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStateTaskData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnforcementPaymentActionTest {

    private static final long CASE_REFERENCE = 2222222222L;
    private static final String PAYMENT_REFERENCE = "RC-2222-4444-1111-4444";

    private final EnforcementPaymentAction underTest = new EnforcementPaymentAction();

    @Test
    void shouldReturnEnforcementPaymentEventId() {
        // Given ... When
        EventId eventId = underTest.eventId();

        // Then
        assertThat(eventId).isEqualTo(EventId.enforcementPayment);
    }

    @Test
    void shouldTransitionToCaseProgressionState() {
        // Given
        PaymentStateTaskData taskData = taskData();

        // When
        SystemEventResult result = underTest.apply(taskData);

        // Then
        assertThat(result.state()).isPresent();
        Enum<?> state = result.state().get();
        assertThat(state).isEqualTo(State.CASE_PROGRESSION);
    }

    @Test
    void shouldUseTaskDataEventIdAsEventIdAndEventName() {
        // Given
        PaymentStateTaskData taskData = taskData();

        // When
        SystemEventResult result = underTest.apply(taskData);

        // Then
        assertThat(result.eventId()).isEqualTo(EventId.enforcementPayment.name());
        assertThat(result.eventName()).isEqualTo(EventId.enforcementPayment.name());
    }

    @Test
    void shouldBuildSummaryFromPaymentAndCaseReferences() {
        // Given
        PaymentStateTaskData taskData = taskData();

        // When
        SystemEventResult result = underTest.apply(taskData);

        // Then
        assertThat(result.summary())
            .contains("Payment " + PAYMENT_REFERENCE + " updated for " + CASE_REFERENCE);
    }

    private PaymentStateTaskData taskData() {
        return PaymentStateTaskData.builder().caseReference(CASE_REFERENCE).eventId(EventId.enforcementPayment)
            .paymentReference(PAYMENT_REFERENCE).idempotencyKey(UUID.randomUUID()).build();
    }

}
