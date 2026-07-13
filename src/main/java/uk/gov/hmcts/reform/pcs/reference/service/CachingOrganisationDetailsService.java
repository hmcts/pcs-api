package uk.gov.hmcts.reform.pcs.reference.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.CachedOrganisationResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CachedOrganisationResponseRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
public class CachingOrganisationDetailsService {

    private final OrganisationDetailsService organisationDetailsService;
    private final CachedOrganisationResponseRepository cachedOrganisationResponseRepository;

    public String getOrganisationIdentifier(String userId) {
        Optional<CachedOrganisationResponseEntity> cachedResponse = cachedOrganisationResponseRepository.findByIdamId(UUID.fromString(
            userId));

        if (cachedResponse.isEmpty()) {
            String organisationId = organisationDetailsService.getOrganisationIdentifier(userId);

            CachedOrganisationResponseEntity newCachedResponse = CachedOrganisationResponseEntity.builder()
                .organisationId(organisationId)
                .lastModifiedDate(LocalDateTime.now())
                .build();

            cachedOrganisationResponseRepository.save(newCachedResponse);
            return organisationId;
        } else {
            CachedOrganisationResponseEntity existingCachedResponse = cachedResponse.get();

            if(isDataRequiringResync(existingCachedResponse.getLastModifiedDate())) {
                String organisationId = organisationDetailsService.getOrganisationIdentifier(userId);

                existingCachedResponse.setOrganisationId(organisationId);
                existingCachedResponse.setLastModifiedDate(LocalDateTime.now());

                cachedOrganisationResponseRepository.save(existingCachedResponse);
            }
            return existingCachedResponse.getOrganisationId();
        }
    }

    private boolean isDataRequiringResync(LocalDateTime lastModifiedDate) {
        return lastModifiedDate.isBefore(LocalDateTime.now().minusHours(24));
    }
}
