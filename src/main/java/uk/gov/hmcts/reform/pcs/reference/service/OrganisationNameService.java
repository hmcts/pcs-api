package uk.gov.hmcts.reform.pcs.reference.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

/**
 * Service to populate organisation name from rd-professional API.
 */
@Service
@Slf4j
@AllArgsConstructor
public class OrganisationNameService {

    private final SecurityContextService securityContextService;
    private final OrganisationDetailsService organisationDetailsService;

    /**
     * Retrieves the organisation name for the current user from the security context.
     * Gets the user ID from security context and fetches the organisation name
     * from the rd-professional API using PRD admin token and S2S token.
     *
     * @return The organisation name, or null if unable to retrieve
     */
    public String getOrganisationNameForCurrentUser() {
        try {
            UUID userId = securityContextService.getCurrentUserId();

            if (userId == null) {
                log.warn("User ID is null from security context, cannot fetch organisation details");
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
}
