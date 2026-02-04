package uk.gov.hmcts.reform.pcs.ccd.renderer;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementCosts;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.LandRegistryFeesPage.WARRANT_FEE_AMOUNT;

@ExtendWith(MockitoExtension.class)
class RepaymentTableHelperTest {

    @Mock
    private MoneyConverter moneyConverter;

    @Mock
    private FeeFormatter feeFormatter;

    @InjectMocks
    private RepaymentTableHelper repaymentTableHelper;

    private static Stream<Arguments> repaymentFeeScenarios() {
        return Stream.of(
                Arguments.of(
                        new EnforcementCosts("12300", "10000", "20000", "404", WARRANT_FEE_AMOUNT), "40400", "82700",
                        new BigDecimal("123.00"), new BigDecimal("100.00"), new BigDecimal("200.00"),
                        new BigDecimal("404.00"), new BigDecimal("827.00"), "£404"),
                Arguments.of(
                        new EnforcementCosts("1500", "500", "999", "50", WARRANT_FEE_AMOUNT), "5000", "7999",
                        new BigDecimal("15.00"), new BigDecimal("5.00"), new BigDecimal("9.99"),
                        new BigDecimal("50"), new BigDecimal("79.99"), "£50"),
                Arguments.of(
                        new EnforcementCosts("0", "0", "0", "0", WARRANT_FEE_AMOUNT), "0", "0",
                        new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                        new BigDecimal("0.00"), new BigDecimal("0.00"), "£0"),
                Arguments.of(
                        new EnforcementCosts("10001", "1", "5000", "0", WARRANT_FEE_AMOUNT), "0", "15002",
                        new BigDecimal("100.01"), new BigDecimal("0.01"), new BigDecimal("50.00"),
                        new BigDecimal("0.00"), new BigDecimal("150.02"), "£0")
        );
    }

    @ParameterizedTest
    @MethodSource("repaymentFeeScenarios")
    void getContext_shouldBuildMapAndCallConverter(EnforcementCosts enforcementCosts, String expectedFeePence,
                                                   String expectedTotalsPence, BigDecimal expectedTotalArrears,
                                                   BigDecimal expectedLegals, BigDecimal expectedLandRegistry,
                                                   BigDecimal expectedFee, BigDecimal expectedTotalFees,
                                                   String expectedFormattedFee) {
        when(moneyConverter.convertPoundsToPence(enforcementCosts.getFeeAmount()))
                .thenReturn(expectedFeePence);
        when(moneyConverter.getTotalPence(enforcementCosts.getTotalArrearsPence(),
                enforcementCosts.getLegalFeesPence(), enforcementCosts.getLandRegistryFeesPence(),
                expectedFeePence)).thenReturn(expectedTotalsPence);
        when(moneyConverter.convertPenceToBigDecimal(enforcementCosts.getTotalArrearsPence()))
                .thenReturn(expectedTotalArrears);
        when(moneyConverter.convertPenceToBigDecimal(enforcementCosts.getLegalFeesPence()))
                .thenReturn(expectedLegals);
        when(moneyConverter.convertPenceToBigDecimal(enforcementCosts.getLandRegistryFeesPence()))
                .thenReturn(expectedLandRegistry);
        when(moneyConverter.convertPenceToBigDecimal(expectedFeePence))
                .thenReturn(expectedFee);
        when(moneyConverter.convertPenceToBigDecimal(expectedTotalsPence)).thenReturn(expectedTotalFees);
        when(feeFormatter.formatFee(expectedFee)).thenReturn(expectedFormattedFee);

        Map<String, Object> context = repaymentTableHelper.getContext(enforcementCosts, "My caption");

        assertThat(context)
                .containsEntry("totalArrears", expectedTotalArrears)
                .containsEntry("legalFees", expectedLegals)
                .containsEntry("landRegistryFees", expectedLandRegistry)
                .containsEntry("warrantFeeAmount", expectedFormattedFee)
                .containsEntry("totalFees", expectedTotalFees)
                .containsEntry("caption", "My caption");
    }
}