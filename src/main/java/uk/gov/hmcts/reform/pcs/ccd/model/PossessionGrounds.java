package uk.gov.hmcts.reform.pcs.ccd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PossessionGrounds {

    private Set<String> mandatoryGrounds;
    private Set<String> discretionaryGrounds;
    private Set<String> mandatoryGroundsAlternativeAccommodation;
    private Set<String> discretionaryGroundsAlternativeAccommodation;

    private SecureOrFlexibleReasonsForGrounds secureOrFlexibleReasonsForGrounds;

}
