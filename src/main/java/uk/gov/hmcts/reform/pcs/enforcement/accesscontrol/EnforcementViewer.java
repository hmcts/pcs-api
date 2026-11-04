package uk.gov.hmcts.reform.pcs.enforcement.accesscontrol;

public enum EnforcementViewer {

    INTERNAL,
    CLAIMANT,
    RESTRICTED;

    public boolean isRestricted() {
        return this == RESTRICTED;
    }
}
