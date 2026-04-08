package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PaymentAgreement;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;

@Service
public class PaymentAgreementService {

    public PaymentAgreementEntity createPaymentAgreementEntity(PaymentAgreement paymentAgreement) {

        if (paymentAgreement == null) {
            return null;
        }

        PaymentAgreementEntity paymentAgreementEntity = PaymentAgreementEntity.builder()
            .anyPaymentsMade(paymentAgreement.getAnyPaymentsMade())
            .repaymentPlanAgreed(paymentAgreement.getRepaymentPlanAgreed())
            .repaymentAgreedDetails(paymentAgreement.getRepaymentAgreedDetails())
            .build();

        return paymentAgreementEntity;
    }
}
