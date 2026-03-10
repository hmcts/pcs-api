package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PaymentAgreement;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentAgreementServiceTest {

    private final PaymentAgreementService service = new PaymentAgreementService();

    @Test
    void shouldMapAnyPaymentsMadeField() {
        PaymentAgreement model = PaymentAgreement.builder()
            .anyPaymentsMade(YesOrNo.YES)
            .build();

        PaymentAgreementEntity entity = service.createPaymentAgreementEntity(model);

        assertThat(entity.getAnyPaymentsMade()).isEqualTo(YesOrNo.YES);
    }

}

