package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.quality.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
class GenAppFeeCalculatorTest {

    @Mock
    private FeeService feeService;

    private GenAppFeeCalculator underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppFeeCalculator(feeService);
    }

    @Test
    void shouldHaveNoFeeForAdjournWhenHearingMoreThan14DaysAway() {
        // Given
        XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
            .applicationType(GenAppType.ADJOURN)
            .within14Days(VerticalYesNo.NO)
            .build();

        // When
        Optional<FeeDetails> feeDetails = underTest.getApplicationFeeDetails(genAppRequest);

        // Then
        assertThat(feeDetails).isEmpty();
        verify(feeService, never()).getFee(any(FeeType.class));
    }

    @Test
    void shouldHaveNoFeeWhenHwfReferenceProvided() {
        // Given
        XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
            .applicationType(GenAppType.SET_ASIDE)
            .needHwf(VerticalYesNo.YES)
            .appliedForHwf(VerticalYesNo.YES)
            .hwfReference("some HWF reference")
            .build();

        // When
        Optional<FeeDetails> feeDetails = underTest.getApplicationFeeDetails(genAppRequest);

        // Then
        assertThat(feeDetails).isEmpty();
        verify(feeService, never()).getFee(any(FeeType.class));
    }

    @Test
    void shouldIgnoreHwfReferenceWhenNeedHwfFlagNotSetToYes() {
        // Given
        XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
            .applicationType(GenAppType.SET_ASIDE)
            .needHwf(VerticalYesNo.NO)
            .appliedForHwf(VerticalYesNo.YES)
            .hwfReference("some HWF reference")
            .build();

        stubFeeService();

        // When
        Optional<FeeDetails> feeDetails = underTest.getApplicationFeeDetails(genAppRequest);

        // Then
        assertThat(feeDetails).isNotEmpty();
    }

    @Test
    void shouldIgnoreHwfReferenceWhenAppliedForHwfFlagNotSetToYes() {
        // Given
        XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
            .applicationType(GenAppType.SET_ASIDE)
            .needHwf(VerticalYesNo.YES)
            .appliedForHwf(VerticalYesNo.NO)
            .hwfReference("some HWF reference")
            .build();

        stubFeeService();

        // When
        Optional<FeeDetails> feeDetails = underTest.getApplicationFeeDetails(genAppRequest);

        // Then
        assertThat(feeDetails).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("applicationTypeScenarios")
    void shouldCalculateFeeBasedOnApplicationType(GenAppRequest genAppRequest, FeeType expectedFeeType) {
        // Given
        BigDecimal expectedFeeAmount = stubFeeService();

        // When
        Optional<FeeDetails> feeDetails = underTest.getApplicationFeeDetails(genAppRequest);

        // Then
        ArgumentCaptor<FeeType> feeTypeCaptor = ArgumentCaptor.forClass(FeeType.class);
        verify(feeService).getFee(feeTypeCaptor.capture());

        assertThat(feeTypeCaptor.getValue()).isEqualTo(expectedFeeType);
        assertThat(feeDetails)
            .map(FeeDetails::getFeeAmount)
            .contains(expectedFeeAmount);
    }

    private static Stream<Arguments> applicationTypeScenarios() {
        return Stream.of(
            argumentSet(
                "Adjourn, hearing within 14 days, parties agreed",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.ADJOURN)
                    .within14Days(VerticalYesNo.YES)
                    .otherPartiesAgreed(VerticalYesNo.YES)
                    .build(),
                FeeType.GEN_APP_STANDARD_FEE
            ),
            argumentSet(
                "Adjourn, hearing within 14 days, without notice",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.ADJOURN)
                    .within14Days(VerticalYesNo.YES)
                    .otherPartiesAgreed(VerticalYesNo.NO)
                    .withoutNotice(VerticalYesNo.YES)
                    .build(),
                FeeType.GEN_APP_STANDARD_FEE
            ),
            argumentSet(
                "Adjourn, hearing within 14 days, with notice",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.ADJOURN)
                    .within14Days(VerticalYesNo.YES)
                    .otherPartiesAgreed(VerticalYesNo.NO)
                    .withoutNotice(VerticalYesNo.NO)
                    .build(),
                FeeType.GEN_APP_MAX_FEE
            ),
            argumentSet(
                "Set Aside, parties agreed",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.SET_ASIDE)
                    .otherPartiesAgreed(VerticalYesNo.YES)
                    .build(),
                FeeType.GEN_APP_STANDARD_FEE
            ),
            argumentSet(
                "Set Aside, without notice",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.SET_ASIDE)
                    .otherPartiesAgreed(VerticalYesNo.NO)
                    .withoutNotice(VerticalYesNo.YES)
                    .build(),
                FeeType.GEN_APP_STANDARD_FEE
            ),
            argumentSet(
                "Set Aside, with notice",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.SET_ASIDE)
                    .otherPartiesAgreed(VerticalYesNo.NO)
                    .withoutNotice(VerticalYesNo.NO)
                    .build(),
                FeeType.GEN_APP_MAX_FEE
            ),
            argumentSet(
                "Something Else, parties agreed",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.SOMETHING_ELSE)
                    .otherPartiesAgreed(VerticalYesNo.YES)
                    .build(),
                FeeType.GEN_APP_STANDARD_FEE
            ),
            argumentSet(
                "Something Else, without notice",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.SOMETHING_ELSE)
                    .otherPartiesAgreed(VerticalYesNo.NO)
                    .withoutNotice(VerticalYesNo.YES)
                    .build(),
                FeeType.GEN_APP_STANDARD_FEE
            ),
            argumentSet(
                "Something Else, with notice",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.SOMETHING_ELSE)
                    .otherPartiesAgreed(VerticalYesNo.NO)
                    .withoutNotice(VerticalYesNo.NO)
                    .build(),
                FeeType.GEN_APP_MAX_FEE
            )
        );
    }

    private BigDecimal stubFeeService() {
        FeeDetails feeDetails = mock(FeeDetails.class, withSettings().strictness(LENIENT));
        BigDecimal expectedFeeAmount = mock(BigDecimal.class);
        when(feeService.getFee(any(FeeType.class))).thenReturn(feeDetails);
        when(feeDetails.getFeeAmount()).thenReturn(expectedFeeAmount);
        return expectedFeeAmount;
    }

}
