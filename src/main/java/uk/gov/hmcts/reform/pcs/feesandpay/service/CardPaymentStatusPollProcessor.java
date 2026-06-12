package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PayHubCardPaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.Payment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.util.Optional;

@Slf4j
@Component
public class CardPaymentStatusPollProcessor {

    private final FeePaymentRepository feePaymentRepository;
    private final ObjectProvider<PaymentService> paymentServiceProvider;

    public CardPaymentStatusPollProcessor(FeePaymentRepository feePaymentRepository,
                                          ObjectProvider<PaymentService> paymentServiceProvider) {
        this.feePaymentRepository = feePaymentRepository;
        this.paymentServiceProvider = paymentServiceProvider;
    }

    public void processIfPaid(PaymentDto paymentDto) {
        if (!PayHubCardPaymentStatus.isSuccessful(paymentDto.getStatus())) {
            return;
        }

        String serviceRequestReference = paymentDto.getPaymentGroupReference();
        if (serviceRequestReference == null || serviceRequestReference.isBlank()) {
            log.warn("Successful card payment status poll missing paymentGroupReference");
            return;
        }

        Optional<FeePaymentEntity> feePayment = feePaymentRepository
            .findByServiceRequestReference(serviceRequestReference);
        if (feePayment.isEmpty()) {
            log.warn("No fee payment found for service request {}", serviceRequestReference);
            return;
        }

        if (PaymentStatus.PAID == feePayment.get().getPaymentStatus()) {
            log.debug("Fee payment already PAID for service request {}", serviceRequestReference);
            return;
        }

        PaymentStatusCallback callback = PaymentStatusCallback.builder()
            .serviceRequestReference(serviceRequestReference)
            .ccdCaseNumber(paymentDto.getCcdCaseNumber())
            .serviceRequestAmount(paymentDto.getAmount())
            .serviceRequestStatus(PaymentStatus.PAID.getValue())
            .payment(Payment.builder()
                .paymentReference(resolvePaymentReference(paymentDto))
                .paymentAmount(paymentDto.getAmount())
                .caseReference(paymentDto.getCaseReference())
                .build())
            .build();

        log.info("Processing paid card payment from status poll for service request {}", serviceRequestReference);
        paymentServiceProvider.getObject().processPaymentResponse(callback);
    }

    private static String resolvePaymentReference(PaymentDto paymentDto) {
        if (paymentDto.getReference() != null && !paymentDto.getReference().isBlank()) {
            return paymentDto.getReference();
        }
        if (paymentDto.getPaymentReference() != null && !paymentDto.getPaymentReference().isBlank()) {
            return paymentDto.getPaymentReference();
        }
        return paymentDto.getExternalReference();
    }
}
