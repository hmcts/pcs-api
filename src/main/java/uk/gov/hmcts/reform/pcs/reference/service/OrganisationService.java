package uk.gov.hmcts.reform.pcs.reference.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.reference.dto.NameAndAddress;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

/**
 * Service to populate organisation info from rd-professional API.
 */
@Service
@Slf4j
@AllArgsConstructor
public class OrganisationService {

    private final SecurityContextService securityContextService;
    private final CachingOrganisationDetailsService cachingOrganisationDetailsService;

    public NameAndAddress getNameAndAddressForCurrentUser() {
        try {
            UUID userId = resolveUserId();
            if (userId == null) {
                return null;
            }
            return cachingOrganisationDetailsService.getNameAndAddress(userId.toString());
        } catch (Exception ex) {
            log.error("Error retrieving organisation name and address from rd-professional API. Error: {}",
                      ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Retrieves the organisation identifier for the current user.
     *
     * @return The organisation identifier, or null if it cannot be resolved
     */
    public String getOrganisationIdForCurrentUser() {
        try {
            UUID userId = resolveUserId();

            if (userId == null) {
                return null;
            }

            return cachingOrganisationDetailsService.getOrganisationIdentifier(userId.toString());

        } catch (OrganisationDetailsException | SecurityContextException ex) {
            log.error("Error retrieving organisation ID from rd-professional API. Error: {}",
                ex.getMessage(), ex);
            return null;
        }
    }

    private UUID resolveUserId() {
        UUID userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            log.warn("User ID is null from security context, cannot fetch organisation details");
        }
        return userId;
    }

}
