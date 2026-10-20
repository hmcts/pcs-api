package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

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

    @Mock
    private FeatureToggleService featureToggleService;

    private GenAppFeeCalculator underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppFeeCalculator(feeService, featureToggleService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldHaveNoFeeForAdjournWhenHearingMoreThan14DaysAwayAndOtherPartiesAgreed(boolean release1bEnabled) {
        // Given
        XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
            .applicationType(GenAppType.ADJOURN)
            .within14Days(VerticalYesNo.NO)
            .otherPartiesAgreed(VerticalYesNo.YES)
            .build();
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)).thenReturn(release1bEnabled);

        // When
        Optional<FeeDetails> feeDetails = underTest.getApplicationFeeDetails(genAppRequest);

        // Then
        assertThat(feeDetails).isEmpty();
        verify(feeService, never()).getFee(any(FeeType.class));
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldHaveNoFeeForAdjournBeyond14DaysWithoutConsentWhenRelease1bDisabled(VerticalYesNo withoutNotice) {
        XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
            .applicationType(GenAppType.ADJOURN)
            .within14Days(VerticalYesNo.NO)
            .otherPartiesAgreed(VerticalYesNo.NO)
            .withoutNotice(withoutNotice)
            .needHwf(VerticalYesNo.NO)
            .build();

        assertThat(underTest.getApplicationFeeDetails(genAppRequest)).isEmpty();
        verify(feeService, never()).getFee(any(FeeType.class));
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldHaveNoFeeForAdjournBeyond14DaysWithHwfWhenRelease1bEnabled(VerticalYesNo withoutNotice) {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)).thenReturn(true);
        XuiGenAppRequest genAppRequest = XuiGenAppRequest.builder()
            .applicationType(GenAppType.ADJOURN)
            .within14Days(VerticalYesNo.NO)
            .otherPartiesAgreed(VerticalYesNo.NO)
            .withoutNotice(withoutNotice)
            .needHwf(VerticalYesNo.YES)
            .appliedForHwf(VerticalYesNo.YES)
            .hwfReference("HWF-123-ABC")
            .build();

        assertThat(underTest.getApplicationFeeDetails(genAppRequest)).isEmpty();
        verify(feeService, never()).getFee(any(FeeType.class));
    }

    @ParameterizedTest
    @MethodSource("adjournRelease1bScenarios")
    void shouldGateAdjournFeesForProfessionalAndCitizenRequests(boolean release1bEnabled,
                                                               GenAppRequest request,
                                                               FeeType expectedFeeType) {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)).thenReturn(release1bEnabled);
        FeeDetails expectedFee = FeeDetails.builder().build();
        if (expectedFeeType != null) {
            when(feeService.getFee(expectedFeeType)).thenReturn(expectedFee);
        }

        Optional<FeeDetails> result = underTest.getApplicationFeeDetails(request);

        if (expectedFeeType == null) {
            assertThat(result).isEmpty();
            verify(feeService, never()).getFee(any(FeeType.class));
        } else {
            assertThat(result).contains(expectedFee);
            verify(feeService).getFee(expectedFeeType);
        }
    }

    private static Stream<Arguments> adjournRelease1bScenarios() {
        return Stream.of(false, true).flatMap(release1bEnabled ->
            Stream.of(VerticalYesNo.values()).flatMap(withoutNotice ->
                Stream.of("no help with fees", "not yet applied", "reference provided").flatMap(hwfStatus -> {
                    boolean needHwf = !"no help with fees".equals(hwfStatus);
                    boolean hasReference = "reference provided".equals(hwfStatus);
                    XuiGenAppRequest professionalRequest = XuiGenAppRequest.builder()
                        .applicationType(GenAppType.ADJOURN)
                        .within14Days(VerticalYesNo.NO)
                        .otherPartiesAgreed(VerticalYesNo.NO)
                        .withoutNotice(withoutNotice)
                        .needHwf(VerticalYesNo.from(needHwf))
                        .appliedForHwf(VerticalYesNo.from(hasReference))
                        .hwfReference(hasReference ? "HWF-123-ABC" : null)
                        .build();
                    CitizenGenAppRequest citizenRequest = CitizenGenAppRequest.builder()
                        .applicationType(professionalRequest.getApplicationType())
                        .within14Days(professionalRequest.getWithin14Days())
                        .otherPartiesAgreed(professionalRequest.getOtherPartiesAgreed())
                        .withoutNotice(withoutNotice)
                        .needHwf(professionalRequest.getNeedHwf())
                        .appliedForHwf(professionalRequest.getAppliedForHwf())
                        .hwfReference(professionalRequest.getHwfReference())
                        .build();
                    FeeType expectedFeeType = !release1bEnabled || hasReference ? null
                        : withoutNotice == VerticalYesNo.YES ? FeeType.GEN_APP_STANDARD_FEE : FeeType.GEN_APP_MAX_FEE;
                    return Stream.of(professionalRequest, citizenRequest).map(request -> argumentSet(
                        "release1b=" + release1bEnabled + ", " + request.getClass().getSimpleName()
                            + ", withoutNotice=" + withoutNotice + ", " + hwfStatus,
                        release1bEnabled, request, expectedFeeType
                    ));
                })
            )
        );
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
        final BigDecimal expectedFeeAmount = stubFeeService();
        if (genAppRequest.getApplicationType() == GenAppType.ADJOURN
            && genAppRequest.getWithin14Days() == VerticalYesNo.NO) {
            when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)).thenReturn(true);
        }

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
                "Adjourn, hearing not within 14 days, without notice",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.ADJOURN)
                    .within14Days(VerticalYesNo.NO)
                    .otherPartiesAgreed(VerticalYesNo.NO)
                    .withoutNotice(VerticalYesNo.YES)
                    .build(),
                FeeType.GEN_APP_STANDARD_FEE
            ),
            argumentSet(
                "Adjourn, hearing not within 14 days, with notice",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.ADJOURN)
                    .within14Days(VerticalYesNo.NO)
                    .otherPartiesAgreed(VerticalYesNo.NO)
                    .withoutNotice(VerticalYesNo.NO)
                    .build(),
                FeeType.GEN_APP_MAX_FEE
            ),
            argumentSet(
                "Suspend, CUI route without notice or consent fields",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.SUSPEND)
                    .build(),
                FeeType.GEN_APP_SUSPEND_FEE
            ),
            argumentSet(
                "Suspend, parties agreed",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.SUSPEND)
                    .otherPartiesAgreed(VerticalYesNo.YES)
                    .build(),
                FeeType.GEN_APP_SUSPEND_FEE
            ),
            argumentSet(
                "Suspend, without notice",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.SUSPEND)
                    .otherPartiesAgreed(VerticalYesNo.NO)
                    .withoutNotice(VerticalYesNo.YES)
                    .build(),
                FeeType.GEN_APP_SUSPEND_FEE
            ),
            argumentSet(
                "Suspend, with notice",
                XuiGenAppRequest.builder()
                    .applicationType(GenAppType.SUSPEND)
                    .otherPartiesAgreed(VerticalYesNo.NO)
                    .withoutNotice(VerticalYesNo.NO)
                    .build(),
                FeeType.GEN_APP_SUSPEND_FEE
            ),
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
        ).flatMap(arguments -> {
            GenAppRequest request = (GenAppRequest) arguments.get()[0];
            CitizenGenAppRequest citizenRequest = CitizenGenAppRequest.builder()
                .applicationType(request.getApplicationType())
                .within14Days(request.getWithin14Days())
                .otherPartiesAgreed(request.getOtherPartiesAgreed())
                .withoutNotice(request.getWithoutNotice())
                .build();
            return Stream.of(arguments, Arguments.arguments(citizenRequest, arguments.get()[1]));
        });
    }

    private BigDecimal stubFeeService() {
        FeeDetails feeDetails = mock(FeeDetails.class, withSettings().strictness(LENIENT));
        BigDecimal expectedFeeAmount = mock(BigDecimal.class);
        when(feeService.getFee(any(FeeType.class))).thenReturn(feeDetails);
        when(feeDetails.getFeeAmount()).thenReturn(expectedFeeAmount);
        return expectedFeeAmount;
    }

}
