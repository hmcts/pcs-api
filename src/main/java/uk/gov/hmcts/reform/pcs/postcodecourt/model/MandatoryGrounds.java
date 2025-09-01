package uk.gov.hmcts.reform.pcs.postcodecourt.model;

import com.fasterxml.jackson.annotation.JsonValue;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.Arrays;

public enum MandatoryGrounds implements HasLabel {

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

    MandatoryGrounds(String label) {
        this.label = label;
    }

    @Override
    @JsonValue
    public String getLabel() {
        return label;
    }

    public static MandatoryGrounds fromLabel(String label) {

        if (label == null) {
            return null;
        }

        return Arrays.stream(values())
            .filter(value -> value.getLabel().equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No value found with label: " + label));
    }
}
