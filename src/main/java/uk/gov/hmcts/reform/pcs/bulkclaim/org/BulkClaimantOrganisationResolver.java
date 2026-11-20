package uk.gov.hmcts.reform.pcs.bulkclaim.org;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimOrganisationException.Reason;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.time.Duration;
import java.util.List;

/**
 * Resolves the claimant organisation for a bulk claim from a trusted PRD organisation identifier,
 * without reference to the current user (bulk claims are created as the system user).
 */
@Service
@Slf4j
public class BulkClaimantOrganisationResolver {

    private static final Duration CACHE_TTL = Duration.ofMinutes(1);
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final OrganisationDetailsService organisationDetailsService;
    // Only successful lookups are cached; a PRD failure or 404 is raised afresh each time.
    private final Cache<String, OrganisationDetailsResponse> cache;

    public BulkClaimantOrganisationResolver(OrganisationDetailsService organisationDetailsService) {
        this.organisationDetailsService = organisationDetailsService;
        this.cache = Caffeine.newBuilder().expireAfterWrite(CACHE_TTL).build();
    }

    /**
     * Resolves an active solicitor organisation, which is always the claimant's solicitor firm.
     *
     * @throws BulkClaimOrganisationException if the organisation does not exist, is not active, or is not a
     *     solicitor organisation
     */
    public OrganisationDetailsResponse resolve(String organisationId) {
        OrganisationDetailsResponse organisation = cache.get(organisationId, this::fetch);

        if (!ACTIVE_STATUS.equalsIgnoreCase(organisation.getStatus())) {
            throw new BulkClaimOrganisationException(Reason.NOT_ACTIVE);
        }
        requireUsableProfile(organisation);

        return organisation;
    }

    /** Whether the PBA number is one of the organisation's accepted accounts, ignoring case. */
    public boolean isPbaAccountValid(OrganisationDetailsResponse organisation, String pbaNumber) {
        if (organisation == null || pbaNumber == null) {
            return false;
        }
        List<String> accepted = organisation.getPaymentAccount();
        return accepted != null && accepted.stream().anyMatch(pbaNumber::equalsIgnoreCase);
    }

    private OrganisationDetailsResponse fetch(String organisationId) {
        OrganisationDetailsResponse organisation;
        try {
            organisation = organisationDetailsService.getOrganisationById(organisationId);
        } catch (FeignException.NotFound | FeignException.BadRequest ex) {
            log.debug("Organisation {} not found in rd-professional", organisationId);
            throw new BulkClaimOrganisationException(Reason.NOT_FOUND);
        }
        if (organisation == null) {
            throw new BulkClaimOrganisationException(Reason.NOT_FOUND);
        }
        return organisation;
    }

    private void requireUsableProfile(OrganisationDetailsResponse organisation) {
        String profileId;
        try {
            profileId = organisation.getOrgProfileId();
        } catch (IllegalArgumentException ex) {
            log.warn("Organisation {} has no single usable profile: {}",
                     organisation.getOrganisationIdentifier(), ex.getMessage());
            throw new BulkClaimOrganisationException(Reason.PROFILE_NOT_USABLE);
        }

        if (!OrganisationProfile.SOLICITOR_PROFILE.getId().equals(profileId)) {
            throw new BulkClaimOrganisationException(Reason.NOT_A_SOLICITOR_ORGANISATION);
        }
    }
}
