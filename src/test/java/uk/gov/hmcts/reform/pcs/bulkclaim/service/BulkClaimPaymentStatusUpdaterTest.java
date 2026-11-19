package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimPaymentStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkClaimPaymentStatusUpdaterTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String SERVICE_REQUEST = "2026-1234567890";

    @Mock
    private BulkClaimSubmissionRepository repository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private BulkClaimPaymentStatusUpdater underTest;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        underTest = new BulkClaimPaymentStatusUpdater(repository, transactionManager);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldRecordServiceRequestAndPendingWhenServiceRequestCreated() {
        BulkClaimSubmissionEntity submission = submission();
        when(repository.findByIdForUpdate(ID)).thenReturn(Optional.of(submission));

        underTest.onServiceRequestCreated(ID, SERVICE_REQUEST);

        assertThat(submission.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST);
        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PENDING);
        verify(repository).save(submission);
    }

    @Test
    void shouldRecordServiceRequestAndPendingWhenSubmitted() {
        BulkClaimSubmissionEntity submission = submission();
        when(repository.findByIdForUpdate(ID)).thenReturn(Optional.of(submission));

        underTest.onPbaPaymentSubmitted(ID, SERVICE_REQUEST);

        assertThat(submission.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST);
        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PENDING);
        verify(repository).save(submission);
    }

    @Test
    void shouldRecordFailureReason() {
        BulkClaimSubmissionEntity submission = submission();
        when(repository.findByIdForUpdate(ID)).thenReturn(Optional.of(submission));

        underTest.onPbaPaymentFailed(ID, SERVICE_REQUEST, "Account on hold");

        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.FAILED);
        assertThat(submission.getPaymentFailureReason()).isEqualTo("Account on hold");
        assertThat(submission.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST);
        verify(repository).save(submission);
    }

    @Test
    void shouldMarkPaidByServiceRequestReferenceWhenNoTransactionIsActive() {
        BulkClaimSubmissionEntity submission = submission();
        submission.setPaymentStatus(BulkClaimPaymentStatus.PENDING);
        when(repository.findByServiceRequestReferenceForUpdate(SERVICE_REQUEST))
            .thenReturn(Optional.of(submission));

        underTest.onPaymentConfirmed(SERVICE_REQUEST);

        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PAID);
        verify(repository).save(submission);
    }

    @Test
    void shouldDeferPaidUntilAfterCommitWhenTransactionIsActive() {
        BulkClaimSubmissionEntity submission = submission();
        submission.setPaymentStatus(BulkClaimPaymentStatus.PENDING);
        when(repository.findByServiceRequestReferenceForUpdate(SERVICE_REQUEST))
            .thenReturn(Optional.of(submission));
        TransactionSynchronizationManager.initSynchronization();

        underTest.onPaymentConfirmed(SERVICE_REQUEST);

        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PENDING);
        verify(repository, never()).save(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PAID);
    }

    @Test
    void shouldEndPaidWhenCallbackArrivesBeforePbaPaymentIsSubmitted() {
        BulkClaimSubmissionEntity submission = submission();
        when(repository.findByIdForUpdate(ID)).thenReturn(Optional.of(submission));
        when(repository.findByServiceRequestReferenceForUpdate(SERVICE_REQUEST))
            .thenReturn(Optional.of(submission));

        underTest.onServiceRequestCreated(ID, SERVICE_REQUEST);
        underTest.onPaymentConfirmed(SERVICE_REQUEST);
        underTest.onPbaPaymentSubmitted(ID, SERVICE_REQUEST);

        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PAID);
        assertThat(submission.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST);
    }

    @Test
    void shouldNeverMovePaidToFailed() {
        BulkClaimSubmissionEntity submission = submission();
        submission.setPaymentStatus(BulkClaimPaymentStatus.PAID);
        when(repository.findByIdForUpdate(ID)).thenReturn(Optional.of(submission));

        underTest.onPbaPaymentFailed(ID, SERVICE_REQUEST, "late failure");

        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PAID);
        assertThat(submission.getPaymentFailureReason()).isNull();
    }

    @Test
    void shouldMoveFailedToPaidWhenCallbackLaterConfirms() {
        BulkClaimSubmissionEntity submission = submission();
        submission.setPaymentStatus(BulkClaimPaymentStatus.FAILED);
        submission.setPaymentFailureReason("timed out");
        when(repository.findByServiceRequestReferenceForUpdate(SERVICE_REQUEST))
            .thenReturn(Optional.of(submission));

        underTest.onPaymentConfirmed(SERVICE_REQUEST);

        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PAID);
        assertThat(submission.getPaymentFailureReason()).isNull();
    }

    @Test
    void shouldNotMoveFailedBackToPending() {
        BulkClaimSubmissionEntity submission = submission();
        submission.setPaymentStatus(BulkClaimPaymentStatus.FAILED);
        when(repository.findByIdForUpdate(ID)).thenReturn(Optional.of(submission));

        underTest.onPbaPaymentSubmitted(ID, SERVICE_REQUEST);

        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.FAILED);
    }

    @Test
    void shouldSwallowFailureMarkingPaid() {
        when(repository.findByServiceRequestReferenceForUpdate(SERVICE_REQUEST))
            .thenThrow(new IllegalStateException("boom"));

        assertThatCode(() -> underTest.onPaymentConfirmed(SERVICE_REQUEST)).doesNotThrowAnyException();
    }

    @Test
    void shouldIgnoreUnknownSubmissionsAndNonBulkPayments() {
        when(repository.findByIdForUpdate(ID)).thenReturn(Optional.empty());
        when(repository.findByServiceRequestReferenceForUpdate(SERVICE_REQUEST)).thenReturn(Optional.empty());

        underTest.onServiceRequestCreated(ID, SERVICE_REQUEST);
        underTest.onPbaPaymentSubmitted(ID, SERVICE_REQUEST);
        underTest.onPbaPaymentFailed(null, SERVICE_REQUEST, "reason");
        underTest.onPaymentConfirmed(SERVICE_REQUEST);

        verify(repository, times(2)).findByIdForUpdate(ID);
        verify(repository).findByServiceRequestReferenceForUpdate(SERVICE_REQUEST);
        verifyNoMoreInteractions(repository);
    }

    private static BulkClaimSubmissionEntity submission() {
        return BulkClaimSubmissionEntity.builder().id(ID).build();
    }
}
