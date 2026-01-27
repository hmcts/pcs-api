package uk.gov.hmcts.reform.pcs.ccd.domain.grounds;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleMandatoryGroundsAlternativeAccomm implements PossessionGroundEnum {

    OVERCROWDING_GROUND9("Overcrowding (ground 9)"),
    LANDLORD_WORKS_GROUND10("Landlord’s works (ground 10)"),
    PROPERTY_SOLD_GROUND10A("Property sold for redevelopment (ground 10A)"),
    CHARITABLE_LANDLORD_GROUND11("Charitable landlords (ground 11)");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
