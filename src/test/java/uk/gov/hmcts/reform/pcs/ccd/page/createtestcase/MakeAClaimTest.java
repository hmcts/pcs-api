package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class MakeAClaimTest {

    @Mock
    private EligibilityService eligibilityService;

    @Mock
    private CaseDetails<PCSCase, State> caseDetails;

    @Mock
    private PCSCase caseData;

    @Mock
    private AddressUK addressUK;

    @InjectMocks
    private MakeAClaim makeAClaim;

    @BeforeEach
    void setUp() {
        when(caseDetails.getData()).thenReturn(caseData);
        when(caseData.getPropertyAddress()).thenReturn(addressUK);
    }

    @Test
    void shouldImplementCcdPageConfiguration() {
        // Then
        assertTrue(makeAClaim instanceof CcdPageConfiguration);
    }

    @Test
    void shouldInstantiateWithoutErrors() {
        // When & Then
        assertDoesNotThrow(() -> new MakeAClaim(eligibilityService));
    }

    @Test
    void shouldHaveEligibilityServiceInjected() {
        // Then
        assertNotNull(makeAClaim);
        assertNotNull(eligibilityService);
    }

    @Test
    void shouldSetShowCrossBorderPageToNoWhenEligibilityStatusIsEligible() throws Exception {
        // Given
        String postcode = "SW1A 1AA";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = invokeMidEvent(caseDetails, null);

        // Then
        assertNotNull(response);
        assertNotNull(response.getData());
        verify(caseData).setShowCrossBorderPage(YesOrNo.NO);
        verify(eligibilityService).checkEligibility(eq(postcode), isNull());
    }

    @Test
    void shouldSetShowCrossBorderPageToNoWhenEligibilityStatusIsNotEligible() throws Exception {
        // Given
        String postcode = "M1 1AA";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.NOT_ELIGIBLE)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = invokeMidEvent(caseDetails, null);

        // Then
        assertNotNull(response);
        assertNotNull(response.getData());
        verify(caseData).setShowCrossBorderPage(YesOrNo.NO);
        verify(eligibilityService).checkEligibility(eq(postcode), isNull());
    }

    @Test
    void shouldSetShowCrossBorderPageToNoWhenEligibilityStatusIsNoMatchFound() throws Exception {
        // Given
        String postcode = "INVALID";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.NO_MATCH_FOUND)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = invokeMidEvent(caseDetails, null);

        // Then
        assertNotNull(response);
        assertNotNull(response.getData());
        verify(caseData).setShowCrossBorderPage(YesOrNo.NO);
        verify(eligibilityService).checkEligibility(eq(postcode), isNull());
    }

    @Test
    void shouldSetShowCrossBorderPageToYesWhenEligibilityStatusIsLegislativeCountryRequired() throws Exception {
        // Given
        String postcode = "TD9 0TU";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        List<LegislativeCountry> legislativeCountries = Arrays.asList(
            LegislativeCountry.ENGLAND,
            LegislativeCountry.SCOTLAND
        );

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(legislativeCountries)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = invokeMidEvent(caseDetails, null);

        // Then
        assertNotNull(response);
        assertNotNull(response.getData());
        verify(caseData).setShowCrossBorderPage(YesOrNo.YES);
        verify(caseData).setCrossBorderCountriesList(any(DynamicStringList.class));
        verify(caseData).setCrossBorderCountry1("England");
        verify(caseData).setCrossBorderCountry2("Scotland");
        verify(eligibilityService).checkEligibility(eq(postcode), isNull());
    }

    @Test
    void shouldSetCrossBorderCountriesListWhenLegislativeCountryRequired() throws Exception {
        // Given
        String postcode = "LL65 1AA";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        List<LegislativeCountry> legislativeCountries = Arrays.asList(
            LegislativeCountry.WALES,
            LegislativeCountry.ENGLAND
        );

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(legislativeCountries)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = invokeMidEvent(caseDetails, null);

        // Then
        assertNotNull(response);
        verify(caseData).setShowCrossBorderPage(YesOrNo.YES);
        
        // Verify that setCrossBorderCountriesList is called with correct structure
        verify(caseData).setCrossBorderCountriesList(any(DynamicStringList.class));
        verify(caseData).setCrossBorderCountry1("Wales");
        verify(caseData).setCrossBorderCountry2("England");
    }

    @Test
    void shouldHandleSingleLegislativeCountryWhenLegislativeCountryRequired() throws Exception {
        // Given
        String postcode = "BT1 1AA";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        List<LegislativeCountry> legislativeCountries = Collections.singletonList(
            LegislativeCountry.NORTHERN_IRELAND
        );

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(legislativeCountries)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = invokeMidEvent(caseDetails, null);

        // Then
        assertNotNull(response);
        verify(caseData).setShowCrossBorderPage(YesOrNo.YES);
        verify(caseData).setCrossBorderCountriesList(any(DynamicStringList.class));
        verify(caseData).setCrossBorderCountry1("Northern Ireland");
        // Should not set second country when only one is available
        verify(caseData, never()).setCrossBorderCountry2(any());
    }

    @Test
    void shouldHandleEmptyLegislativeCountriesListWhenLegislativeCountryRequired() throws Exception {
        // Given
        String postcode = "XX99 9XX";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        List<LegislativeCountry> legislativeCountries = Collections.emptyList();

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(legislativeCountries)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = invokeMidEvent(caseDetails, null);

        // Then
        assertNotNull(response);
        verify(caseData).setShowCrossBorderPage(YesOrNo.YES);
        verify(caseData).setCrossBorderCountriesList(any(DynamicStringList.class));
        // Should not set any individual countries when list is empty
        verify(caseData, never()).setCrossBorderCountry1(any());
        verify(caseData, never()).setCrossBorderCountry2(any());
    }

    @Test
    void shouldHandleThreeLegislativeCountriesWhenLegislativeCountryRequired() throws Exception {
        // Given
        String postcode = "AB99 9XX";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        List<LegislativeCountry> legislativeCountries = Arrays.asList(
            LegislativeCountry.ENGLAND,
            LegislativeCountry.SCOTLAND,
            LegislativeCountry.WALES
        );

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(legislativeCountries)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = invokeMidEvent(caseDetails, null);

        // Then
        assertNotNull(response);
        verify(caseData).setShowCrossBorderPage(YesOrNo.YES);
        verify(caseData).setCrossBorderCountriesList(any(DynamicStringList.class));
        verify(caseData).setCrossBorderCountry1("England");
        verify(caseData).setCrossBorderCountry2("Scotland");
        // Should only set first two countries even if more are available
    }

    @Test
    void shouldReturnCorrectResponseStructureForCrossBorderCase() throws Exception {
        // Given
        String postcode = "TD9 0TU";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        List<LegislativeCountry> legislativeCountries = Arrays.asList(
            LegislativeCountry.ENGLAND,
            LegislativeCountry.SCOTLAND
        );

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
            .legislativeCountries(legislativeCountries)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = invokeMidEvent(caseDetails, null);

        // Then
        assertNotNull(response);
        assertEquals(caseData, response.getData());
        assertNull(response.getErrors());
        assertNull(response.getWarnings());
    }

    @Test
    void shouldReturnCorrectResponseStructureForNonCrossBorderCase() throws Exception {
        // Given
        String postcode = "SW1A 1AA";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = invokeMidEvent(caseDetails, null);

        // Then
        assertNotNull(response);
        assertEquals(caseData, response.getData());
        assertNull(response.getErrors());
        assertNull(response.getWarnings());
    }

    @Test
    void shouldCallEligibilityServiceWithCorrectParameters() throws Exception {
        // Given
        String postcode = "EC1A 1BB";
        when(addressUK.getPostCode()).thenReturn(postcode);
        
        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .build();

        when(eligibilityService.checkEligibility(eq(postcode), isNull())).thenReturn(eligibilityResult);

        // When
        invokeMidEvent(caseDetails, null);

        // Then
        verify(eligibilityService).checkEligibility(eq(postcode), isNull());
    }

    @Test
    void shouldExtractPostcodeFromCaseDataCorrectly() throws Exception {
        // Given
        String expectedPostcode = "M1 5HR";
        when(addressUK.getPostCode()).thenReturn(expectedPostcode);
        
        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .build();

        when(eligibilityService.checkEligibility(eq(expectedPostcode), isNull())).thenReturn(eligibilityResult);

        // When
        invokeMidEvent(caseDetails, null);

        // Then
        verify(caseData).getPropertyAddress();
        verify(addressUK).getPostCode();
        verify(eligibilityService).checkEligibility(eq(expectedPostcode), isNull());
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> invokeMidEvent(
            CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore) throws Exception {
        
        Method midEventMethod = MakeAClaim.class.getDeclaredMethod(
            "midEvent",
            CaseDetails.class,
            CaseDetails.class
        );
        midEventMethod.setAccessible(true);
        
        return (AboutToStartOrSubmitResponse<PCSCase, State>) midEventMethod.invoke(makeAClaim, details, detailsBefore);
    }
} 