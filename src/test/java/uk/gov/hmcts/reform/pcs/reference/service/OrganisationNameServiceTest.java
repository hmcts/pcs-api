package uk.gov.hmcts.reform.pcs.reference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationNameServiceTest {

    private static final UUID USER_ID = UUID.fromString("dc3f786d-4ad4-4b5d-a79f-6e35a6520ace");
    private static final String ORGANISATION_NAME = "Possession Claims Solicitor Org";

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    private OrganisationNameService organisationNameService;

    @BeforeEach
    void setUp() {
        organisationNameService = new OrganisationNameService(
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
        String result = organisationNameService.getOrganisationNameForCurrentUser();

        // Then
        assertThat(result).isEqualTo(ORGANISATION_NAME);
        verify(securityContextService).getCurrentUserId();
        verify(organisationDetailsService).getOrganisationName(USER_ID.toString());
    }

    @Test
    @DisplayName("Should return null when user ID is null")
    void shouldReturnNullWhenUserIdIsNull() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(null);

        // When
        String result = organisationNameService.getOrganisationNameForCurrentUser();

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
        String result = organisationNameService.getOrganisationNameForCurrentUser();

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
        String result = organisationNameService.getOrganisationNameForCurrentUser();

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
        String result = organisationNameService.getOrganisationNameForCurrentUser();

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
        String result = organisationNameService.getOrganisationNameForCurrentUser();

        // Then
        assertThat(result).isNull();
    }
}
