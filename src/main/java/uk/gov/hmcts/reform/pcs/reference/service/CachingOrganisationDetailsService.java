package uk.gov.hmcts.reform.pcs.reference.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.CachedOrganisationResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CachedOrganisationResponseRepository;
import uk.gov.hmcts.reform.pcs.reference.dto.NameAndAddress;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

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
        return getCachedOrganisationResponseEntity(userId).getOrganisationId();
    }

    public NameAndAddress getNameAndAddress(String userId) {
        CachedOrganisationResponseEntity entity = getCachedOrganisationResponseEntity(userId);

        AddressUK address = AddressUK.builder()
            .addressLine1(entity.getAddressLine1())
            .addressLine2(entity.getAddressLine2())
            .addressLine3(entity.getAddressLine3())
            .postTown(entity.getPostTown())
            .county(entity.getCounty())
            .country(entity.getCountry())
            .postCode(entity.getPostCode())
            .build();

        return new NameAndAddress(entity.getOrganisationName(), address);
    }

    private CachedOrganisationResponseEntity getCachedOrganisationResponseEntity(String userId) {
        UUID userIdam = UUID.fromString(userId);
        Optional<CachedOrganisationResponseEntity> cachedResponse =
            cachedOrganisationResponseRepository.findByIdamId(userIdam);

        if (cachedResponse.isEmpty()) {
            OrganisationDetailsResponse response = organisationDetailsService.getOrganisationDetails(userId);

            CachedOrganisationResponseEntity newCachedResponse = mapResponseToEntity(userIdam, response);

            cachedOrganisationResponseRepository.save(newCachedResponse);
            return newCachedResponse;
        } else {
            CachedOrganisationResponseEntity existingCachedResponse = cachedResponse.get();

            if (isDataRequiringResync(existingCachedResponse.getLastModifiedDate())) {
                OrganisationDetailsResponse response = organisationDetailsService.getOrganisationDetails(userId);
                updateFields(existingCachedResponse, response, userId);
                cachedOrganisationResponseRepository.save(existingCachedResponse);
                log.debug("Cached OrganisationDetails response refreshed");
            }
            return existingCachedResponse;
        }
    }

    private CachedOrganisationResponseEntity mapResponseToEntity(UUID userIdam, OrganisationDetailsResponse response) {

        CachedOrganisationResponseEntity.CachedOrganisationResponseEntityBuilder builder =
            CachedOrganisationResponseEntity.builder()
                .idamId(userIdam)
                .lastModifiedDate(localDateTimeSupplier.get());
        builder.organisationId(response.getOrganisationIdentifier())
            .organisationName(response.getName());

        if (response.getContactInformation() != null && !response.getContactInformation().isEmpty()) {
            OrganisationDetailsResponse.ContactInformation contactInfo = response.getContactInformation().getFirst();
            builder.addressLine1(contactInfo.getAddressLine1())
                .addressLine2(contactInfo.getAddressLine2())
                .addressLine3(contactInfo.getAddressLine3())
                .postTown(contactInfo.getTownCity())
                .county(contactInfo.getCounty())
                .country(contactInfo.getCountry())
                .postCode(contactInfo.getPostCode());
        }

        return builder.build();
    }

    private void updateFields(CachedOrganisationResponseEntity existingCachedResponse,
                              OrganisationDetailsResponse response,
                              String userId) {
        existingCachedResponse.setOrganisationId(response.getOrganisationIdentifier());
        existingCachedResponse.setOrganisationName(response.getName());

        if (response.getContactInformation() != null && !response.getContactInformation().isEmpty()) {
            OrganisationDetailsResponse.ContactInformation contactInfo = response.getContactInformation().getFirst();
            existingCachedResponse.setAddressLine1(contactInfo.getAddressLine1());
            existingCachedResponse.setAddressLine2(contactInfo.getAddressLine2());
            existingCachedResponse.setAddressLine3(contactInfo.getAddressLine3());
            existingCachedResponse.setPostTown(contactInfo.getTownCity());
            existingCachedResponse.setCounty(contactInfo.getCounty());
            existingCachedResponse.setCountry(contactInfo.getCountry());
            existingCachedResponse.setPostCode(contactInfo.getPostCode());
        } else {
            log.warn("Organisation address is null or empty for user ID: {}", userId);
            existingCachedResponse.setAddressLine1(null);
            existingCachedResponse.setAddressLine2(null);
            existingCachedResponse.setAddressLine3(null);
            existingCachedResponse.setPostTown(null);
            existingCachedResponse.setCounty(null);
            existingCachedResponse.setCountry(null);
            existingCachedResponse.setPostCode(null);
        }
        existingCachedResponse.setLastModifiedDate(localDateTimeSupplier.get());
    }

    private boolean isDataRequiringResync(LocalDateTime lastModifiedDate) {
        return localDateTimeSupplier.get().isAfter(lastModifiedDate.plusMinutes(ttlInMinutes));
    }
}
