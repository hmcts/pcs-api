package uk.gov.hmcts.reform.pcs.reference.service;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationDetailsServiceTest {

    private static final String USER_ID = "dc3f786d-4ad4-4b5d-a79f-6e35a6520ace";
    private static final String S2S_TOKEN = "test-s2s-token";
    private static final String PRD_ADMIN_TOKEN = "Bearer test-prd-admin-token";
    private static final String ORGANISATION_NAME = "Possession Claims Solicitor Org";
    private static final String ORGANISATION_IDENTIFIER = "E71FH4Q";

    @Mock
    private RdProfessionalApi rdProfessionalApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private IdamTokenProvider prdAdminTokenProvider;

    private OrganisationDetailsService underTest;

    @BeforeEach
    void setUp() {
        underTest = new OrganisationDetailsService(
            rdProfessionalApi,
            authTokenGenerator,
            prdAdminTokenProvider
        );
    }

    @Test
    @DisplayName("Should successfully retrieve organisation details")
    void shouldSuccessfullyRetrieveOrganisationDetails() {
        // Given
        OrganisationDetailsResponse expectedResponse = OrganisationDetailsResponse.builder()
            .name(ORGANISATION_NAME)
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .status("ACTIVE")
            .build();

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(expectedResponse);

        // When
        OrganisationDetailsResponse result = underTest.getOrganisationDetails(USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(ORGANISATION_NAME);
        assertThat(result.getOrganisationIdentifier()).isEqualTo(ORGANISATION_IDENTIFIER);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        verify(authTokenGenerator).generate();
        verify(prdAdminTokenProvider).getAuthToken();
        verify(rdProfessionalApi).getOrganisationDetails(USER_ID, S2S_TOKEN, PRD_ADMIN_TOKEN);
    }

    @Test
    @DisplayName("Should successfully retrieve null organisation details")
    void shouldSuccessfullyRetrieveNullOrganisationDetails() {
        // Given
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(null);

        // When
        OrganisationDetailsResponse result = underTest.getOrganisationDetails(USER_ID);

        // Then
        assertThat(result).isNull();

        verify(authTokenGenerator).generate();
        verify(prdAdminTokenProvider).getAuthToken();
        verify(rdProfessionalApi).getOrganisationDetails(USER_ID, S2S_TOKEN, PRD_ADMIN_TOKEN);
    }

    @Test
    @DisplayName("Should throw OrganisationDetailsException when Feign client throws exception")
    void shouldThrowOrganisationDetailsExceptionWhenFeignClientThrowsException() {
        // Given
        RuntimeException runtimeException = new RuntimeException("Feign client error");

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenThrow(runtimeException);

        // When & Then
        assertThatThrownBy(() -> underTest.getOrganisationDetails(USER_ID))
            .isInstanceOf(OrganisationDetailsException.class)
            .hasMessage("Unexpected error retrieving organisation details")
            .hasCause(runtimeException);
    }

    @Test
    @DisplayName("Should throw OrganisationDetailsException when general exception occurs")
    void shouldThrowOrganisationDetailsExceptionWhenGeneralExceptionOccurs() {
        // Given
        RuntimeException generalException = new RuntimeException("Connection failed");

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenThrow(generalException);

        // When & Then
        assertThatThrownBy(() -> underTest.getOrganisationDetails(USER_ID))
            .isInstanceOf(OrganisationDetailsException.class)
            .hasMessage("Unexpected error retrieving organisation details")
            .hasCause(generalException);
    }

    @Test
    @DisplayName("Should wrap FeignException as OrganisationDetailsException with feign cause")
    void shouldWrapFeignExceptionAsOrganisationDetailsException() {
        // Given
        FeignException feignEx = mock(FeignException.class);
        when(feignEx.status()).thenReturn(500);
        when(feignEx.getMessage()).thenReturn("PRD upstream failure");

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenThrow(feignEx);

        // When / Then
        assertThatThrownBy(() -> underTest.getOrganisationDetails(USER_ID))
            .isInstanceOf(OrganisationDetailsException.class)
            .hasMessage("Failed to retrieve organisation details")
            .hasCause(feignEx);

        verify(rdProfessionalApi).getOrganisationDetails(USER_ID, S2S_TOKEN, PRD_ADMIN_TOKEN);
    }

    @Test
    @DisplayName("Should wrap unexpected RuntimeException as OrganisationDetailsException")
    void shouldWrapUnexpectedExceptionAsOrganisationDetailsException() {
        // Given — anything other than FeignException must hit the generic catch (Exception) branch.
        RuntimeException unexpected = new RuntimeException("token generator blew up");
        when(authTokenGenerator.generate()).thenThrow(unexpected);

        // When / Then
        assertThatThrownBy(() -> underTest.getOrganisationDetails(USER_ID))
            .isInstanceOf(OrganisationDetailsException.class)
            .hasMessage("Unexpected error retrieving organisation details")
            .hasCause(unexpected);
    }

}
