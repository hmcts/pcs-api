package uk.gov.hmcts.reform.pcs.postcodecourt.model;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PostcodeCourtMapping {

    private final String postcode;

    private final String legislativeCountry;

    private final int epimsId;

    private final LocalDate mappingEffectiveFrom;

    private final LocalDate mappingEffectiveTo;

    private final LocalDate courtEligibleFrom;

}
