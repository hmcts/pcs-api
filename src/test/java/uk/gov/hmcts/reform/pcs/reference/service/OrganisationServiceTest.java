package uk.gov.hmcts.reform.pcs.reference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import feign.FeignException;
import feign.Request;
import feign.Response;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationServiceTest {

    private static final UUID USER_ID = UUID.fromString("dc3f786d-4ad4-4b5d-a79f-6e35a6520ace");
    private static final String ORGANISATION_NAME = "Possession Claims Solicitor Org";
    private static final String ORGANISATION_IDENTIFIER = "ORG-123";
    private static final String S2S_TOKEN = "test-s2s-token";
    private static final String PRD_ADMIN_TOKEN = "Bearer test-prd-admin-token";

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    private OrganisationService organisationService;

    @BeforeEach
    void setUp() {
        organisationService = new OrganisationService(
            securityContextService,
            organisationDetailsService
        );
    }

    @Test
    @DisplayName("Should successfully retrieve organisation details")
    void getOrganisationDetails_shouldSuccessfullyRetrieveOrganisationDetails() {
        stubPrd(OrganisationDetailsResponse.builder()
                    .name(ORGANISATION_NAME)
                    .organisationIdentifier(ORGANISATION_IDENTIFIER)
                    .status("ACTIVE")
                    .build());

        OrganisationDetailsResponse result = organisationService.getOrganisationDetails(USER_ID.toString());

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(ORGANISATION_NAME);
        assertThat(result.getOrganisationIdentifier()).isEqualTo(ORGANISATION_IDENTIFIER);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        verify(organisationDetailsService).getOrganisationDetails(USER_ID.toString());
    }

    @Test
    @DisplayName("Should return null when retrieving organisation details and an exception occurs")
    void getOrganisationDetails_shouldReturnNullWhenAnExceptionOccurs() {
        when(organisationDetailsService.getOrganisationDetails(USER_ID.toString()))
            .thenThrow(new OrganisationDetailsException("", null));

        OrganisationDetailsResponse result = organisationService.getOrganisationDetails(USER_ID.toString());

        assertThat(result).isNull();
        verify(organisationDetailsService).getOrganisationDetails(USER_ID.toString());
    }

    @Test
    @DisplayName("Should successfully retrieve organisation name for current user")
    void shouldSuccessfullyRetrieveOrganisationNameForCurrentUser() {
        // Given
        stubCurrentUser();
        stubPrd(OrganisationDetailsResponse.builder().name(ORGANISATION_NAME).build());

        // When
        String result = organisationService.getOrganisationNameForCurrentUser();

        // Then
        assertThat(result).isEqualTo(ORGANISATION_NAME);
        verify(securityContextService).getCurrentUserId();
        verify(organisationDetailsService).getOrganisationDetails(USER_ID.toString());
    }

    @Test
    @DisplayName("Should retrieve organisation profile ids skipping the generic profile")
    void shouldRetrieveOrgProfileIdsSkippingGenericProfile() {
        stubCurrentUser();
        stubPrd(OrganisationDetailsResponse.builder()
                    .organisationIdentifier(ORGANISATION_IDENTIFIER)
                    .organisationProfileIds(List.of("ORGANISATION_PROFILE", "LOCALAUTH_PROFILE"))
                    .build());

        String result = organisationService.getOrgProfileIdForCurrentUser();

        assertThat(result).isEqualTo("LOCALAUTH_PROFILE");
    }

    @Test
    @DisplayName("Should return null profile ids when absent from the organisation details")
    void shouldReturnNullWhenProfileIdsAbsent() {
        stubCurrentUser();
        stubPrd(OrganisationDetailsResponse.builder()
                    .organisationIdentifier(ORGANISATION_IDENTIFIER)
                    .build());

        String result = organisationService.getOrgProfileIdForCurrentUser();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should successfully retrieve organisation ID for current user")
    void shouldSuccessfullyRetrieveOrganisationIdForCurrentUser() {
        stubCurrentUser();
        stubRequiredPrd(OrganisationDetailsResponse.builder()
                            .organisationIdentifier(ORGANISATION_IDENTIFIER)
                            .build());

        String result = organisationService.getOrganisationIdForCurrentUser();

        assertThat(result).isEqualTo(ORGANISATION_IDENTIFIER);
        verify(organisationDetailsService).requireOrganisationDetails(USER_ID.toString());
    }

    @Test
    @DisplayName("Should return null for the system user without calling rd-professional")
    void getOrganisationIdForCurrentUser_ShouldReturnNullForSystemUser() {
        when(securityContextService.isSystemUser()).thenReturn(true);

        String result = organisationService.getOrganisationIdForCurrentUser();

        assertThat(result).isNull();
        verifyNoInteractions(organisationDetailsService);
    }

    @Test
    @DisplayName("Should return null when user ID is null")
    void getOrganisationIdForCurrentUser_ShouldReturnNullWhenUserIdIsNull() {
        String result = organisationService.getOrganisationIdForCurrentUser();

        assertThat(result).isNull();
        verify(organisationDetailsService, never()).requireOrganisationDetails(USER_ID.toString());
    }

    @Test
    @DisplayName("Should return null when exception thrown")
    void getOrganisationIdForCurrentUser_ShouldReturnNullWhenOrganisationDetailsExceptionThrown() {
        stubCurrentUser();
        when(organisationDetailsService.requireOrganisationDetails(USER_ID.toString()))
            .thenThrow(new OrganisationDetailsException("", null));

        String result = organisationService.getOrganisationIdForCurrentUser();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when exception thrown")
    void getOrganisationIdForCurrentUser_ShouldReturnNullWhenSecurityContextExceptionThrown() {
        when(securityContextService.getCurrentUserId()).thenThrow(new SecurityContextException(""));

        String result = organisationService.getOrganisationIdForCurrentUser();

        assertThat(result).isNull();
        verify(organisationDetailsService, never()).requireOrganisationDetails(USER_ID.toString());
    }

    @Test
    @DisplayName("Should successfully retrieve organisation ID for current user when required")
    void shouldSuccessfullyRetrieveRequiredOrganisationIdForCurrentUser() {
        stubCurrentUser();
        stubRequiredPrd(OrganisationDetailsResponse.builder()
                            .organisationIdentifier(ORGANISATION_IDENTIFIER)
                            .build());

        String result = organisationService.requireOrganisationIdForCurrentUser();

        assertThat(result).isEqualTo(ORGANISATION_IDENTIFIER);
        verify(organisationDetailsService).requireOrganisationDetails(USER_ID.toString());
    }

    @Test
    @DisplayName("Should return null for the system user without calling rd-professional")
    void requireOrganisationIdForCurrentUser_ShouldReturnNullForSystemUser() {
        when(securityContextService.isSystemUser()).thenReturn(true);

        String result = organisationService.requireOrganisationIdForCurrentUser();

        assertThat(result).isNull();
        verifyNoInteractions(organisationDetailsService);
    }

    @Test
    @DisplayName("Should return null when user ID is null")
    void requireOrganisationIdForCurrentUser_ShouldReturnNullWhenUserIdIsNull() {
        String result = organisationService.requireOrganisationIdForCurrentUser();

        assertThat(result).isNull();
        verify(organisationDetailsService, never()).requireOrganisationDetails(USER_ID.toString());
    }

    @Test
    @DisplayName("Should throw OrganisationDetailsException")
    void requireOrganisationIdForCurrentUser_ShouldThrowExceptionWhenOrganisationDetailsExceptionThrown() {
        stubCurrentUser();
        when(organisationDetailsService.requireOrganisationDetails(USER_ID.toString()))
            .thenThrow(new OrganisationDetailsException("", null));

        assertThatThrownBy(() -> organisationService.requireOrganisationIdForCurrentUser())
            .isInstanceOf(OrganisationDetailsException.class);
    }

    @Test
    @DisplayName("Should throw SecurityContextException")
    void requireOrganisationIdForCurrentUser_ShouldReturnNullWhenSecurityContextExceptionThrown() {
        when(securityContextService.getCurrentUserId()).thenThrow(new SecurityContextException(""));

        assertThatThrownBy(() -> organisationService.requireOrganisationIdForCurrentUser())
            .isInstanceOf(SecurityContextException.class);
    }

    @Test
    @DisplayName("Should return null when user ID is null")
    void shouldReturnNullWhenUserIdIsNull() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(null);

        // When
        String result = organisationService.getOrganisationNameForCurrentUser();

        // Then
        assertThat(result).isNull();
        verify(securityContextService).getCurrentUserId();
    }

    @Test
    @DisplayName("Should return null when organisation name is null")
    void shouldReturnNullWhenOrganisationNameIsNull() {
        // Given
        stubCurrentUser();
        stubPrd(OrganisationDetailsResponse.builder()
                    .organisationIdentifier(ORGANISATION_IDENTIFIER)
                    .build());

        // When
        String result = organisationService.getOrganisationNameForCurrentUser();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when OrganisationDetailsService throws exception")
    void shouldReturnNullWhenOrganisationDetailsServiceThrowsException() {
        // Given
        stubCurrentUser();
        when(organisationDetailsService.getOrganisationDetails(USER_ID.toString()))
            .thenThrow(new RuntimeException("Service unavailable"));

        // When
        String result = organisationService.getOrganisationNameForCurrentUser();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when SecurityContextService throws exception")
    void shouldReturnNullWhenSecurityContextServiceThrowsException() {
        // Given
        when(securityContextService.getCurrentUserId())
            .thenThrow(new RuntimeException("Security context error"));

        // When
        String result = organisationService.getOrganisationNameForCurrentUser();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when organisation is null")
    void shouldReturnNullWhenOrganisationIsNull() {
        // When
        AddressUK result = organisationService.getOrganisationAddress(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when organisation contact information is null")
    void shouldReturnNullWhenOrganisationAddressIsNull() {
        // Given
        OrganisationDetailsResponse organisationDetails = OrganisationDetailsResponse.builder()
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .contactInformation(null)
            .build();

        // When
        AddressUK result = organisationService.getOrganisationAddress(organisationDetails);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should successfully retrieve organisation address for current user")
    void shouldSuccessfullyRetrieveOrganisationAddressForCurrentUser() {
        // Given
        OrganisationDetailsResponse organisationDetails = OrganisationDetailsResponse.builder()
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .contactInformation(List.of(
                OrganisationDetailsResponse.ContactInformation.builder()
                    .addressLine1("27 Feather street")
                    .townCity("London")
                    .postCode("B8 7FH")
                    .build()
            )).build();

        AddressUK orgAddress = AddressUK.builder()
            .addressLine1("27 Feather street")
            .postTown("London")
            .postCode("B8 7FH")
            .build();

        // When
        AddressUK result = organisationService.getOrganisationAddress(organisationDetails);

        // Then
        assertThat(result).isEqualTo(orgAddress);
    }

    @Test
    @DisplayName("Should successfully get first organisation address")
    void shouldSuccessfullyGetOrganisationAddress() {
        // Given
        OrganisationDetailsResponse.ContactInformation contactInfo1 = OrganisationDetailsResponse.ContactInformation
            .builder()
            .addressLine1("27 Feather Street")
            .townCity("London")
            .postCode("B8 7FH")
            .build();

        OrganisationDetailsResponse.ContactInformation contactInfo2 = OrganisationDetailsResponse.ContactInformation
            .builder()
            .addressLine1("1 Additional Street")
            .townCity("London")
            .postCode("AD1 5TR")
            .build();

        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .contactInformation(List.of(contactInfo1, contactInfo2))
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .build();

        // When
        AddressUK result = organisationService.getOrganisationAddress(response);

        // Then
        assertThat(result.getAddressLine1()).isEqualTo(contactInfo1.getAddressLine1());
        assertThat(result.getPostTown()).isEqualTo(contactInfo1.getTownCity());
        assertThat(result.getPostCode()).isEqualTo(contactInfo1.getPostCode());
    }

    @Test
    @DisplayName("The organisation is resolved once and reused, not fetched per draft operation")
    void shouldResolveTheOrganisationOnlyOnceForRepeatedLookups() {
        stubCurrentUser();
        stubRequiredPrd(OrganisationDetailsResponse.builder()
                            .organisationIdentifier(ORGANISATION_IDENTIFIER)
                            .build());

        assertThat(organisationService.getOrganisationIdForCurrentUser()).isEqualTo(ORGANISATION_IDENTIFIER);
        assertThat(organisationService.getOrganisationIdForCurrentUser()).isEqualTo(ORGANISATION_IDENTIFIER);

        verify(organisationDetailsService, times(1)).requireOrganisationDetails(anyString());
    }

    @Test
    @DisplayName("Having no organisation is a settled answer and is reused")
    void shouldReuseTheAnswerThatAUserHasNoOrganisation() {
        stubCurrentUser();
        stubRequiredPrd(null);

        assertThat(organisationService.getOrganisationIdForCurrentUser()).isNull();
        assertThat(organisationService.getOrganisationIdForCurrentUser()).isNull();

        verify(organisationDetailsService, times(1)).requireOrganisationDetails(anyString());
    }

    @Test
    @DisplayName("A failed lookup must not be remembered: it would extend a blip into a stale answer")
    void shouldNotReuseAFailedLookup() {
        stubCurrentUser();
        when(organisationDetailsService.requireOrganisationDetails(anyString()))
            .thenThrow(new OrganisationDetailsException("rd-professional unavailable", new RuntimeException()))
            .thenReturn(OrganisationDetailsResponse.builder()
                            .organisationIdentifier(ORGANISATION_IDENTIFIER)
                            .build());

        assertThat(organisationService.getOrganisationIdForCurrentUser()).isNull();
        assertThat(organisationService.getOrganisationIdForCurrentUser()).isEqualTo(ORGANISATION_IDENTIFIER);

        verify(organisationDetailsService, times(2)).requireOrganisationDetails(anyString());
    }

    @Test
    @DisplayName("A blip in rd-professional must not be remembered as the user having no organisation")
    void shouldRetryAfterATransientOrganisationLookupFailure() {
        RdProfessionalApi rdProfessionalApi = mock(RdProfessionalApi.class);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenThrow(feignError(500))
            .thenReturn(OrganisationDetailsResponse.builder()
                            .organisationIdentifier(ORGANISATION_IDENTIFIER)
                            .build());

        OrganisationService service = serviceBackedBy(rdProfessionalApi);
        stubCurrentUser();

        assertThat(service.getOrganisationIdForCurrentUser()).isNull();
        assertThat(service.getOrganisationIdForCurrentUser()).isEqualTo(ORGANISATION_IDENTIFIER);

        verify(rdProfessionalApi, times(2)).getOrganisationDetails(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("A user rd-professional does not hold is a settled answer and is not looked up again")
    void shouldNotRepeatTheLookupForAUserWithNoOrganisation() {
        RdProfessionalApi rdProfessionalApi = mock(RdProfessionalApi.class);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenThrow(feignError(404));

        OrganisationService service = serviceBackedBy(rdProfessionalApi);
        stubCurrentUser();

        assertThat(service.getOrganisationIdForCurrentUser()).isNull();
        assertThat(service.getOrganisationIdForCurrentUser()).isNull();

        verify(rdProfessionalApi, times(1)).getOrganisationDetails(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip the rd-professional lookup for a citizen user")
    void shouldSkipOrganisationLookupForCitizen() {
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().roles(List.of(UserRole.CITIZEN.getRole())).build());

        String result = organisationService.getOrganisationIdForCurrentUser();

        assertThat(result).isNull();
        verify(organisationDetailsService, never()).requireOrganisationDetails(anyString());
    }

    @Test
    @DisplayName("Should successfully get organisation name")
    void shouldSuccessfullyGetOrganisationName() {
        // Given
        stubPrd(OrganisationDetailsResponse.builder().name(ORGANISATION_NAME).build());

        // When
        String result = organisationService.getOrganisationName(USER_ID.toString());

        // Then
        assertThat(result).isEqualTo(ORGANISATION_NAME);
    }

    @Test
    @DisplayName("Should successfully get organisation identifier")
    void shouldSuccessfullyGetOrganisationIdentifier() {
        // Given
        stubPrd(OrganisationDetailsResponse.builder()
                    .name(ORGANISATION_NAME)
                    .organisationIdentifier(ORGANISATION_IDENTIFIER)
                    .build());

        // When
        String result = organisationService.getOrganisationIdentifier(USER_ID.toString());

        // Then
        assertThat(result).isEqualTo(ORGANISATION_IDENTIFIER);
    }

    @Test
    @DisplayName("Should successfully get organisation payment accounts")
    void shouldSuccessfullyGetOrganisationPaymentAccounts() {
        // Given
        List<String> paymentAccounts = List.of("PBA1234567", "PBA7654321");
        stubPrd(OrganisationDetailsResponse.builder().paymentAccount(paymentAccounts).build());

        // When
        List<String> result = organisationService.getOrganisationPaymentAccount(USER_ID.toString());

        // Then
        assertThat(result).isEqualTo(paymentAccounts);
    }

    @Test
    @DisplayName("Should return null when organisation payment accounts are null")
    void shouldReturnNullWhenOrganisationPaymentAccountsAreNull() {
        // Given
        stubPrd(OrganisationDetailsResponse.builder().build());

        // When
        List<String> result = organisationService.getOrganisationPaymentAccount(USER_ID.toString());

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getOrganisationName should return null when underlying call throws FeignException")
    void getOrganisationNameShouldReturnNullOnFeignFailure() {
        // Given
        RdProfessionalApi rdProfessionalApi = mock(RdProfessionalApi.class);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenThrow(feignError(503));

        OrganisationService service = serviceBackedBy(rdProfessionalApi);

        // When
        String result = service.getOrganisationName(USER_ID.toString());

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("A null response body must not blow up the identifier accessor")
    void shouldReturnNullIdentifierWhenTheResponseBodyIsNull() {
        RdProfessionalApi rdProfessionalApi = mock(RdProfessionalApi.class);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(null);

        OrganisationService service = serviceBackedBy(rdProfessionalApi);

        assertThat(service.getOrganisationIdentifier(USER_ID.toString())).isNull();
    }

    private void stubCurrentUser() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
    }

    private void stubPrd(OrganisationDetailsResponse response) {
        when(organisationDetailsService.getOrganisationDetails(USER_ID.toString())).thenReturn(response);
    }

    private void stubRequiredPrd(OrganisationDetailsResponse response) {
        when(organisationDetailsService.requireOrganisationDetails(USER_ID.toString())).thenReturn(response);
    }

    private OrganisationService serviceBackedBy(RdProfessionalApi rdProfessionalApi) {
        AuthTokenGenerator authTokenGenerator = mock(AuthTokenGenerator.class);
        IdamTokenProvider prdAdminTokenProvider = mock(IdamTokenProvider.class);
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);

        return new OrganisationService(
            securityContextService,
            new OrganisationDetailsService(rdProfessionalApi, authTokenGenerator, prdAdminTokenProvider)
        );
    }

    private static FeignException feignError(int status) {
        Request request = Request.create(
            Request.HttpMethod.GET, "/orgDetails", Map.of(), null,
            StandardCharsets.UTF_8, null
        );
        return FeignException.errorStatus(
            "getOrganisationDetails",
            Response.builder().status(status).reason("test").request(request).headers(Map.of()).build()
        );
    }
}
