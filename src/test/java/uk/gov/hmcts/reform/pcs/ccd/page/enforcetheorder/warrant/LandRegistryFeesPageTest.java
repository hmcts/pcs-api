package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.LandRegistryFees;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.MoneyOwedByDefendants;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RepaymentCosts;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RepaymentTableRenderer;
import uk.gov.hmcts.reform.pcs.ccd.service.FeeValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyConverter;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandRegistryFeesPageTest extends BasePageTest {

    @Mock
    private RepaymentTableRenderer repaymentTableRenderer;

    @BeforeEach
    void setUp() {
        MoneyConverter moneyConverter = new MoneyConverter();
        FeeValidationService feeValidationService = new FeeValidationService();
        setPageUnderTest(new LandRegistryFeesPage(moneyConverter, repaymentTableRenderer, feeValidationService));
    }

    @ParameterizedTest
    @MethodSource("feeTotals")
    void shouldFormatRepaymentFeesCorrectly(BigDecimal landRegistryAmount,
                                            BigDecimal legalCostsAmount,
                                            BigDecimal totalArrearsAmount,
                                            String warrantFeeAmount,
                                            BigDecimal totalAmount) {
        // Given
        LegalCosts legalCosts = LegalCosts.builder()
            .amountOfLegalCosts(legalCostsAmount)
            .build();

        LandRegistryFees landRegistryFees = LandRegistryFees.builder()
            .amountOfLandRegistryFees(landRegistryAmount)
            .haveLandRegistryFeesBeenPaid(VerticalYesNo.NO)
            .build();

        MoneyOwedByDefendants moneyOwedByDefendants = MoneyOwedByDefendants.builder()
            .amountOwed(totalArrearsAmount)
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
            totalArrearsAmount,
            legalCostsAmount,
            landRegistryAmount,
            warrantFeeAmount,
            totalAmount
        )).thenReturn("<table>Mock Repayment Table</table>");
        when(repaymentTableRenderer.render(
            totalArrearsAmount,
            legalCostsAmount,
            landRegistryAmount,
            warrantFeeAmount,
            totalAmount,
            "The payments due"
        )).thenReturn("<table>Mock SOT Repayment Table</table>");

        // When
        callMidEventHandler(caseData);

        // Then
        ArgumentCaptor<BigDecimal> totalCaptor = ArgumentCaptor.forClass(BigDecimal.class);

        verify(repaymentTableRenderer).render(
            eq(totalArrearsAmount),
            eq(legalCostsAmount),
            eq(landRegistryAmount),
            eq(warrantFeeAmount),
            totalCaptor.capture()
        );
        verify(repaymentTableRenderer).render(
            eq(totalArrearsAmount),
            eq(legalCostsAmount),
            eq(landRegistryAmount),
            eq(warrantFeeAmount),
            totalCaptor.capture(),
            eq("The payments due")
        );

        assertThat(totalCaptor.getValue())
            .isEqualByComparingTo(totalAmount);

        assertThat(caseData.getEnforcementOrder().getWarrantDetails().getRepaymentCosts().getRepaymentSummaryMarkdown())
            .isEqualTo("<table>Mock Repayment Table</table>");
        assertThat(caseData.getEnforcementOrder().getWarrantDetails().getRepaymentCosts()
            .getStatementOfTruthRepaymentSummaryMarkdown())
            .isEqualTo("<table>Mock SOT Repayment Table</table>");
    }

    @ParameterizedTest
    @MethodSource("validFees")
    void shouldAcceptFees(BigDecimal validFee) {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantFeeAmount("£404")
            .warrantDetails(WarrantDetails.builder()
                                .repaymentCosts(RepaymentCosts.builder().build())
                                .landRegistryFees(LandRegistryFees.builder()
                                                      .haveLandRegistryFeesBeenPaid(VerticalYesNo.YES)
                                                      .amountOfLandRegistryFees(validFee)
                                                      .build())
                                .legalCosts(LegalCosts.builder().build())
                                .moneyOwedByDefendants(MoneyOwedByDefendants.builder().build())
                                .build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(enforcementOrder)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getEnforcementOrder().getWarrantDetails()
                       .getLandRegistryFees().getAmountOfLandRegistryFees()).isEqualTo(validFee);
    }

    @ParameterizedTest
    @MethodSource("invalidFees")
    void shouldAcceptInvalidFees(BigDecimal invalidFee) {
        // Given
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantFeeAmount("£404")
            .warrantDetails(WarrantDetails.builder()
                                .repaymentCosts(RepaymentCosts.builder().build())
                                .landRegistryFees(LandRegistryFees.builder()
                                                      .haveLandRegistryFeesBeenPaid(VerticalYesNo.YES)
                                                      .amountOfLandRegistryFees(invalidFee)
                                .build())
                                .legalCosts(LegalCosts.builder().build())
                                .moneyOwedByDefendants(MoneyOwedByDefendants.builder().build())
                                .build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(enforcementOrder)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        String expectedError = "Land Registry cost should be more than 0.01";

        assertThat(response.getErrors()).containsExactly(expectedError);
        assertThat(response.getData().getEnforcementOrder().getWarrantDetails()
                       .getLandRegistryFees().getAmountOfLandRegistryFees()).isEqualTo(invalidFee);
    }

    private static Stream<Arguments> validFees() {
        return Stream.of(
            Arguments.of(new BigDecimal("100.00")),
            Arguments.of(new BigDecimal("1000000000")),
            Arguments.of(new BigDecimal("998999")),
            Arguments.of(BigDecimal.TEN)
        );
    }

    private static Stream<Arguments> invalidFees() {
        return Stream.of(
            Arguments.of(new BigDecimal("-100.00")),
            Arguments.of(new BigDecimal("100000000000000.00")),
            Arguments.of(new BigDecimal("10001999999999999")),
            Arguments.of(BigDecimal.ZERO)
        );
    }

    private static Stream<Arguments> feeTotals() {
        return Stream.of(
            Arguments.of(
                new BigDecimal("123.00"),
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                "£404",
                new BigDecimal("827.00")
            ),
            Arguments.of(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "£0",
                new BigDecimal("0.00")
            ),
            Arguments.of(
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                null,
                "£0",
                new BigDecimal("15.00")
            )
        );
    }
}
