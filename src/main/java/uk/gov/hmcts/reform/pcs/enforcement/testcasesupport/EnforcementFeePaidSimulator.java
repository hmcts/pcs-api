package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementPaymentCallbackHandler;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;

import java.util.Optional;

/**
 * Pays an enforcement application's fee the way a Payments callback does, for local stacks, which have no
 * Payments service to take it.
 *
 * <p>When the fees-and-pay task has raised the service request (locally against the wiremock stub), this sends
 * a "Paid" callback for it through {@link PaymentService#processPaymentResponse}, the same path
 * {@code PaymentCallBackController} takes. When there is no fee payment yet, because the task has not run or
 * could not reach Payments, or because the application was seeded by test case generation rather than
 * submitted, it hands {@link EnforcementPaymentCallbackHandler} an unsaved paid fee payment for the enforcement
 * instead. Either way the enforcement moves on through the real callback handler.</p>
 *
 * <p>The local wiremock stub for an enforcement fee's service request
 * ({@code wiremock/mappings/payment-api/enforcement-service-request.json}) deliberately sends no "Paid" webhook,
 * unlike the claim fee's, so an application stays awaiting payment until this is called.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
public class EnforcementFeePaidSimulator {

    private final PcsCaseService pcsCaseService;
    private final EnforcementOrderRepository enforcementOrderRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final PaymentService paymentService;
    private final EnforcementPaymentCallbackHandler enforcementPaymentCallbackHandler;

    /**
     * Pays the fee for the case's application that is awaiting payment.
     *
     * @return whether an application was awaiting payment
     */
    @Transactional
    public boolean recordFeePaid(long caseReference) {
        ClaimEntity claim = Optional.ofNullable(pcsCaseService.loadCase(caseReference).getMainClaim())
            .orElseThrow(() -> new ClaimNotFoundException(caseReference));
        Optional<EnforcementOrderEntity> awaitingPayment = enforcementOrderRepository
            .findByClaimIdAndStatus(claim.getId(), EnforcementStatus.AWAITING_PAYMENT)
            .stream()
            .findFirst();
        awaitingPayment.ifPresent(enforcement -> pay(caseReference, enforcement));
        return awaitingPayment.isPresent();
    }

    private void pay(long caseReference, EnforcementOrderEntity enforcement) {
        PaymentStatusCallback callback = PaymentStatusCallback.builder()
            .ccdCaseNumber(String.valueOf(caseReference))
            .serviceRequestStatus(PaymentStatus.PAID.getValue())
            .build();

        feePaymentRepository.findByRelatedEntityId(enforcement.getId())
            .filter(feePayment -> feePayment.getPaymentCallbackHandlerType() == PaymentCallbackHandlerType.ENFORCEMENT
                && feePayment.getServiceRequestReference() != null)
            .ifPresentOrElse(
                feePayment -> {
                    log.info("Sending a paid callback for service request {} on case {}",
                             feePayment.getServiceRequestReference(), caseReference);
                    callback.setServiceRequestReference(feePayment.getServiceRequestReference());
                    paymentService.processPaymentResponse(callback);
                },
                () -> {
                    log.info("No fee payment for enforcement {} on case {}, so paying it without a service request",
                             enforcement.getId(), caseReference);
                    enforcementPaymentCallbackHandler.handle(callback, FeePaymentEntity.builder()
                        .claim(enforcement.getClaim())
                        .paymentCallbackHandlerType(PaymentCallbackHandlerType.ENFORCEMENT)
                        .paymentStatus(PaymentStatus.PAID)
                        .relatedEntityId(enforcement.getId())
                        .build());
                });
    }
}
