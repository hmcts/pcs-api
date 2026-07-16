package uk.gov.hmcts.reform.pcs.reference.service;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@Slf4j
public class CachingOrganisationDetailsService {

    private final CachedOrganisationResponseRepository cachedOrganisationResponseRepository;
    private final int ttlInMinutes;
    private final Supplier<LocalDateTime> localDateTimeSupplier;
    private final RdProfessionalApi rdProfessionalApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider prdAdminTokenProvider;

    @Autowired
    public CachingOrganisationDetailsService(CachedOrganisationResponseRepository cachedOrganisationResponseRepository,
                                             @Value("${cache.organisationDetails.ttlInMinutes}") int ttlInMinutes,
                                             RdProfessionalApi rdProfessionalApi,
                                             AuthTokenGenerator authTokenGenerator,
                                             @Qualifier("prdAdminTokenProvider")
                                                 IdamTokenProvider prdAdminTokenProvider) {
        this(cachedOrganisationResponseRepository, ttlInMinutes, LocalDateTime::now,
             rdProfessionalApi, authTokenGenerator, prdAdminTokenProvider);
    }

    public CachingOrganisationDetailsService(CachedOrganisationResponseRepository cachedOrganisationResponseRepository,
                                             @Value("${cache.organisationDetails.ttlInMinutes}") int ttlInMinutes,
                                             Supplier<LocalDateTime> localDateTimeSupplier,
                                             RdProfessionalApi rdProfessionalApi,
                                             AuthTokenGenerator authTokenGenerator,
                                             @Qualifier("prdAdminTokenProvider")
                                             IdamTokenProvider prdAdminTokenProvider) {
        this.cachedOrganisationResponseRepository = cachedOrganisationResponseRepository;
        this.ttlInMinutes = ttlInMinutes;
        this.localDateTimeSupplier = localDateTimeSupplier;
        this.rdProfessionalApi = rdProfessionalApi;
        this.authTokenGenerator = authTokenGenerator;
        this.prdAdminTokenProvider = prdAdminTokenProvider;
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
            OrganisationDetailsResponse response = getOrganisationDetails(userId);

            CachedOrganisationResponseEntity newCachedResponse = mapResponseToEntity(userIdam, response);

            cachedOrganisationResponseRepository.save(newCachedResponse);
            return newCachedResponse;
        } else {
            CachedOrganisationResponseEntity existingCachedResponse = cachedResponse.get();

            if (isDataRequiringResync(existingCachedResponse.getLastModifiedDate())) {
                OrganisationDetailsResponse response = getOrganisationDetails(userId);
                updateFields(existingCachedResponse, response, userId);
                cachedOrganisationResponseRepository.save(existingCachedResponse);
                log.debug("Cached OrganisationDetails response refreshed");
            }
            return existingCachedResponse;
        }
    }

    /**
     * Retrieves organisation details for a given user ID.
     * @param userId The user ID to get organisation details for
     * @return OrganisationDetailsResponse containing organisation information
     */
    private OrganisationDetailsResponse getOrganisationDetails(String userId) {
        try {
            String s2sToken = authTokenGenerator.generate();
            String prdAdminToken = prdAdminTokenProvider.getAuthToken();

            OrganisationDetailsResponse details = rdProfessionalApi.getOrganisationDetails(
                userId, s2sToken, prdAdminToken
            );

            if (details == null) {
                log.warn("Organisation details response is null for userId: {}", userId);
            }

            return details;

        } catch (FeignException ex) {
            log.error("Feign error retrieving organisation details for userId: {}. Status: {}, Message: {}",
                      userId, ex.status(), ex.getMessage(), ex);
            throw new OrganisationDetailsException("Failed to retrieve organisation details", ex);
        } catch (Exception ex) {
            log.error("Unexpected error retrieving organisation details for userId: {}. Error: {}",
                      userId, ex.getMessage(), ex);
            throw new OrganisationDetailsException("Unexpected error retrieving organisation details", ex);
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
