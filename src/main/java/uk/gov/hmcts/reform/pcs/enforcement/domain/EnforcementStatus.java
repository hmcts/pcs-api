package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

import java.util.EnumSet;
import java.util.Set;

/**
 * The lifecycle status of a single warrant or writ. It is held on the enforcement record, not on the
 * CCD case, so enforcement never changes the case state.
 */
@AllArgsConstructor
@Getter
public enum EnforcementStatus implements HasLabel {

    AWAITING_PAYMENT("Awaiting payment"),
    AWAITING_REVIEW("Awaiting review"),
    REJECTED("Rejected"),
    AWAITING_BAILIFF_ALLOCATION("Awaiting bailiff allocation"),
    AWAITING_SCHEDULING("Awaiting scheduling"),
    AWAITING_CLAIMANT_CONFIRMATION("Awaiting claimant confirmation"),
    CLAIMANT_CONFIRMED("Claimant confirmed"),
    CLAIMANT_UNABLE_TO_ATTEND("Claimant unable to attend"),
    AWAITING_PROPERTY_VISIT("Awaiting property visit"),
    BREATHING_SPACE("Breathing space"),
    /**
     * An approved writ goes to a High Court Enforcement Officer, not to county court bailiffs, so
     * PCS takes no further part in it.
     */
    TRANSFERRED_TO_HIGH_COURT("Transferred to the High Court"),
    CLOSED("Closed");

    private static final Set<EnforcementStatus> NOT_LIVE = EnumSet.of(CLOSED, REJECTED, TRANSFERRED_TO_HIGH_COURT);

    /**
     * Before a caseworker approves the application, or when they reject it, no warrant or writ is generated.
     */
    private static final Set<EnforcementStatus> NOT_GENERATED = EnumSet.of(AWAITING_PAYMENT, AWAITING_REVIEW, REJECTED);

    private final String label;

    /**
     * A claim can have at most one live enforcement.
     */
    public boolean isLive() {
        return !NOT_LIVE.contains(this);
    }

    /**
     * Whether the warrant or writ has been generated, which happens when a caseworker approves the application.
     */
    public boolean isGenerated() {
        return !NOT_GENERATED.contains(this);
    }

    public static Set<EnforcementStatus> notLive() {
        return EnumSet.copyOf(NOT_LIVE);
    }
}
