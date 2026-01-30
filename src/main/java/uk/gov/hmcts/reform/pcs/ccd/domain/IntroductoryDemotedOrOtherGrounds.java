package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum IntroductoryDemotedOrOtherGrounds implements PossessionGroundEnum {

    RENT_ARREARS("Rent arrears"),
    ANTI_SOCIAL("Antisocial behaviour"),
    BREACH_OF_THE_TENANCY("Breach of the tenancy"),
    ABSOLUTE_GROUNDS("Absolute grounds"),
    OTHER("Other");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
