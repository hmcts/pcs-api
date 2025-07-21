package uk.gov.hmcts.reform.pcs.postcodecourt.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public final class EligibilityResult {

    private final EligibilityStatus status;
    private final int epimsId;
    private final LegislativeCountry legislativeCountry;
    private final List<LegislativeCountry> legislativeCountries;

}
