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

        return PaymentAgreementEntity.builder()
            .anyPaymentsMade(paymentAgreement.getAnyPaymentsMade())
            .paymentDetails(paymentAgreement.getPaymentDetails())
            .paidMoneyToHousingOrg(paymentAgreement.getPaidMoneyToHousingOrg())
            .repaymentPlanAgreed(paymentAgreement.getRepaymentPlanAgreed())
            .repaymentAgreedDetails(paymentAgreement.getRepaymentAgreedDetails())
            .repayArrearsInstalments(paymentAgreement.getRepayArrearsInstalments())
            .additionalRentContribution(paymentAgreement.getAdditionalRentContribution())
            .additionalContributionFrequency(paymentAgreement.getAdditionalContributionFrequency())
            .build();
    }
}
