package uk.gov.hmcts.reform.pcs.reference.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Service to populate organisation info from rd-professional API.
 */
@Service
@Slf4j
@AllArgsConstructor
public class OrganisationService {

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
            UUID userId = resolveUserId();

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

    /**
     * Retrieves the organisation address for the current user.
     * Gets the user ID from security context and fetches the organisation address
     * from the rd-professional API using PRD admin token and S2S token.
     *
     * @return The organisation address, or null if the user ID is missing or the address cannot be retrieved
     */
    public AddressUK getOrganisationAddressForCurrentUser() {

        try {
            UUID userId = resolveUserId();

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

    private UUID resolveUserId() {
        UUID userId = securityContextService.getCurrentUserId();
        if (userId == null) {
            log.warn("User ID is null from security context, cannot fetch organisation details");
        }
        return userId;
    }

    private boolean keyAddressFieldsEmpty(AddressUK organisationAddress) {
        return organisationAddress == null || (isBlank(organisationAddress.getAddressLine1())
            && isBlank(organisationAddress.getPostTown())
            && isBlank(organisationAddress.getPostCode()));
    }

}
