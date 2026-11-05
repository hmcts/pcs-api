package uk.gov.hmcts.reform.pcs.reference.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Duration;
import java.util.Optional;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationUserRoles.belongsToOrganisation;

/**
 * Service to populate organisation info from rd-professional API.
 */
@Service
@Slf4j
public class OrganisationService {

    /**
     * The draft journey resolves the org on every operation; cached so each page isn't an
     * rd-professional round trip. TTL short enough to pick up an org move within a minute.
     */
    private static final Duration ORGANISATION_CACHE_TTL = Duration.ofMinutes(1);

    private final SecurityContextService securityContextService;
    private final OrganisationDetailsService organisationDetailsService;
    private final UserRoleService userRoleService;
    /**
     * {@link Optional#empty()} = genuinely no organisation (citizens stop re-asking). Failed
     * lookups throw and are not cached - a blip must not be remembered as "no organisation".
     */
    private final Cache<String, Optional<OrganisationDetailsResponse>> organisationsCache;

    public OrganisationService(SecurityContextService securityContextService,
                               OrganisationDetailsService organisationDetailsService,
                               UserRoleService userRoleService) {
        this.securityContextService = securityContextService;
        this.organisationDetailsService = organisationDetailsService;
        this.userRoleService = userRoleService;
        this.organisationsCache = Caffeine.newBuilder()
            .expireAfterWrite(ORGANISATION_CACHE_TTL)
            .build();
    }

    /** Organisation name for a user (claimant name population). */
    public String getOrganisationName(String userId) {
        OrganisationDetailsResponse organisationDetails = this.getCachedOrganisationDetails(userId);
        return organisationDetails != null ? organisationDetails.getName() : null;
    }

    /** Organisation name for the current user, or null if unable to retrieve. */
    public String getOrganisationNameForCurrentUser() {
        try {
            String userId = resolveProfessionalUserId();

            if (userId == null) {
                return null;
            }
            return this.getOrganisationName(userId.toString());
        } catch (Exception ex) {
            log.error("Error retrieving organisation name from rd-professional API", ex);
            // Return null instead of throwing to allow graceful degradation
            return null;
        }
    }

    /** Organisation identifier for a user. */
    public String getOrganisationIdentifier(String userId) {
        OrganisationDetailsResponse organisationDetails = this.getCachedOrganisationDetails(userId);
        return organisationDetails != null ? organisationDetails.getOrganisationIdentifier() : null;
    }

    /** Organisation identifier for the current user, or null if it cannot be resolved. */
    public String getOrganisationIdForCurrentUser() {
        try {
            String userId = resolveProfessionalUserId();
            if (userId == null) {
                return null;
            }

            OrganisationDetailsResponse organisationDetails =
                this.getCachedRequiredOrganisationDetails(userId);
            return organisationDetails != null ? organisationDetails.getOrganisationIdentifier() : null;
        } catch (OrganisationDetailsException | SecurityContextException ex) {
            log.error("Error retrieving organisation ID from rd-professional API", ex);
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
        String userId = resolveProfessionalUserId();
        if (userId == null) {
            return null;
        }

        OrganisationDetailsResponse organisationDetails = this.getCachedRequiredOrganisationDetails(userId);
        return organisationDetails != null ? organisationDetails.getOrganisationIdentifier() : null;
    }

    /**
     * The whole organisation record for the current user, so a caller needing more than one field
     * can read them from a single call rather than one round trip each.
     *
     * @return The organisation details, or null if they cannot be retrieved
     */
    public OrganisationDetailsResponse getOrganisationDetailsForCurrentUser() {
        try {
            String userId = resolveProfessionalUserId();
            if (userId == null) {
                return null;
            }

            return this.getOrganisationDetails(userId);
        } catch (OrganisationDetailsException | SecurityContextException ex) {
            log.error("Error retrieving organisation details from rd-professional API", ex);
            return null;
        }
    }

    public OrganisationDetailsResponse getOrganisationDetails(String userId) {
        try {
            return this.getCachedOrganisationDetails(userId);
        } catch (OrganisationDetailsException | SecurityContextException ex) {
            log.error("Error retrieving organisation details from rd-professional API", ex);
            return null;
        }
    }

    /**
     * Gets the organisation payment accounts for a given user ID.
     * @param userId The user ID to get organisation payment accounts for
     * @return Organisation payment accounts
     */
    public List<String> getOrganisationPaymentAccount(String userId) {
        OrganisationDetailsResponse details = getOrganisationDetails(userId);
        return details.getPaymentAccount();
    }

    /** Organisation address from a details response, or null if none. */
    public AddressUK getOrganisationAddress(OrganisationDetailsResponse organisationDetails) {
        if (organisationDetails == null || organisationDetails.getContactInformation() == null) {
            return null;
        }

        OrganisationDetailsResponse.ContactInformation contactInfo = organisationDetails
            .getContactInformation().getFirst();
        if (contactInfo == null) {
            return null;
        }

        return AddressUK.builder()
            .addressLine1(contactInfo.getAddressLine1())
            .addressLine2(contactInfo.getAddressLine2())
            .addressLine3(contactInfo.getAddressLine3())
            .postTown(contactInfo.getTownCity())
            .county(contactInfo.getCounty())
            .country(contactInfo.getCountry())
            .postCode(contactInfo.getPostCode())
            .build();
    }

    /**
     * Only users who belong to a professional organisation have one to look up. Citizens, the system
     * identity, HMCTS staff and judiciary do not, and asking rd-professional on their behalf is a
     * round trip that can only 404 - which PRD logs as an error. Returning null here is the same
     * "no organisation" answer the 404 produced, reached without the call.
     *
     * <p>Gated on the PRM group-access roles rather than on {@code caseworker-pcs-solicitor}: that
     * IDAM role is being retired under HDPI-7333, and it never distinguished the organisation types
     * anyway.
     */
    private String resolveProfessionalUserId() {
        if (securityContextService.isSystemUser() || currentUserIsCitizen() || !currentUserBelongsToOrganisation()) {
            return null;
        }
        return securityContextService.getCurrentUserId();
    }

    private boolean currentUserIsCitizen() {
        UserInfo details = securityContextService.getCurrentUserDetails();
        return details != null && details.getRoles() != null
            && details.getRoles().contains(UserRole.CITIZEN.getRole());
    }

    private boolean currentUserBelongsToOrganisation() {
        return belongsToOrganisation(userRoleService.getCurrentUserOrganisationalRoles().roles());
    }

    private OrganisationDetailsResponse getCachedOrganisationDetails(String userId) {
        return organisationsCache.get(
            userId,
            id -> Optional.ofNullable(organisationDetailsService.getOrganisationDetails(id))
        ).orElse(null);
    }

    private OrganisationDetailsResponse getCachedRequiredOrganisationDetails(String userId) {
        return organisationsCache.get(
            userId,
            id -> Optional.ofNullable(organisationDetailsService.requireOrganisationDetails(id))
        ).orElse(null);
    }
}
