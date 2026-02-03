package uk.gov.hmcts.reform.pcs.ccd.entity;

public enum ClaimGroundCategory implements HasRank {

    ASSURED_MANDATORY,
    ASSURED_DISCRETIONARY,
    SECURE_OR_FLEXIBLE_DISCRETIONARY,
    SECURE_OR_FLEXIBLE_MANDATORY,
    SECURE_OR_FLEXIBLE_MANDATORY_ALT,
    SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT,
    INTRODUCTORY_DEMOTED_OTHER,
    INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}
