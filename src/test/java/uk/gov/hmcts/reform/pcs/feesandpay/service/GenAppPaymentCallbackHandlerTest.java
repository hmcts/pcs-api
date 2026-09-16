package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.exception.GenAppNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

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

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private PaymentStatusCallback paymentStatusCallback;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CcdPaymentStateUpdateService ccdPaymentStateUpdateService;

    private GenAppPaymentCallbackHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppPaymentCallbackHandler(genAppRepository, notificationService,
                                                      ccdPaymentStateUpdateService);
    }

    @Test
    void shouldSubmitGenAppPaymentSuccessWhenPaid() {
        // Given
        UUID genAppId = UUID.randomUUID();

        FeePaymentEntity feePaymentEntity = mock(FeePaymentEntity.class);
        when(feePaymentEntity.getPaymentStatus()).thenReturn(PaymentStatus.PAID);
        when(feePaymentEntity.getRelatedEntityId()).thenReturn(genAppId);

        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(genAppEntity.getPcsCase()).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getCaseReference()).thenReturn(CASE_REFERENCE);
        when(genAppRepository.findById(genAppId)).thenReturn(Optional.of(genAppEntity));

        // When
        underTest.handle(paymentStatusCallback, feePaymentEntity);

        // Then
        verify(notificationService).sendGenAppReceivedEmail(genAppEntity);
        verify(ccdPaymentStateUpdateService).submitGenAppPaymentSuccess(CASE_REFERENCE, genAppId);
    }

    @Test
    void shouldNotSubmitGenAppPaymentSuccessWhenNotPaid() {
        // Given
        FeePaymentEntity feePaymentEntity = mock(FeePaymentEntity.class);
        when(feePaymentEntity.getPaymentStatus()).thenReturn(PaymentStatus.NOT_PAID);

        // When
        underTest.handle(paymentStatusCallback, feePaymentEntity);

        // Then
        verifyNoInteractions(notificationService, ccdPaymentStateUpdateService);
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
