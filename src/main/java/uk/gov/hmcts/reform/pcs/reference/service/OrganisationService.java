package uk.gov.hmcts.reform.pcs.reference.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Service to populate organisation info from rd-professional API.
 */
@Service
@Slf4j
public class OrganisationService {

    private static final String GENERIC_ORGANISATION_PROFILE = "ORGANISATION_PROFILE";

    /**
     * The draft journey resolves the org on every operation; cached so each page isn't an
     * rd-professional round trip. TTL short enough to pick up an org move within a minute.
     */
    private static final Duration ORGANISATION_CACHE_TTL = Duration.ofMinutes(1);

    private final SecurityContextService securityContextService;
    private final OrganisationDetailsService organisationDetailsService;
    /**
     * {@link Optional#empty()} = genuinely no organisation (citizens stop re-asking). Failed
     * lookups throw and are not cached - a blip must not be remembered as "no organisation".
     */
    private final Cache<String, Optional<String>> organisationIdCache;

    public OrganisationService(SecurityContextService securityContextService,
                               OrganisationDetailsService organisationDetailsService) {
        this.securityContextService = securityContextService;
        this.organisationDetailsService = organisationDetailsService;
        this.organisationIdCache = Caffeine.newBuilder()
            .expireAfterWrite(ORGANISATION_CACHE_TTL)
            .build();
    }

    /** Organisation name for the current user, or null if unable to retrieve. */
    public String getOrganisationNameForCurrentUser() {
        try {
            UUID userId = resolveProfessionalUserId();

            if (userId == null) {
                return null;
            }

            String organisationName = organisationDetailsService.getOrganisationName(userId.toString());

            if (organisationName == null || organisationName.isEmpty()) {
                log.warn("Organisation name is null or empty for user ID: {}", userId);
            }

            return organisationName;

        } catch (Exception ex) {
            log.error("Error retrieving organisation name from rd-professional API. Error: {}",
                ex.getMessage(), ex);
            // Return null instead of throwing to allow graceful degradation
            return null;
        }
    }

    /** Organisation identifier for the current user, or null if it cannot be resolved. */
    public String getOrganisationIdForCurrentUser() {
        try {
            UUID userId = resolveProfessionalUserId();

            if (userId == null) {
                return null;
            }

            return organisationIdCache.get(
                userId.toString(),
                id -> Optional.ofNullable(organisationDetailsService.getOrganisationIdentifier(id))
            ).orElse(null);

        } catch (OrganisationDetailsException | SecurityContextException ex) {
            log.error("Error retrieving organisation ID from rd-professional API. Error: {}",
                ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * The same lookup, but a failure is raised rather than reported as "no organisation". Callers
     * that key stored data on the organisation need the two kept apart: treating an unavailable
     * rd-professional as "this user has no firm" writes data the firm cannot see.
     *
     * @return The organisation identifier, or null if the user genuinely has none
     */
    public String requireOrganisationIdForCurrentUser() {
        UUID userId = resolveProfessionalUserId();

        if (userId == null) {
            return null;
        }

        return organisationIdCache.get(
            userId.toString(),
            id -> Optional.ofNullable(organisationDetailsService.getOrganisationIdentifier(id))
        ).orElse(null);
    }

    /**
     * The whole organisation record for the current user, so a caller needing more than one field
     * can read them from a single call rather than one round trip each.
     *
     * @return The organisation details, or null if they cannot be retrieved
     */
    public OrganisationDetailsResponse getOrganisationDetailsForCurrentUser() {
        try {
            UUID userId = resolveProfessionalUserId();

            if (userId == null) {
                return null;
            }

            return organisationDetailsService.getOrganisationDetails(userId.toString());

        } catch (OrganisationDetailsException | SecurityContextException ex) {
            log.error("Error retrieving organisation details from rd-professional API. Error: {}",
                ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Reads the organisation identifier off an already-fetched record.
     *
     * @return The organisation identifier, or null if there is none
     */
    public String getOrganisationId(OrganisationDetailsResponse organisationDetails) {
        return organisationDetails == null ? null : organisationDetails.getOrganisationIdentifier();
    }

    /**
     * Fetches the record and resolves the profile from it in one call.
     *
     * @return The organisation profile for the current user, or null if it cannot be resolved
     */
    public String getOrgProfileIdForCurrentUser() {
        return getOrgProfileId(getOrganisationDetailsForCurrentUser());
    }

    /**
     * The organisation profile PRM keys the group access catalogue on. Every organisation also
     * carries the generic ORGANISATION_PROFILE alongside its real one, so skipping that leaves the
     * single profile that identifies an access type.
     *
     * @return The organisation profile, or null if there is none
     */
    public String getOrgProfileId(OrganisationDetailsResponse organisationDetails) {
        if (organisationDetails == null || organisationDetails.getOrganisationProfileIds() == null) {
            return null;
        }
        return organisationDetails.getOrganisationProfileIds().stream()
            .filter(profile -> !GENERIC_ORGANISATION_PROFILE.equals(profile))
            .findFirst().orElse(null);
    }

    /**
     * Retrieves the organisation address for the current user.
     * Gets the user ID from security context and fetches the organisation address
     * from the rd-professional API using PRD admin token and S2S token.
     *
     * @return The organisation address, or null if the user ID is missing or the address cannot be retrieved
     */
    public AddressUK getOrganisationAddressForCurrentUser() {

        try {
            UUID userId = resolveProfessionalUserId();

            if (userId == null) {
                return null;
            }

            AddressUK organisationAddress = organisationDetailsService.getOrganisationAddress(userId.toString());

            // Return null if address is null or all key address fields to be displayed are empty
            if (keyAddressFieldsEmpty(organisationAddress)) {
                log.warn("Organisation address is null or empty for user ID: {}", userId);
                return null;
            }

            return organisationAddress;

        } catch (Exception ex) {
            log.error("Error retrieving organisation address from rd-professional API. Error: {}",
                      ex.getMessage(), ex);
            return null;
        }
    }

    private UUID resolveProfessionalUserId() {
        if (currentUserIsCitizen()) {
            return null;
        }
        UUID userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            log.warn("User ID is null from security context, cannot fetch organisation details");
        }
        return userId;
    }

    private boolean currentUserIsCitizen() {
        UserInfo details = securityContextService.getCurrentUserDetails();
        return details != null && details.getRoles() != null
            && details.getRoles().contains(UserRole.CITIZEN.getRole());
    }

    private boolean keyAddressFieldsEmpty(AddressUK organisationAddress) {
        return organisationAddress == null || (isBlank(organisationAddress.getAddressLine1())
            && isBlank(organisationAddress.getPostTown())
            && isBlank(organisationAddress.getPostCode()));
    }

}
