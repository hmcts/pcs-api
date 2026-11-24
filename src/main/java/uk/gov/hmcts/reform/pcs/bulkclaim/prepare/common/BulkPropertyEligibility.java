package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.AddressType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.LegislativeCountryType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.InvalidPostCodeException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

/**
 * Decides whether a property can be claimed on in this service and which court and region manage the case.
 */
@Component
public class BulkPropertyEligibility {

    private final EligibilityService eligibilityService;
    private final PcsCaseService pcsCaseService;

    public BulkPropertyEligibility(EligibilityService eligibilityService, PcsCaseService pcsCaseService) {
        this.eligibilityService = eligibilityService;
        this.pcsCaseService = pcsCaseService;
    }

    /**
     * Either the legislative country with the managing court and region, or the reason the claim is rejected.
     */
    public record PropertyEligibility(BulkFailReason failReason,
                                      LegislativeCountry legislativeCountry,
                                      Integer caseManagementLocation,
                                      Integer regionId) {

        public boolean isEligible() {
            return failReason == null;
        }

        public void applyTo(PCSCase pcsCase) {
            pcsCase.setLegislativeCountry(legislativeCountry);
            pcsCase.setCaseManagementLocationNumber(caseManagementLocation);
            pcsCase.setRegionId(regionId);
        }

        static PropertyEligibility rejected(BulkFailReason reason) {
            return new PropertyEligibility(reason, null, null, null);
        }
    }

    /**
     * Never throws for an ineligible property; every outcome of the eligibility check is a result.
     *
     * @param suppliedCountry the claim's own legislative country, needed only for cross-border postcodes
     */
    public PropertyEligibility check(AddressType property, LegislativeCountryType suppliedCountry) {
        EligibilityResult result;
        try {
            result = eligibilityService.checkEligibility(
                BulkAddressMapper.normalisePostcode(property.getPostCode()), toCountry(suppliedCountry));
        } catch (InvalidPostCodeException e) {
            return PropertyEligibility.rejected(BulkFailReason.NO_COURT_FOR_POSTCODE);
        }

        return switch (result.getStatus()) {
            case ELIGIBLE -> eligible(result, property);
            case LEGISLATIVE_COUNTRY_REQUIRED ->
                PropertyEligibility.rejected(BulkFailReason.LEGISLATIVE_COUNTRY_REQUIRED);
            case NO_MATCH_FOUND -> PropertyEligibility.rejected(BulkFailReason.NO_COURT_FOR_POSTCODE);
            case NOT_ELIGIBLE -> PropertyEligibility.rejected(BulkFailReason.PROPERTY_NOT_ELIGIBLE);
            case MULTIPLE_MATCHES_FOUND -> PropertyEligibility.rejected(BulkFailReason.MULTIPLE_COURTS_FOR_POSTCODE);
        };
    }

    private PropertyEligibility eligible(EligibilityResult result, AddressType property) {
        PCSCase located = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .postCode(BulkAddressMapper.normalisePostcode(property.getPostCode()))
                                 .build())
            .legislativeCountry(result.getLegislativeCountry())
            .build();
        pcsCaseService.allocateRegionId(located);
        return new PropertyEligibility(
            null, result.getLegislativeCountry(), result.getEpimsId(), located.getRegionId());
    }

    private static LegislativeCountry toCountry(LegislativeCountryType country) {
        return country == null ? null : LegislativeCountry.valueOf(country.name());
    }
}
