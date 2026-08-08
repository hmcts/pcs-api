package uk.gov.hmcts.reform.pcs.reference.service;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import uk.gov.hmcts.reform.pcs.reference.dto.NameAndAddress;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.List;

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
    @DisplayName("Should successfully get organisation name")
    void shouldSuccessfullyGetOrganisationName() {
        // Given
        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .name(ORGANISATION_NAME)
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .build();

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(response);

        // When
        String result = underTest.getOrganisationName(USER_ID);

        // Then
        assertThat(result).isEqualTo(ORGANISATION_NAME);
    }

    @Test
    @DisplayName("Should successfully get organisation identifier")
    void shouldSuccessfullyGetOrganisationIdentifier() {
        // Given
        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .name(ORGANISATION_NAME)
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .build();

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(response);

        // When
        String result = underTest.getOrganisationIdentifier(USER_ID);

        // Then
        assertThat(result).isEqualTo(ORGANISATION_IDENTIFIER);
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
    @DisplayName("Should return null when organisation details is null")
    void shouldReturnNullWhenOrganisationDetailsIsNull() {
        // Given
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(null);

        // When
        AddressUK result = underTest.getOrganisationAddress(USER_ID);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when contact information is empty")
    void shouldReturnNullWhenContactInformationIsEmpty() {
        // Given
        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .contactInformation(List.of())
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .build();

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(response);

        // When
        AddressUK result = underTest.getOrganisationAddress(USER_ID);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should successfully get first organisation address")
    void shouldSuccessfullyGetOrganisationAddress() {
        // Given
        OrganisationDetailsResponse.ContactInformation contactInfo1 =  OrganisationDetailsResponse.ContactInformation
            .builder()
            .addressLine1("27 Feather Street")
            .townCity("London")
            .postCode("B8 7FH")
            .build();

        OrganisationDetailsResponse.ContactInformation contactInfo2 =  OrganisationDetailsResponse.ContactInformation
            .builder()
            .addressLine1("1 Additional Street")
            .townCity("London")
            .postCode("AD1 5TR")
            .build();

        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .contactInformation(List.of(contactInfo1, contactInfo2))
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .build();

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(response);

        // When
        AddressUK result = underTest.getOrganisationAddress(USER_ID);

        // Then
        assertThat(result.getAddressLine1()).isEqualTo(contactInfo1.getAddressLine1());
        assertThat(result.getPostTown()).isEqualTo(contactInfo1.getTownCity());
        assertThat(result.getPostCode()).isEqualTo(contactInfo1.getPostCode());
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

    @Test
    @DisplayName("getOrganisationName should propagate OrganisationDetailsException when underlying call throws Feign")
    void getOrganisationNameShouldPropagateOrganisationDetailsExceptionOnFeignFailure() {
        // Given
        FeignException feignEx = mock(FeignException.class);
        when(feignEx.status()).thenReturn(503);

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenThrow(feignEx);

        // When / Then
        assertThatThrownBy(() -> underTest.getOrganisationName(USER_ID))
            .isInstanceOf(OrganisationDetailsException.class)
            .hasCause(feignEx);
    }

    @Test
    void shouldGetNameAndAddress() {
        // Given
        OrganisationDetailsResponse.ContactInformation primaryContact =
            OrganisationDetailsResponse.ContactInformation.builder().addressLine1("21 ZYZ").townCity("London")
                .county("Greater London").country("UK").postCode("B8 7FH").build();

        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .name(ORGANISATION_NAME)
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .contactInformation(List.of(primaryContact))
            .build();

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(response);

        // When
        NameAndAddress result = underTest.getNameAndAddress(USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(ORGANISATION_NAME);
        assertThat(result.address()).isNotNull();
        assertThat(result.address().getAddressLine1()).isEqualTo("21 ZYZ");
        assertThat(result.address().getPostTown()).isEqualTo("London");
        assertThat(result.address().getCounty()).isEqualTo("Greater London");
        assertThat(result.address().getCountry()).isEqualTo("UK");
        assertThat(result.address().getPostCode()).isEqualTo("B8 7FH");

        verify(rdProfessionalApi).getOrganisationDetails(USER_ID, S2S_TOKEN, PRD_ADMIN_TOKEN);
    }

    @Test
    void shouldGetNameAndAddressWithNullAddressWhenContactInformationEmpty() {
        // Given
        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .name(ORGANISATION_NAME)
            .organisationIdentifier(ORGANISATION_IDENTIFIER)
            .contactInformation(List.of())
            .build();

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(response);

        // When
        NameAndAddress result = underTest.getNameAndAddress(USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(ORGANISATION_NAME);
        assertThat(result.address()).isNull();
    }

    @Test
    void shouldGetNameAndAddressShouldPropagateOrganisationDetailsExceptionOnFeignFailure() {
        // Given
        FeignException feignEx = mock(FeignException.class);
        when(feignEx.status()).thenReturn(500);

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenThrow(feignEx);

        // When / Then
        assertThatThrownBy(() -> underTest.getNameAndAddress(USER_ID))
            .isInstanceOf(OrganisationDetailsException.class)
            .hasMessage("Failed to retrieve organisation details")
            .hasCause(feignEx);
    }

    @Test
    void shouldGetNameAndAddressShouldThrowWhenOrganisationDetailsIsNull() {
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> underTest.getNameAndAddress(USER_ID))
            .isInstanceOf(NullPointerException.class);
    }

}
