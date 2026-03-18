package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PaymentAgreement;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PaymentAgreementServiceTest {

    private PaymentAgreementService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PaymentAgreementService();
    }

    @ParameterizedTest
    @MethodSource("paymentsMadeScenarios")
    void shouldMapAnyPaymentsMadeField(YesOrNo expected) {
        //Given
        PaymentAgreement paymentAgreement = PaymentAgreement.builder()
            .anyPaymentsMade(expected)
            .build();

        //When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(paymentAgreement);

        //Then
        assertThat(entity).isNotNull();
        assertThat(entity.getAnyPaymentsMade()).isEqualTo(expected);
    }

    private static Stream<Arguments> paymentsMadeScenarios() {
        return Stream.of(
            Arguments.of(YesOrNo.YES),
            Arguments.of(YesOrNo.NO),
            Arguments.of((YesOrNo) null)
        );
    }

    @Test
    void shouldReturnNullWhenPaymentAgreementIsNull() {
        // When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(null);

        // Then
        assertThat(entity).isNull();
    }

}

