package uk.gov.hmcts.reform.pcs.reference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.CachedOrganisationResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CachedOrganisationResponseRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        String orgId = "org1";
        LocalDateTime now = LocalDateTime.now();
        when(cachedOrganisationResponseRepository.findByIdamId(userId)).thenReturn(Optional.empty());
        when(organisationDetailsService.getOrganisationIdentifier(userId.toString())).thenReturn(orgId);
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        cachingOrganisationDetailsService.getOrganisationIdentifier(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());

        CachedOrganisationResponseEntity actual = cachedOrganisationResponseEntityCaptor.getValue();

        assertEquals(orgId, actual.getOrganisationId());
        assertEquals(userId, actual.getIdamId());
        assertEquals(now, actual.getLastModifiedDate());
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
        verify(organisationDetailsService, never()).getOrganisationIdentifier(anyString());

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
        when(cachedOrganisationResponseRepository.findByIdamId(userId))
            .thenReturn(Optional.of(cachedOrganisationResponseEntity));
        when(organisationDetailsService.getOrganisationIdentifier(userId.toString())).thenReturn(orgId2);
        when(localDateTimeSupplier.get()).thenReturn(now);

        // when
        cachingOrganisationDetailsService.getOrganisationIdentifier(userId.toString());

        // then
        verify(cachedOrganisationResponseRepository).save(cachedOrganisationResponseEntityCaptor.capture());

        CachedOrganisationResponseEntity actual = cachedOrganisationResponseEntityCaptor.getValue();

        assertEquals(orgId2, actual.getOrganisationId());
        assertEquals(userId, actual.getIdamId());
        assertEquals(now, actual.getLastModifiedDate());
    }

}
