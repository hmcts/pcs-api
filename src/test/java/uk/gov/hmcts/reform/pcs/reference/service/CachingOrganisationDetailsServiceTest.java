package uk.gov.hmcts.reform.pcs.reference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.CachedOrganisationResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CachedOrganisationResponseRepository;
import uk.gov.hmcts.reform.pcs.reference.dto.NameAndAddress;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingOrganisationDetailsServiceTest {

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    @Mock
    private CachedOrganisationResponseRepository cachedOrganisationResponseRepository;

    @Mock
    private Supplier<LocalDateTime> localDateTimeSupplier;

    @Captor
    private ArgumentCaptor<CachedOrganisationResponseEntity> cachedOrganisationResponseEntityCaptor;

    private CachingOrganisationDetailsService cachingOrganisationDetailsService;

    private static final int CACHE_TTL_IN_MINUTES = 5;

    @BeforeEach
    void setUp() {
        cachingOrganisationDetailsService = new CachingOrganisationDetailsService(organisationDetailsService,
                                                                                  cachedOrganisationResponseRepository,
                                                                                  CACHE_TTL_IN_MINUTES,
                                                                                  localDateTimeSupplier);
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

        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.empty());
        when(organisationDetailsService.getOrganisationDetails(userId.toString())).thenReturn(response);
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        cachingOrganisationDetailsService.getOrganisationIdentifier(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());

        CachedOrganisationResponseEntity actual = cachedOrganisationResponseEntityCaptor.getValue();

        assertEquals(orgId, actual.getOrganisationId());
        assertEquals(userId, actual.getIdamId());
        assertEquals(now, actual.getLastModifiedDate());
        assertEquals(orgName, actual.getOrganisationName());
        assertEquals(addressLine1, actual.getAddressLine1());
        assertEquals(addressLine2, actual.getAddressLine2());
        assertEquals(addressLine3, actual.getAddressLine3());
        assertEquals(townCity, actual.getPostTown());
        assertEquals(county, actual.getCounty());
        assertEquals(country, actual.getCountry());
        assertEquals(postCode, actual.getPostCode());
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
        verify(organisationDetailsService, never()).getOrganisationDetails(any());

        assertEquals(orgId, actual);
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
        when(organisationDetailsService.getOrganisationDetails(userId.toString())).thenReturn(response);
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        cachingOrganisationDetailsService.getOrganisationIdentifier(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());

        CachedOrganisationResponseEntity actual = cachedOrganisationResponseEntityCaptor.getValue();

        assertEquals(orgId2, actual.getOrganisationId());
        assertEquals(userId, actual.getIdamId());
        assertEquals(now, actual.getLastModifiedDate());

        assertEquals(orgId2, actual.getOrganisationId());
        assertEquals(userId, actual.getIdamId());
        assertEquals(now, actual.getLastModifiedDate());
        assertEquals(orgName2, actual.getOrganisationName());
        assertEquals(addressLine1, actual.getAddressLine1());
        assertEquals(addressLine2, actual.getAddressLine2());
        assertEquals(addressLine3, actual.getAddressLine3());
        assertEquals(townCity, actual.getPostTown());
        assertEquals(county, actual.getCounty());
        assertEquals(country, actual.getCountry());
        assertEquals(postCode, actual.getPostCode());
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
        when(organisationDetailsService.getOrganisationDetails(userId.toString())).thenReturn(response);
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        assertNotNull(actual);
        assertEquals(orgName, actual.name());
        AddressUK address = actual.address();
        assertNotNull(address);
        assertEquals(addressLine1, address.getAddressLine1());
        assertEquals(addressLine2, address.getAddressLine2());
        assertEquals(addressLine3, address.getAddressLine3());
        assertEquals(townCity, address.getPostTown());
        assertEquals(county, address.getCounty());
        assertEquals(country, address.getCountry());
        assertEquals(postCode, address.getPostCode());

        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());
        CachedOrganisationResponseEntity savedEntity = cachedOrganisationResponseEntityCaptor.getValue();

        assertEquals(orgId, savedEntity.getOrganisationId());
        assertEquals(orgName, savedEntity.getOrganisationName());
        assertEquals(addressLine1, savedEntity.getAddressLine1());
        assertEquals(addressLine2, savedEntity.getAddressLine2());
        assertEquals(addressLine3, savedEntity.getAddressLine3());
        assertEquals(townCity, savedEntity.getPostTown());
        assertEquals(county, savedEntity.getCounty());
        assertEquals(country, savedEntity.getCountry());
        assertEquals(postCode, savedEntity.getPostCode());
        assertEquals(now, savedEntity.getLastModifiedDate());
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
        when(organisationDetailsService.getOrganisationDetails(userId.toString())).thenReturn(response);
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        assertNotNull(actual);
        assertEquals(orgName, actual.name());
        AddressUK address = actual.address();
        assertNotNull(address);

        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());
        CachedOrganisationResponseEntity savedEntity = cachedOrganisationResponseEntityCaptor.getValue();

        assertEquals(orgId, savedEntity.getOrganisationId());
        assertEquals(orgName, savedEntity.getOrganisationName());
        assertNull(savedEntity.getAddressLine1());
    }

    @Test
    void getNameAndAddress_WithCachedResponseNotExpired_ReturnsCachedValue() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        CachedOrganisationResponseEntity cachedEntity = CachedOrganisationResponseEntity.builder()
            .idamId(userId)
            .organisationId("org1")
            .organisationName("Org Name")
            .addressLine1("123 Street")
            .postCode("SW1A 1AA")
            .lastModifiedDate(now.minusMinutes(4))
            .build();

        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.of(cachedEntity));
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository, never()).save(any());
        verify(organisationDetailsService, never()).getOrganisationDetails(any());

        assertNotNull(actual);
        assertEquals("Org Name", actual.name());
        assertEquals("123 Street", actual.address().getAddressLine1());
        assertEquals("SW1A 1AA", actual.address().getPostCode());
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
        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.of(cachedEntity));
        when(organisationDetailsService.getOrganisationDetails(userId.toString())).thenReturn(response);
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        assertNotNull(actual);
        assertEquals(orgName, actual.name());
        assertEquals(addressLine1, actual.address().getAddressLine1());

        verify(cachedOrganisationResponseRepository).save(cachedEntity);
        assertEquals(orgId, cachedEntity.getOrganisationId());
        assertEquals(orgName, cachedEntity.getOrganisationName());
        assertEquals(addressLine1, cachedEntity.getAddressLine1());
        assertEquals(addressLine2, cachedEntity.getAddressLine2());
        assertEquals(now, cachedEntity.getLastModifiedDate());
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
        when(organisationDetailsService.getOrganisationDetails(userId.toString())).thenReturn(response);
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        NameAndAddress actual = cachingOrganisationDetailsService.getNameAndAddress(userId.toString());

        // then
        assertNotNull(actual);
        assertNull(actual.address().getAddressLine1());

        verify(cachedOrganisationResponseRepository).save(cachedEntity);
        assertNull(cachedEntity.getAddressLine1());
        assertNull(cachedEntity.getPostCode());

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
        verify(organisationDetailsService, never()).getOrganisationDetails(any());
        assertEquals("org1", actual);
    }
}
