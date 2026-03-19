package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PaymentAgreement;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentAgreementServiceTest {

    private PaymentAgreementService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PaymentAgreementService();
    }

    @Test
    void shouldMapAllFields() {
        //Given
        PaymentAgreement paymentAgreement = PaymentAgreement.builder()
            .anyPaymentsMade(YesOrNo.YES)
            .paymentDetails("Some details")
            .paidMoneyToHousingOrg(YesOrNo.NO)
            .repaymentPlanAgreed(YesNoNotSure.YES)
            .repaymentAgreedDetails("Agreed to pay weekly")
            .repayArrearsInstalments(YesOrNo.YES)
            .additionalRentContribution(new java.math.BigDecimal("123.45"))
            .additionalContributionFrequency("WEEKLY")
            .build();

        //When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(paymentAgreement);

        //Then
        assertThat(entity.getAnyPaymentsMade()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getPaymentDetails()).isEqualTo("Some details");
        assertThat(entity.getPaidMoneyToHousingOrg()).isEqualTo(YesOrNo.NO);
        assertThat(entity.getRepaymentPlanAgreed()).isEqualTo(YesNoNotSure.YES);
        assertThat(entity.getRepaymentAgreedDetails()).isEqualTo("Agreed to pay weekly");
        assertThat(entity.getRepayArrearsInstalments()).isEqualTo(YesOrNo.YES);
        assertThat(entity.getAdditionalRentContribution()).isEqualByComparingTo("123.45");
        assertThat(entity.getAdditionalContributionFrequency()).isEqualTo("WEEKLY");
    }

    @Test
    void shouldReturnNullWhenPaymentAgreementIsNull() {
        // When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(null);

        // Then
        assertThat(entity).isNull();
    }

}

