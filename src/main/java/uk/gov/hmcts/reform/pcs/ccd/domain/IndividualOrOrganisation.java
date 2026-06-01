package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * Indicates whether a party is an individual (a person) or an organisation,
 * so their name can be captured and stored in the appropriate fields.
 */
public enum IndividualOrOrganisation implements HasLabel {

    INDIVIDUAL("Individual"),
    ORGANISATION("Organisation");

    private final String label;

    IndividualOrOrganisation(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

}
