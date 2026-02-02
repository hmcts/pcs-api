package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RepaymentTableRenderer;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
    void shouldRenderTableCorrectly(String warrantFeeAmount, BigDecimal landRegistryAmount,
                                            BigDecimal legalCostsAmount, BigDecimal rentArrearsAmount,
                                            BigDecimal expectedTotalFees
    ) {
        // Given
        LegalCosts legalCosts = LegalCosts.builder()
            .amountOfLegalCosts(legalCostsAmount)
            .build();

        LandRegistryFees landRegistryFees = LandRegistryFees.builder()
            .amountOfLandRegistryFees(landRegistryAmount)
            .build();

        MoneyOwedByDefendants moneyOwedByDefendants = MoneyOwedByDefendants.builder()
            .amountOwed(rentArrearsAmount)
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantFeeAmount(warrantFeeAmount)
            .warrantDetails(WarrantDetails.builder()
                                .repaymentCosts(RepaymentCosts.builder().build())
                                .landRegistryFees(landRegistryFees)
                                .legalCosts(legalCosts)
                                .moneyOwedByDefendants(moneyOwedByDefendants)
                                .build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(enforcementOrder)
            .build();

        when(repaymentTableRenderer.render(
            rentArrearsAmount,
            legalCostsAmount,
            landRegistryAmount,
            warrantFeeAmount,
            expectedTotalFees
        )).thenReturn("<table>Mock Repayment Table</table>");
        when(repaymentTableRenderer.render(
            rentArrearsAmount,
            legalCostsAmount,
            landRegistryAmount,
            warrantFeeAmount,
            expectedTotalFees,
            "The payments due"
        )).thenReturn("<table>Mock SOT Repayment Table</table>");

        // When
        callMidEventHandler(caseData);

        // Then
        verify(repaymentTableRenderer).render(
            rentArrearsAmount,
            legalCostsAmount,
            landRegistryAmount,
            warrantFeeAmount,
            expectedTotalFees
        );
        verify(repaymentTableRenderer).render(
            rentArrearsAmount,
            legalCostsAmount,
            landRegistryAmount,
            warrantFeeAmount,
            expectedTotalFees,
            "The payments due"
        );

        assertThat(caseData.getEnforcementOrder().getWarrantDetails().getRepaymentCosts().getRepaymentSummaryMarkdown())
            .isEqualTo("<table>Mock Repayment Table</table>");
        assertThat(caseData.getEnforcementOrder().getWarrantDetails().getRepaymentCosts()
                       .getStatementOfTruthRepaymentSummaryMarkdown())
            .isEqualTo("<table>Mock SOT Repayment Table</table>");
    }

    private static Stream<Arguments> repaymentFeeScenarios() {
        return Stream.of(
            Arguments.of(
              "£404", new BigDecimal("123.00"),
                new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("827.00")
            ),
            Arguments.of(
                "£50", new BigDecimal("15.00"), new BigDecimal("5.00"),
                new BigDecimal("9.99"), new BigDecimal("79.99")
            ),
            Arguments.of(
                "£0", new BigDecimal("0.00"), new BigDecimal("0.00"),
                new BigDecimal("0.00"), new BigDecimal("0.00")
            ),
            Arguments.of(
                "£0", new BigDecimal("100.01"), new BigDecimal("0.01"),
                new BigDecimal("50.00"), new BigDecimal("150.02")
            )
        );
    }
}
