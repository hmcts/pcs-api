package uk.gov.hmcts.reform.pcs.reference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationServiceTest {

    private static final UUID USER_ID = UUID.fromString("dc3f786d-4ad4-4b5d-a79f-6e35a6520ace");
    private static final String ORGANISATION_NAME = "Possession Claims Solicitor Org";
    private static final String ORGANISATION_IDENTIFIER = "ORG-123";

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
    @DisplayName("Should successfully retrieve organisation name for current user")
    void shouldSuccessfullyRetrieveOrganisationNameForCurrentUser() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationName(USER_ID.toString()))
            .thenReturn(ORGANISATION_NAME);

        // When
        String result = organisationService.getOrganisationNameForCurrentUser();

        // Then
        assertThat(result).isEqualTo(ORGANISATION_NAME);
        verify(securityContextService).getCurrentUserId();
        verify(organisationDetailsService).getOrganisationName(USER_ID.toString());
    }

    @Test
    @DisplayName("Should retrieve organisation profile ids skipping the generic profile")
    void shouldRetrieveOrgProfileIdsSkippingGenericProfile() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationDetails(USER_ID.toString()))
            .thenReturn(OrganisationDetailsResponse.builder()
                            .organisationIdentifier(ORGANISATION_IDENTIFIER)
                            .organisationProfileIds(List.of("ORGANISATION_PROFILE", "LOCALAUTH_PROFILE"))
                            .build());

        String result = organisationService.getOrgProfileIdForCurrentUser();

        assertThat(result).isEqualTo("LOCALAUTH_PROFILE");
    }

    @Test
    @DisplayName("Should return null profile ids when absent from the organisation details")
    void shouldReturnNullWhenProfileIdsAbsent() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationDetails(USER_ID.toString()))
            .thenReturn(OrganisationDetailsResponse.builder()
                            .organisationIdentifier(ORGANISATION_IDENTIFIER)
                            .build());

        String result = organisationService.getOrgProfileIdForCurrentUser();

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should successfully retrieve organisation ID for current user")
    void shouldSuccessfullyRetrieveOrganisationIdForCurrentUser() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationIdentifier(USER_ID.toString()))
            .thenReturn(ORGANISATION_IDENTIFIER);

        String result = organisationService.getOrganisationIdForCurrentUser();

        assertThat(result).isEqualTo(ORGANISATION_IDENTIFIER);
        verify(organisationDetailsService).getOrganisationIdentifier(USER_ID.toString());
    }

    @Test
    @DisplayName("Should return null when user ID is null")
    void getOrganisationIdForCurrentUser_ShouldReturnNullWhenUserIdIsNull() {
        String result = organisationService.getOrganisationIdForCurrentUser();

        assertThat(result).isNull();
        verify(organisationDetailsService, never()).getOrganisationIdentifier(USER_ID.toString());
    }

    @Test
    @DisplayName("Should return null when exception thrown")
    void getOrganisationIdForCurrentUser_ShouldReturnNullWhenOrganisationDetailsExceptionThrown() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationIdentifier(USER_ID.toString()))
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
        verify(organisationDetailsService, never()).getOrganisationIdentifier(USER_ID.toString());
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
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationName(USER_ID.toString()))
            .thenReturn(null);

        // When
        String result = organisationService.getOrganisationNameForCurrentUser();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when organisation name is empty")
    void shouldReturnNullWhenOrganisationNameIsEmpty() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationName(USER_ID.toString()))
            .thenReturn("");

        // When
        String result = organisationService.getOrganisationNameForCurrentUser();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return null when OrganisationDetailsService throws exception")
    void shouldReturnNullWhenOrganisationDetailsServiceThrowsException() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationName(anyString()))
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
    @DisplayName("Should return null when organisation address key fields are empty")
    void shouldReturnNullWhenOrganisationAddressIsEmpty() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationAddress(USER_ID.toString()))
            .thenReturn(AddressUK.builder().build());

        // When
        AddressUK result = organisationService.getOrganisationAddressForCurrentUser();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when organisation address is null")
    void shouldReturnNullWhenOrganisationAddressIsNull() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationAddress(USER_ID.toString()))
            .thenReturn(null);

        // When
        AddressUK result = organisationService.getOrganisationAddressForCurrentUser();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should successfully retrieve organisation address for current user")
    void shouldSuccessfullyRetrieveOrganisationAddressForCurrentUser() {
        // Given
        AddressUK orgAddress =  AddressUK.builder()
            .addressLine1("27 Feather street")
            .postTown("London")
            .postCode("B8 7FH")
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationAddress(USER_ID.toString()))
            .thenReturn(orgAddress);

        // When
        AddressUK result = organisationService.getOrganisationAddressForCurrentUser();

        // Then
        assertThat(result).isEqualTo(orgAddress);
        verify(securityContextService).getCurrentUserId();
        verify(organisationDetailsService).getOrganisationAddress(USER_ID.toString());
    }

    @Test
    @DisplayName("The organisation is resolved once and reused, not fetched per draft operation")
    void shouldResolveTheOrganisationOnlyOnceForRepeatedLookups() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationIdentifier(anyString()))
            .thenReturn(ORGANISATION_IDENTIFIER);

        assertThat(organisationService.getOrganisationIdForCurrentUser()).isEqualTo(ORGANISATION_IDENTIFIER);
        assertThat(organisationService.getOrganisationIdForCurrentUser()).isEqualTo(ORGANISATION_IDENTIFIER);

        verify(organisationDetailsService, times(1)).getOrganisationIdentifier(anyString());
    }

    @Test
    @DisplayName("Having no organisation is a settled answer and is reused")
    void shouldReuseTheAnswerThatAUserHasNoOrganisation() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationIdentifier(anyString())).thenReturn(null);

        assertThat(organisationService.getOrganisationIdForCurrentUser()).isNull();
        assertThat(organisationService.getOrganisationIdForCurrentUser()).isNull();

        verify(organisationDetailsService, times(1)).getOrganisationIdentifier(anyString());
    }

    @Test
    @DisplayName("A failed lookup must not be remembered: it would extend a blip into a stale answer")
    void shouldNotReuseAFailedLookup() {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(organisationDetailsService.getOrganisationIdentifier(anyString()))
            .thenThrow(new OrganisationDetailsException("rd-professional unavailable", new RuntimeException()))
            .thenReturn(ORGANISATION_IDENTIFIER);

        assertThat(organisationService.getOrganisationIdForCurrentUser()).isNull();
        assertThat(organisationService.getOrganisationIdForCurrentUser()).isEqualTo(ORGANISATION_IDENTIFIER);

        verify(organisationDetailsService, times(2)).getOrganisationIdentifier(anyString());
    }

    @DisplayName("Should skip the rd-professional lookup for a citizen user")
    void shouldSkipOrganisationLookupForCitizen() {
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().roles(List.of(UserRole.CITIZEN.getRole())).build());

        String result = organisationService.getOrganisationIdForCurrentUser();

        assertThat(result).isNull();
        verify(organisationDetailsService, never()).getOrganisationIdentifier(anyString());
    }
}
