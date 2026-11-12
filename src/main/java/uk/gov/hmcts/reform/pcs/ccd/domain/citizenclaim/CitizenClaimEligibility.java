package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimEligibility {

    private ClaimantType claimantType;

    private VerticalYesNo isTrespass;

    private CitizenClaimWhoIsClaiming whoIsClaiming;

    private LegislativeCountry legislativeCountry;

}
