package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleMandatoryGroundsAlternativeAccomm implements HasLabel {

    OVERCROWDING("Overcrowding (ground 9)"),
    LANDLORD_WORKS("Landlord's works (ground 10)"),
    PROPERTY_SOLD("Property sold for redevelopment (ground 10A)"),
    CHARITABLE_LANDLORD("Charitable landlords (ground 11)");

    private final String label;

    public static SecureOrFlexibleMandatoryGroundsAlternativeAccomm fromLabel(String label) {
        return Arrays.stream(values())
            .filter(g -> g.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No enum constant with label: " + label));
    }
}
