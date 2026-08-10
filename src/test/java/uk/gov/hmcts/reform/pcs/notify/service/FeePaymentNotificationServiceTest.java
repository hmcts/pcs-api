package uk.gov.hmcts.reform.pcs.notify.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimDocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.exception.FeePaymentNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeePaymentNotificationServiceTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private FeePaymentRepository feePaymentRepository;
    @Mock
    private CamundaService camundaService;
    @Mock
    private TaskDescriptionService taskDescriptionService;

    @InjectMocks
    private FeePaymentNotificationService underTest;

    @Test
    void shouldSendClaimantClaimIssuedEmailNotification() {
        Integer feePaymentId = 1;
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(1234L).build();
        ClaimEntity claim = ClaimEntity.builder().pcsCase(pcsCaseEntity).languageUsed(LanguageUsed.ENGLISH).build();
        FeePaymentEntity feePayment = FeePaymentEntity.builder()
            .id(feePaymentId)
            .claim(claim)
            .build();
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));

        underTest.sendClaimantPaidCaseIssuedNotification(feePaymentId);

        verify(notificationService).sendClaimantClaimIssuedEmailNotification(claim);
        verify(camundaService).createTask(1234L, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    void shouldNotCreateHearingTaskWhenLanguageIsNotEnglish(LanguageUsed languageUsed) {
        Integer feePaymentId = 1;
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(1234L).build();
        ClaimEntity claim = ClaimEntity.builder().pcsCase(pcsCaseEntity).languageUsed(languageUsed).build();
        FeePaymentEntity feePayment = FeePaymentEntity.builder()
            .id(feePaymentId)
            .claim(claim)
            .build();
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));

        underTest.sendClaimantPaidCaseIssuedNotification(feePaymentId);

        verify(notificationService).sendClaimantClaimIssuedEmailNotification(claim);
        verify(camundaService, never()).createTask(1234L, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    void shouldNotCreateTranslateTaskWhenNoDocumentsExist(LanguageUsed languageUsed) {
        Integer feePaymentId = 1;
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(1234L).build();
        ClaimEntity claim = ClaimEntity.builder().pcsCase(pcsCaseEntity).languageUsed(languageUsed).build();
        FeePaymentEntity feePayment = FeePaymentEntity.builder()
            .id(feePaymentId)
            .claim(claim)
            .build();
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));

        underTest.sendClaimantPaidCaseIssuedNotification(feePaymentId);

        verifyNoInteractions(taskDescriptionService);
        verify(camundaService, never()).createTask(anyLong(), eq(TaskType.TRANSLATE_CLAIMANT_SUBMITTED_DOCUMENT),
            anyString());
    }

    @ParameterizedTest
    @EnumSource(value = LanguageUsed.class, names = {"WELSH", "ENGLISH_AND_WELSH"})
    void shouldCreateTranslateTaskWhenLanguageIsNotEnglishAndDocumentsExist(LanguageUsed languageUsed) {
        Integer feePaymentId = 1;
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(1234L).build();
        DocumentEntity documentEntity = DocumentEntity.builder().fileName("claim-form.pdf").build();
        ClaimDocumentEntity claimDocumentEntity = ClaimDocumentEntity.builder().document(documentEntity).build();
        ClaimEntity claim = ClaimEntity.builder()
            .pcsCase(pcsCaseEntity)
            .languageUsed(languageUsed)
            .claimDocuments(List.of(claimDocumentEntity))
            .build();
        FeePaymentEntity feePayment = FeePaymentEntity.builder()
            .id(feePaymentId)
            .claim(claim)
            .build();
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));

        String expectedDescription = "Translate the following documents: claim-form.pdf";
        when(taskDescriptionService.createTranslateClaimantDocumentDescription(1234L, List.of(documentEntity)))
            .thenReturn(expectedDescription);

        underTest.sendClaimantPaidCaseIssuedNotification(feePaymentId);

        verify(camundaService).createTask(
            1234L, TaskType.TRANSLATE_CLAIMANT_SUBMITTED_DOCUMENT, expectedDescription);
        verify(camundaService, never()).createTask(1234L, TaskType.NEW_CLAIM_CREATE_NEW_HEARING);
    }

    @Test
    void shouldNotCreateTranslateTaskWhenLanguageIsEnglish() {
        Integer feePaymentId = 1;
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(1234L).build();
        ClaimEntity claim = ClaimEntity.builder().pcsCase(pcsCaseEntity).languageUsed(LanguageUsed.ENGLISH).build();
        FeePaymentEntity feePayment = FeePaymentEntity.builder()
            .id(feePaymentId)
            .claim(claim)
            .build();
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.of(feePayment));

        underTest.sendClaimantPaidCaseIssuedNotification(feePaymentId);

        verifyNoInteractions(taskDescriptionService);
        verify(camundaService, never()).createTask(anyLong(), eq(TaskType.TRANSLATE_CLAIMANT_SUBMITTED_DOCUMENT),
                                                   anyString());
    }

    @Test
    void shouldThrowExceptionWhenFeePaymentNotFound() {
        Integer feePaymentId = 1;
        when(feePaymentRepository.findById(feePaymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.sendClaimantPaidCaseIssuedNotification(feePaymentId))
            .isInstanceOf(FeePaymentNotFoundException.class)
            .hasMessageContaining("Fee payment not found: " + feePaymentId);
    }
}
