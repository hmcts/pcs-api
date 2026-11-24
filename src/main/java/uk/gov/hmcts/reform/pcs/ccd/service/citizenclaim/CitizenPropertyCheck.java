package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

/**
 * The outcome of checking a property for the citizen journey: the country it is in, or why it cannot be claimed for.
 */
public record CitizenPropertyCheck(String refusal, LegislativeCountry legislativeCountry) {

    public static CitizenPropertyCheck refused(String refusal) {
        return new CitizenPropertyCheck(refusal, null);
    }

    public static CitizenPropertyCheck accepted(LegislativeCountry legislativeCountry) {
        return new CitizenPropertyCheck(null, legislativeCountry);
    }

    public boolean isRefused() {
        return refusal != null;
    }

}
