package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class Claim {

    @CCD(ignore = true)
    @JsonIgnore
    private final UUID id;

    @CCD(label = "Summary")
    private String summary;

    @CCD(
        label = "Claim Amount",
        typeOverride = FieldType.MoneyGBP
    )
    private String amountInPence;

    @CCD(ignore = true)
    @JsonIgnore
    private List<Party> claimants;

    @CCD(ignore = true)
    @JsonIgnore
    private List<Party> defendants;

    @CCD(ignore = true)
    @JsonIgnore
    private List<Party> interestedParties;

    public void addClaimant(Party party) {
        claimants.add(party);
    }

    public void addDefendant(Party party) {
        defendants.add(party);
    }

    public void addInterestedParty(Party party) {
        interestedParties.add(party);
    }

    @JsonIgnore
    public BigDecimal getAmountInPounds() {
        return new BigDecimal(amountInPence).movePointLeft(2);
    }

}
