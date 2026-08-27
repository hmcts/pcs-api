package uk.gov.hmcts.reform.pcs.reference.service;

import static java.util.Objects.isNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

/**
 * Service for accessing reference data using PRD Admin token.
 */
@Service
@Slf4j
public class ReferenceDataService {

    private final OrganisationDetailsService organisationDetailsService;

    public ReferenceDataService(OrganisationDetailsService organisationDetailsService) {
        this.organisationDetailsService = organisationDetailsService;
    }

    /**
     * Gets organisation details for a given user ID.
     * @param userId The user ID to get organisation details for
     * @return OrganisationDetailsResponse containing organisation information
     */
    public OrganisationDetailsResponse getOrganisationDetails(String userId) {
        try {
            log.info("Retrieving organisation details");

            OrganisationDetailsResponse details = organisationDetailsService.getOrganisationDetails(userId);

            log.info("Successfully retrieved organisation details");
            return details;

        } catch (Exception ex) {
            log.error("Failed to retrieve organisation details", ex);
            throw ex;
        }
    }

    /**
     * Gets the organisation name for a given user ID (for claimant name population).
     * @param userId The user ID to get organisation name for
     * @return Organisation name
     */
    public String getOrganisationName(String userId) {
        try {
            log.info("Retrieving organisation name");

            String organisationName = organisationDetailsService.getOrganisationName(userId);

            log.info("Successfully retrieved organisation name");
            return organisationName;

        } catch (Exception ex) {
            log.error("Failed to retrieve organisation name", ex);
            throw ex;
        }
    }

    /**
     * Gets the organisation identifier for a given user ID.
     * @param userId The user ID to get organisation identifier for
     * @return Organisation identifier
     */
    public String getOrganisationIdentifier(String userId) {
        try {
            log.info("Retrieving organisation identifier");

            String organisationIdentifier = organisationDetailsService.getOrganisationIdentifier(userId);

            log.info("Successfully retrieved organisation identifier");
            return organisationIdentifier;

        } catch (Exception ex) {
            log.error("Failed to retrieve organisation identifier", ex);
            throw ex;
        }
    }

    /**
     * Populates claimant information from organisation details.
     * @param userId The user ID to get claimant information for
     * @return Claimant information object
     */
    public ClaimantInformation populateClaimantInformation(String userId) {
        try {
            log.info("Populating claimant information");

            OrganisationDetailsResponse details = organisationDetailsService.getOrganisationDetails(userId);

            if (isNull(details)) {
                log.warn("No organisation details found");
                return null;
            }

            ClaimantInformation claimantInfo = ClaimantInformation.builder()
                .name(details.getName())
                .organisationIdentifier(details.getOrganisationIdentifier())
                .status(details.getStatus())
                .sraRegulated(details.getSraRegulated())
                .build();

            log.info("Successfully populated claimant information");
            return claimantInfo;

        } catch (Exception ex) {
            log.error("Failed to populate claimant information", ex);
            throw ex;
        }
    }

    /**
     * DTO for claimant information.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ClaimantInformation {
        private String name;
        private String organisationIdentifier;
        private String status;
        private Boolean sraRegulated;
    }
}
