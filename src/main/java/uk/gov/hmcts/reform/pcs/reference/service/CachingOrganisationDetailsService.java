package uk.gov.hmcts.reform.pcs.reference.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.CachedOrganisationResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CachedOrganisationResponseRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@Slf4j
public class CachingOrganisationDetailsService {

    private final OrganisationDetailsService organisationDetailsService;
    private final CachedOrganisationResponseRepository cachedOrganisationResponseRepository;

    private final int ttlInMinutes;

    private final Supplier<LocalDateTime> localDateTimeSupplier;

    @Autowired
    public CachingOrganisationDetailsService(OrganisationDetailsService organisationDetailsService,
                                             CachedOrganisationResponseRepository cachedOrganisationResponseRepository,
                                             @Value("${cache.organisationDetails.ttlInMinutes}") int ttlInMinutes) {
        this(organisationDetailsService, cachedOrganisationResponseRepository, ttlInMinutes, LocalDateTime::now);
    }

    public CachingOrganisationDetailsService(OrganisationDetailsService organisationDetailsService,
                                             CachedOrganisationResponseRepository cachedOrganisationResponseRepository,
                                             @Value("${cache.organisationDetails.ttlInMinutes}") int ttlInMinutes,
                                             Supplier<LocalDateTime> localDateTimeSupplier) {
        this.organisationDetailsService = organisationDetailsService;
        this.cachedOrganisationResponseRepository = cachedOrganisationResponseRepository;
        this.ttlInMinutes = ttlInMinutes;
        this.localDateTimeSupplier = localDateTimeSupplier;
    }

    public String getOrganisationIdentifier(String userId) {
        UUID userIdam = UUID.fromString(userId);
        Optional<CachedOrganisationResponseEntity> cachedResponse =
            cachedOrganisationResponseRepository.findByIdamId(userIdam);

        if (cachedResponse.isEmpty()) {
            String organisationId = organisationDetailsService.getOrganisationIdentifier(userId);

            CachedOrganisationResponseEntity newCachedResponse = CachedOrganisationResponseEntity.builder()
                .organisationId(organisationId)
                .idamId(userIdam)
                .lastModifiedDate(localDateTimeSupplier.get())
                .build();

            cachedOrganisationResponseRepository.save(newCachedResponse);
            return organisationId;
        } else {
            CachedOrganisationResponseEntity existingCachedResponse = cachedResponse.get();

            if (isDataRequiringResync(existingCachedResponse.getLastModifiedDate())) {
                String organisationId = organisationDetailsService.getOrganisationIdentifier(userId);

                existingCachedResponse.setOrganisationId(organisationId);
                existingCachedResponse.setLastModifiedDate(localDateTimeSupplier.get());

                cachedOrganisationResponseRepository.save(existingCachedResponse);
                log.debug("Cached OrganisationDetails response refreshed");
            }
            return existingCachedResponse.getOrganisationId();
        }
    }

    private boolean isDataRequiringResync(LocalDateTime lastModifiedDate) {
        return localDateTimeSupplier.get().isAfter(lastModifiedDate.plusMinutes(ttlInMinutes));
    }
}
