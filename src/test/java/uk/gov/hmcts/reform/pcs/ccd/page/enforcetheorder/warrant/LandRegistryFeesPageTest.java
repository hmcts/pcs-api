package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RepaymentTableRenderer;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandRegistryFeesPageTest extends BasePageTest {

    @Mock
    private RepaymentTableRenderer repaymentTableRenderer;

    @BeforeEach
    void setUp() {
        MoneyConverter moneyConverter = new MoneyConverter();
        setPageUnderTest(new LandRegistryFeesPage(moneyConverter, repaymentTableRenderer));
    }

    @ParameterizedTest
    @MethodSource("repaymentFeeScenarios")
    void shouldFormatRepaymentFeesCorrectly(String landRegistryPence, String legalCostsPence, String rentArrearsPence,
                                            String warrantFeeAmount, BigDecimal expectedLandRegistry,
                                            BigDecimal expectedLegals, BigDecimal expectedArrears,
                                            BigDecimal expectedTotalFees
    ) {
        // Given
        LegalCosts legalCosts = LegalCosts.builder()
            .amountOfLegalCosts(legalCostsPence)
            .build();

        LandRegistryFees landRegistryFees = LandRegistryFees.builder()
            .amountOfLandRegistryFees(landRegistryPence)
            .build();

        MoneyOwedByDefendants moneyOwedByDefendants = MoneyOwedByDefendants.builder()
            .amountOwed(rentArrearsPence)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .repaymentCosts(RepaymentCosts.builder().build())
            .landRegistryFees(landRegistryFees)
            .legalCosts(legalCosts)
            .warrantFeeAmount(warrantFeeAmount)
            .moneyOwedByDefendants(moneyOwedByDefendants)
            .build();

        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(enforcementOrder)
            .build();

        when(repaymentTableRenderer.render(
            expectedArrears,
            expectedLegals,
            expectedLandRegistry,
            warrantFeeAmount,
            expectedTotalFees
        )).thenReturn("<table>Mock Repayment Table</table>");

        // When
        callMidEventHandler(caseData);

        // Then
        verify(repaymentTableRenderer).render(
            expectedArrears,
            expectedLegals,
            expectedLandRegistry,
            warrantFeeAmount,
            expectedTotalFees
        );

        assertThat(caseData.getEnforcementOrder().getRepaymentCosts().getRepaymentSummaryMarkdown())
            .isEqualTo("<table>Mock Repayment Table</table>");
    }

    private static Stream<Arguments> repaymentFeeScenarios() {
        return Stream.of(
            Arguments.of(
                "12300", "10000", "20000", "£404", new BigDecimal("123.00"),
                new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("827.00")
            ),
            Arguments.of(
                "1500", "500", "999", "£50", new BigDecimal("15.00"), new BigDecimal("5.00"),
                new BigDecimal("9.99"), new BigDecimal("79.99")
            ),
            Arguments.of(
                "0", "0", "0", "£0", new BigDecimal("0.00"), new BigDecimal("0.00"),
                new BigDecimal("0.00"), new BigDecimal("0.00")
            ),
            Arguments.of(
                "10001", "1", "5000", "£0", new BigDecimal("100.01"), new BigDecimal("0.01"),
                new BigDecimal("50.00"), new BigDecimal("150.02")
            )
        );
    }
}
