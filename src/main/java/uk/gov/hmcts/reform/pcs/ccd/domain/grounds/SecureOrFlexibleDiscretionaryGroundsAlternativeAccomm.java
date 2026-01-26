package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm implements PossessionGroundEnum {

    ACCOMMODATION_NEEDED_FOR_EMPLOYEE_GROUND12("Tied accommodation needed for another employee (ground 12)"),
    ADAPTED_ACCOMMODATION_GROUND13("Adapted accommodation (ground 13)"),
    HOUSING_ASSOCIATION_SPECIAL_GROUND14("Housing association special circumstances accommodation (ground 14)"),
    SPECIAL_NEEDS_ACCOMMODATION_GROUND15("Special needs accommodation (ground 15)"),
    UNDER_OCCUPYING_GROUND15A("Under occupying after succession (ground 15A)");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
