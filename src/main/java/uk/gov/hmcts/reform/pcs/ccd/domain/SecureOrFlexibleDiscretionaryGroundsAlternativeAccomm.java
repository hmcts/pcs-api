package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm implements HasLabel {

    TIED_ACCOMMODATION_NEEDED_FOR_EMPLOYEE("Tied accommodation needed for another employee (ground 12)"),
    ADAPTED_ACCOMMODATION("Adapted accommodation (ground 13)"),
    HOUSING_ASSOCIATION_SPECIAL_CIRCUMSTANCES("Housing association special circumstances accommodation (ground 14)"),
    SPECIAL_NEEDS_ACCOMMODATION("Special needs accommodation (ground 15)"),
    UNDER_OCCUPYING_AFTER_SUCCESSION("Under occupying after succession (ground 15A)");

    private final String label;

    public static SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm fromLabel(String label) {
        return Arrays.stream(values())
            .filter(g -> g.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No enum constant with label: " + label));
    }
}
