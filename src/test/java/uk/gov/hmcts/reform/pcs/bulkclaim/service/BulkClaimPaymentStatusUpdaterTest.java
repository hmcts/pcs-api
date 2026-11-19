package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimPaymentStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkClaimPaymentStatusUpdaterTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String SERVICE_REQUEST = "2026-1234567890";

    @Mock
    private BulkClaimSubmissionRepository repository;
    @InjectMocks
    private BulkClaimPaymentStatusUpdater underTest;

    @Test
    void shouldRecordServiceRequestAndPendingWhenSubmitted() {
        BulkClaimSubmissionEntity submission = submission();
        when(repository.findById(ID)).thenReturn(Optional.of(submission));

        underTest.onPbaPaymentSubmitted(ID, SERVICE_REQUEST);

        assertThat(submission.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST);
        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PENDING);
        verify(repository).save(submission);
    }

    @Test
    void shouldRecordFailureReason() {
        BulkClaimSubmissionEntity submission = submission();
        when(repository.findById(ID)).thenReturn(Optional.of(submission));

        underTest.onPbaPaymentFailed(ID, SERVICE_REQUEST, "Account on hold");

        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.FAILED);
        assertThat(submission.getPaymentFailureReason()).isEqualTo("Account on hold");
        assertThat(submission.getServiceRequestReference()).isEqualTo(SERVICE_REQUEST);
        verify(repository).save(submission);
    }

    @Test
    void shouldMarkPaidByServiceRequestReference() {
        BulkClaimSubmissionEntity submission = submission();
        submission.setPaymentStatus(BulkClaimPaymentStatus.PENDING);
        when(repository.findByServiceRequestReference(SERVICE_REQUEST)).thenReturn(Optional.of(submission));

        underTest.onPaymentConfirmed(SERVICE_REQUEST);

        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PAID);
        verify(repository).save(submission);
    }

    @Test
    void shouldIgnoreUnknownSubmissionsAndNonBulkPayments() {
        when(repository.findById(ID)).thenReturn(Optional.empty());
        when(repository.findByServiceRequestReference(SERVICE_REQUEST)).thenReturn(Optional.empty());

        underTest.onPbaPaymentSubmitted(ID, SERVICE_REQUEST);
        underTest.onPbaPaymentFailed(null, SERVICE_REQUEST, "reason");
        underTest.onPaymentConfirmed(SERVICE_REQUEST);

        verify(repository).findById(ID);
        verify(repository).findByServiceRequestReference(SERVICE_REQUEST);
        verifyNoMoreInteractions(repository);
    }

    private static BulkClaimSubmissionEntity submission() {
        return BulkClaimSubmissionEntity.builder().id(ID).build();
    }
}
