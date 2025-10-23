package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProhibitedConduct {

    private String claimForProhibitedConductContract;
    private String agreedTermsOfPeriodicContract;
    private String detailsOfTerms;
    private String whyMakingClaim;

}
