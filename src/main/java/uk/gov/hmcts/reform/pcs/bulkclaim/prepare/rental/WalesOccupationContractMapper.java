package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkLimits;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ParticularsRentedType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.TenancyTypeCode;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.TenancyDateRules;

/**
 * Maps the occupation contract of a property in Wales. No copy of the contract is supplied on this path.
 */
@Component
public class WalesOccupationContractMapper {

    private static final String OTHER_DETAILS_LABEL =
        "Details of what type of occupation contract or licence is in place";

    private final TenancyDateRules tenancyDateRules;

    public WalesOccupationContractMapper(TenancyDateRules tenancyDateRules) {
        this.tenancyDateRules = tenancyDateRules;
    }

    /**
     * Maps the occupation contract onto the case.
     *
     * @param path path of the particulars element relative to the claim
     * @return the contract type, or null when the claim's tenancy type is not an occupation contract type
     */
    public OccupationLicenceTypeWales apply(ParticularsRentedType particulars,
                                            PCSCase target,
                                            String path,
                                            BulkErrorCollector errors) {
        TenancyTypeCode code = particulars.getTenancyType();
        OccupationLicenceTypeWales type = switch (code) {
            case SECURE_CONTRACT -> OccupationLicenceTypeWales.SECURE_CONTRACT;
            case STANDARD_CONTRACT -> OccupationLicenceTypeWales.STANDARD_CONTRACT;
            case OTHER -> OccupationLicenceTypeWales.OTHER;
            case ASSURED_TENANCY, SECURE_TENANCY, INTRODUCTORY_TENANCY, FLEXIBLE_TENANCY, DEMOTED_TENANCY -> null;
        };
        if (type == null) {
            errors.add(path + "/TenancyType", BulkErrorCollector.INVALID_VALUE,
                       code + " is a tenancy type and is not valid for a property in Wales");
            return null;
        }

        OccupationLicenceDetailsWales.OccupationLicenceDetailsWalesBuilder details =
            OccupationLicenceDetailsWales.builder()
                .occupationLicenceTypeWales(type)
                .licenceStartDate(particulars.getTenancyCommenceDate());

        if (type == OccupationLicenceTypeWales.OTHER) {
            String otherDetails = particulars.getOtherTenancyTypeDetails();
            errors.requiredWithMaxLength(path + "/OtherTenancyTypeDetails", otherDetails, BulkLimits.MEDIUM_TEXT,
                                         OTHER_DETAILS_LABEL);
            details.otherLicenceTypeDetails(otherDetails);
        }
        errors.addAll(path + "/TenancyCommenceDate", BulkErrorCollector.NOT_IN_PAST,
                      tenancyDateRules.validateWalesLicenceStartDate(particulars.getTenancyCommenceDate()));

        target.setOccupationLicenceDetailsWales(details.build());
        return type;
    }
}
