package uk.gov.hmcts.reform.pcs.ccd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.DiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.MandatoryGrounds;

import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PossessionGrounds {

    private Set<MandatoryGrounds> selectedMandatoryGrounds;
    private Set<DiscretionaryGrounds> selectedDiscretionaryGrounds;

    private SecureOrFlexibleReasonForGrounds secureOrFlexibleReasonForGrounds;

}
