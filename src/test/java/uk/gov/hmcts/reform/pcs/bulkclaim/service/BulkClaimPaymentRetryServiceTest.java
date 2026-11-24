package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimPaymentStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.BulkPbaDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkClaimPaymentRetryServiceTest {

    private static final UUID ID = UUID.randomUUID();
    private static final long CASE_REFERENCE = 1781000000000001L;
    private static final String SERVICE_REQUEST = "2026-1";

    @Mock
    private BulkClaimPaymentStatusUpdater statusUpdater;
    @Mock
    private PaymentService paymentService;
    @Mock
    private BulkClaimCaseStateReader caseStateReader;
    @InjectMocks
    private BulkClaimPaymentRetryService underTest;

    @Test
    void shouldRetryWhenFailedAwaitingPaymentAndFeeUnpaid() {
        when(caseStateReader.stateOf(CASE_REFERENCE)).thenReturn(Optional.of("PENDING_CASE_ISSUED"));
        when(paymentService.isServiceRequestPaid(SERVICE_REQUEST)).thenReturn(false);

        assertThat(underTest.canRetry(submission(BulkClaimPaymentStatus.FAILED))).isTrue();
    }

    @Test
    void shouldNotRetryWhenPaymentIsNotFailed() {
        assertThat(underTest.canRetry(submission(BulkClaimPaymentStatus.PENDING))).isFalse();
        assertThat(underTest.canRetry(submission(BulkClaimPaymentStatus.PAID))).isFalse();
    }

    @Test
    void shouldNotRetryWhenCaseHasMovedOnFromPendingCaseIssued() {
        when(caseStateReader.stateOf(CASE_REFERENCE)).thenReturn(Optional.of("CASE_ISSUED"));

        assertThat(underTest.canRetry(submission(BulkClaimPaymentStatus.FAILED))).isFalse();
    }

    @Test
    void shouldNotRetryWhenFeeIsAlreadyPaid() {
        when(caseStateReader.stateOf(CASE_REFERENCE)).thenReturn(Optional.of("PENDING_CASE_ISSUED"));
        when(paymentService.isServiceRequestPaid(SERVICE_REQUEST)).thenReturn(true);

        assertThat(underTest.canRetry(submission(BulkClaimPaymentStatus.FAILED))).isFalse();
    }

    @Test
    void shouldRestartPaymentThenScheduleItAgain() {
        BulkClaimSubmissionEntity submission = submission(BulkClaimPaymentStatus.FAILED);
        when(statusUpdater.restartFailedPayment(ID, "PBA2", "REF2")).thenReturn(true);
        BulkPbaDetails details = new BulkPbaDetails("PBA2", "REF2", "Org One", ID);
        when(paymentService.rescheduleBulkPbaPayment(SERVICE_REQUEST, CASE_REFERENCE, details)).thenReturn(true);

        assertThat(underTest.retry(submission, "PBA2", "REF2", "Org One")).isTrue();
    }

    @Test
    void shouldNotScheduleWhenAnotherRetryAlreadyRestartedThePayment() {
        when(statusUpdater.restartFailedPayment(ID, "PBA2", "REF2")).thenReturn(false);

        assertThat(underTest.retry(submission(BulkClaimPaymentStatus.FAILED), "PBA2", "REF2", "Org One")).isFalse();

        verify(paymentService, never()).rescheduleBulkPbaPayment(any(), anyLong(), any());
    }

    @Test
    void shouldMarkPaymentFailedAgainWhenSchedulingThrows() {
        when(statusUpdater.restartFailedPayment(ID, "PBA2", "REF2")).thenReturn(true);
        when(paymentService.rescheduleBulkPbaPayment(any(), anyLong(), any()))
            .thenThrow(new IllegalStateException("db down"));

        BulkClaimSubmissionEntity failed = submission(BulkClaimPaymentStatus.FAILED);

        assertThatThrownBy(() -> underTest.retry(failed, "PBA2", "REF2", "Org"))
            .isInstanceOf(IllegalStateException.class);

        verify(statusUpdater).onPbaPaymentFailed(ID, SERVICE_REQUEST, "Payment could not be rescheduled");
    }

    private static BulkClaimSubmissionEntity submission(BulkClaimPaymentStatus paymentStatus) {
        return BulkClaimSubmissionEntity.builder()
            .id(ID)
            .caseReference(CASE_REFERENCE)
            .serviceRequestReference(SERVICE_REQUEST)
            .paymentStatus(paymentStatus)
            .build();
    }
}
