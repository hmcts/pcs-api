package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum NoRentArrearsMandatoryGrounds implements HasLabel {

    OWNER_OCCUPIER("Owner occupier (ground 1)"),
    REPOSSESSION_BY_LENDER("Repossession by the landlord's mortgage lender (ground 2)"),
    HOLIDAY_LET("Holiday let (ground 3)"),
    STUDENT_LET("Student let (ground 4)"),
    MINISTER_OF_RELIGION("Property required for minister of religion (ground 5)"),
    REDEVELOPMENT("Property required for redevelopment (ground 6)"),
    DEATH_OF_TENANT("Death of the tenant (ground 7)"),
    ANTISOCIAL_BEHAVIOUR("Antisocial behaviour (ground 7A)"),
    NO_RIGHT_TO_RENT("Tenant does not have a right to rent (ground 7B)"),
    SERIOUS_RENT_ARREARS("Serious rent arrears (ground 8)");

    private final String label;

    public static NoRentArrearsMandatoryGrounds fromLabel(String label) {
        return Arrays.stream(values())
            .filter(g -> g.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No enum constant with label: " + label));
    }
}

