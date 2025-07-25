package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.exception.CrossBorderPostcodeException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrossBorderPostcodeCallbackServiceTest {

    @Mock
    private EligibilityService eligibilityService;

    private CrossBorderPostcodeCallbackService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CrossBorderPostcodeCallbackService(eligibilityService);
    }

    @Test
    void shouldReturnEligibleResultForValidPostcode() {
        // Given
        String postcode = "SW1A 1AA";
        EligibilityResult expectedResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .epimsId(12345)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.checkCrossBorderPostcode(postcode);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
    }

    @Test
    void shouldReturnCrossBorderResultForCrossBorderPostcode() {
        // Given
        String postcode = "CH5 1AA"; // Chester postcode (cross-border)
        EligibilityResult expectedResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(List.of(LegislativeCountry.ENGLAND, LegislativeCountry.WALES))
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.checkCrossBorderPostcode(postcode);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED);
        assertThat(result.getLegislativeCountries()).containsExactlyInAnyOrder(
            LegislativeCountry.ENGLAND, LegislativeCountry.WALES);
    }

    @Test
    void shouldReturnIneligibleResultForIneligiblePostcode() {
        // Given
        String postcode = "EH1 1AA"; // Edinburgh postcode (Scotland)
        EligibilityResult expectedResult = EligibilityResult.builder()
            .status(EligibilityStatus.NOT_ELIGIBLE)
            .epimsId(45678)
            .legislativeCountry(LegislativeCountry.SCOTLAND)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.checkCrossBorderPostcode(postcode);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.NOT_ELIGIBLE);
    }

    @Test
    void shouldThrowCrossBorderPostcodeExceptionWhenEligibilityServiceThrowsException() {
        // Given
        String postcode = "INVALID";
        RuntimeException serviceException = new RuntimeException("Service unavailable");

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenThrow(serviceException);

        // When/Then
        assertThatThrownBy(() -> underTest.checkCrossBorderPostcode(postcode))
            .isInstanceOf(CrossBorderPostcodeException.class)
            .hasMessage("Failed to check cross-border postcode status")
            .hasCause(serviceException);
    }

    @Test
    void shouldValidateSelectedCountrySuccessfully() {
        // Given
        String postcode = "CH5 1AA";
        LegislativeCountry selectedCountry = LegislativeCountry.ENGLAND;
        EligibilityResult expectedResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .epimsId(12345)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(selectedCountry)))
            .thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.validateSelectedCountry(postcode, selectedCountry);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
    }

    @Test
    void shouldThrowCrossBorderPostcodeExceptionWhenValidatingSelectedCountryFails() {
        // Given
        String postcode = "CH5 1AA";
        LegislativeCountry selectedCountry = LegislativeCountry.ENGLAND;
        RuntimeException serviceException = new RuntimeException("Validation failed");

        when(eligibilityService.checkEligibility(eq(postcode), eq(selectedCountry)))
            .thenThrow(serviceException);

        // When/Then
        assertThatThrownBy(() -> underTest.validateSelectedCountry(postcode, selectedCountry))
            .isInstanceOf(CrossBorderPostcodeException.class)
            .hasMessage("Failed to validate selected country")
            .hasCause(serviceException);
    }

    @Test
    void shouldHandleNullPostcode() {
        // Given
        String postcode = null;
        RuntimeException serviceException = new RuntimeException("Postcode cannot be null");

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenThrow(serviceException);

        // When/Then
        assertThatThrownBy(() -> underTest.checkCrossBorderPostcode(postcode))
            .isInstanceOf(CrossBorderPostcodeException.class)
            .hasMessage("Failed to check cross-border postcode status")
            .hasCause(serviceException);
    }

    @Test
    void shouldHandleEmptyPostcode() {
        // Given
        String postcode = "";
        RuntimeException serviceException = new RuntimeException("Postcode cannot be empty");

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenThrow(serviceException);

        // When/Then
        assertThatThrownBy(() -> underTest.checkCrossBorderPostcode(postcode))
            .isInstanceOf(CrossBorderPostcodeException.class)
            .hasMessage("Failed to check cross-border postcode status")
            .hasCause(serviceException);
    }

    @Test
    void shouldHandleCrossBorderPostcodeWithMultipleCountries() {
        // Given
        String postcode = "CH5 1AA"; // Chester postcode (cross-border)
        EligibilityResult expectedResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(List.of(LegislativeCountry.ENGLAND, LegislativeCountry.WALES))
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.checkCrossBorderPostcode(postcode);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED);
        assertThat(result.getLegislativeCountries()).hasSize(2);
        assertThat(result.getLegislativeCountries()).containsExactlyInAnyOrder(
            LegislativeCountry.ENGLAND, LegislativeCountry.WALES);
    }

    @Test
    void shouldValidateEnglandSelectionForCrossBorderPostcode() {
        // Given
        String postcode = "CH5 1AA";
        LegislativeCountry selectedCountry = LegislativeCountry.ENGLAND;
        EligibilityResult expectedResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .epimsId(12345)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(selectedCountry)))
            .thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.validateSelectedCountry(postcode, selectedCountry);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
    }

    @Test
    void shouldValidateWalesSelectionForCrossBorderPostcode() {
        // Given
        String postcode = "CH5 1AA";
        LegislativeCountry selectedCountry = LegislativeCountry.WALES;
        EligibilityResult expectedResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .epimsId(67890)
            .legislativeCountry(LegislativeCountry.WALES)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(selectedCountry)))
            .thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.validateSelectedCountry(postcode, selectedCountry);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.getLegislativeCountry()).isEqualTo(LegislativeCountry.WALES);
    }

    @Test
    void shouldHandleInvalidCountrySelectionForCrossBorderPostcode() {
        // Given
        String postcode = "CH5 1AA";
        LegislativeCountry selectedCountry = LegislativeCountry.SCOTLAND; // Invalid for this postcode
        RuntimeException serviceException = new RuntimeException("Invalid country selection");

        when(eligibilityService.checkEligibility(eq(postcode), eq(selectedCountry)))
            .thenThrow(serviceException);

        // When/Then
        assertThatThrownBy(() -> underTest.validateSelectedCountry(postcode, selectedCountry))
            .isInstanceOf(CrossBorderPostcodeException.class)
            .hasMessage("Failed to validate selected country")
            .hasCause(serviceException);
    }

    @Test
    void shouldHandleCrossBorderPostcodeEnglandScotland() {
        // Given
        String postcode = "TD15 1AA"; // Example cross-border postcode between England and Scotland
        EligibilityResult expectedResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(List.of(LegislativeCountry.ENGLAND, LegislativeCountry.SCOTLAND))
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.checkCrossBorderPostcode(postcode);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED);
        assertThat(result.getLegislativeCountries()).containsExactlyInAnyOrder(
            LegislativeCountry.ENGLAND, LegislativeCountry.SCOTLAND);
    }

    @Test
    void shouldValidateScotlandSelectionForCrossBorderPostcode() {
        // Given
        String postcode = "TD15 1AA"; // Example cross-border postcode between England and Scotland
        LegislativeCountry selectedCountry = LegislativeCountry.SCOTLAND;
        EligibilityResult expectedResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .epimsId(78901)
            .legislativeCountry(LegislativeCountry.SCOTLAND)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(selectedCountry)))
            .thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.validateSelectedCountry(postcode, selectedCountry);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.getLegislativeCountry()).isEqualTo(LegislativeCountry.SCOTLAND);
        assertThat(result.getEpimsId()).isEqualTo(78901);
    }

    @Test
    void shouldValidateEnglandSelectionForEnglandScotlandCrossBorderPostcode() {
        // Given
        String postcode = "TD15 1AA"; // Example cross-border postcode between England and Scotland
        LegislativeCountry selectedCountry = LegislativeCountry.ENGLAND;
        EligibilityResult expectedResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .epimsId(12345)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(selectedCountry)))
            .thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.validateSelectedCountry(postcode, selectedCountry);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
        assertThat(result.getEpimsId()).isEqualTo(12345);
    }

    @Test
    void shouldHandleInvalidCountrySelectionForEnglandScotlandCrossBorderPostcode() {
        // Given
        String postcode = "TD15 1AA"; // Example cross-border postcode between England and Scotland
        LegislativeCountry selectedCountry = LegislativeCountry.WALES; // Invalid for this postcode
        RuntimeException serviceException = new RuntimeException("Invalid country selection");

        when(eligibilityService.checkEligibility(eq(postcode), eq(selectedCountry)))
            .thenThrow(serviceException);

        // When/Then
        assertThatThrownBy(() -> underTest.validateSelectedCountry(postcode, selectedCountry))
            .isInstanceOf(CrossBorderPostcodeException.class)
            .hasMessage("Failed to validate selected country")
            .hasCause(serviceException);
    }

    @Test
    void shouldPopulateCrossBorderCountriesForEnglandWales() {
        // Given
        String postcode = "CH5 1AA"; // Chester postcode (cross-border)
        PCSCase pcsCase = PCSCase.builder().build();
        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(List.of(LegislativeCountry.ENGLAND, LegislativeCountry.WALES))
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenReturn(eligibilityResult);

        // When
        PCSCase result = underTest.populateCrossBorderCountries(pcsCase, postcode);

        // Then
        assertThat(result.getCrossBorderCountries()).isEqualTo("England and Wales");
    }

    @Test
    void shouldPopulateCrossBorderCountriesForEnglandScotland() {
        // Given
        String postcode = "TD15 1AA"; // Example cross-border postcode between England and Scotland
        PCSCase pcsCase = PCSCase.builder().build();
        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(List.of(LegislativeCountry.ENGLAND, LegislativeCountry.SCOTLAND))
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenReturn(eligibilityResult);

        // When
        PCSCase result = underTest.populateCrossBorderCountries(pcsCase, postcode);

        // Then
        assertThat(result.getCrossBorderCountries()).isEqualTo("England and Scotland");
    }

    @Test
    void shouldClearCrossBorderCountriesForNonCrossBorderPostcode() {
        // Given
        String postcode = "SW1A 1AA"; // London postcode (not cross-border)
        PCSCase pcsCase = PCSCase.builder()
            .crossBorderCountries("England and Wales") // Pre-existing value
            .build();
        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .epimsId(12345)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenReturn(eligibilityResult);

        // When
        PCSCase result = underTest.populateCrossBorderCountries(pcsCase, postcode);

        // Then
        assertThat(result.getCrossBorderCountries()).isNull();
    }

    @Test
    void shouldHandleExceptionWhenPopulatingCrossBorderCountries() {
        // Given
        String postcode = "INVALID";
        PCSCase pcsCase = PCSCase.builder().build();
        RuntimeException serviceException = new RuntimeException("Service unavailable");

        when(eligibilityService.checkEligibility(eq(postcode), eq(null)))
            .thenThrow(serviceException);

        // When/Then
        assertThatThrownBy(() -> underTest.populateCrossBorderCountries(pcsCase, postcode))
            .isInstanceOf(CrossBorderPostcodeException.class)
            .hasMessage("Failed to populate cross-border countries")
            .hasCause(serviceException);
    }
} 