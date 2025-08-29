package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
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

class EnterPropertyAddressTest extends BasePageTest {

    private EligibilityService eligibilityService;

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        eligibilityService = mock(EligibilityService.class);
        event = buildPageInTestEvent(new EnterPropertyAddress(eligibilityService));
    }

    @Test
    void shouldSetFormattedContactAddressInMidEventCallback() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        String postCode = "NW1 6XE";
        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("123 Baker Street")
            .addressLine2("Marylebone")
            .postTown("London")
            .county("Greater London")
            .postCode(postCode)
            .build();
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        caseDetails.setData(caseData);

        String formattedAddress = formattedContactAddress(propertyAddress);
        EligibilityResult eligibilityResult = EligibilityResult.builder()
                .status(EligibilityStatus.NO_MATCH_FOUND)
                .legislativeCountries(List.of())
                .build();

        // When
        when(eligibilityService.checkEligibility(postCode, null)).thenReturn(eligibilityResult);
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "enterPropertyAddress");
        midEvent.handle(caseDetails, null);

        // Then
        assertThat(caseData.getFormattedClaimantContactAddress()).isEqualTo(formattedAddress);
    }

    private String formattedContactAddress(AddressUK propertyAddress) {
        return  String.format(
            "%s<br>%s<br>%s",
            propertyAddress.getAddressLine1(),
            propertyAddress.getPostTown(),
            propertyAddress.getPostCode()
        );
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
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        AddressUK propertyAddress = AddressUK.builder()
            .postCode(postcode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        caseDetails.setData(caseData);

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(status)
            .legislativeCountries(countries)
            .build();

        when(eligibilityService.checkEligibility(postcode, null)).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = getMidEventForPage(event, "enterPropertyAddress")
            .handle(caseDetails, null);

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
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        AddressUK propertyAddress = AddressUK.builder()
            .postCode(postcode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        caseDetails.setData(caseData);

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(countries)
            .build();

        when(eligibilityService.checkEligibility(postcode, null)).thenReturn(eligibilityResult);

        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "enterPropertyAddress");

        // When & Then
        assertThatThrownBy(() -> midEvent.handle(caseDetails, null))
            .isInstanceOf(EligibilityCheckException.class)
            .hasMessageContaining("Expected at least 2 legislative countries")
            .hasMessageContaining(expectedMessageFragment)
            .hasMessageContaining(postcode);
    }

    @Test
    void shouldShowPropertyNotEligiblePageAndSetCountryOnNotEligible() {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        AddressUK propertyAddress = AddressUK.builder().postCode("M1 1AA").build();
        PCSCase caseData = PCSCase.builder().propertyAddress(propertyAddress).build();
        caseDetails.setData(caseData);

        var result = uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult.builder()
            .status(uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus.NOT_ELIGIBLE)
            .legislativeCountry(uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND)
            .build();

        when(eligibilityService.checkEligibility("M1 1AA", null)).thenReturn(result);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> resp =
            getMidEventForPage(event, "enterPropertyAddress").handle(caseDetails, null);

        // Then
        PCSCase data = resp.getData();
        assertThat(data.getShowCrossBorderPage()).isEqualTo(YesOrNo.NO);
        assertThat(data.getShowPropertyNotEligiblePage()).isEqualTo(YesOrNo.YES);
        assertThat(data.getLegislativeCountry()).isEqualTo("England");
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
                Collections.singletonList(LegislativeCountry.NORTHERN_IRELAND),
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
                Arrays.asList(LegislativeCountry.ENGLAND, LegislativeCountry.SCOTLAND),
                YES,
                "England",
                "Scotland"
            ),

            // Cross-border Wales/England
            arguments(
                "LL65 1AA",
                EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED,
                Arrays.asList(LegislativeCountry.WALES, LegislativeCountry.ENGLAND),
                YES,
                "Wales",
                "England"
            )
        );
    }
}
