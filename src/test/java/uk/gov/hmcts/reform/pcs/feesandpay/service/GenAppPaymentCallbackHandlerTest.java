package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppFormScheduler;
import uk.gov.hmcts.reform.pcs.exception.GenAppNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAppPaymentCallbackHandlerTest {

    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private GenAppFormScheduler genAppFormScheduler;
    @Mock
    private PaymentStatusCallback paymentStatusCallback;

    private GenAppPaymentCallbackHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppPaymentCallbackHandler(genAppRepository, genAppFormScheduler);
    }

    @Test
    void shouldIssueGenAppWhenNotAlreadyIssued() {
        // Given
        UUID genAppId = UUID.randomUUID();

        FeePaymentEntity feePaymentEntity = mock(FeePaymentEntity.class);
        when(feePaymentEntity.getPaymentStatus()).thenReturn(PaymentStatus.PAID);

        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppEntity.getState()).thenReturn(GenAppState.PENDING_GEN_APP_ISSUED);

        when(feePaymentEntity.getRelatedEntityId()).thenReturn(genAppId);
        when(genAppRepository.findById(genAppId)).thenReturn(Optional.of(genAppEntity));

        // When
        underTest.handle(paymentStatusCallback, feePaymentEntity);

        // Then
        verify(genAppFormScheduler).scheduleGenAppDocumentGeneration(genAppId);
        verify(genAppEntity).setState(GenAppState.GEN_APP_ISSUED);
    }

    @Test
    void shouldNotIssueGenAppIfAlreadyIssued() {
        // Given
        UUID genAppId = UUID.randomUUID();

        FeePaymentEntity feePaymentEntity = mock(FeePaymentEntity.class);
        when(feePaymentEntity.getPaymentStatus()).thenReturn(PaymentStatus.PAID);

        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppEntity.getState()).thenReturn(GenAppState.GEN_APP_ISSUED);

        when(feePaymentEntity.getRelatedEntityId()).thenReturn(genAppId);
        when(genAppRepository.findById(genAppId)).thenReturn(Optional.of(genAppEntity));

        // When
        underTest.handle(paymentStatusCallback, feePaymentEntity);

        // Then
        verifyNoInteractions(genAppFormScheduler);
    }

    @Test
    void shouldThrowExceptionForUnknownGenAppEntityId() {
        // Given
        UUID unknownGenAppId = UUID.randomUUID();

        FeePaymentEntity feePaymentEntity = mock(FeePaymentEntity.class);
        when(feePaymentEntity.getRelatedEntityId()).thenReturn(unknownGenAppId);
        when(feePaymentEntity.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
        when(genAppRepository.findById(unknownGenAppId)).thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> underTest.handle(paymentStatusCallback, feePaymentEntity));

        // Then
        assertThat(throwable).isInstanceOf(GenAppNotFoundException.class);
    }

}
