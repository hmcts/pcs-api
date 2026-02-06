package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.model.EnforcementCosts;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RepaymentTableRenderer;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeFormatter;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ.LandRegistryFeesWritPage.WRIT_FEE_AMOUNT;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ.LandRegistryFeesWritPage.TEMPLATE;

@ExtendWith(MockitoExtension.class)
class LandRegistryFeesWritPageTest extends BasePageTest {

    @Mock
    private RepaymentTableRenderer repaymentTableRenderer;
    @Mock
    private FeeFormatter feeFormatter;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new LandRegistryFeesWritPage(repaymentTableRenderer, feeFormatter));
    }

    @ParameterizedTest
    @MethodSource("repaymentFeeScenarios")
    void shouldFormatRepaymentFeesCorrectly(final EnforcementCosts enforcementCosts,
                                            final String expectedFormattedFee) {
        // Given
        final LegalCosts legalCosts = LegalCosts.builder()
                .amountOfLegalCosts(enforcementCosts.getLegalFees())
                .build();

        final LandRegistryFees landRegistryFees = LandRegistryFees.builder()
                .amountOfLandRegistryFees(enforcementCosts.getLandRegistryFees())
                .build();

        final MoneyOwedByDefendants moneyOwedByDefendants = MoneyOwedByDefendants.builder()
                .amountOwed(enforcementCosts.getTotalArrears())
                .build();

        final EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .writFeeAmount(expectedFormattedFee)
                .writDetails(WritDetails.builder()
                        .repaymentCosts(RepaymentCosts.builder().build())
                        .landRegistryFees(landRegistryFees)
                        .legalCosts(legalCosts)
                        .moneyOwedByDefendants(moneyOwedByDefendants)
                        .build())
                .build();

        final PCSCase caseData = PCSCase.builder()
                .enforcementOrder(enforcementOrder)
                .build();

        when(feeFormatter.deformatFee(caseData.getEnforcementOrder().getWritFeeAmount()))
                .thenReturn(enforcementCosts.getFeeAmount());
        when(repaymentTableRenderer.render(
                enforcementCosts,
                TEMPLATE
        )).thenReturn("<table>Mock Repayment Table</table>");
        when(repaymentTableRenderer.render(
                enforcementCosts,
                "The payments due",
                TEMPLATE
        )).thenReturn("<table>Mock SOT Repayment Table</table>");

        // When
        callMidEventHandler(caseData);

        // Then
        verify(repaymentTableRenderer).render(
                enforcementCosts,
                TEMPLATE
        );
        verify(repaymentTableRenderer).render(
                enforcementCosts,
                "The payments due",
                TEMPLATE
        );

        assertThat(caseData.getEnforcementOrder().getWritDetails().getRepaymentCosts().getRepaymentSummaryMarkdown())
                .isEqualTo("<table>Mock Repayment Table</table>");
        assertThat(caseData.getEnforcementOrder().getWritDetails().getRepaymentCosts()
                .getStatementOfTruthRepaymentSummaryMarkdown())
                .isEqualTo("<table>Mock SOT Repayment Table</table>");
    }

    private static Stream<Arguments> repaymentFeeScenarios() {
        return Stream.of(
                Arguments.of(
                        new EnforcementCosts(new BigDecimal("123"), new BigDecimal("100"), new BigDecimal("200"),
                                new BigDecimal("404"), WRIT_FEE_AMOUNT), "£404"),
                Arguments.of(
                        new EnforcementCosts(new BigDecimal("15"), new BigDecimal("5"), new BigDecimal("9.99"),
                                new BigDecimal(".50"), WRIT_FEE_AMOUNT), "£0.50"),
                Arguments.of(
                        new EnforcementCosts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, WRIT_FEE_AMOUNT), "£0"),
                Arguments.of(
                        new EnforcementCosts(new BigDecimal("100.01"), new BigDecimal("0.01"), new BigDecimal("50.00"),
                                BigDecimal.ZERO, WRIT_FEE_AMOUNT), "£0")
        );
    }
}
