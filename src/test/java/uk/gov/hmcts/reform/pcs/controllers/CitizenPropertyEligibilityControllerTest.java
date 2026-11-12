package uk.gov.hmcts.reform.pcs.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPropertyEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPropertyEligibilityResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenPropertyEligibilityControllerTest {

    private static final String AUTH_HEADER = "Bearer user-token";
    private static final String S2S_TOKEN = "service-token";
    private static final String POSTCODE = "BD10 0JB";

    @Mock
    private IdamAuthenticator idamAuthenticator;
    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private CitizenPossessionClaimCaseService caseService;

    private CitizenPropertyEligibilityController underTest;

    @BeforeEach
    void setUp() {
        underTest = new CitizenPropertyEligibilityController(idamAuthenticator, featureToggleService, caseService);
        lenient().when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
    }

    @Test
    void shouldReportAnEligibleProperty() {
        signedInWith("citizen");
        when(caseService.checkPostcodeInEngland(POSTCODE, null)).thenReturn(Optional.empty());

        assertThat(statusFor(POSTCODE, null)).isEqualTo(CitizenPropertyEligibility.ELIGIBLE);
    }

    @ParameterizedTest
    @CsvSource({
        "PROPERTY_NOT_IN_ENGLAND, NOT_ENGLAND",
        "POSTCODE_NOT_ASSIGNED_TO_COURT, NOT_COVERED",
        "PROPERTY_NOT_ELIGIBLE, NOT_COVERED",
        "LEGISLATIVE_COUNTRY_REQUIRED, LEGISLATIVE_COUNTRY_REQUIRED"
    })
    void shouldMapEachCreateRefusalToAStatus(String refusal, CitizenPropertyEligibility expected) {
        signedInWith("citizen");
        when(caseService.checkPostcodeInEngland(POSTCODE, null)).thenReturn(Optional.of(refusal));

        assertThat(statusFor(POSTCODE, null)).isEqualTo(expected);
    }

    @Test
    void shouldPassTheChosenCountryForACrossBorderPostcode() {
        signedInWith("citizen");
        when(caseService.checkPostcodeInEngland(POSTCODE, LegislativeCountry.ENGLAND)).thenReturn(Optional.empty());

        assertThat(statusFor(POSTCODE, "England")).isEqualTo(CitizenPropertyEligibility.ELIGIBLE);
    }

    @Test
    void shouldBeNotFoundWhenTheFeatureIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(false);

        ResponseEntity<?> response = underTest.getPropertyEligibility(AUTH_HEADER, S2S_TOKEN, POSTCODE, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(idamAuthenticator, caseService);
    }

    @Test
    void shouldRefuseUsersWhoAreNotCitizens() {
        signedInWith("caseworker-pcs-solicitor");

        ResponseEntity<?> response = underTest.getPropertyEligibility(AUTH_HEADER, S2S_TOKEN, POSTCODE, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(caseService, never()).checkPostcodeInEngland(anyString(), any());
    }

    @Test
    void shouldRefuseAMissingOrOverlongPostcode() {
        signedInWith("citizen");

        assertThat(underTest.getPropertyEligibility(AUTH_HEADER, S2S_TOKEN, null, null).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(underTest.getPropertyEligibility(AUTH_HEADER, S2S_TOKEN, " ", null).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(underTest.getPropertyEligibility(AUTH_HEADER, S2S_TOKEN, "x".repeat(15), null).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(caseService, never()).checkPostcodeInEngland(anyString(), any());
    }

    @Test
    void shouldRefuseAnUnknownLegislativeCountry() {
        signedInWith("citizen");

        assertThat(underTest.getPropertyEligibility(AUTH_HEADER, S2S_TOKEN, POSTCODE, "Narnia").getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private CitizenPropertyEligibility statusFor(String postcode, String legislativeCountry) {
        ResponseEntity<?> response = underTest.getPropertyEligibility(AUTH_HEADER, S2S_TOKEN, postcode,
                                                                      legislativeCountry);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((CitizenPropertyEligibilityResponse) response.getBody()).status();
    }

    private void signedInWith(String role) {
        UserInfo userInfo = UserInfo.builder().uid("citizen-1").roles(List.of(role)).build();
        User user = mock(User.class);
        when(user.getUserDetails()).thenReturn(userInfo);
        when(idamAuthenticator.validateAuthToken(AUTH_HEADER)).thenReturn(user);
    }

}
