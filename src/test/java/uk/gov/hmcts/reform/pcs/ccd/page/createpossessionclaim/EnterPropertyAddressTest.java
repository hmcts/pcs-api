package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.EligibilityCheckException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.NORTHERN_IRELAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.SCOTLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@ExtendWith(MockitoExtension.class)
class EnterPropertyAddressTest extends BasePageTest {

    @Mock
    private EligibilityService eligibilityService;
    @Mock
    private AddressValidator addressValidator;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new EnterPropertyAddress(eligibilityService, addressValidator));
    }

    @ParameterizedTest
    @EnumSource(value = EligibilityStatus.class, names = {"ELIGIBLE", "NOT_ELIGIBLE"})
    void shouldSetLegislativeCountryWhenEligibilityMatchFound(EligibilityStatus eligibilityStatus) {
        // Given
        String postCode = "CF10 1EP";
        LegislativeCountry expectedLegislativeCountry = WALES;

        AddressUK propertyAddress = AddressUK.builder()
            .postCode(postCode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(eligibilityStatus)
            .legislativeCountry(expectedLegislativeCountry)
            .build();

        when(eligibilityService.checkEligibility(postCode, null)).thenReturn(eligibilityResult);

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getLegislativeCountry()).isEqualTo(expectedLegislativeCountry);
    }

    @ParameterizedTest
    @MethodSource("postcodeEligibilityScenarios")
    void shouldHandlePostcodeEligibilityCheck(
        String postcode,
        EligibilityStatus status,
        List<LegislativeCountry> countries,
        YesOrNo expectedShowCrossBorder,
        String expectedCountry1,
        String expectedCountry2
    ) {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .postCode(postcode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(status)
            .legislativeCountries(countries)
            .build();

        when(eligibilityService.checkEligibility(postcode, null)).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);
        // Then
        PCSCase resultData = response.getData();
        assertThat(resultData.getShowCrossBorderPage()).isEqualTo(expectedShowCrossBorder);

        if (status == EligibilityStatus.NO_MATCH_FOUND) {
            assertThat(resultData.getShowPostcodeNotAssignedToCourt()).isEqualTo(YES);
            assertThat(resultData.getPostcodeNotAssignedView()).isEqualTo("ALL_COUNTRIES");
        }

        if (expectedShowCrossBorder == YES) {
            assertThat(resultData.getCrossBorderCountriesList()).isNotNull();
            assertThat(resultData.getCrossBorderCountry1()).isEqualTo(expectedCountry1);
            assertThat(resultData.getCrossBorderCountry2()).isEqualTo(expectedCountry2);
        }
    }

    @ParameterizedTest
    @MethodSource("invalidLegislativeCountryScenarios")
    void shouldThrowExceptionWhenLegislativeCountryRequiredButInvalidCountries(
        String postcode,
        List<LegislativeCountry> countries,
        String expectedMessageFragment
    ) {
        // Given
        AddressUK propertyAddress = AddressUK.builder()
            .postCode(postcode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(countries)
            .build();

        when(eligibilityService.checkEligibility(postcode, null)).thenReturn(eligibilityResult);

        // When & Then
        assertThatThrownBy(() -> callMidEventHandler(caseData))
            .isInstanceOf(EligibilityCheckException.class)
            .hasMessageContaining("Expected at least 2 legislative countries")
            .hasMessageContaining(expectedMessageFragment)
            .hasMessageContaining(postcode);
    }

    @Test
    void shouldShowPropertyNotEligiblePageOnNotEligible() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("M1 1AA").build();
        PCSCase caseData = PCSCase.builder().propertyAddress(propertyAddress).build();

        var result = EligibilityResult.builder()
            .status(EligibilityStatus.NOT_ELIGIBLE)
            .legislativeCountry(ENGLAND)
            .build();

        when(eligibilityService.checkEligibility("M1 1AA", null)).thenReturn(result);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> resp = callMidEventHandler(caseData);

        // Then
        PCSCase data = resp.getData();
        assertThat(data.getShowCrossBorderPage()).isEqualTo(YesOrNo.NO);
        assertThat(data.getShowPropertyNotEligiblePage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldReturnValidationErrorsWhenAddressInvalid() {
        // Given
        AddressUK propertyAddress = mock(AddressUK.class);
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        List<String> expectedValidationErrors = List.of("error 1", "error 2");
        when(addressValidator.validateAddressFields(propertyAddress)).thenReturn(expectedValidationErrors);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEqualTo(expectedValidationErrors);
    }

    private static Stream<Arguments> invalidLegislativeCountryScenarios() {
        return Stream.of(
            // Empty list case
            arguments(
                "XX99 9XX",
                Collections.emptyList(),
                "but got 0"
            ),

            // Single country case
            arguments(
                "BT1 1AA",
                Collections.singletonList(NORTHERN_IRELAND),
                "but got 1"
            ),

            // Null list case
            arguments(
                "NULL_CASE",
                null,
                "but got 0"
            )
        );
    }

    private static Stream<Arguments> postcodeEligibilityScenarios() {
        return Stream.of(
            // Regular eligible postcode
            arguments(
                "SW1A 1AA",
                EligibilityStatus.ELIGIBLE,
                Collections.emptyList(),
                NO,
                null,
                null
            ),

            // Not eligible postcode
            arguments(
                "M1 1AA",
                EligibilityStatus.NOT_ELIGIBLE,
                Collections.emptyList(),
                NO,
                null,
                null
            ),

            // No match found
            arguments(
                "INVALID",
                EligibilityStatus.NO_MATCH_FOUND,
                Collections.emptyList(),
                NO,
                null,
                null
            ),

            // No match found - should set appropriate flags
            arguments(
                "NO_MATCH",
                EligibilityStatus.NO_MATCH_FOUND,
                Collections.emptyList(),
                NO,
                null,
                null
            ),

            // Cross-border England/Scotland
            arguments(
                "TD9 0TU",
                EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED,
                Arrays.asList(ENGLAND, SCOTLAND),
                YES,
                "England",
                "Scotland"
            ),

            // Cross-border Wales/England
            arguments(
                "LL65 1AA",
                EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED,
                Arrays.asList(WALES, ENGLAND),
                YES,
                "Wales",
                "England"
            )
        );
    }
}
