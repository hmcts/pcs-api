package uk.gov.hmcts.reform.pcs.reference.service;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.entity.CachedOrganisationResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CachedOrganisationResponseRepository;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import uk.gov.hmcts.reform.pcs.reference.dto.NameAndAddress;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingOrganisationDetailsServiceTest {

    @Mock
    private CachedOrganisationResponseRepository cachedOrganisationResponseRepository;

    @Mock
    private Supplier<LocalDateTime> localDateTimeSupplier;

    @Mock
    private RdProfessionalApi rdProfessionalApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private IdamTokenProvider prdAdminTokenProvider;

    @Captor
    private ArgumentCaptor<CachedOrganisationResponseEntity> cachedOrganisationResponseEntityCaptor;

    private CachingOrganisationDetailsService cachingOrganisationDetailsService;

    private static final int CACHE_TTL_IN_MINUTES = 5;
    private static final String USER_ID = "dc3f786d-4ad4-4b5d-a79f-6e35a6520ace";
    private static final String S2S_TOKEN = "test-s2s-token";
    private static final String PRD_ADMIN_TOKEN = "Bearer test-prd-admin-token";
    private static final int RD_PROFESSIONAL_NULL_RESPONSE_TLL_OFFSET = 50;

    @BeforeEach
    void setUp() {
        cachingOrganisationDetailsService = new CachingOrganisationDetailsService(cachedOrganisationResponseRepository,
                                                                                  CACHE_TTL_IN_MINUTES,
                                                                                  localDateTimeSupplier,
                                                                                  rdProfessionalApi,
                                                                                  authTokenGenerator,
                                                                                  prdAdminTokenProvider);
    }

    private void mockOrganisationDetails(OrganisationDetailsResponse expectedResponse) {
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenReturn(expectedResponse);
    }

    private void verifyNoCallsForOrganisationDetails() {
        verify(authTokenGenerator, never()).generate();
        verify(prdAdminTokenProvider, never()).getAuthToken();
        verify(rdProfessionalApi, never()).getOrganisationDetails(anyString(), anyString(), anyString());
    }


    @Test
    void getOrganisationIdentifier_WithCachedResponseNotFound_SavesNewRecord() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        String orgId = "org1";
        String orgName = "Org Name";
        String addressLine1 = "Addr1";
        String addressLine2 = "Addr2";
        String addressLine3 = "Addr3";
        String townCity = "City";
        String county = "County";
        String country = "Country";
        String postCode = "PostCode";

        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .organisationIdentifier(orgId)
            .name(orgName)
            .contactInformation(List.of(OrganisationDetailsResponse.ContactInformation.builder()
                                            .addressLine1(addressLine1)
                                            .addressLine2(addressLine2)
                                            .addressLine3(addressLine3)
                                            .townCity(townCity)
                                            .county(county)
                                            .country(country)
                                            .postCode(postCode)
                                            .build()))
            .build();

        mockOrganisationDetails(response);
        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.empty());
        when(localDateTimeSupplier.get()).thenReturn(now);
        when(cachedOrganisationResponseRepository.save(any(CachedOrganisationResponseEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        cachingOrganisationDetailsService.getOrganisationIdentifier(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());

        CachedOrganisationResponseEntity actual = cachedOrganisationResponseEntityCaptor.getValue();

        assertThat(actual.getOrganisationId()).isEqualTo(orgId);
        assertThat(actual.getIdamId()).isEqualTo(userId);
        assertThat(actual.getLastModifiedDate()).isEqualTo(now);
        assertThat(actual.getOrganisationName()).isEqualTo(orgName);
        assertThat(actual.getAddressLine1()).isEqualTo(addressLine1);
        assertThat(actual.getAddressLine2()).isEqualTo(addressLine2);
        assertThat(actual.getAddressLine3()).isEqualTo(addressLine3);
        assertThat(actual.getPostTown()).isEqualTo(townCity);
        assertThat(actual.getCounty()).isEqualTo(county);
        assertThat(actual.getCountry()).isEqualTo(country);
        assertThat(actual.getPostCode()).isEqualTo(postCode);
    }

    @Test
    void getOrganisationIdentifier_WithCachedResponseNotYetExpired_DoesNotUpdate() {
        // given
        UUID userId = UUID.randomUUID();
        String orgId = "org1";
        LocalDateTime now = LocalDateTime.now();
        CachedOrganisationResponseEntity cachedOrganisationResponseEntity = CachedOrganisationResponseEntity.builder()
            .organisationId(orgId)
            .idamId(userId)
            .lastModifiedDate(now)
            .build();
        when(cachedOrganisationResponseRepository.findByIdamId(userId))
            .thenReturn(Optional.of(cachedOrganisationResponseEntity));
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        String actual = cachingOrganisationDetailsService.getOrganisationIdentifier(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository, never()).save(any());
        verifyNoCallsForOrganisationDetails();

        assertThat(actual).isEqualTo(orgId);
    }

    @Test
    void getOrganisationIdentifier_WithCachedResponseExpired_UpdatesRecord() {
        // given
        UUID userId = UUID.randomUUID();
        String orgId = "org1";
        String orgId2 = "org2";
        LocalDateTime now = LocalDateTime.now();
        CachedOrganisationResponseEntity cachedOrganisationResponseEntity = CachedOrganisationResponseEntity.builder()
            .organisationId(orgId)
            .idamId(userId)
            .lastModifiedDate(now.minusHours(1))
            .build();

        String orgName2 = "Org Name2";
        String addressLine1 = "Addr1";
        String addressLine2 = "Addr2";
        String addressLine3 = "Addr3";
        String townCity = "City";
        String county = "County";
        String country = "Country";
        String postCode = "PostCode";

        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .organisationIdentifier(orgId2)
            .name(orgName2)
            .contactInformation(List.of(OrganisationDetailsResponse.ContactInformation.builder()
                                            .addressLine1(addressLine1)
                                            .addressLine2(addressLine2)
                                            .addressLine3(addressLine3)
                                            .townCity(townCity)
                                            .county(county)
                                            .country(country)
                                            .postCode(postCode)
                                            .build()))
            .build();
        when(cachedOrganisationResponseRepository.findByIdamId(userId))
            .thenReturn(Optional.of(cachedOrganisationResponseEntity));
        mockOrganisationDetails(response);
        when(localDateTimeSupplier.get()).thenReturn(now);
        when(cachedOrganisationResponseRepository.save(cachedOrganisationResponseEntity))
            .thenReturn(cachedOrganisationResponseEntity);

        // when
        cachingOrganisationDetailsService.getOrganisationIdentifier(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());

        CachedOrganisationResponseEntity actual = cachedOrganisationResponseEntityCaptor.getValue();

        assertThat(actual.getOrganisationId()).isEqualTo(orgId2);
        assertThat(actual.getIdamId()).isEqualTo(userId);
        assertThat(actual.getLastModifiedDate()).isEqualTo(now);

        assertThat(actual.getOrganisationId()).isEqualTo(orgId2);
        assertThat(actual.getIdamId()).isEqualTo(userId);
        assertThat(actual.getLastModifiedDate()).isEqualTo(now);
        assertThat(actual.getOrganisationName()).isEqualTo(orgName2);
        assertThat(actual.getAddressLine1()).isEqualTo(addressLine1);
        assertThat(actual.getAddressLine2()).isEqualTo(addressLine2);
        assertThat(actual.getAddressLine3()).isEqualTo(addressLine3);
        assertThat(actual.getPostTown()).isEqualTo(townCity);
        assertThat(actual.getCounty()).isEqualTo(county);
        assertThat(actual.getCountry()).isEqualTo(country);
        assertThat(actual.getPostCode()).isEqualTo(postCode);
    }

    @Test
    void getOrganisationIdentifier_WithCachedResponseExpiredAndNullOrganisationDetailsResponse_UpdatesRecord() {
        // given
        UUID userId = UUID.randomUUID();
        String orgId = "org1";
        LocalDateTime now = LocalDateTime.now();
        CachedOrganisationResponseEntity cachedOrganisationResponseEntity = CachedOrganisationResponseEntity.builder()
            .organisationId(orgId)
            .idamId(userId)
            .lastModifiedDate(now.minusHours(1))
            .build();

        when(cachedOrganisationResponseRepository.findByIdamId(userId))
            .thenReturn(Optional.of(cachedOrganisationResponseEntity));
        mockOrganisationDetails(null);
        when(localDateTimeSupplier.get()).thenReturn(now);
        when(cachedOrganisationResponseRepository.save(cachedOrganisationResponseEntity))
            .thenReturn(cachedOrganisationResponseEntity);

        // when
        cachingOrganisationDetailsService.getOrganisationIdentifier(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());

        CachedOrganisationResponseEntity actual = cachedOrganisationResponseEntityCaptor.getValue();

        assertThat(actual.getIdamId()).isEqualTo(userId);
        assertThat(actual.getLastModifiedDate()).isEqualTo(now.minusMinutes(RD_PROFESSIONAL_NULL_RESPONSE_TLL_OFFSET));

        assertThat(actual.getOrganisationId()).isEqualTo(orgId);
        assertThat(actual.getOrganisationName()).isNull();
        assertThat(actual.getAddressLine1()).isNull();
        assertThat(actual.getAddressLine2()).isNull();
        assertThat(actual.getAddressLine3()).isNull();
        assertThat(actual.getPostTown()).isNull();
        assertThat(actual.getCounty()).isNull();
        assertThat(actual.getCountry()).isNull();
        assertThat(actual.getPostCode()).isNull();
    }

    @Test
    void getNameAndAddress_WithCachedResponseNotFound_SavesNewRecord_WithContactInfo() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String orgId = "org1";
        String orgName = "Org Name2";
        String addressLine1 = "Addr1";
        String addressLine2 = "Addr2";
        String addressLine3 = "Addr3";
        String townCity = "City";
        String county = "County";
        String country = "Country";
        String postCode = "PostCode";

        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .organisationIdentifier(orgId)
            .name(orgName)
            .contactInformation(List.of(OrganisationDetailsResponse.ContactInformation.builder()
                                            .addressLine1(addressLine1)
                                            .addressLine2(addressLine2)
                                            .addressLine3(addressLine3)
                                            .townCity(townCity)
                                            .county(county)
                                            .country(country)
                                            .postCode(postCode)
                                            .build()))
            .build();
        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.empty());
        when(localDateTimeSupplier.get()).thenReturn(now);
        mockOrganisationDetails(response);
        when(cachedOrganisationResponseRepository.save(any(CachedOrganisationResponseEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.name()).isEqualTo(orgName);
        AddressUK address = actual.address();
        assertThat(address).isNotNull();
        assertThat(address.getAddressLine1()).isEqualTo(addressLine1);
        assertThat(address.getAddressLine2()).isEqualTo(addressLine2);
        assertThat(address.getAddressLine3()).isEqualTo(addressLine3);
        assertThat(address.getPostTown()).isEqualTo(townCity);
        assertThat(address.getCounty()).isEqualTo(county);
        assertThat(address.getCountry()).isEqualTo(country);
        assertThat(address.getPostCode()).isEqualTo(postCode);

        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());
        CachedOrganisationResponseEntity savedEntity = cachedOrganisationResponseEntityCaptor.getValue();

        assertThat(savedEntity.getOrganisationId()).isEqualTo(orgId);
        assertThat(savedEntity.getOrganisationName()).isEqualTo(orgName);
        assertThat(savedEntity.getAddressLine1()).isEqualTo(addressLine1);
        assertThat(savedEntity.getAddressLine2()).isEqualTo(addressLine2);
        assertThat(savedEntity.getAddressLine3()).isEqualTo(addressLine3);
        assertThat(savedEntity.getPostTown()).isEqualTo(townCity);
        assertThat(savedEntity.getCounty()).isEqualTo(county);
        assertThat(savedEntity.getCountry()).isEqualTo(country);
        assertThat(savedEntity.getPostCode()).isEqualTo(postCode);
        assertThat(savedEntity.getLastModifiedDate()).isEqualTo(now);
    }

    @Test
    void getNameAndAddress_WithCachedResponseNotFound_SavesNewRecord_WithoutContactInfo() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String orgId = "org1";
        String orgName = "Org Name2";

        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .organisationIdentifier(orgId)
            .name(orgName)
            .build();

        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.empty());
        mockOrganisationDetails(response);
        when(localDateTimeSupplier.get()).thenReturn(now);
        when(cachedOrganisationResponseRepository.save(any(CachedOrganisationResponseEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.name()).isEqualTo(orgName);
        AddressUK address = actual.address();
        assertThat(address).isNotNull();

        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());
        CachedOrganisationResponseEntity savedEntity = cachedOrganisationResponseEntityCaptor.getValue();

        assertThat(savedEntity.getOrganisationId()).isEqualTo(orgId);
        assertThat(savedEntity.getOrganisationName()).isEqualTo(orgName);
        assertThat(savedEntity.getAddressLine1()).isNull();
        assertThat(savedEntity.getAddressLine2()).isNull();
        assertThat(savedEntity.getAddressLine3()).isNull();
        assertThat(savedEntity.getPostTown()).isNull();
        assertThat(savedEntity.getPostCode()).isNull();
        assertThat(savedEntity.getCounty()).isNull();
        assertThat(savedEntity.getCountry()).isNull();
    }

    @Test
    void getNameAndAddress_WithCachedResponseNotFoundAndNullOrgDetailsResponse_SavesNewRecord_WithoutContactInfo() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.empty());
        mockOrganisationDetails(null);
        when(localDateTimeSupplier.get()).thenReturn(now);
        when(cachedOrganisationResponseRepository.save(any(CachedOrganisationResponseEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.name()).isNull();
        assertThat(actual.address()).isNotNull();
        assertThat(actual.address().getAddressLine1()).isNull();

        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());
        CachedOrganisationResponseEntity savedEntity = cachedOrganisationResponseEntityCaptor.getValue();

        assertThat(savedEntity.getIdamId()).isEqualTo(userId);
        assertThat(savedEntity.getLastModifiedDate())
            .isEqualTo(now.minusMinutes(RD_PROFESSIONAL_NULL_RESPONSE_TLL_OFFSET));
        assertThat(savedEntity.getOrganisationId()).isNull();
        assertThat(savedEntity.getOrganisationName()).isNull();
        assertThat(savedEntity.getAddressLine1()).isNull();
        assertThat(savedEntity.getAddressLine2()).isNull();
        assertThat(savedEntity.getAddressLine3()).isNull();
        assertThat(savedEntity.getPostTown()).isNull();
        assertThat(savedEntity.getPostCode()).isNull();
        assertThat(savedEntity.getCounty()).isNull();
        assertThat(savedEntity.getCountry()).isNull();
    }

    @Test
    void getNameAndAddress_WithCachedResponseNotExpired_ReturnsCachedValue() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String orgId = "org2";
        String orgName = "Org Name2";
        String addressLine1 = "Addr1";
        String addressLine2 = "Addr2";
        String addressLine3 = "Addr3";
        String townCity = "City";
        String county = "County";
        String country = "Country";
        String postCode = "PostCode";
        CachedOrganisationResponseEntity cachedEntity = CachedOrganisationResponseEntity.builder()
            .idamId(userId)
            .organisationId(orgId)
            .organisationName(orgName)
            .addressLine1(addressLine1)
            .addressLine2(addressLine2)
            .addressLine3(addressLine3)
            .postTown(townCity)
            .county(county)
            .country(country)
            .postCode(postCode)
            .lastModifiedDate(now.minusMinutes(4))
            .build();

        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.of(cachedEntity));
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository, never()).save(any());
        verifyNoCallsForOrganisationDetails();

        assertThat(actual).isNotNull();
        assertThat(actual.address().getAddressLine1()).isEqualTo(addressLine1);
        assertThat(actual.address().getAddressLine2()).isEqualTo(addressLine2);
        assertThat(actual.address().getAddressLine3()).isEqualTo(addressLine3);
        assertThat(actual.address().getPostTown()).isEqualTo(townCity);
        assertThat(actual.address().getPostCode()).isEqualTo(postCode);
        assertThat(actual.address().getCounty()).isEqualTo(county);
        assertThat(actual.address().getCountry()).isEqualTo(country);
    }

    @Test
    void getNameAndAddress_WithCachedResponseExpired_UpdatesAndReturnsNewValue() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        CachedOrganisationResponseEntity cachedEntity = CachedOrganisationResponseEntity.builder()
            .idamId(userId)
            .organisationId("org1")
            .organisationName("Org Name")
            .addressLine1("Old Street")
            .postCode("Old Postcode")
            .lastModifiedDate(now.minusMinutes(6))
            .build();

        String orgId = "org2";
        String orgName = "Org Name2";
        String addressLine1 = "Addr1";
        String addressLine2 = "Addr2";
        String addressLine3 = "Addr3";
        String townCity = "City";
        String county = "County";
        String country = "Country";
        String postCode = "PostCode";

        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.of(cachedEntity));
        when(cachedOrganisationResponseRepository.save(cachedEntity)).thenReturn(cachedEntity);
        when(localDateTimeSupplier.get()).thenReturn(now);
        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .organisationIdentifier(orgId)
            .name(orgName)
            .contactInformation(List.of(OrganisationDetailsResponse.ContactInformation.builder()
                                            .addressLine1(addressLine1)
                                            .addressLine2(addressLine2)
                                            .addressLine3(addressLine3)
                                            .townCity(townCity)
                                            .county(county)
                                            .country(country)
                                            .postCode(postCode)
                                            .build()))
            .build();
        mockOrganisationDetails(response);

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.name()).isEqualTo(orgName);

        verify(cachedOrganisationResponseRepository).save(cachedEntity);
        assertThat(cachedEntity.getOrganisationId()).isEqualTo(orgId);
        assertThat(cachedEntity.getOrganisationName()).isEqualTo(orgName);
        assertThat(cachedEntity.getAddressLine1()).isEqualTo(addressLine1);
        assertThat(cachedEntity.getAddressLine2()).isEqualTo(addressLine2);
        assertThat(cachedEntity.getAddressLine3()).isEqualTo(addressLine3);
        assertThat(cachedEntity.getPostTown()).isEqualTo(townCity);
        assertThat(cachedEntity.getPostCode()).isEqualTo(postCode);
        assertThat(cachedEntity.getCounty()).isEqualTo(county);
        assertThat(cachedEntity.getCountry()).isEqualTo(country);
        assertThat(cachedEntity.getLastModifiedDate()).isEqualTo(now);
    }

    @Test
    void getNameAndAddress_WithCachedResponseExpiredAndContactInfoRemoved_ClearsCachedAddress() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String orgId = "org1";
        String orgName = "orgName";
        CachedOrganisationResponseEntity cachedEntity = CachedOrganisationResponseEntity.builder()
            .idamId(userId)
            .organisationId(orgId)
            .organisationName(orgName)
            .addressLine1("Old Street")
            .postCode("Old Postcode")
            .lastModifiedDate(now.minusMinutes(6))
            .build();

        OrganisationDetailsResponse response = OrganisationDetailsResponse.builder()
            .organisationIdentifier(orgId)
            .name(orgName)
            .contactInformation(List.of())
            .build();
        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.of(cachedEntity));
        when(localDateTimeSupplier.get()).thenReturn(now);
        mockOrganisationDetails(response);
        when(cachedOrganisationResponseRepository.save(cachedEntity)).thenReturn(cachedEntity);

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.address().getAddressLine1()).isNull();

        verify(cachedOrganisationResponseRepository).save(cachedEntity);
        assertThat(cachedEntity.getAddressLine1()).isNull();
        assertThat(cachedEntity.getAddressLine2()).isNull();
        assertThat(cachedEntity.getAddressLine3()).isNull();
        assertThat(cachedEntity.getPostTown()).isNull();
        assertThat(cachedEntity.getCounty()).isNull();
        assertThat(cachedEntity.getCountry()).isNull();
        assertThat(cachedEntity.getPostCode()).isNull();

    }

    @Test
    void getOrganisationIdentifier_ExactlyAtTtlBoundary_DoesNotUpdate() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        CachedOrganisationResponseEntity cachedEntity = CachedOrganisationResponseEntity.builder()
            .organisationId("org1")
            .idamId(userId)
            .lastModifiedDate(now.minusMinutes(CACHE_TTL_IN_MINUTES))
            .build();

        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.of(cachedEntity));
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        String actual = cachingOrganisationDetailsService.getOrganisationIdentifier(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository, never()).save(any());
        verifyNoCallsForOrganisationDetails();
        assertThat(actual).isEqualTo("org1");
    }

    @Test
    void shouldThrowOrganisationDetailsExceptionWhenFeignClientThrowsException() {
        // Given
        RuntimeException runtimeException = new RuntimeException("Feign client error");

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenThrow(runtimeException);

        // When & Then
        assertThatThrownBy(() -> cachingOrganisationDetailsService.getOrganisationIdentifier(USER_ID))
            .isInstanceOf(OrganisationDetailsException.class)
            .hasMessage("Unexpected error retrieving organisation details")
            .hasCause(runtimeException);
    }

    @Test
    void shouldThrowOrganisationDetailsExceptionWhenGeneralExceptionOccurs() {
        // Given
        RuntimeException generalException = new RuntimeException("Connection failed");

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        when(prdAdminTokenProvider.getAuthToken()).thenReturn(PRD_ADMIN_TOKEN);
        when(rdProfessionalApi.getOrganisationDetails(anyString(), anyString(), anyString()))
            .thenThrow(generalException);

        // When & Then
        assertThatThrownBy(() -> cachingOrganisationDetailsService.getOrganisationIdentifier(USER_ID))
            .isInstanceOf(OrganisationDetailsException.class)
            .hasMessage("Unexpected error retrieving organisation details")
            .hasCause(generalException);
    }

    @Test
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
        assertThatThrownBy(() -> cachingOrganisationDetailsService.getOrganisationIdentifier(USER_ID))
            .isInstanceOf(OrganisationDetailsException.class)
            .hasMessage("Failed to retrieve organisation details")
            .hasCause(feignEx);

        verify(rdProfessionalApi).getOrganisationDetails(USER_ID, S2S_TOKEN, PRD_ADMIN_TOKEN);
    }

    @Test
    void shouldWrapUnexpectedExceptionAsOrganisationDetailsException() {
        // Given — anything other than FeignException must hit the generic catch (Exception) branch.
        RuntimeException unexpected = new RuntimeException("token generator blew up");
        when(authTokenGenerator.generate()).thenThrow(unexpected);

        // When / Then
        assertThatThrownBy(() -> cachingOrganisationDetailsService.getOrganisationIdentifier(USER_ID))
            .isInstanceOf(OrganisationDetailsException.class)
            .hasMessage("Unexpected error retrieving organisation details")
            .hasCause(unexpected);
    }
}
