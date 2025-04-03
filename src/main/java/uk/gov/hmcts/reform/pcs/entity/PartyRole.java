package uk.gov.hmcts.reform.pcs.entity;

public enum PartyRole {

    CLAIMANT("Claimant"),
    DEFENDANT("Defendant"),
    INTERESTED_PARTY("Interested Party");

    private final String displayValue;

    PartyRole(String displayValue) {
        this.displayValue = displayValue;
    }

    @Override
    public String toString() {
        return displayValue;
    }

}
