package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleMandatoryGroundsAlternativeAccomm implements PossessionGroundEnum {

    OVERCROWDING("Overcrowding (ground 9)"),
    LANDLORD_WORKS("Landlord’s works (ground 10)"),
    PROPERTY_SOLD("Property sold for redevelopment (ground 10A)"),
    CHARITABLE_LANDLORD("Charitable landlords (ground 11)");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
