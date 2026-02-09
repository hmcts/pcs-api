package uk.gov.hmcts.reform.pcs.ccd.renderer;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementCosts;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.LandRegistryFeesPage.WARRANT_FEE_AMOUNT;

@ExtendWith(MockitoExtension.class)
class RepaymentTableHelperTest {

    @Mock
    private MoneyFormatter moneyFormatter;

    @InjectMocks
    private RepaymentTableHelper repaymentTableHelper;

    @ParameterizedTest
    @MethodSource("repaymentFeeScenarios")
    void getContext_shouldBuildMapAndCallConverter(EnforcementCosts enforcementCosts, String expectedFormattedFee,
                                                   BigDecimal expectedTotalFees) {

        when(moneyFormatter.formatFee(enforcementCosts.getFeeAmount())).thenReturn(expectedFormattedFee);

        Map<String, Object> context = repaymentTableHelper.getContext(enforcementCosts, "My caption");

        assertThat(context)
                .containsEntry("totalArrears", enforcementCosts.getTotalArrears())
                .containsEntry("legalFees", enforcementCosts.getLegalFees())
                .containsEntry("landRegistryFees", enforcementCosts.getLandRegistryFees())
                .containsEntry("warrantFeeAmount", expectedFormattedFee)
                .containsEntry("totalFees", expectedTotalFees)
                .containsEntry("caption", "My caption");
    }

    private static Stream<Arguments> repaymentFeeScenarios() {
        return Stream.of(
                arguments(
                        new EnforcementCosts(new BigDecimal("123"), new BigDecimal("100"), new BigDecimal("200.00"),
                        new BigDecimal("404"), WARRANT_FEE_AMOUNT), "£404", new BigDecimal("827.00")
                ),
                arguments(
                        new EnforcementCosts(new BigDecimal("15.00"), new BigDecimal("5.00"), new BigDecimal("9.99"),
                        new BigDecimal("50.00"), WARRANT_FEE_AMOUNT), "£50", new BigDecimal("79.99")
                ),
                arguments(
                        new EnforcementCosts(new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                        new BigDecimal("0.00"), WARRANT_FEE_AMOUNT), "£0", new BigDecimal("0.00")
                ),
                arguments(
                        new EnforcementCosts(new BigDecimal("100.01"), new BigDecimal("0.01"), new BigDecimal("50.00"),
                        new BigDecimal("0.00"), WARRANT_FEE_AMOUNT), "£0", new BigDecimal("150.02")
                )
        );
    }
}