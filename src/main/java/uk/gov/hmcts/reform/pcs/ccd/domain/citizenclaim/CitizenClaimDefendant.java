package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenClaimDefendant {

    private VerticalYesNo nameKnown;

    private String firstName;

    private String lastName;

    private VerticalYesNo addressKnown;

    private VerticalYesNo addressSameAsProperty;

    private AddressUK address;

}
