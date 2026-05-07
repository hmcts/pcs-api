package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm implements PossessionGroundEnum {

    TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE("Tied accommodation needed for another employee (ground 12)"),
    ADAPTED_ACCOMMODATION("Adapted accommodation (ground 13)"),
    HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES("Housing association special circumstances accommodation (ground 14)"),
    SPECIAL_NEEDS_ACCOMMODATION("Special needs accommodation (ground 15)"),
    UNDER_OCCUPYING_AFTER_SUCCESSION("Under occupying after succession (ground 15A)");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
