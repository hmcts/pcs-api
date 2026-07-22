package uk.gov.hmcts.reform.pcs.reference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.reference.dto.NameAndAddress;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
    private CachingOrganisationDetailsService cachingOrganisationDetailsService;

    private OrganisationService organisationService;

    @BeforeEach
    void setUp() {
        organisationService = new OrganisationService(
            securityContextService,
            cachingOrganisationDetailsService
        );
    }

    @Nested
    @DisplayName("getNameAndAddressForCurrentUser")
    class GetNameAndAddressForCurrentUser {

        @Test
        @DisplayName("Should successfully retrieve organisation name and address for current user")
        void shouldSuccessfullyRetrieveOrganisationNameAndAddressForCurrentUser() {
            // Given
            AddressUK addressUK = AddressUK.builder()
                .addressLine1("21 Abc")
                .postTown("London")
                .postCode("L2 3FF")
                .build();
            NameAndAddress nameAndAddress = new NameAndAddress(ORGANISATION_NAME, addressUK);

            when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
            when(cachingOrganisationDetailsService.getNameAndAddress(USER_ID.toString()))
                .thenReturn(nameAndAddress);

            // When
            NameAndAddress result = organisationService.getNameAndAddressForCurrentUser();

            // Then
            assertThat(result).isEqualTo(nameAndAddress);
            verify(securityContextService).getCurrentUserId();
            verify(cachingOrganisationDetailsService).getNameAndAddress(USER_ID.toString());
        }

        @Test
        @DisplayName("Should return null when user ID is null")
        void shouldReturnNullWhenUserIdIsNull() {
            // Given
            when(securityContextService.getCurrentUserId()).thenReturn(null);

            // When
            NameAndAddress result = organisationService.getNameAndAddressForCurrentUser();

            // Then
            assertThat(result).isNull();
            verify(securityContextService).getCurrentUserId();
            verify(cachingOrganisationDetailsService, never()).getNameAndAddress(anyString());
        }

        @Test
        @DisplayName("Should return null when caching service returns null")
        void shouldReturnNullWhenCachingServiceReturnsNull() {
            // Given
            when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
            when(cachingOrganisationDetailsService.getNameAndAddress(USER_ID.toString()))
                .thenReturn(null);

            // When
            NameAndAddress result = organisationService.getNameAndAddressForCurrentUser();

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null and log error when caching service throws an exception")
        void shouldReturnNullWhenCachingServiceThrowsException() {
            // Given
            when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
            when(cachingOrganisationDetailsService.getNameAndAddress(USER_ID.toString()))
                .thenThrow(new RuntimeException("Service unavailable"));

            // When
            NameAndAddress result = organisationService.getNameAndAddressForCurrentUser();

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null and log error when security context throws an exception")
        void shouldReturnNullWhenSecurityContextThrowsException() {
            // Given
            when(securityContextService.getCurrentUserId())
                .thenThrow(new RuntimeException("Security context error"));

            // When
            NameAndAddress result = organisationService.getNameAndAddressForCurrentUser();

            // Then
            assertThat(result).isNull();
            verify(cachingOrganisationDetailsService, never()).getNameAndAddress(anyString());
        }
    }

    @Nested
    @DisplayName("getNameAndAddress")
    class GetNameAndAddress {

        @Test
        @DisplayName("Should successfully retrieve organisation name and address for provided user id")
        void shouldSuccessfullyRetrieveOrganisationNameAndAddressForCurrentUser() {
            // Given
            AddressUK addressUK = AddressUK.builder()
                .addressLine1("21 Abc")
                .postTown("London")
                .postCode("L2 3FF")
                .build();
            NameAndAddress nameAndAddress = new NameAndAddress(ORGANISATION_NAME, addressUK);

            when(cachingOrganisationDetailsService.getNameAndAddress(USER_ID.toString()))
                .thenReturn(nameAndAddress);

            // When
            NameAndAddress result = organisationService.getNameAndAddress(USER_ID.toString());

            // Then
            assertThat(result).isEqualTo(nameAndAddress);
            verify(securityContextService, never()).getCurrentUserId();
            verify(cachingOrganisationDetailsService).getNameAndAddress(USER_ID.toString());
        }
    }

    @Nested
    @DisplayName("getOrganisationIdForCurrentUser")
    class GetOrganisationIdForCurrentUser {

        @Test
        @DisplayName("Should successfully retrieve organisation ID for current user")
        void shouldSuccessfullyRetrieveOrganisationIdForCurrentUser() {
            // Given
            when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
            when(cachingOrganisationDetailsService.getOrganisationIdentifier(USER_ID.toString()))
                .thenReturn(ORGANISATION_IDENTIFIER);

            // When
            String result = organisationService.getOrganisationIdForCurrentUser();

            // Then
            assertThat(result).isEqualTo(ORGANISATION_IDENTIFIER);
            verify(cachingOrganisationDetailsService).getOrganisationIdentifier(USER_ID.toString());
        }

        @Test
        @DisplayName("Should return null when user ID is null")
        void shouldReturnNullWhenUserIdIsNull() {
            // Given
            when(securityContextService.getCurrentUserId()).thenReturn(null);

            // When
            String result = organisationService.getOrganisationIdForCurrentUser();

            // Then
            assertThat(result).isNull();
            verify(cachingOrganisationDetailsService, never()).getOrganisationIdentifier(anyString());
        }

        @Test
        @DisplayName("Should return null when OrganisationDetailsException is thrown")
        void shouldReturnNullWhenOrganisationDetailsExceptionThrown() {
            // Given
            when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
            when(cachingOrganisationDetailsService.getOrganisationIdentifier(USER_ID.toString()))
                .thenThrow(new OrganisationDetailsException("Error fetching details", null));

            // When
            String result = organisationService.getOrganisationIdForCurrentUser();

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null when SecurityContextException is thrown")
        void shouldReturnNullWhenSecurityContextExceptionThrown() {
            // Given
            when(securityContextService.getCurrentUserId()).thenThrow(new SecurityContextException("Unauthorized"));

            // When
            String result = organisationService.getOrganisationIdForCurrentUser();

            // Then
            assertThat(result).isNull();
            verify(cachingOrganisationDetailsService, never()).getOrganisationIdentifier(anyString());
        }
    }

    @Nested
    @DisplayName("getOrganisationIdF")
    class GetOrganisationId {

        @Test
        @DisplayName("Should successfully retrieve organisation ID for provided user id")
        void shouldSuccessfullyRetrieveOrganisationIdForCurrentUser() {
            // Given
            when(cachingOrganisationDetailsService.getOrganisationIdentifier(USER_ID.toString()))
                .thenReturn(ORGANISATION_IDENTIFIER);

            // When
            String result = organisationService.getOrganisationId(USER_ID.toString());

            // Then
            assertThat(result).isEqualTo(ORGANISATION_IDENTIFIER);
            verify(cachingOrganisationDetailsService).getOrganisationIdentifier(USER_ID.toString());
            verify(securityContextService, never()).getCurrentUserId();
        }
    }
}
