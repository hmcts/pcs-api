package uk.gov.hmcts.reform.pcs.reference.service;

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
            log.info("Retrieving organisation details for userId: {}", userId);
            
            OrganisationDetailsResponse details = organisationDetailsService.getOrganisationDetails(userId);
            
            log.info("Successfully retrieved organisation details for userId: {}", userId);
            return details;
            
        } catch (Exception ex) {
            log.error("Failed to retrieve organisation details for userId: {}", userId, ex);
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
            log.info("Retrieving organisation name for userId: {}", userId);
            
            String organisationName = organisationDetailsService.getOrganisationName(userId);
            
            log.info("Successfully retrieved organisation name: {} for userId: {}", organisationName, userId);
            return organisationName;
            
        } catch (Exception ex) {
            log.error("Failed to retrieve organisation name for userId: {}", userId, ex);
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
            log.info("Retrieving organisation identifier for userId: {}", userId);
            
            String organisationIdentifier = organisationDetailsService.getOrganisationIdentifier(userId);
            
            log.info("Successfully retrieved organisation identifier: {} for userId: {}",
                organisationIdentifier, userId);
            return organisationIdentifier;
            
        } catch (Exception ex) {
            log.error("Failed to retrieve organisation identifier for userId: {}", userId, ex);
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
            log.info("Populating claimant information for userId: {}", userId);
            
            OrganisationDetailsResponse details = organisationDetailsService.getOrganisationDetails(userId);
            
            ClaimantInformation claimantInfo = ClaimantInformation.builder()
                .name(details.getName())
                .organisationIdentifier(details.getOrganisationIdentifier())
                .status(details.getStatus())
                .sraRegulated(details.getSraRegulated())
                .build();
            
            log.info("Successfully populated claimant information for userId: {}", userId);
            return claimantInfo;
            
        } catch (Exception ex) {
            log.error("Failed to populate claimant information for userId: {}", userId, ex);
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
