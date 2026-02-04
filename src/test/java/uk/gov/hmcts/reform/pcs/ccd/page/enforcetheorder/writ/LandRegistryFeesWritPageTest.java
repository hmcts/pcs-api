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

    @BeforeEach
    void setUp() {
        setPageUnderTest(new LandRegistryFeesWritPage(repaymentTableRenderer));
    }

    @ParameterizedTest
    @MethodSource("repaymentFeeScenarios")
    void shouldFormatRepaymentFeesCorrectly(EnforcementCosts enforcementCosts) {
        // Given
        LegalCosts legalCosts = LegalCosts.builder()
            .amountOfLegalCosts(enforcementCosts.getLegalFeesPence())
            .build();

        LandRegistryFees landRegistryFees = LandRegistryFees.builder()
            .amountOfLandRegistryFees(enforcementCosts.getLandRegistryFeesPence())
            .build();

        MoneyOwedByDefendants moneyOwedByDefendants = MoneyOwedByDefendants.builder()
            .amountOwed(enforcementCosts.getTotalArrearsPence())
            .build();

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .writFeeAmount(enforcementCosts.getFeeAmount())
            .writDetails(WritDetails.builder()
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
                        new EnforcementCosts("12300", "10000", "20000", "404", WRIT_FEE_AMOUNT)),
                Arguments.of(
                        new EnforcementCosts("1500", "500", "999", "50", WRIT_FEE_AMOUNT)),
                Arguments.of(
                        new EnforcementCosts("0", "0", "0", "0", WRIT_FEE_AMOUNT)),
                Arguments.of(
                        new EnforcementCosts("10001", "1", "5000", "0", WRIT_FEE_AMOUNT))
        );
    }
}
