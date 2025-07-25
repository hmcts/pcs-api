package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.exception.CrossBorderPostcodeException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import java.util.stream.Collectors;

/**
 * Service to handle cross-border postcode callback logic for CCD mid-event callbacks.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CrossBorderPostcodeCallbackService {

    private final EligibilityService eligibilityService;

    /**
     * Checks if a postcode is cross-border and returns the appropriate result.
     * This method is designed to be called from CCD mid-event callbacks.
     *
     * @param postcode The postcode to check
     * @return EligibilityResult containing the cross-border status and available countries
     */
    public EligibilityResult checkCrossBorderPostcode(String postcode) {
        log.info("Checking cross-border status for postcode: {}", postcode);
        
        try {
            EligibilityResult result = eligibilityService.checkEligibility(postcode, null);
            log.info("Cross-border check result for postcode {}: {}", postcode, result.getStatus());
            return result;
        } catch (Exception e) {
            log.error("Error checking cross-border status for postcode: {}", postcode, e);
            throw new CrossBorderPostcodeException("Failed to check cross-border postcode status", e);
        }
    }

    /**
     * Validates the selected country for a cross-border postcode.
     * This method is designed to be called from CCD mid-event callbacks.
     *
     * @param postcode The postcode
     * @param selectedCountry The selected country
     * @return EligibilityResult with the final eligibility status
     */
    public EligibilityResult validateSelectedCountry(String postcode, LegislativeCountry selectedCountry) {
        log.info("Validating selected country {} for postcode: {}", selectedCountry, postcode);
        
        try {
            EligibilityResult result = eligibilityService.checkEligibility(postcode, selectedCountry);
            log.info("Country validation result for postcode {} with country {}: {}", 
                    postcode, selectedCountry, result.getStatus());
            return result;
        } catch (Exception e) {
            log.error("Error validating selected country for postcode: {}", postcode, e);
            throw new CrossBorderPostcodeException("Failed to validate selected country", e);
        }
    }

    /**
     * Populates the crossBorderCountries field in the PCSCase with the actual countries
     * returned from the API. This method is designed to be called from CCD mid-event callbacks.
     *
     * @param pcsCase The case to update
     * @param postcode The postcode to check
     * @return The updated PCSCase with crossBorderCountries populated
     */
    public PCSCase populateCrossBorderCountries(PCSCase pcsCase, String postcode) {
        log.info("Populating cross-border countries for postcode: {}", postcode);
        
        try {
            EligibilityResult result = eligibilityService.checkEligibility(postcode, null);
            
            if (result.getStatus() == EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED 
                && result.getLegislativeCountries() != null) {
                
                String countryList = result.getLegislativeCountries().stream()
                    .map(LegislativeCountry::getLabel)
                    .collect(Collectors.joining(" and "));
                
                pcsCase.setCrossBorderCountries(countryList);
                log.info("Set cross-border countries for postcode {}: {}", postcode, countryList);
            } else {
                // Clear the field if not cross-border
                pcsCase.setCrossBorderCountries(null);
                log.info("Cleared cross-border countries for postcode {} (not cross-border)", postcode);
            }
            
            return pcsCase;
        } catch (Exception e) {
            log.error("Error populating cross-border countries for postcode: {}", postcode, e);
            throw new CrossBorderPostcodeException("Failed to populate cross-border countries", e);
        }
    }
}