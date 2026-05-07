package uk.gov.hmcts.reform.pcs.reference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    private ReferenceDataService referenceDataService;

    private static final String USER_ID = "dc3f786d-4ad4-4b5d-a79f-6e35a6520ace";
    private static final String ORGANISATION_NAME = "Possession Claims Solicitor Org";
    private static final String ORGANISATION_IDENTIFIER = "ORG123456";

    @BeforeEach
    void setUp() {
        referenceDataService = new ReferenceDataService(organisationDetailsService);
    }

    @Test
    @DisplayName("Should successfully get organisation details")
    void shouldSuccessfullyGetOrganisationDetails() {
        // Given
        OrganisationDetailsResponse expectedDetails = OrganisationDetailsResponse.builder()
            .name(ORGANISATION_NAME)
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .status("ACTIVE")
            .sraRegulated(true)
            .build();

        when(organisationDetailsService.getOrganisationDetails(USER_ID)).thenReturn(expectedDetails);

        // When
        OrganisationDetailsResponse result = referenceDataService.getOrganisationDetails(USER_ID);

        // Then
        assertThat(result).isEqualTo(expectedDetails);
        verify(organisationDetailsService).getOrganisationDetails(USER_ID);
    }

    @Test
    @DisplayName("Should successfully get organisation name")
    void shouldSuccessfullyGetOrganisationName() {
        // Given
        when(organisationDetailsService.getOrganisationName(USER_ID)).thenReturn(ORGANISATION_NAME);

        // When
        String result = referenceDataService.getOrganisationName(USER_ID);

        // Then
        assertThat(result).isEqualTo(ORGANISATION_NAME);
        verify(organisationDetailsService).getOrganisationName(USER_ID);
    }

    @Test
    @DisplayName("Should successfully get organisation identifier")
    void shouldSuccessfullyGetOrganisationIdentifier() {
        // Given
        when(organisationDetailsService.getOrganisationIdentifier(USER_ID)).thenReturn(ORGANISATION_IDENTIFIER);

        // When
        String result = referenceDataService.getOrganisationIdentifier(USER_ID);

        // Then
        assertThat(result).isEqualTo(ORGANISATION_IDENTIFIER);
        verify(organisationDetailsService).getOrganisationIdentifier(USER_ID);
    }

    @Test
    @DisplayName("Should successfully populate claimant information")
    void shouldSuccessfullyPopulateClaimantInformation() {
        // Given
        OrganisationDetailsResponse details = OrganisationDetailsResponse.builder()
            .name(ORGANISATION_NAME)
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .status("ACTIVE")
            .sraRegulated(true)
            .build();

        when(organisationDetailsService.getOrganisationDetails(USER_ID)).thenReturn(details);

        // When
        ReferenceDataService.ClaimantInformation result = referenceDataService.populateClaimantInformation(USER_ID);

        // Then
        assertThat(result.getName()).isEqualTo(ORGANISATION_NAME);
        assertThat(result.getOrganisationIdentifier()).isEqualTo(ORGANISATION_IDENTIFIER);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getSraRegulated()).isTrue();
        verify(organisationDetailsService).getOrganisationDetails(USER_ID);
    }

    @Test
    @DisplayName("Should throw exception when organisation details service fails")
    void shouldThrowExceptionWhenOrganisationDetailsServiceFails() {
        // Given
        when(organisationDetailsService.getOrganisationDetails(anyString()))
            .thenThrow(new RuntimeException("Organisation service error"));

        // When & Then
        assertThatThrownBy(() -> referenceDataService.getOrganisationDetails(USER_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Organisation service error");
    }

    @Test
    @DisplayName("Should throw exception when organisation name service fails")
    void shouldThrowExceptionWhenOrganisationNameServiceFails() {
        // Given
        when(organisationDetailsService.getOrganisationName(anyString()))
            .thenThrow(new RuntimeException("Organisation service error"));

        // When & Then
        assertThatThrownBy(() -> referenceDataService.getOrganisationName(USER_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Organisation service error");
    }

    @Test
    @DisplayName("Should throw exception when organisation identifier service fails")
    void shouldThrowExceptionWhenOrganisationIdentifierServiceFails() {
        // Given
        when(organisationDetailsService.getOrganisationIdentifier(anyString()))
            .thenThrow(new RuntimeException("Organisation service error"));

        // When & Then
        assertThatThrownBy(() -> referenceDataService.getOrganisationIdentifier(USER_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Organisation service error");
    }

    @Test
    @DisplayName("Should throw exception when populate claimant information fails")
    void shouldThrowExceptionWhenPopulateClaimantInformationFails() {
        // Given
        when(organisationDetailsService.getOrganisationDetails(anyString()))
            .thenThrow(new RuntimeException("Organisation service error"));

        // When & Then
        assertThatThrownBy(() -> referenceDataService.populateClaimantInformation(USER_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Organisation service error");
    }
}