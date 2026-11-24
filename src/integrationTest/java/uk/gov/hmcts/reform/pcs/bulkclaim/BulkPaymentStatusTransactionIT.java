package uk.gov.hmcts.reform.pcs.bulkclaim;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimKind;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimPaymentStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.feesandpay.service.BulkPaymentStatusListener;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentCallbackStrategy;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentCallbackStrategyFactory;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

/**
 * Runs against the real transaction manager (no test-managed transaction) so that commit ordering and
 * rollback-only behaviour are exercised.
 */
@SpringBootTest
@ActiveProfiles("integration")
class BulkPaymentStatusTransactionIT extends AbstractPostgresContainerIT {

    private static final long CASE_REFERENCE = 1781000000000777L;

    @MockitoBean
    private IdamUserInfoApi idamUserInfoApi;
    @MockitoBean
    private PaymentCallbackStrategyFactory paymentCallbackStrategyFactory;
    @MockitoSpyBean
    private BulkClaimSubmissionRepository submissionRepository;

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BulkPaymentStatusListener bulkPaymentStatusListener;
    @Autowired
    private FeePaymentRepository feePaymentRepository;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;

    private String serviceRequestReference;
    private UUID submissionId;
    private UUID caseId;

    @BeforeEach
    void setUp() {
        serviceRequestReference = "SR-" + UUID.randomUUID();

        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(CASE_REFERENCE);
        ClaimEntity claim = ClaimEntity.builder().build();
        caseEntity.addClaim(claim);
        caseEntity = pcsCaseRepository.save(caseEntity);
        caseId = caseEntity.getId();

        feePaymentRepository.save(FeePaymentEntity.builder()
            .claim(caseEntity.getClaims().getFirst())
            .serviceRequestReference(serviceRequestReference)
            .amount(new BigDecimal("123.40"))
            .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM)
            .taskData("{}")
            .build());

        submissionId = submissionRepository.save(BulkClaimSubmissionEntity.builder()
            .organisationId("ORG-" + UUID.randomUUID())
            .customerUniqueIdentifier("CUST-1")
            .claimKind(BulkClaimKind.RENTAL)
            .requestHash("hash")
            .status(BulkClaimSubmissionStatus.CASE_CREATED)
            .build()).getId();

        PaymentCallbackStrategy strategy = Mockito.mock(PaymentCallbackStrategy.class);
        doAnswer(invocation -> {
            bulkPaymentStatusListener.onPaymentConfirmed(serviceRequestReference);
            return null;
        }).when(strategy).handle(any(), any());
        Mockito.when(paymentCallbackStrategyFactory.getStrategy(any())).thenReturn(strategy);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(submissionRepository);
        submissionRepository.deleteById(submissionId);
        feePaymentRepository.findByServiceRequestReference(serviceRequestReference)
            .ifPresent(feePaymentRepository::delete);
        pcsCaseRepository.deleteById(caseId);
    }

    @Test
    void callbackBeforePbaPaymentSubmittedEndsPaid() {
        bulkPaymentStatusListener.onServiceRequestCreated(submissionId, serviceRequestReference);

        paymentService.processPaymentResponse(callback(PaymentStatus.PAID));
        bulkPaymentStatusListener.onPbaPaymentSubmitted(submissionId, serviceRequestReference);

        BulkClaimSubmissionEntity submission = submissionRepository.findById(submissionId).orElseThrow();
        assertThat(submission.getPaymentStatus()).isEqualTo(BulkClaimPaymentStatus.PAID);
        assertThat(submission.getServiceRequestReference()).isEqualTo(serviceRequestReference);
    }

    @Test
    void failureOfBulkStatusUpdateDoesNotRollBackThePaymentCallback() {
        bulkPaymentStatusListener.onServiceRequestCreated(submissionId, serviceRequestReference);
        doThrow(new IllegalStateException("boom")).when(submissionRepository)
            .findByServiceRequestReferenceForUpdate(serviceRequestReference);

        assertThatCode(() -> paymentService.processPaymentResponse(callback(PaymentStatus.PAID)))
            .doesNotThrowAnyException();

        FeePaymentEntity feePayment = feePaymentRepository.findByServiceRequestReference(serviceRequestReference)
            .orElseThrow();
        assertThat(feePayment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(submissionRepository.findById(submissionId).orElseThrow().getPaymentStatus())
            .isEqualTo(BulkClaimPaymentStatus.PENDING);
    }

    @Test
    void serviceRequestReferenceIsUnique() {
        bulkPaymentStatusListener.onServiceRequestCreated(submissionId, serviceRequestReference);
        BulkClaimSubmissionEntity duplicate = BulkClaimSubmissionEntity.builder()
            .organisationId("ORG-" + UUID.randomUUID())
            .customerUniqueIdentifier("CUST-2")
            .claimKind(BulkClaimKind.RENTAL)
            .requestHash("hash")
            .status(BulkClaimSubmissionStatus.CASE_CREATED)
            .serviceRequestReference(serviceRequestReference)
            .build();

        assertThatThrownBy(() -> submissionRepository.saveAndFlush(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private PaymentStatusCallback callback(PaymentStatus status) {
        return PaymentStatusCallback.builder()
            .serviceRequestReference(serviceRequestReference)
            .ccdCaseNumber(String.valueOf(CASE_REFERENCE))
            .serviceRequestStatus(status.getValue())
            .build();
    }
}
