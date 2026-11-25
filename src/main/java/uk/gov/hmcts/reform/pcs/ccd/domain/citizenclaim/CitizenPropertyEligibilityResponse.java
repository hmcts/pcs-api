package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import com.fasterxml.jackson.annotation.JsonInclude;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

/**
 * The property's eligibility, and the country it is in when it is eligible.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CitizenPropertyEligibilityResponse(CitizenPropertyEligibility status,
                                                 LegislativeCountry legislativeCountry) {
}
