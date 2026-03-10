package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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
    void shouldMapAnyPaymentsMadeField() {
        //Given
        PaymentAgreement model = PaymentAgreement.builder()
            .anyPaymentsMade(YesOrNo.YES)
            .build();

        //When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(model);

        //Then
        assertThat(entity.getAnyPaymentsMade()).isEqualTo(YesOrNo.YES);
    }

}

