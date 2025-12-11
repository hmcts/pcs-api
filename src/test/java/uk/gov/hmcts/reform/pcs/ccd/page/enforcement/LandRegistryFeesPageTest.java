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

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
    void shouldFormatRepaymentFeesCorrectly(String landRegistryPence, String legalCostsPence,
                                            String rentArrearsPence, String warrantFeeAmount,
                                            String expectedFormattedLandRegistry, String expectedFormattedLegals,
                                            String expectedFormattedArrears, String expectedFormattedTotalFees
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
            expectedFormattedArrears,
            expectedFormattedLegals,
            expectedFormattedLandRegistry,
            warrantFeeAmount,
            expectedFormattedTotalFees
        )).thenReturn("<table>Mock Repayment Table</table>");

        // When
        callMidEventHandler(caseData);

        // Then
        verify(repaymentTableRenderer).render(
            expectedFormattedArrears,
            expectedFormattedLegals,
            expectedFormattedLandRegistry,
            warrantFeeAmount,
            expectedFormattedTotalFees
        );

        assertThat(caseData.getEnforcementOrder().getRepaymentCosts().getRepaymentSummaryMarkdown())
            .isEqualTo("<table>Mock Repayment Table</table>");
    }

    private static Stream<Arguments> repaymentFeeScenarios() {
        return Stream.of(
            arguments("12300", "10000", "20000", "£404", "£123", "£100", "£200", "£827"),
            arguments("1500", "500", "999", "£150", "£15", "£5", "£9.99", "£179.99"),
            arguments("0", "0", "0", "£0", "£0", "£0", "£0", "£0"),
            arguments("10001", "1", "5000", "£100.01", "£100.01", "£0.01", "£50", "£250.03")
        );
    }

}
