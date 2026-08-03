package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PaymentAgreement;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;

import java.math.BigDecimal;
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
    void shouldMapAnyPaymentsMadeField(VerticalYesNo expected) {
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
            Arguments.of(VerticalYesNo.YES),
            Arguments.of(VerticalYesNo.NO),
            Arguments.of((VerticalYesNo) null)
        );
    }

    @Test
    void shouldReturnNullWhenPaymentAgreementIsNull() {
        // When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(null);

        // Then
        assertThat(entity).isNull();
    }

    @ParameterizedTest
    @MethodSource("repaymentPlanAgreedScenarios")
    void shouldMapRepaymentPlanAgreedField(YesNoNotSure expected) {
        // Given
        PaymentAgreement model = PaymentAgreement.builder()
                .repaymentPlanAgreed(expected)
                .build();

        // When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(model);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getRepaymentPlanAgreed()).isEqualTo(expected);
    }

    private static Stream<Arguments> repaymentPlanAgreedScenarios() {
        return Stream.of(
                Arguments.of(YesNoNotSure.YES),
                Arguments.of(YesNoNotSure.NO),
                Arguments.of(YesNoNotSure.NOT_SURE),
                Arguments.of((YesNoNotSure) null)
        );
    }

    @ParameterizedTest
    @MethodSource("repaymentAgreedDetailsScenarios")
    void shouldMapRepaymentAgreedDetailsField(String expected) {
        // Given
        PaymentAgreement model = PaymentAgreement.builder()
                .repaymentAgreedDetails(expected)
                .build();

        // When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(model);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getRepaymentAgreedDetails()).isEqualTo(expected);
    }

    private static Stream<Arguments> repaymentAgreedDetailsScenarios() {
        return Stream.of(
                Arguments.of("Monthly installments"),
                Arguments.of(""),
                Arguments.of((String) null)
        );
    }

    @ParameterizedTest
    @MethodSource("paymentDetailsScenarios")
    void shouldMapPaymentDetailsField(String expected) {
        // Given
        PaymentAgreement model = PaymentAgreement.builder()
                .paymentDetails(expected)
                .build();

        // When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(model);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getPaymentDetails()).isEqualTo(expected);
    }

    private static Stream<Arguments> paymentDetailsScenarios() {
        return Stream.of(
                Arguments.of("Paid £500 on 01/01/2024"),
                Arguments.of(""),
                Arguments.of((String) null)
        );
    }

    @ParameterizedTest
    @MethodSource("repayArrearsInstalmentsScenarios")
    void shouldMapRepayArrearsInstalmentsField(VerticalYesNo expected) {
        // Given
        PaymentAgreement model = PaymentAgreement.builder()
                .repayArrearsInstalments(expected)
                .build();

        // When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(model);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getRepayArrearsInstalments()).isEqualTo(expected);
    }

    private static Stream<Arguments> repayArrearsInstalmentsScenarios() {
        return Stream.of(
                Arguments.of(VerticalYesNo.YES),
                Arguments.of(VerticalYesNo.NO),
                Arguments.of((VerticalYesNo) null)
        );
    }

    @ParameterizedTest
    @MethodSource("additionalRentContributionScenarios")
    void shouldMapAdditionalRentContributionField(BigDecimal expected) {
        // Given
        PaymentAgreement model = PaymentAgreement.builder()
                .additionalRentContribution(expected)
                .build();

        // When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(model);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getAdditionalRentContribution()).isEqualTo(expected);
    }

    private static Stream<Arguments> additionalRentContributionScenarios() {
        return Stream.of(
                Arguments.of(new BigDecimal("150000")),
                Arguments.of(BigDecimal.ZERO),
                Arguments.of((BigDecimal) null)
        );
    }

    @ParameterizedTest
    @MethodSource("additionalContributionFrequencyScenarios")
    void shouldMapAdditionalContributionFrequencyField(String expected) {
        // Given
        PaymentAgreement model = PaymentAgreement.builder()
                .additionalContributionFrequency(expected)
                .build();

        // When
        PaymentAgreementEntity entity = underTest.createPaymentAgreementEntity(model);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getAdditionalContributionFrequency()).isEqualTo(expected);
    }

    private static Stream<Arguments> additionalContributionFrequencyScenarios() {
        return Stream.of(
                Arguments.of("weekly"),
                Arguments.of("monthly"),
                Arguments.of(""),
                Arguments.of((String) null)
        );
    }

}

