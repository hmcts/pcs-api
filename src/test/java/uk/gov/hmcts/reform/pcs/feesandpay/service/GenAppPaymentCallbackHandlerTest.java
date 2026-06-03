package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.exception.GenAppNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenAppPaymentCallbackHandlerTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private GenAppRepository genAppRepository;
    @Mock
    private GenAppDocumentGenerator genAppDocumentGenerator;

    private GenAppPaymentCallbackHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppPaymentCallbackHandler(genAppRepository, genAppDocumentGenerator);
    }

    @Test
    void shouldIssueGenAppIfNotAlreadyIssued() {
        // Given
        UUID genAppId = UUID.randomUUID();

        FeePaymentEntity feePaymentEntity = mock(FeePaymentEntity.class);
        when(feePaymentEntity.getPaymentStatus()).thenReturn(PaymentStatus.PAID);

        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(genAppEntity.getPcsCase()).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getCaseReference()).thenReturn(CASE_REFERENCE);
        when(genAppEntity.getState()).thenReturn(GenAppState.PENDING_GEN_APP_ISSUED);

        when(feePaymentEntity.getRelatedEntityId()).thenReturn(genAppId);
        when(genAppRepository.findById(genAppId)).thenReturn(Optional.of(genAppEntity));

        // When
        underTest.handle(feePaymentEntity);

        // Then
        verify(genAppDocumentGenerator).createSubmissionDocument(CASE_REFERENCE, genAppEntity);
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
        underTest.handle(feePaymentEntity);

        // Then
        verify(genAppDocumentGenerator, never()).createSubmissionDocument(CASE_REFERENCE, genAppEntity);
    }

    @Test
    void shouldThrowExceptionForUnknownGenAppEntityId() {
        // Given
        UUID unknownGenAppId = UUID.randomUUID();

        FeePaymentEntity feePaymentEntity = mock(FeePaymentEntity.class);
        when(feePaymentEntity.getRelatedEntityId()).thenReturn(unknownGenAppId);
        when(genAppRepository.findById(unknownGenAppId)).thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> underTest.handle(feePaymentEntity));

        // Then
        assertThat(throwable).isInstanceOf(GenAppNotFoundException.class);
    }

}
